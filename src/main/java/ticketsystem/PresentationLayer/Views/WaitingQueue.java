package ticketsystem.PresentationLayer.Views;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Presenters.WaitingQueueSnapshot;
import ticketsystem.PresentationLayer.Presenters.WaitingQueueStatus;
import ticketsystem.PresentationLayer.Session.UiSession;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.shared.Registration;

@Route(UiRoutes.WAITING_QUEUE)
public class WaitingQueue extends VerticalLayout implements BeforeEnterObserver {

    private final WaitingQueuePresenter presenter;

    private long eventId;

    private final H1 title = new H1("אתה בתור לכרטיסים");
    private final Paragraph subtitle = new Paragraph("אל תרענן את הדף. נכניס אותך לבחירת כרטיסים ברגע שהתור שלך יגיע.");
    private final Span eventName = new Span();
    private final Span estimatedWait = new Span();
    private final Span position = new Span();
    private final Span helperText = new Span();
    private final Div progressRing = new Div();
    private final Button enterSelectionButton = new Button("כניסה לבחירת כרטיסים", VaadinIcon.TICKET.create());
    private final Button leaveQueueButton = new Button("יציאה מהתור", VaadinIcon.CLOSE_SMALL.create());
    private Registration pollRegistration;

    /*
     * Later, when a real presenter exists as a Spring bean, annotate this constructor
     * with @Autowired and remove the no-args constructor above.
     */
    public WaitingQueue(WaitingQueuePresenter presenter) {
        this.presenter = presenter;
        buildView();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> parsedEventId = event.getRouteParameters()
                .get("eventId")
                .flatMap(this::parseLongSafely);

        if (parsedEventId.isEmpty()) {
            event.rerouteTo(UiRoutes.EVENTS);
            return;
        }

        eventId = parsedEventId.get();
        loadQueueState();
    }

    private void buildView() {
        getElement().setAttribute("dir", "rtl");
        addClassName("waiting-queue-page");
        setWidthFull();
        setMinHeight("100vh");
        setPadding(false);
        setSpacing(false);

        Div shell = new Div();
        shell.addClassName("waiting-queue-shell");

        Div panel = new Div();
        panel.addClassName("waiting-queue-panel");

        Div glow = new Div();
        glow.addClassName("waiting-queue-glow");

        Span brand = new Span("TixNow");
        brand.addClassName("waiting-queue-brand");

        title.addClassName("waiting-queue-title");
        subtitle.addClassName("waiting-queue-subtitle");

        eventName.addClassName("waiting-queue-event-name");
        helperText.addClassName("waiting-queue-helper-text");

        enterSelectionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        enterSelectionButton.addClassName("waiting-queue-enter-button");
        enterSelectionButton.addClickListener(event -> navigateToTicketSelection());
        enterSelectionButton.setVisible(false);

        leaveQueueButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        leaveQueueButton.addClassName("waiting-queue-leave-button");
        leaveQueueButton.addClickListener(event -> leaveQueue());

        Div actions = new Div(leaveQueueButton, enterSelectionButton);
        actions.addClassName("waiting-queue-actions");

        panel.add(
                glow,
                brand,
                title,
                subtitle,
                eventName,
                createProgressBlock(),
                createStatsGrid(),
                helperText,
                actions
        );

        shell.add(panel);
        add(shell);
    }

    private Div createProgressBlock() {
        progressRing.addClassName("waiting-queue-progress-ring");

        Div outerPulse = new Div();
        outerPulse.addClassName("waiting-queue-pulse-ring");
        outerPulse.addClassName("waiting-queue-pulse-ring-outer");

        Div innerPulse = new Div();
        innerPulse.addClassName("waiting-queue-pulse-ring");
        innerPulse.addClassName("waiting-queue-pulse-ring-inner");

        Div center = new Div();
        center.addClassName("waiting-queue-progress-center");

        Icon ticketIcon = VaadinIcon.TICKET.create();
        ticketIcon.addClassName("waiting-queue-main-icon");

        center.add(ticketIcon);
        progressRing.add(outerPulse, innerPulse, center);
        return progressRing;
    }

