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
    private boolean navigationInProgress = false;
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

        leaveQueueButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        leaveQueueButton.addClassName("waiting-queue-leave-button");
        leaveQueueButton.addClickListener(event -> leaveQueue());

        Div actions = new Div(leaveQueueButton);
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

    /**
     * Reloads the current user's queue state.
     *
     * Poll requests are ignored once navigation to ticket selection has started,
     * preventing repeated navigation attempts while the current view is being
     * detached.
     */
    private void loadQueueState() {
        if (navigationInProgress) {
            return;
        }

        try {
            WaitingQueueSnapshot snapshot =
                    presenter.getQueueSnapshot(eventId, getCurrentSessionToken());

            renderSnapshot(snapshot);

        } catch (ticketsystem.PresentationLayer.Presenters.PresentationException e) {
            if (e.isSessionTimeout()) {
                if (UiSession.isLoggedIn()) {
                    UiSession.handleTimeoutRedirect();
                } else {
                    UiSession.exit();
                    Notification notification = Notification.show("זמן ההמתנה בתור פג עקב חוסר פעילות. אנא היכנסו לתור מחדש.", 5000, Notification.Position.TOP_CENTER);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    UI.getCurrent().navigate(UiRoutes.HOME);
                }
                return;
            }
            renderSnapshot(WaitingQueueSnapshot.error("האירוע", "לא ניתן לטעון את מצב התור כרגע: " + e.getMessage()));
        } catch (Exception exception) {
            renderSnapshot(
                    WaitingQueueSnapshot.error(
                            "האירוע",
                            "לא ניתן לטעון את מצב התור כרגע"
                    )
            );
        }
    }

    /**
     * Renders the latest waiting-queue state.
     *
     * When queue access has been granted, the user is moved directly to ticket
     * selection. No intermediate ready screen or confirmation button is shown.
     *
     * @param snapshot latest queue state returned by the presenter
     */
    private void renderSnapshot(WaitingQueueSnapshot snapshot) {
        WaitingQueueSnapshot safeSnapshot = snapshot == null
                ? WaitingQueueSnapshot.error(
                        "האירוע",
                        "לא ניתן לטעון את מצב התור כרגע"
                )
                : snapshot;

        if (safeSnapshot.status() == WaitingQueueStatus.READY) {
            navigateToTicketSelection();
            return;
        }

        eventName.setText(safeSnapshot.eventName());
        estimatedWait.setText(
                formatEstimatedWait(safeSnapshot.estimatedWaitMinutes())
        );
        position.setText(formatPosition(safeSnapshot.position()));
        helperText.setText(safeSnapshot.message());

        renderProgress(safeSnapshot.progressPercent());
        leaveQueueButton.setVisible(true);
    }

    private void renderProgress(int progressPercent) {
        int safePercent = Math.max(0, Math.min(progressPercent, 100));
        progressRing.getElement().getStyle().set("--queue-progress", safePercent + "%");
    }

    /**
     * Explicitly leaves the waiting queue and returns to the home page.
     */
    private void leaveQueue() {
        if (navigationInProgress) {
            return;
        }

        try {
            navigationInProgress = true;
            stopPolling();

            presenter.leaveQueue(eventId, getCurrentSessionToken());
            UI.getCurrent().navigate(UiRoutes.HOME);

        } catch (ticketsystem.PresentationLayer.Presenters.PresentationException e) {
            if (e.isSessionTimeout()) {
                UiSession.handleTimeoutRedirect();
                return;
            }
            Notification notification = Notification.show("שגיאה ביציאה מהתור: " + e.getMessage(), 3500, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception exception) {
            navigationInProgress = false;

            Notification notification = Notification.show(
                    "לא ניתן לצאת מהתור כרגע",
                    3500,
                    Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Stops queue polling and moves the approved user directly to ticket
     * selection.
     */
    private void navigateToTicketSelection() {
        if (navigationInProgress) {
            return;
        }

        navigationInProgress = true;
        stopPolling();

        UI.getCurrent().navigate(
                routeForEvent(UiRoutes.TICKET_SELECTION)
        );
    }

    /**
     * Stops the polling listener owned by this waiting-queue view.
     */
    private void stopPolling() {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        getUI().ifPresent(ui -> ui.setPollInterval(-1));
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
            return "—";
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
