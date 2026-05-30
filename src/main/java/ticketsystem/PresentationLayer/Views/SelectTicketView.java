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
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.EventTicketSelectionDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.MapElementDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.MapPositionDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatStatusDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatingAreaDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.StandingAreaDto;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


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
        String routeEventId = event.getRouteParameters().get("eventId").orElse(null);
        this.eventId = parseEventId(routeEventId);

        if (this.eventId == null) {
            Notifications.error("לא ניתן לטעון אירוע לא תקין");
            setEventData(null);
            return;
        }

        loadTicketSelectionEventData();
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

    private void loadTicketSelectionEventData() {
        String token = UiSession.getCurrentToken();

        try {
            EventTicketSelectionDto data = reservationPresenter.loadTicketSelectionEvent(token, eventId);
            setEventData(data);

        } catch (PresentationException e) {
            Notifications.error(e.getMessage());
            setEventData(null);

        } catch (Exception e) {
            Notifications.error("לא ניתן לטעון את מפת האירוע. יש לנסות שוב");
            setEventData(null);
        }
    }

    private void reloadTicketSelectionEventDataKeepingSelection() {
        String token = UiSession.getCurrentToken();

        try {
            this.eventData = reservationPresenter.loadTicketSelectionEvent(token, eventId);
            standingQuantityFields.clear();
            renderMap();
            refreshSummary();

        } catch (PresentationException e) {
            Notifications.error(e.getMessage());

        } catch (Exception e) {
            Notifications.error("לא ניתן לרענן את מפת האירוע. יש לנסות שוב");
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
        continueButton.addClickListener(event -> handleContinue());

        totalBox.add(totalText, totalPrice, continueButton);

        summary.add(header, selectedTicketsList, emptySelection, totalBox);
        return summary;
    }

    private void handleContinue() {
        if (eventId == null) {
            Notifications.error("לא ניתן לבצע הזמנה עבור אירוע לא תקין");
            return;
        }

        if (selectedSeats.isEmpty() && selectedStandingAreas.isEmpty()) {
            Notifications.error("יש לבחור לפחות כרטיס אחד לפני מעבר לתשלום");
            return;
        }

        UI.getCurrent().navigate(UiRoutes.CHECKOUT.replace(":eventId", String.valueOf(eventId)));
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
        if (eventId == null) {
            Notifications.error("לא ניתן לבצע הזמנה עבור אירוע לא תקין");
            return;
        }

        SeatKey key = new SeatKey(area.id(), seat.row(), seat.number());
        String token = UiSession.getCurrentToken();

        try {
            if (selectedSeats.containsKey(key)) {
                reservationPresenter.removeSeatTicketFromActiveOrder(
                        token,
                        eventId,
                        area.id(),
                        seat.row(),
                        seat.number()
                );

                selectedSeats.remove(key);

            } else if (seat.status() == SeatStatusDto.AVAILABLE) {
                reservationPresenter.selectSeatTicket(
                        token,
                        eventId,
                        area.id(),
                        seat.row(),
                        seat.number(),
                        null
                );

                selectedSeats.put(
                        key,
                        new SelectedSeat(area.id(), area.name(), seat.row(), seat.number(), area.ticketPrice())
                );
            }

            renderMap();
            refreshSummary();

        } catch (PresentationException e) {
            Notifications.error(e.getMessage());
            reloadTicketSelectionEventDataKeepingSelection();

        } catch (Exception e) {
            Notifications.error("לא ניתן לעדכן את בחירת המושב. יש לנסות שוב");
            reloadTicketSelectionEventDataKeepingSelection();
        }
    }

    private void updateStandingSelection(StandingAreaDto area, int quantity) {
        if (eventId == null) {
            Notifications.error("לא ניתן לבצע הזמנה עבור אירוע לא תקין");
            return;
        }

        int safeQuantity = Math.max(0, Math.min(quantity, area.availableCapacity()));
        int currentQuantity = selectedStandingAreas
                .getOrDefault(area.id(), SelectedStandingArea.empty(area))
                .quantity();

        int delta = safeQuantity - currentQuantity;
        String token = UiSession.getCurrentToken();
        try {
            if (delta > 0) {
                reservationPresenter.selectStandingTicket(
                        token,
                        eventId,
                        area.id(),
                        delta,
                        null
                );
            } else if (delta < 0) {
                reservationPresenter.removeStandingTicketsFromActiveOrder(
                        token,
                        eventId,
                        area.id(),
                        -delta
                );
            }

            if (safeQuantity == 0) {
                selectedStandingAreas.remove(area.id());
            } else {
                selectedStandingAreas.put(
                        area.id(),
                        new SelectedStandingArea(area.id(), area.name(), safeQuantity, area.ticketPrice())
                );
            }

            IntegerField field = standingQuantityFields.get(area.id());
            if (field != null && !Integer.valueOf(safeQuantity).equals(field.getValue())) {
                field.setValue(safeQuantity);
            }

            refreshSummary();

        } catch (PresentationException e) {
            Notifications.error(e.getMessage());

            IntegerField field = standingQuantityFields.get(area.id());
            if (field != null && !Integer.valueOf(currentQuantity).equals(field.getValue())) {
                field.setValue(currentQuantity);
            }
            reloadTicketSelectionEventDataKeepingSelection();

        } catch (Exception e) {
        Notifications.error("לא ניתן לעדכן את כמות כרטיסי העמידה. יש לנסות שוב");

        IntegerField field = standingQuantityFields.get(area.id());
        if (field != null && !Integer.valueOf(currentQuantity).equals(field.getValue())) {
            field.setValue(currentQuantity);
        }

        reloadTicketSelectionEventDataKeepingSelection();
        }
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
            try {
                String token = UiSession.getCurrentToken();
                reservationPresenter.removeSeatTicketFromActiveOrder(
                        token,
                        eventId,
                        selectedSeat.areaId(),
                        selectedSeat.row(),
                        selectedSeat.number()
                );

                selectedSeats.remove(new SeatKey(selectedSeat.areaId(), selectedSeat.row(), selectedSeat.number()));
                renderMap();
                refreshSummary();

            } catch (PresentationException e) {
                Notifications.error(e.getMessage());
                reloadTicketSelectionEventDataKeepingSelection();

            } catch (Exception e) {
                Notifications.error("לא ניתן להסיר את המושב מההזמנה. יש לנסות שוב");
                reloadTicketSelectionEventDataKeepingSelection();
            }
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
            try {
                String token = UiSession.getCurrentToken();
                reservationPresenter.removeStandingTicketsFromActiveOrder(
                        token,
                        eventId,
                        selectedArea.areaId(),
                        selectedArea.quantity()
                );

                selectedStandingAreas.remove(selectedArea.areaId());

                IntegerField field = standingQuantityFields.get(selectedArea.areaId());
                if (field != null) {
                    field.setValue(0);
                }

                refreshSummary();

            } catch (PresentationException e) {
                Notifications.error(e.getMessage());
                reloadTicketSelectionEventDataKeepingSelection();

            } catch (Exception e) {
                Notifications.error("לא ניתן להסיר את כרטיסי העמידה מההזמנה. יש לנסות שוב");
                reloadTicketSelectionEventDataKeepingSelection();
            }
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


    private record SeatKey(Long areaId, int row, int number) {
    }

    private record SelectedSeat(Long areaId, String areaName, int row, int number, BigDecimal price) {
    }

    private record SelectedStandingArea(Long areaId, String areaName, int quantity, BigDecimal price) {
        static SelectedStandingArea empty(StandingAreaDto area) {
            return new SelectedStandingArea(area.id(), area.name(), 0, area.ticketPrice());
        }
    }

}
