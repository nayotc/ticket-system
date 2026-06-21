package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatDTO;
import ticketsystem.DTO.Event.SeatPositionDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Components.ReservationTimer;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.BookingLayout;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Presenters.ReservationPresenter;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.router.BeforeLeaveObserver;

@PageTitle("TixNow | Ticket Selection")
@Route(value = UiRoutes.TICKET_SELECTION, layout = BookingLayout.class)
public class SelectTicketView extends Div implements BeforeEnterObserver, BeforeLeaveObserver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int BASE_MAP_CELL_SIZE = 36;
    private static final int MIN_ZOOM = 60;
    private static final int MAX_ZOOM = 160;
    private static final int ZOOM_STEP = 10;

    private final Map<SeatKey, SelectedSeat> selectedSeats = new LinkedHashMap<>();
    private final Map<Long, SelectedStandingArea> selectedStandingAreas = new LinkedHashMap<>();
    private final Map<Long, IntegerField> standingQuantityFields = new HashMap<>();

    private final Div mapCanvas = new Div();
    private final Div selectedTicketsList = new Div();
    private final Div emptySelection = new Div();
    private final Span totalTickets = new Span("0 כרטיסים");
    private final Span totalPrice = new Span("₪0");
    private final Button continueButton = new Button("המשך לסיכום הזמנה");
    private final Span zoomValue = new Span("100%");
    private final ReservationTimer reservationTimer = new ReservationTimer();
    private final Span selectionAccessTimer = new Span();
    private Registration selectionAccessPollRegistration;
    private boolean allowLeavingSelectionPage = false;

    private final ReservationPresenter reservationPresenter;
    private final UiVisitCoordinator visitCoordinator;
    private EventDTO eventDTO;
    private EventMapDTO mapDTO;
    private Long eventId;
    private int zoomPercent = 100;
    private int cellSize = BASE_MAP_CELL_SIZE;

    @Autowired
    public SelectTicketView(ReservationPresenter reservationPresenter, UiVisitCoordinator visitCoordinator) {
        this.reservationPresenter = reservationPresenter;
        this.visitCoordinator = visitCoordinator;

        addClassName("ticket-selection-page");
        setSizeFull();

        Div shell = new Div();
        shell.addClassName("ticket-selection-shell");

        Div mapSection = new Div();
        mapSection.addClassName("ticket-map-section");
        selectionAccessTimer.addClassName("selection-access-timer");
        selectionAccessTimer.setId("selection-access-timer");
        selectionAccessTimer.setText("זמן לבחירת כרטיסים: --:--");
        mapSection.add(selectionAccessTimer, createMapToolbar(), createFloatingZoomControls(), mapCanvas);

        Div summarySection = createSummarySection();

        shell.add(reservationTimer, mapSection, summarySection);
        add(shell);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String routeEventId = event.getRouteParameters().get("eventId").orElse(null);
        this.eventId = parseEventId(routeEventId);

        if (this.eventId == null) {
            Notifications.error("לא ניתן לטעון אירוע לא תקין");
            setEventData(null, null);
            return;
        }

        visitCoordinator.ensureVisitAndNotifications(UI.getCurrent());
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
        String token = currentToken();

        try {
            EventDTO loadedEvent = reservationPresenter.loadEvent(token, eventId);
            EventMapDTO loadedMap = reservationPresenter.loadEventMap(token, eventId);
            setEventData(loadedEvent, loadedMap);

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            Notifications.error(e.getMessage());
            setEventData(null, null);

        } catch (Exception e) {
            Notifications.error("לא ניתן לטעון את מפת האירוע. יש לנסות שוב");
            setEventData(null, null);
        }
    }

    private void reloadTicketSelectionEventDataKeepingSelection() {
        String token = currentToken();

        try {
            this.eventDTO = reservationPresenter.loadEvent(token, eventId);
            this.mapDTO = reservationPresenter.loadEventMap(token, eventId);

            selectedSeats.clear();
            selectedStandingAreas.clear();
            standingQuantityFields.clear();

            syncSelectedSeatsFromActiveOrder();
            syncSelectedStandingFromActiveOrder();

            renderMap();
            refreshSummary();
            refreshReservationTimer();

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            Notifications.error(e.getMessage());

        } catch (Exception e) {
            Notifications.error("לא ניתן לרענן את מפת האירוע. יש לנסות שוב");
        }
    }

    public void setEventData(EventDTO eventDTO, EventMapDTO mapDTO) {
        this.eventDTO = eventDTO;
        this.mapDTO = mapDTO;

        selectedSeats.clear();
        selectedStandingAreas.clear();
        standingQuantityFields.clear();
        syncSelectedSeatsFromActiveOrder();
        syncSelectedStandingFromActiveOrder();
        renderMap();
        refreshSummary();
        refreshReservationTimer();
        refreshSelectionAccessTimer();
    }

    private void syncSelectedSeatsFromActiveOrder() {
        try {
            ActiveOrderDTO order = loadCurrentEventActiveOrder();

            if (order == null || order.getTickets() == null || mapDTO == null || mapDTO.elements() == null) {
                return;
            }

            for (TicketDTO ticket : order.getTickets()) {
                if (ticket.getEventId() == null || !ticket.getEventId().equals(eventId)) {
                    continue;
                }

                for (IMapElementDTO element : mapDTO.elements()) {
                    if (!(element instanceof SeatingAreaDTO area)) {
                        continue;
                    }
                    if (ticket.getAreaId() == null || !ticket.getAreaId().equals(area.id())) {
                        continue;
                    }

                    SeatDTO seat = findSeat(area, ticket.getRow(), ticket.getChair());

                    if (seatRow(seat) == ticket.getRow() && seatNumber(seat) == ticket.getChair()) {
                        SeatKey key = new SeatKey(area.id(), ticket.getRow(), ticket.getChair());

                        selectedSeats.put(
                                key,
                                new SelectedSeat(
                                        area.id(),
                                        safeText(area.name(), "אזור ישיבה"),
                                        ticket.getRow(),
                                        ticket.getChair(),
                                        ticket.getPrice() == null ? ticketPrice() : ticket.getPrice()));

                        break;
                    }
                }
            }
        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
        } catch (Exception ignored) {
        }
    }

    private Div createMapToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("ticket-map-toolbar");

        Div legend = new Div();
        legend.addClassName("ticket-map-legend");
        legend.add(
                legendItem("תפוס", "legend-seat-occupied"),
                legendItem("פנוי", "legend-seat-available"),
                legendItem("נבחר", "legend-seat-selected"));

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

    private Div createFloatingZoomControls() {
        Div zoom = new Div();
        zoom.addClassName("hall-floating-zoom-controls");
        zoom.addClassName("ticket-floating-zoom-controls");

        Button zoomIn = createZoomButton("+", this::zoomIn);
        Button zoomOut = createZoomButton("-", this::zoomOut);

        zoomValue.addClassName("hall-floating-zoom-value");
        zoomValue.addClassName("ticket-floating-zoom-value");

        zoom.add(zoomIn, zoomValue, zoomOut);
        return zoom;
    }

    private Button createZoomButton(String symbol, Runnable action) {
        Button button = new Button();
        button.addClassName("hall-floating-zoom-button");
        button.addClassName("ticket-floating-zoom-button");

        Div icon = new Div();
        icon.addClassName("hall-zoom-composite-icon");
        icon.addClassName("ticket-zoom-composite-icon");

        Span sign = new Span(symbol);
        sign.addClassName("hall-zoom-sign");
        sign.addClassName("ticket-zoom-sign");

        icon.add(VaadinIcon.SEARCH.create(), sign);
        button.setIcon(icon);
        button.getElement().setAttribute(
                "aria-label",
                symbol.equals("+") ? "הגדלת תצוגה" : "הקטנת תצוגה");
        button.addClickListener(event -> action.run());

        return button;
    }

    private void zoomIn() {
        zoomPercent = Math.min(MAX_ZOOM, zoomPercent + ZOOM_STEP);
        refreshZoom();
    }

    private void zoomOut() {
        zoomPercent = Math.max(MIN_ZOOM, zoomPercent - ZOOM_STEP);
        refreshZoom();
    }

    private void refreshZoom() {
        zoomValue.setText(zoomPercent + "%");
        renderMap();
    }

    private void setCellSize() {
        this.cellSize = Math.max(18, (int) Math.round(BASE_MAP_CELL_SIZE * (zoomPercent / 100.0)));
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

        reservationPresenter.releaseQueueAccess(currentToken(), eventId);

        allowLeavingSelectionPage = true;

        UI.getCurrent().navigate(
                UiRoutes.CHECKOUT.replace(":eventId", String.valueOf(eventId)));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (allowLeavingSelectionPage || eventId == null) {
            return;
        }

        BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("עזיבת בחירת הכרטיסים");
        dialog.setText("עזיבה של העמוד עלולה להחזיר אותך לסוף התור. האם להמשיך?");
        dialog.setConfirmText("כן, לצאת");
        dialog.setCancelText("להישאר");
        dialog.setCancelable(true);

        dialog.addConfirmListener(e -> {
            try {
                reservationPresenter.releaseQueueAccess(currentToken(), eventId);
            } catch (PresentationException ex) {
                Notification.show(ex.getMessage());
            }

            allowLeavingSelectionPage = true;
            action.proceed();
        });

        dialog.addCancelListener(e -> {
            allowLeavingSelectionPage = false;
        });

        dialog.open();
    }

    private void renderMap() {
        mapCanvas.removeAll();
        mapCanvas.addClassName("ticket-map-canvas");

        if (eventDTO == null || mapDTO == null || mapDTO.elements() == null) {
            mapCanvas.add(createEmptyMapMessage());
            return;
        }

        setCellSize();

        Div infoCard = createEventInfoCard();

        Div viewport = new Div();
        viewport.addClassName("ticket-map-viewport");
        viewport.getElement().setAttribute("dir", "ltr");

        Div surface = new Div();
        surface.addClassName("ticket-map-surface");
        surface.getElement().setAttribute("dir", "ltr");
        surface.getStyle().set("--ticket-cell-size", cellSize + "px");
        surface.getStyle().set("width", mapColumns() * cellSize + "px");
        surface.getStyle().set("height", mapRows() * cellSize + "px");

        for (IMapElementDTO element : mapDTO.elements()) {
            if (element == null) {
                continue;
            }

            Div elementComponent = createElementComponent(element);
            if (elementComponent == null) {
                continue;
            }

            positionOnMap(elementComponent, locationOf(element), sizeOf(element));
            surface.add(elementComponent);
        }

        viewport.add(surface);
        mapCanvas.add(infoCard, viewport);
    }

    private Div createElementComponent(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO seatingArea) {
            return createSeatingAreaElement(seatingArea);
        }

        if (element instanceof StandingAreaDTO standingArea) {
            return createStandingAreaElement(standingArea);
        }

        if (element instanceof ElementDTO plainElement) {
            if (isStage(plainElement)) {
                return createStageElement(plainElement);
            }

            return createPlainElement(
                    plainElement,
                    classNameForPlainElement(plainElement),
                    iconForPlainElement(plainElement),
                    safeText(plainElement.name(), "אלמנט"));
        }

        return null;
    }

    private Div createEventInfoCard() {
        Div card = new Div();
        card.addClassName("ticket-event-info-card");

        H2 name = new H2(safeText(eventDTO.name(), "אירוע"));
        name.addClassName("ticket-event-name");

        Paragraph details = new Paragraph(formatDate(eventDTO.date()) + " • " + formatLocation(eventDTO.location()));
        details.addClassName("ticket-event-details");

        card.add(name, details);
        return card;
    }

    private Div createStageElement(ElementDTO element) {
        Div stage = new Div();
        stage.addClassName("map-element-stage");
        stage.addClassName("ticket-map-stage");

        Span name = new Span(safeText(element.name(), "במה"));
        name.addClassName("map-element-name");

        stage.add(name);
        return stage;
    }

    private Div createPlainElement(ElementDTO element, String className, String iconText, String text) {
        Div div = new Div();
        div.addClassName("map-element");
        div.addClassName(className);

        if (iconText != null && !iconText.isBlank()) {
            Span icon = new Span(iconText);
            icon.addClassName("map-element-icon");
            div.add(icon);
        }

        Span name = new Span(text);
        name.addClassName("map-element-name");
        div.add(name);

        return div;
    }

    private Div createSeatingAreaElement(SeatingAreaDTO area) {
        Div areaCard = new Div();
        areaCard.addClassName("map-area-card");
        areaCard.addClassName("map-seating-area");

        Div header = new Div();
        header.addClassName("map-area-header");
        header.add(new Span(safeText(area.name(), "אזור ישיבה")), new Span(formatMoney(ticketPrice())));

        Div seatsGrid = new Div();
        seatsGrid.addClassName("seat-grid");
        seatsGrid.getStyle().set("grid-template-columns",
                "repeat(" + Math.max(area.columns(), 1) + ", minmax(0, 1fr))");
        seatsGrid.getStyle().set("grid-template-rows", "repeat(" + Math.max(area.rows(), 1) + ", minmax(0, 1fr))");

        for (int row = 1; row <= area.rows(); row++) {
            for (int number = 1; number <= area.columns(); number++) {
                SeatDTO seat = findSeat(area, row, number);
                seatsGrid.add(createSeat(area, seat));
            }
        }

        areaCard.add(header, seatsGrid);
        return areaCard;
    }

    private SeatDTO findSeat(SeatingAreaDTO area, int row, int number) {
        if (area.seats() == null) {
            return soldSeat(row, number);
        }

        return area.seats()
                .stream()
                .filter(seat -> seat != null && seat.position() != null)
                .filter(seat -> seat.position().row() == row && seat.position().number() == number)
                .findFirst()
                .orElseGet(() -> soldSeat(row, number));
    }

    private SeatDTO soldSeat(int row, int number) {
        return new SeatDTO(new SeatPositionDTO(row, number), "SOLD");
    }

    private Div createSeat(SeatingAreaDTO area, SeatDTO seat) {
        int row = seatRow(seat);
        int number = seatNumber(seat);

        SeatKey key = new SeatKey(area.id(), row, number);
        boolean selected = selectedSeats.containsKey(key);
        boolean available = isSeatAvailable(seat);

        Div seatBox = new Div();
        seatBox.addClassName("map-seat");
        seatBox.getElement().setAttribute("title",
                safeText(area.name(), "אזור ישיבה") + " שורה " + row + " מושב " + number);

        if (selected) {
            seatBox.addClassName("seat-selected");
            seatBox.add(new Span("✓"));
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

    private Div createStandingAreaElement(StandingAreaDTO area) {
        Div areaCard = new Div();
        areaCard.addClassName("map-area-card");
        areaCard.addClassName("map-standing-area");

        int selectedQuantity = selectedStandingAreas
                .getOrDefault(area.id(), SelectedStandingArea.empty(area, ticketPrice()))
                .quantity();

        int available = availableCapacity(area);
        int maxSelectable = available + selectedQuantity;

        Div content = new Div();
        content.addClassName("standing-area-content");

        Div icon = new Div();
        icon.addClassName("standing-area-icon");
        icon.add(new Span("👥"));

        Div header = new Div();
        header.addClassName("map-area-header");
        header.add(
                new Span(safeText(area.name(), "אזור עמידה")),
                new Span(formatMoney(ticketPrice())));

        IntegerField quantity = new IntegerField();
        quantity.addClassName("standing-quantity-field");
        quantity.setLabel("כמות כרטיסים");
        quantity.setMin(0);
        quantity.setMax(Math.max(maxSelectable, 0));
        quantity.setStepButtonsVisible(true);
        quantity.setValue(selectedQuantity);

        quantity.addValueChangeListener(event -> {
            int value = event.getValue() == null ? 0 : event.getValue();
            updateStandingSelection(area, value);
        });

        standingQuantityFields.put(area.id(), quantity);

        content.add(icon, header, quantity);
        areaCard.add(content);

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

    private void positionOnMap(Div component, PairDTO<Integer, Integer> location, PairDTO<Integer, Integer> size) {
        int x = clamp(location.first(), 1, mapColumns());
        int y = clamp(location.second(), 1, mapRows());
        int width = clamp(size.first(), 1, Math.max(1, mapColumns() - x + 1));
        int height = clamp(size.second(), 1, Math.max(1, mapRows() - y + 1));

        component.addClassName("ticket-map-element-positioned");
        component.getStyle().set("left", (x - 1) * cellSize + "px");
        component.getStyle().set("top", (y - 1) * cellSize + "px");
        component.getStyle().set("width", width * cellSize + "px");
        component.getStyle().set("height", height * cellSize + "px");
    }

    private void toggleSeat(SeatingAreaDTO area, SeatDTO seat) {
        if (eventId == null) {
            Notifications.error("לא ניתן לבצע הזמנה עבור אירוע לא תקין");
            return;
        }

        int row = seatRow(seat);
        int number = seatNumber(seat);
        SeatKey key = new SeatKey(area.id(), row, number);
        String token = currentToken();

        try {
            if (selectedSeats.containsKey(key)) {
                removeSeatFromOrderByPosition(area.id(), row, number);
                selectedSeats.remove(key);

                reloadTicketSelectionEventDataKeepingSelection();
                return;

            } else if (isSeatAvailable(seat)) {
                reservationPresenter.selectSeatTicket(token, eventId, area.id(), row, number, currentLotteryCode());
                refreshReservationTimer();

                reloadTicketSelectionEventDataKeepingSelection();
                return;
            }

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            Notifications.error(e.getMessage());
            reloadTicketSelectionEventDataKeepingSelection();

        } catch (Exception e) {
            Notifications.error("לא ניתן לעדכן את בחירת המושב. יש לנסות שוב");
            reloadTicketSelectionEventDataKeepingSelection();
        }
    }

    private void removeSeatFromOrderByPosition(Long areaId, int row, int chair) {
        try {
            ActiveOrderDTO order = reservationPresenter.loadActiveOrder(currentToken());

            if (order == null || order.getTickets() == null) {
                return;
            }

            for (TicketDTO ticket : order.getTickets()) {
                if (ticket.getEventId() == null || !ticket.getEventId().equals(eventId)) {
                    continue;
                }
                if (ticket.getAreaId() == null || !ticket.getAreaId().equals(areaId)) {
                    continue;
                }
                if (ticket.getRow() == row && ticket.getChair() == chair) {

                    reservationPresenter.removeTicketFromActiveOrder(
                            currentToken(),
                            eventId,
                            ticket.getTicketId());

                    return;
                }
            }
        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            Notifications.error(e.getMessage());
        } catch (Exception e) {
            Notifications.error("לא ניתן להסיר את המושב מההזמנה. יש לנסות שוב");
        }
    }

    private void updateStandingSelection(StandingAreaDTO area, int quantity) {
        if (eventId == null) {
            Notifications.error("לא ניתן לבצע הזמנה עבור אירוע לא תקין");
            return;
        }

        int currentQuantity = selectedStandingAreas
                .getOrDefault(area.id(), SelectedStandingArea.empty(area, ticketPrice()))
                .quantity();

        int maxSelectable = availableCapacity(area) + currentQuantity;
        int safeQuantity = Math.max(0, Math.min(quantity, maxSelectable));
        int delta = safeQuantity - currentQuantity;

        String token = currentToken();

        try {
            if (delta > 0) {
                reservationPresenter.selectStandingTicket(token, eventId, area.id(), delta, currentLotteryCode());
                refreshReservationTimer();
            } else if (delta < 0) {
                reservationPresenter.removeStandingTicketsFromActiveOrder(token, eventId, area.id(), -delta);
            }

            if (safeQuantity == 0) {
                selectedStandingAreas.remove(area.id());
            } else {
                selectedStandingAreas.put(area.id(), new SelectedStandingArea(area.id(),
                        safeText(area.name(), "אזור עמידה"), safeQuantity, ticketPrice()));
            }

            IntegerField field = standingQuantityFields.get(area.id());
            if (field != null && !Integer.valueOf(safeQuantity).equals(field.getValue())) {
                field.setValue(safeQuantity);
            }

            refreshSummary();

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            Notifications.error(e.getMessage());
            restoreStandingQuantity(area.id(), currentQuantity);
            reloadTicketSelectionEventDataKeepingSelection();

        } catch (Exception e) {
            Notifications.error("לא ניתן לעדכן את כמות כרטיסי העמידה. יש לנסות שוב");
            restoreStandingQuantity(area.id(), currentQuantity);
            reloadTicketSelectionEventDataKeepingSelection();
        }
    }

    private void restoreStandingQuantity(Long areaId, int quantity) {
        IntegerField field = standingQuantityFields.get(areaId);
        if (field != null && !Integer.valueOf(quantity).equals(field.getValue())) {
            field.setValue(quantity);
        }
    }

    private void refreshSummary() {
        selectedTicketsList.removeAll();

        try {
            ActiveOrderDTO order = loadCurrentEventActiveOrder();
            if (order == null || order.getTickets() == null || order.getTickets().isEmpty()) {
                emptySelection.setVisible(true);
                selectedTicketsList.setVisible(false);
                totalTickets.setText("0 כרטיסים");
                totalPrice.setText("₪0");
                continueButton.setEnabled(false);
                return;
            }

            for (TicketDTO ticket : order.getTickets()) {
                selectedTicketsList.add(createSelectedTicketRowFromOrder(ticket));
            }

            int count = order.getTickets().size();

            BigDecimal total = order.getTickets().stream()
                    .map(TicketDTO::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalTickets.setText(count + " כרטיסים");
            totalPrice.setText(formatMoney(total));
            emptySelection.setVisible(false);
            selectedTicketsList.setVisible(true);
            continueButton.setEnabled(true);

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }

        } catch (Exception e) {
            Notifications.error("שגיאה בעדכון סל הכרטיסים.");
        }
    }

    private ActiveOrderDTO loadCurrentEventActiveOrder() {
        ActiveOrderDTO order = reservationPresenter.loadActiveOrder(currentToken());

        if (order == null || order.getEventId() == null || !order.getEventId().equals(eventId)) {
            return null;
        }

        return order;
    }

    private void syncSelectedStandingFromActiveOrder() {
        try {
            ActiveOrderDTO order = loadCurrentEventActiveOrder();

            if (order == null || order.getTickets() == null || mapDTO == null || mapDTO.elements() == null) {
                return;
            }

            for (IMapElementDTO element : mapDTO.elements()) {
                if (!(element instanceof StandingAreaDTO area)) {
                    continue;
                }

                int quantity = 0;
                for (TicketDTO ticket : order.getTickets()) {
                    if (ticket.getRow() == 0 && ticket.getChair() == 0) {
                        quantity++;
                    }
                }

                if (quantity > 0) {
                    selectedStandingAreas.put(
                            area.id(),
                            new SelectedStandingArea(
                                    area.id(),
                                    safeText(area.name(), "אזור עמידה"),
                                    quantity,
                                    ticketPrice()));
                }
                break;
            }
        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
        } catch (Exception e) {
        }
    }

    private Div createSelectedTicketRowFromOrder(TicketDTO ticket) {
        Div row = new Div();
        row.addClassName("selected-ticket-row");

        Div text = new Div();
        text.addClassName("selected-ticket-text");
        String areaName = findAreaNameById(ticket.getAreaId());

        text.add(
                new Span(areaName),
                new Span("שורה " + ticket.getRow() + " • כיסא " + ticket.getChair()));

        Span price = new Span(formatMoney(ticket.getPrice()));
        price.addClassName("selected-ticket-price");

        Button remove = new Button("הסר");
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.addClassName("selected-ticket-remove");

        remove.addClickListener(event -> {
            try {
                reservationPresenter.removeTicketFromActiveOrder(
                        currentToken(),
                        eventId,
                        ticket.getTicketId());

                reloadTicketSelectionEventDataKeepingSelection();
                refreshSummary();

            } catch (PresentationException e) {
                if (e.isSessionTimeout()) {
                    handleSelectionSessionTimeout();
                    return;
                }

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

    private String findAreaNameById(Long areaId) {
        if (areaId == null || mapDTO == null || mapDTO.elements() == null) {
            return "כרטיס";
        }

        for (IMapElementDTO element : mapDTO.elements()) {
            if (element instanceof SeatingAreaDTO area && areaId.equals(area.id())) {
                return safeText(area.name(), "אזור ישיבה");
            }

            if (element instanceof StandingAreaDTO area && areaId.equals(area.id())) {
                return safeText(area.name(), "אזור עמידה");
            }
        }

        return "כרטיס";
    }

    private PairDTO<Integer, Integer> locationOf(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO area) {
            return safePair(area.location(), 1, 1);
        }
        if (element instanceof StandingAreaDTO area) {
            return safePair(area.location(), 1, 1);
        }
        if (element instanceof ElementDTO plainElement) {
            return safePair(plainElement.location(), 1, 1);
        }
        return new PairDTO<>(1, 1);
    }

    private PairDTO<Integer, Integer> sizeOf(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO area) {
            return safePair(area.size(), 1, 1);
        }
        if (element instanceof StandingAreaDTO area) {
            return safePair(area.size(), 1, 1);
        }
        if (element instanceof ElementDTO plainElement) {
            return safePair(plainElement.size(), 1, 1);
        }
        return new PairDTO<>(1, 1);
    }

    private PairDTO<Integer, Integer> safePair(PairDTO<Integer, Integer> pair, int defaultFirst, int defaultSecond) {
        if (pair == null) {
            return new PairDTO<>(defaultFirst, defaultSecond);
        }
        return new PairDTO<>(positive(pair.first(), defaultFirst), positive(pair.second(), defaultSecond));
    }

    private int mapRows() {
        return mapDTO == null ? 1 : positive(mapDTO.size() == null ? null : mapDTO.size().first(), 1);
    }

    private int mapColumns() {
        return mapDTO == null ? 1 : positive(mapDTO.size() == null ? null : mapDTO.size().second(), 1);
    }

    private int availableCapacity(StandingAreaDTO area) {
        long available = Math.max(0L, area.capacity() - area.reserved() - area.sold());
        return available > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) available;
    }

    private int seatRow(SeatDTO seat) {
        return seat == null || seat.position() == null ? 0 : seat.position().row();
    }

    private int seatNumber(SeatDTO seat) {
        return seat == null || seat.position() == null ? 0 : seat.position().number();
    }

    private boolean isSeatAvailable(SeatDTO seat) {
        String status = seat == null || seat.status() == null ? "" : seat.status().trim().toUpperCase();
        return status.equals("AVAILABLE") || status.equals("FREE");
    }

    private boolean isStage(ElementDTO element) {
        return typeOf(element).equals("STAGE");
    }

    private String classNameForPlainElement(ElementDTO element) {
        return switch (typeOf(element)) {
            case "ENTRANCE" -> "map-element-entrance";
            case "EXIT" -> "map-element-exit";
            case "BAR" -> "map-element-bar";
            case "FIRSTAID", "FIRST_AID" -> "map-element-first-aid";
            default -> "map-element-generic";
        };
    }

    private String iconForPlainElement(ElementDTO element) {
        return switch (typeOf(element)) {
            case "ENTRANCE" -> "↳";
            case "EXIT" -> "↲";
            case "BAR" -> "🍸";
            case "FIRSTAID", "FIRST_AID" -> "✚";
            default -> "";
        };
    }

    private String typeOf(ElementDTO element) {
        return element == null || element.type() == null
                ? ""
                : element.type().replace(" ", "_").replace("-", "_").trim().toUpperCase();
    }

    private BigDecimal ticketPrice() {
        return eventDTO == null || eventDTO.ticketPrice() == null ? BigDecimal.ZERO : eventDTO.ticketPrice();
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String currentToken() {
        return UiSession.getCurrentToken();
    }

    private String currentLotteryCode() {
        return UiSession.getLotteryCode(eventId);
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "תאריך לא זמין" : date.format(DATE_FORMATTER);
    }

    private String formatLocation(String location) {
        return location == null || location.isBlank() ? "מיקום לא זמין" : location.replace("_", " ");
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "₪0";
        }
        return "₪" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private void refreshSelectionAccessTimer() {
        long secondsLeft = reservationPresenter.getSelectionAccessSecondsLeft(
                currentToken(), eventId

        );

        selectionAccessTimer.getElement()
                .setAttribute("data-seconds-left", String.valueOf(secondsLeft));

        selectionAccessTimer.setText(formatSeconds(secondsLeft));
    }

    private void handleSelectionSessionTimeout() {
        try {
            if (eventId != null) {
                reservationPresenter.releaseQueueAccess(currentToken(), eventId);
            }
        } catch (Exception ignored) {
            // לא חוסמים redirect בגלל כשל בשחרור התור
        }

        if (UiSession.isLoggedIn()) {
            UiSession.handleTimeoutRedirect();
        } else {
            UiSession.exit();
            Notifications.error("זמן החיבור של בחירת הכרטיסים פג, אנא בחרו כרטיסים מחדש.");
            UI.getCurrent().navigate(UiRoutes.HOME);
        }
    }

    private String formatSeconds(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        long minutesPart = safeSeconds / 60;
        long secondsPart = safeSeconds % 60;

        return String.format("%02d:%02d", minutesPart, secondsPart);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        refreshSelectionAccessTimer();
        startClientSideSelectionTimer();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopClientSideSelectionTimer();
        super.onDetach(detachEvent);
    }

    private record SeatKey(Long areaId, int row, int number) {
    }

    private record SelectedSeat(Long areaId, String areaName, int row, int number, BigDecimal price) {
    }

    private record SelectedStandingArea(Long areaId, String areaName, int quantity, BigDecimal price) {
        static SelectedStandingArea empty(StandingAreaDTO area, BigDecimal price) {
            return new SelectedStandingArea(area.id(), area.name(), 0, price);
        }
    }

    private void startClientSideSelectionTimer() {
        getElement().executeJs("""
                    const root = this;

                    if (root.__selectionTimerInterval) {
                        clearInterval(root.__selectionTimerInterval);
                    }

                    root.__selectionTimerInterval = setInterval(() => {
                        const timer = root.querySelector('#selection-access-timer');

                        if (!timer) {
                            return;
                        }

                        let secondsLeft = Number(timer.dataset.secondsLeft || '0');

                        if (secondsLeft <= 0) {
                            timer.textContent = 'זמן לבחירת כרטיסים: 00:00';
                            clearInterval(root.__selectionTimerInterval);
                            root.$server.onSelectionAccessTimerExpired();
                            return;
                        }

                        secondsLeft--;
                        timer.dataset.secondsLeft = String(secondsLeft);

                        const minutes = Math.floor(secondsLeft / 60);
                        const seconds = secondsLeft % 60;

                        timer.textContent =
                            'זמן לבחירת כרטיסים: ' +
                            String(minutes).padStart(2, '0') + ':' +
                            String(seconds).padStart(2, '0');
                    }, 1000);
                """);
    }

    private void stopClientSideSelectionTimer() {
        getElement().executeJs("""
                    if (this.__selectionTimerInterval) {
                        clearInterval(this.__selectionTimerInterval);
                        this.__selectionTimerInterval = null;
                    }
                """);
    }

    @ClientCallable
    private void onSelectionAccessTimerExpired() {
        boolean expired = reservationPresenter.expireSelectionAccessIfNeeded(
                currentToken(),
                eventId);

        if (expired) {
            allowLeavingSelectionPage = true;
            UI.getCurrent().navigate("waiting-queue/" + eventId);
            return;
        }

        refreshSelectionAccessTimer();
        startClientSideSelectionTimer();
    }

    private void refreshReservationTimer() {
        try {
            ActiveOrderDTO order = reservationPresenter.loadActiveOrder(currentToken());

            if (order == null || order.getTickets() == null || order.getTickets().isEmpty()) {
                reservationTimer.setVisible(false);
                return;
            }

            reservationTimer.setDeadline(order.getExpiresAtEpochMillis());

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                handleSelectionSessionTimeout();
                return;
            }
            reservationTimer.setVisible(false);

        } catch (Exception e) {
            reservationTimer.setVisible(false);
        }
    }
}
