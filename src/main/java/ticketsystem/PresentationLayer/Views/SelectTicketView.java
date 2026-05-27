package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.BookingLayout;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Presenters.ReservationPresenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PageTitle("Ticket Selection")
@Route(value = UiRoutes.TICKET_SELECTION, layout = BookingLayout.class)
public class SelectTicketView extends Div implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Map<SeatKey, SelectedSeat> selectedSeats = new LinkedHashMap<>();
    private final Map<Long, SelectedStandingArea> selectedStandingAreas = new LinkedHashMap<>();
    private final Map<Long, IntegerField> standingQuantityFields = new HashMap<>();

    private final Div mapCanvas = new Div();
    private final Div selectedTicketsList = new Div();
    private final Div emptySelection = new Div();
    private final Span totalTickets = new Span("0 כרטיסים");
    private final Span totalPrice = new Span("₪0");
    private final Button continueButton = new Button("המשך לסיכום הזמנה");

    private EventTicketSelectionDto eventData;
    private final ReservationPresenter reservationPresenter;
    private Long eventId;

    @Autowired
    public SelectTicketView(ReservationPresenter reservationPresenter) {
        this.reservationPresenter = reservationPresenter;

        addClassName("ticket-selection-page");
        setSizeFull();

        Div shell = new Div();
        shell.addClassName("ticket-selection-shell");

        Div mapSection = new Div();
        mapSection.addClassName("ticket-map-section");
        mapSection.add(createMapToolbar(), mapCanvas);

        Div summarySection = createSummarySection();

        shell.add(mapSection, summarySection);
        add(shell);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String routeEventId = event.getRouteParameters().get("eventId").orElse("demo");
        this.eventId = parseEventId(routeEventId);

        // Replace this line later with:
        // setEventData(ticketSelectionPresenter.getEventMapAndAvailability(routeEventId));
        setEventData(createDemoData(routeEventId));
    }

    private Long parseEventId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public void setEventData(EventTicketSelectionDto eventData) {
        this.eventData = eventData;
        selectedSeats.clear();
        selectedStandingAreas.clear();
        standingQuantityFields.clear();
        renderMap();
        refreshSummary();
    }

    private Div createMapToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("ticket-map-toolbar");

        Div legend = new Div();
        legend.addClassName("ticket-map-legend");
        legend.add(
                legendItem("תפוס", "legend-seat-occupied"),
                legendItem("פנוי", "legend-seat-available"),
                legendItem("נבחר", "legend-seat-selected")
        );

        Div hint = new Div();
        hint.addClassName("ticket-map-hint");
        Span hintIcon = new Span("ⓘ");
        hintIcon.addClassName("ticket-map-hint-icon");
        hint.add(hintIcon, new Span("בחר מושבים פנויים או כמות באזורי עמידה"));

        toolbar.add(legend, hint);
        return toolbar;
    }

    private Div legendItem(String text, String markerClass) {
        Div item = new Div();
        item.addClassName("legend-item");

        Span marker = new Span();
        marker.addClassName("legend-seat");
        marker.addClassName(markerClass);

        item.add(marker, new Span(text));
        return item;
    }

    private Div createSummarySection() {
        Div summary = new Div();
        summary.addClassName("ticket-summary-section");

        Div header = new Div();
        header.addClassName("ticket-summary-header");

        H2 title = new H2("סיכום הזמנה");
        title.addClassName("ticket-summary-title");
        header.add(title);

        selectedTicketsList.addClassName("selected-tickets-list");

        emptySelection.addClassName("selected-tickets-empty");
        Span emptyIcon = new Span("🎟");
        emptyIcon.addClassName("selected-tickets-empty-icon");
        emptySelection.add(emptyIcon, new Span("עדיין לא נבחרו כרטיסים"));

        Div totalBox = new Div();
        totalBox.addClassName("ticket-summary-total");

        Div totalText = new Div();
        totalText.addClassName("ticket-summary-total-text");

        Span label = new Span("סה״כ לתשלום");
        label.addClassName("ticket-summary-total-label");

        totalTickets.addClassName("ticket-summary-total-count");
        totalPrice.addClassName("ticket-summary-total-price");

        totalText.add(label, totalTickets);

        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClassName("ticket-summary-continue-button");
//        continueButton.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.CHECKOUT));
        continueButton.addClickListener(event -> handleContinue());

        totalBox.add(totalText, totalPrice, continueButton);

        summary.add(header, selectedTicketsList, emptySelection, totalBox);
        return summary;
    }

    private void handleContinue() {
        UI.getCurrent().navigate(UiRoutes.CHECKOUT);
    }

    private void renderMap() {
        mapCanvas.removeAll();
        mapCanvas.addClassName("ticket-map-canvas");

        if (eventData == null || eventData.map() == null) {
            mapCanvas.add(createEmptyMapMessage());
            return;
        }

        Div mapWrapper = new Div();
        mapWrapper.addClassName("ticket-map-wrapper");
        mapWrapper.getStyle().set("grid-template-columns", "repeat(" + eventData.map().columns() + ", 48px)");
        mapWrapper.getStyle().set("grid-template-rows", "repeat(" + eventData.map().rows() + ", 48px)");

        mapWrapper.add(createEventInfoCard());

        for (MapElementDto element : eventData.map().elements()) {
            Div elementComponent = switch (element.type()) {
                case STAGE -> createStageElement(element);
                case ENTRANCE -> createPlainElement(element, "map-element-entrance", "↳", element.name());
                case EXIT -> createPlainElement(element, "map-element-exit", "↲", element.name());
                case GENERIC -> createPlainElement(element, "map-element-generic", "•", element.name());
                case SEATING_AREA -> createSeatingAreaElement((SeatingAreaDto) element);
                case STANDING_AREA -> createStandingAreaElement((StandingAreaDto) element);
            };

            positionOnMap(elementComponent, element.position());
            mapWrapper.add(elementComponent);
        }

        mapCanvas.add(mapWrapper);
    }

    private Div createEventInfoCard() {
        Div card = new Div();
        card.addClassName("ticket-event-info-card");
        card.getStyle().set("grid-column", "1 / span 6");
        card.getStyle().set("grid-row", "1 / span 2");

        H2 name = new H2(eventData.eventName());
        name.addClassName("ticket-event-name");

        Paragraph details = new Paragraph(formatDate(eventData.date()) + " • " + eventData.location());
        details.addClassName("ticket-event-details");

        card.add(name, details);
        return card;
    }

    private Div createStageElement(MapElementDto element) {
        Div stage = new Div();
        stage.addClassName("map-element-stage");
        stage.add(new Span(element.name() == null || element.name().isBlank() ? "במה" : element.name()));
        return stage;
    }

    private Div createPlainElement(MapElementDto element, String className, String iconText, String text) {
        Div div = new Div();
        div.addClassName("map-element");
        div.addClassName(className);

        Span icon = new Span(iconText);
        icon.addClassName("map-element-icon");

        div.add(icon, new Span(text));
        return div;
    }

    private Div createSeatingAreaElement(SeatingAreaDto area) {
        Div areaCard = new Div();
        areaCard.addClassName("map-area-card");
        areaCard.addClassName("map-seating-area");

        Div header = new Div();
        header.addClassName("map-area-header");
        header.add(new Span(area.name()), new Span(formatMoney(area.ticketPrice())));

        Div seatsGrid = new Div();
        seatsGrid.addClassName("seat-grid");
        seatsGrid.getStyle().set("grid-template-columns", "repeat(" + area.columns() + ", 28px)");

        for (int row = 1; row <= area.rows(); row++) {
            for (int number = 1; number <= area.columns(); number++) {
                SeatDto seat = area.findSeat(row, number).orElse(new SeatDto(row, number, SeatStatusDto.SOLD));
                seatsGrid.add(createSeat(area, seat));
            }
        }

        areaCard.add(header, seatsGrid);
        return areaCard;
    }

    private Div createSeat(SeatingAreaDto area, SeatDto seat) {
        SeatKey key = new SeatKey(area.id(), seat.row(), seat.number());
        boolean selected = selectedSeats.containsKey(key);
        boolean available = seat.status() == SeatStatusDto.AVAILABLE;

        Div seatBox = new Div();
        seatBox.addClassName("map-seat");
        seatBox.getElement().setAttribute("title", area.name() + " שורה " + seat.row() + " מושב " + seat.number());

        if (selected) {
            seatBox.addClassName("seat-selected");
        } else if (available) {
            seatBox.addClassName("seat-available");
        } else {
            seatBox.addClassName("seat-occupied");
        }

        if (available || selected) {
            seatBox.addClickListener(event -> toggleSeat(area, seat));
        }

        return seatBox;
    }

    private Div createStandingAreaElement(StandingAreaDto area) {
        Div areaCard = new Div();
        areaCard.addClassName("map-area-card");
        areaCard.addClassName("map-standing-area");

        Div header = new Div();
        header.addClassName("map-area-header");
        header.add(new Span(area.name()), new Span(formatMoney(area.ticketPrice())));

        Span availability = new Span("נותרו " + area.availableCapacity() + " מתוך " + area.capacity());
        availability.addClassName("standing-availability");

        IntegerField quantity = new IntegerField();
        quantity.addClassName("standing-quantity-field");
        quantity.setLabel("כמות כרטיסים");
        quantity.setMin(0);
        quantity.setMax(area.availableCapacity());
        quantity.setStepButtonsVisible(true);
        quantity.setValue(selectedStandingAreas.getOrDefault(area.id(), SelectedStandingArea.empty(area)).quantity());

        quantity.addValueChangeListener(event -> {
            int value = event.getValue() == null ? 0 : event.getValue();
            updateStandingSelection(area, value);
        });

        standingQuantityFields.put(area.id(), quantity);

        areaCard.add(header, availability, quantity);
        return areaCard;
    }

    private Div createEmptyMapMessage() {
        Div empty = new Div();
        empty.addClassName("ticket-map-empty");
        Span mapIcon = new Span("⌖");
        mapIcon.addClassName("ticket-map-empty-icon");
        empty.add(mapIcon, new Span("לא נמצאה מפה לאירוע"));
        return empty;
    }

    private void positionOnMap(Div component, MapPositionDto position) {
        component.getStyle().set("grid-column", position.column() + " / span " + position.columnSpan());
        component.getStyle().set("grid-row", position.row() + " / span " + position.rowSpan());
    }

    private void toggleSeat(SeatingAreaDto area, SeatDto seat) {
        SeatKey key = new SeatKey(area.id(), seat.row(), seat.number());

        if (selectedSeats.containsKey(key)) {
            selectedSeats.remove(key);
        } else if (seat.status() == SeatStatusDto.AVAILABLE) {
            selectedSeats.put(key, new SelectedSeat(area.id(), area.name(), seat.row(), seat.number(), area.ticketPrice()));
        }

        renderMap();
        refreshSummary();
    }

    private void updateStandingSelection(StandingAreaDto area, int quantity) {
        int safeQuantity = Math.max(0, Math.min(quantity, area.availableCapacity()));

        if (safeQuantity == 0) {
            selectedStandingAreas.remove(area.id());
        } else {
            selectedStandingAreas.put(area.id(), new SelectedStandingArea(area.id(), area.name(), safeQuantity, area.ticketPrice()));
        }

        IntegerField field = standingQuantityFields.get(area.id());
        if (field != null && !Integer.valueOf(safeQuantity).equals(field.getValue())) {
            field.setValue(safeQuantity);
        }

        refreshSummary();
    }

    private void refreshSummary() {
        selectedTicketsList.removeAll();

        for (SelectedSeat seat : selectedSeats.values()) {
            selectedTicketsList.add(createSelectedSeatRow(seat));
        }

        for (SelectedStandingArea standingArea : selectedStandingAreas.values()) {
            selectedTicketsList.add(createSelectedStandingRow(standingArea));
        }

        int count = selectedSeats.size() + selectedStandingAreas.values().stream().mapToInt(SelectedStandingArea::quantity).sum();
        BigDecimal total = selectedSeats.values().stream()
                .map(SelectedSeat::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(selectedStandingAreas.values().stream()
                        .map(area -> area.price().multiply(BigDecimal.valueOf(area.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        totalTickets.setText(count + " כרטיסים");
        totalPrice.setText(formatMoney(total));
        emptySelection.setVisible(count == 0);
        selectedTicketsList.setVisible(count > 0);
        continueButton.setEnabled(count > 0);
    }

    private Div createSelectedSeatRow(SelectedSeat selectedSeat) {
        Div row = new Div();
        row.addClassName("selected-ticket-row");

        Div text = new Div();
        text.addClassName("selected-ticket-text");
        text.add(new Span(selectedSeat.areaName()), new Span("שורה " + selectedSeat.row() + " • מושב " + selectedSeat.number()));

        Span price = new Span(formatMoney(selectedSeat.price()));
        price.addClassName("selected-ticket-price");

        Button remove = new Button("הסר");
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.addClassName("selected-ticket-remove");
        remove.addClickListener(event -> {
            selectedSeats.remove(new SeatKey(selectedSeat.areaId(), selectedSeat.row(), selectedSeat.number()));
            renderMap();
            refreshSummary();
        });

        row.add(text, price, remove);
        return row;
    }

    private Div createSelectedStandingRow(SelectedStandingArea selectedArea) {
        Div row = new Div();
        row.addClassName("selected-ticket-row");

        Div text = new Div();
        text.addClassName("selected-ticket-text");
        text.add(new Span(selectedArea.areaName()), new Span(selectedArea.quantity() + " כרטיסי עמידה"));

        Span price = new Span(formatMoney(selectedArea.price().multiply(BigDecimal.valueOf(selectedArea.quantity()))));
        price.addClassName("selected-ticket-price");

        Button remove = new Button("הסר");
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.addClassName("selected-ticket-remove");
        remove.addClickListener(event -> {
            selectedStandingAreas.remove(selectedArea.areaId());
            IntegerField field = standingQuantityFields.get(selectedArea.areaId());
            if (field != null) {
                field.setValue(0);
            }
            refreshSummary();
        });

        row.add(text, price, remove);
        return row;
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "תאריך לא זמין" : date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "₪0";
        }
        return "₪" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private EventTicketSelectionDto createDemoData(String eventId) {
        List<MapElementDto> elements = new ArrayList<>();
        elements.add(new MapElementDto(1L, "במה", MapElementTypeDto.STAGE, new MapPositionDto(3, 7, 2, 10)));
        elements.add(new MapElementDto(2L, "כניסה ראשית", MapElementTypeDto.ENTRANCE, new MapPositionDto(18, 8, 1, 4)));

        elements.add(new SeatingAreaDto(
                10L,
                "אזור A - VIP",
                new MapPositionDto(6, 5, 5, 14),
                new BigDecimal("350"),
                4,
                12,
                List.of(
                        new SeatDto(1, 1, SeatStatusDto.SOLD),
                        new SeatDto(1, 2, SeatStatusDto.SOLD),
                        new SeatDto(1, 3, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 4, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 5, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 6, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 7, SeatStatusDto.RESERVED),
                        new SeatDto(1, 8, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 9, SeatStatusDto.AVAILABLE),
                        new SeatDto(1, 10, SeatStatusDto.SOLD),
                        new SeatDto(1, 11, SeatStatusDto.SOLD),
                        new SeatDto(1, 12, SeatStatusDto.AVAILABLE)
                )
        ));

        elements.add(new SeatingAreaDto(
                11L,
                "אזור B - אולם",
                new MapPositionDto(12, 3, 5, 18),
                new BigDecimal("220"),
                5,
                16,
                demoSeats(5, 16)
        ));

        elements.add(new StandingAreaDto(
                20L,
                "רחבת עמידה",
                new MapPositionDto(6, 20, 9, 8),
                new BigDecimal("180"),
                300,
                42,
                180
        ));

        return new EventTicketSelectionDto(
                eventId,
                "פסטיבל אורות הלילה",
                LocalDateTime.of(2026, 10, 24, 21, 0),
                "היכל מנורה, תל אביב",
                new EventMapDto(20, 30, elements)
        );
    }

    private List<SeatDto> demoSeats(int rows, int columns) {
        List<SeatDto> seats = new ArrayList<>();

        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= columns; col++) {
                SeatStatusDto status = (row + col) % 6 == 0 ? SeatStatusDto.SOLD : SeatStatusDto.AVAILABLE;
                if ((row * col) % 17 == 0) {
                    status = SeatStatusDto.RESERVED;
                }
                seats.add(new SeatDto(row, col, status));
            }
        }

        return seats;
    }

    private record SeatKey(Long areaId, int row, int number) {
    }

    private record SelectedSeat(Long areaId, String areaName, int row, int number, BigDecimal price) {
    }

    private record SelectedStandingArea(Long areaId, String areaName, int quantity, BigDecimal price) {
        static SelectedStandingArea empty(StandingAreaDto area) {
            return new SelectedStandingArea(area.id(), area.name(), 0, area.ticketPrice());
        }
    }

    public record EventTicketSelectionDto(
            String eventId,
            String eventName,
            LocalDateTime date,
            String location,
            EventMapDto map
    ) {
    }

    public record EventMapDto(
            int rows,
            int columns,
            List<MapElementDto> elements
    ) {
    }

    public record MapPositionDto(
            int row,
            int column,
            int rowSpan,
            int columnSpan
    ) {
    }

    public enum MapElementTypeDto {
        STAGE,
        ENTRANCE,
        EXIT,
        GENERIC,
        SEATING_AREA,
        STANDING_AREA
    }

    public static class MapElementDto {
        private final Long id;
        private final String name;
        private final MapElementTypeDto type;
        private final MapPositionDto position;

        public MapElementDto(Long id, String name, MapElementTypeDto type, MapPositionDto position) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.position = position;
        }

        public Long id() {
            return id;
        }

        public String name() {
            return name;
        }

        public MapElementTypeDto type() {
            return type;
        }

        public MapPositionDto position() {
            return position;
        }
    }

    public static class SeatingAreaDto extends MapElementDto {
        private final BigDecimal ticketPrice;
        private final int rows;
        private final int columns;
        private final List<SeatDto> seats;

        public SeatingAreaDto(Long id, String name, MapPositionDto position, BigDecimal ticketPrice, int rows, int columns, List<SeatDto> seats) {
            super(id, name, MapElementTypeDto.SEATING_AREA, position);
            this.ticketPrice = ticketPrice;
            this.rows = rows;
            this.columns = columns;
            this.seats = seats == null ? List.of() : seats;
        }

        public BigDecimal ticketPrice() {
            return ticketPrice;
        }

        public int rows() {
            return rows;
        }

        public int columns() {
            return columns;
        }

        public List<SeatDto> seats() {
            return seats;
        }

        public Optional<SeatDto> findSeat(int row, int number) {
            return seats.stream()
                    .filter(seat -> seat.row() == row && seat.number() == number)
                    .findFirst();
        }
    }

    public static class StandingAreaDto extends MapElementDto {
        private final BigDecimal ticketPrice;
        private final int capacity;
        private final int reserved;
        private final int sold;

        public StandingAreaDto(Long id, String name, MapPositionDto position, BigDecimal ticketPrice, int capacity, int reserved, int sold) {
            super(id, name, MapElementTypeDto.STANDING_AREA, position);
            this.ticketPrice = ticketPrice;
            this.capacity = capacity;
            this.reserved = reserved;
            this.sold = sold;
        }

        public BigDecimal ticketPrice() {
            return ticketPrice;
        }

        public int capacity() {
            return capacity;
        }

        public int reserved() {
            return reserved;
        }

        public int sold() {
            return sold;
        }

        public int availableCapacity() {
            return Math.max(0, capacity - reserved - sold);
        }
    }

    public record SeatDto(int row, int number, SeatStatusDto status) {
    }

    public enum SeatStatusDto {
        AVAILABLE,
        RESERVED,
        SOLD
    }
}
