package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;

import java.time.Duration;
import java.time.Instant;

public class ReservationTimer extends Div {

    private static final String DEADLINE_SESSION_KEY = ReservationTimer.class.getName() + ".deadlineEpochMillis";
    private static final Duration DEFAULT_RESERVATION_DURATION = Duration.ofMinutes(10);

    private final Span time = new Span("10:00");

    public ReservationTimer() {
        getElement().setAttribute("dir", "rtl");
        addClassName("reservation-floating-timer");

        Span icon = new Span();
        icon.add(VaadinIcon.CLOCK.create());
        icon.addClassName("reservation-timer-icon");

        Span label = new Span("הזמן שנותר לשריון הכרטיסים");
        label.addClassName("reservation-timer-label");

        time.addClassName("reservation-timer-time");
        time.getElement().setAttribute("data-reservation-timer-time", "true");

        add(icon, label, time);
        refreshFromSession();
    }

    public static void startIfNeeded() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }

        Long existingDeadline = readDeadline(session);
        if (existingDeadline != null && existingDeadline > Instant.now().toEpochMilli()) {
            return;
        }

        session.setAttribute(DEADLINE_SESSION_KEY, Instant.now().plus(DEFAULT_RESERVATION_DURATION).toEpochMilli());
    }

    public static void clear() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(DEADLINE_SESSION_KEY, null);
        }
    }

    public static boolean isActive() {
        return remainingMillis() > 0;
    }

    public static long remainingMillis() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return 0L;
        }

        Long deadline = readDeadline(session);
        if (deadline == null) {
            return 0L;
        }

        return Math.max(0L, deadline - Instant.now().toEpochMilli());
    }

    public void refreshFromSession() {
        VaadinSession session = VaadinSession.getCurrent();
        Long deadline = session == null ? null : readDeadline(session);

        if (deadline == null || deadline <= Instant.now().toEpochMilli()) {
            setVisible(false);
            getElement().removeAttribute("data-deadline-epoch-millis");
            time.setText("00:00");
            return;
        }

        setVisible(true);
        getElement().setAttribute("data-deadline-epoch-millis", String.valueOf(deadline));
        time.setText(formatRemaining(deadline - Instant.now().toEpochMilli()));

        if (getUI().isPresent()) {
            startClientCountdown();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshFromSession();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        getElement().executeJs("if (this.__reservationTimerInterval) { clearInterval(this.__reservationTimerInterval); this.__reservationTimerInterval = null; }");
        super.onDetach(detachEvent);
    }

    private void startClientCountdown() {
        getElement().executeJs("""
                const root = this;
                const time = root.querySelector('[data-reservation-timer-time]');
                const deadline = Number(root.getAttribute('data-deadline-epoch-millis'));

                if (root.__reservationTimerInterval) {
                    clearInterval(root.__reservationTimerInterval);
                }

                const format = (millis) => {
                    const totalSeconds = Math.max(0, Math.ceil(millis / 1000));
                    const minutes = Math.floor(totalSeconds / 60);
                    const seconds = totalSeconds % 60;
                    return String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
                };

                const update = () => {
                    if (!time || !deadline) {
                        root.style.display = 'none';
                        return;
                    }

                    const remaining = deadline - Date.now();
                    time.textContent = format(remaining);

                    if (remaining <= 0) {
                        root.classList.add('reservation-timer-expired');
                        clearInterval(root.__reservationTimerInterval);
                        root.__reservationTimerInterval = null;
                    }
                };

                root.classList.remove('reservation-timer-expired');
                root.style.display = '';
                update();
                root.__reservationTimerInterval = setInterval(update, 1000);
                """);
    }

    private static Long readDeadline(VaadinSession session) {
        Object value = session.getAttribute(DEADLINE_SESSION_KEY);
        if (value instanceof Long deadline) {
            return deadline;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, (long) Math.ceil(millis / 1000.0));
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void setDeadline(Long expiresAtEpochMillis) {
    VaadinSession session = VaadinSession.getCurrent();
    long now = System.currentTimeMillis();

    if (expiresAtEpochMillis == null || expiresAtEpochMillis <= now) {
        if (session != null) {
            session.setAttribute(DEADLINE_SESSION_KEY, null);
        }

        setVisible(false);
        getElement().removeAttribute("data-deadline-epoch-millis");
        time.setText("00:00");
        return;
    }

    if (session != null) {
        session.setAttribute(DEADLINE_SESSION_KEY, expiresAtEpochMillis);
    }

    setVisible(true);
    getElement().setAttribute("data-deadline-epoch-millis", String.valueOf(expiresAtEpochMillis));
    time.setText(formatRemaining(expiresAtEpochMillis - now));

    if (getUI().isPresent()) {
        startClientCountdown();
    }
}
}
