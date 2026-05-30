package ticketsystem.PresentationLayer.Session;

import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;
import ticketsystem.PresentationLayer.Notifications.NotificationCenter;
import ticketsystem.PresentationLayer.Presenters.AuthPresenter;

@Component
public class UiVisitCoordinator {

    private final AuthPresenter authPresenter;
    private final NotificationCenter notificationCenter;

    public UiVisitCoordinator(AuthPresenter authPresenter,
                              NotificationCenter notificationCenter) {
        this.authPresenter = authPresenter;
        this.notificationCenter = notificationCenter;
    }

    public void ensureVisitAndNotifications(UI ui) {
        if (ui == null) {
            return;
        }

        if (!UiSession.isLoggedIn() && !UiSession.hasGuestSession()) {
            String guestToken = authPresenter.visitSystem();
        }

        connectCurrentTarget(ui);
    }

    public void login(UI ui, String email, String password) {
        if (ui == null) {
            return;
        }

        ensureVisitAndNotifications(ui);

        authPresenter.login(
                UiSession.getGuestToken(),
                email,
                password
        );

        notificationCenter.disconnect();

        connectCurrentTarget(ui);

        notificationCenter.showPending(
                ui,
                UiSession.getNotificationTargetId()
        );
    }

    public void logoutToGuest(UI ui) {
        if (ui == null) {
            return;
        }

        authPresenter.logOut(UiSession.getCurrentToken());

        notificationCenter.disconnect();

        connectCurrentTarget(ui);
    }

    public void disconnect() {
        notificationCenter.disconnect();
    }

    private void connectCurrentTarget(UI ui) {
        String targetId = UiSession.getNotificationTargetId();

        if (targetId != null && !targetId.isBlank()) {
            notificationCenter.connect(ui, targetId);
        }
    }
}