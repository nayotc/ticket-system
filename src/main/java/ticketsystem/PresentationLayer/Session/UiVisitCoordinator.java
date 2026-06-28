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

        authPresenter.ensureGuestSession();

        syncBrowserExitToken(ui);
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
        syncBrowserExitToken(ui);
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
        syncBrowserExitToken(ui);
        connectCurrentTarget(ui);
    }

    public void registerBrowserExitHook(UI ui) {
        if (ui == null) {
            return;
        }

        ui.getPage().executeJs(
                """
                window.__tixnowCancelPendingExit = function() {
                    const pending = sessionStorage.getItem('__tixnow_pending_exit_token');
                    if (!pending) {
                        return;
                    }

                    sessionStorage.removeItem('__tixnow_pending_exit_token');
                    navigator.sendBeacon('/api/session/cancel-exit', pending);
                };

                if (!window.__tixnowExitHookInstalled) {
                    window.__tixnowExitHookInstalled = true;

                    window.addEventListener('pageshow', function() {
                        window.__tixnowCancelPendingExit();
                    });

                    window.addEventListener('pagehide', function(event) {
                        if (event.persisted || !window.__tixnowExitToken) {
                            return;
                        }

                        sessionStorage.setItem('__tixnow_pending_exit_token', window.__tixnowExitToken);
                        navigator.sendBeacon('/api/session/exit', window.__tixnowExitToken);
                    });
                }

                window.__tixnowCancelPendingExit();
                """
        );
    }

    public void syncBrowserExitToken(UI ui) {
        if (ui == null) {
            return;
        }

        String token = UiSession.getCurrentToken();
        if (token == null || token.isBlank()) {
            ui.getPage().executeJs("window.__tixnowExitToken = null;");
        } else {
            ui.getPage().executeJs("window.__tixnowExitToken = $0;", token);
        }
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

    public void forceShowPendingNotifications(UI ui) {
        if (ui != null) {
            notificationCenter.showPending(ui, UiSession.getNotificationTargetId());
        }
    }
}