    private Div createStatsGrid() {
        position.addClassName("waiting-queue-stat-value");
        position.addClassName("waiting-queue-position-value");

        estimatedWait.addClassName("waiting-queue-stat-value");
        estimatedWait.addClassName("waiting-queue-estimated-wait");

        return new Div(
                createStatCard("המיקום שלך בתור", position),
                createStatCard("זמן המתנה משוער", estimatedWait)
        ) {{
            addClassName("waiting-queue-stats-grid");
        }};
    }

    private Div createStatCard(String label, Span value) {
        Span labelElement = new Span(label);
        labelElement.addClassName("waiting-queue-stat-label");

        Div card = new Div(labelElement, value);
        card.addClassName("waiting-queue-stat-card");
        return card;
    }

    private void loadQueueState() {
        try {
            WaitingQueueSnapshot snapshot = presenter.getQueueSnapshot(eventId, getCurrentSessionToken());
            renderSnapshot(snapshot);
        } catch (Exception exception) {
            renderSnapshot(WaitingQueueSnapshot.error("האירוע", "לא ניתן לטעון את מצב התור כרגע"));
        }
    }

    private void renderSnapshot(WaitingQueueSnapshot snapshot) {
        WaitingQueueSnapshot safeSnapshot = snapshot == null
                ? WaitingQueueSnapshot.error("האירוע", "לא ניתן לטעון את מצב התור כרגע")
                : snapshot;

        eventName.setText(safeSnapshot.eventName());
        estimatedWait.setText(formatEstimatedWait(safeSnapshot.estimatedWaitMinutes()));
        position.setText(formatPosition(safeSnapshot.position()));
        helperText.setText(safeSnapshot.message());

        renderProgress(safeSnapshot.progressPercent());
        renderActions(safeSnapshot.status());
    }

    private void renderProgress(int progressPercent) {
        int safePercent = Math.max(0, Math.min(progressPercent, 100));
        progressRing.getElement().getStyle().set("--queue-progress", safePercent + "%");
    }

    private void renderActions(WaitingQueueStatus status) {
        boolean ready = status == WaitingQueueStatus.READY;
        enterSelectionButton.setVisible(ready);
        leaveQueueButton.setVisible(!ready);
    }

    private void leaveQueue() {
        try {
            presenter.leaveQueue(eventId, getCurrentSessionToken());
            UI.getCurrent().navigate(UiRoutes.HOME);
        } catch (Exception exception) {
            Notification notification = Notification.show("לא ניתן לצאת מהתור כרגע", 3500, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void navigateToTicketSelection() {
        UI.getCurrent().navigate(routeForEvent(UiRoutes.TICKET_SELECTION));
    }

    private String getCurrentSessionToken() {
        return UiSession.getCurrentToken();
    }

    private String routeForEvent(String routeTemplate) {
        return routeTemplate.replace(":eventId", String.valueOf(eventId));
    }

    private Optional<Long> parseLongSafely(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String formatEstimatedWait(int minutes) {
        if (minutes <= 0) {
            return "פחות מדקה";
        }
        return minutes + " דק׳";
    }

    private String formatPosition(int value) {
        if (value <= 0) {
            return "ממתין לעדכון";
        }
        return String.valueOf(value);
    }

    public interface WaitingQueuePresenter {

        WaitingQueueSnapshot getQueueSnapshot(long eventId, String sessionToken) throws Exception;

        void leaveQueue(long eventId, String sessionToken) throws Exception;
    }
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();
        ui.setPollInterval(3000);

        pollRegistration = ui.addPollListener(event -> loadQueueState());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        detachEvent.getUI().setPollInterval(-1);
        super.onDetach(detachEvent);
    }
}
