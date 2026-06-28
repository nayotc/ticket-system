package ticketsystem.PresentationLayer.Notifications;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.IBrodcaster;
import ticketsystem.ApplicationLayer.NotificationService;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.PresentationLayer.Components.MessagePopup;
import ticketsystem.PresentationLayer.Presenters.MembershipPresenter;
import ticketsystem.PresentationLayer.Session.UiSession;

@Component
public class NotificationCenter {

    private static final String PUSH_UNREGISTER_KEY = "pushUnregister";
    private static final String PUSH_TARGET_KEY = "pushTargetId";

    private final IBrodcaster broadcaster;
    private final NotificationService notificationService;
    private final MembershipPresenter membershipPresenter;

    public NotificationCenter(IBrodcaster broadcaster,
                              NotificationService notificationService,
                              MembershipPresenter membershipPresenter) {
        this.broadcaster = broadcaster;
        this.notificationService = notificationService;
        this.membershipPresenter = membershipPresenter;
    }

    public void connect(UI ui, String targetId) {
        if (ui == null || targetId == null || targetId.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        Object currentTarget = session.getAttribute(PUSH_TARGET_KEY);
        Object currentUnregister = session.getAttribute(PUSH_UNREGISTER_KEY);

        if (targetId.equals(currentTarget) && currentUnregister instanceof Runnable) {
            return;
        }

        disconnect();

        Runnable unregister = broadcaster.registerListener(targetId, notification -> {
            ui.access(() -> {
                show(notification);

                if (notification.getId() != null) {
                    notificationService.markAsDelivered(notification.getId());
                }
            });
        });

        session.setAttribute(PUSH_UNREGISTER_KEY, unregister);
        session.setAttribute(PUSH_TARGET_KEY, targetId);
    }

    public void disconnect() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        Object unregister = session.getAttribute(PUSH_UNREGISTER_KEY);

        if (unregister instanceof Runnable runnable) {
            runnable.run();
        }

        session.setAttribute(PUSH_UNREGISTER_KEY, null);
        session.setAttribute(PUSH_TARGET_KEY, null);
    }

    public void showPending(UI ui, String targetId) {
        if (ui == null || targetId == null || targetId.isBlank()) {
            return;
        }

        notificationService.getPendingNotifications(targetId).forEach(notification -> {
            ui.access(() -> {
                show(notification);

                if (notification.getId() != null) {
                    notificationService.markAsDelivered(notification.getId());
                }
            });
        });
    }

    private void show(Notification notification) {
        String rawMessage = safeRawMessage(notification);

        if (rawMessage.contains("deactivated by a system administrator")) {
            showDeactivationAndLogout();
            return;
        }

        if (isAssignmentRequest(notification)) {
            showAssignmentRequest(notification);
            return;
        }

        MessagePopup.showNotification(resolveMessage(notification));
    }

    private void showAssignmentRequest(Notification notification) {
        Long companyId = notification.getCompanyId();

        if (companyId == null) {
            MessagePopup.showNotification(resolveMessage(notification));
            return;
        }

        String rawMessage = safeRawMessage(notification);
        boolean ownerAssignment = isOwnerAssignmentRequest(rawMessage);

        String title = ownerAssignment
                ? "בקשת מינוי לבעלים"
                : "בקשת מינוי למנהל";

        String message = buildAssignmentMessage(rawMessage, ownerAssignment);

        MessagePopup.showAssignmentRequest(
                title,
                message,
                () -> approveAssignment(companyId),
                () -> rejectAssignment(companyId)
        );
    }

    private void approveAssignment(Long companyId) {
        try {
            String token = UiSession.getMemberToken();
            if (token == null || token.isBlank()) {
                MessagePopup.showError("יש להתחבר למערכת כדי לאשר את המינוי.");
                return;
            }

            membershipPresenter.approveAssignment(token, companyId);
            MessagePopup.showSuccess("המינוי אושר בהצלחה.");
        } catch (Exception e) {
            MessagePopup.showError(resolveErrorMessage(e, "אישור המינוי נכשל."));
        }
    }

    private void rejectAssignment(Long companyId) {
        try {
            String token = UiSession.getMemberToken();
            if (token == null || token.isBlank()) {
                MessagePopup.showError("יש להתחבר למערכת כדי לדחות את המינוי.");
                return;
            }

            membershipPresenter.rejectAssignment(token, companyId);
            MessagePopup.showSuccess("המינוי נדחה בהצלחה.");
        } catch (Exception e) {
            MessagePopup.showError(resolveErrorMessage(e, "דחיית המינוי נכשלה."));
        }
    }

    private boolean isAssignmentRequest(Notification notification) {
        String message = safeRawMessage(notification);
        return isManagerAssignmentRequest(message) || isOwnerAssignmentRequest(message);
    }

    private boolean isManagerAssignmentRequest(String message) {
        return message.startsWith("You received a request to become a manager of the production company \"")
                || message.contains(" requested to appoint you as manager of the production company \"");
    }

    private boolean isOwnerAssignmentRequest(String message) {
        return message.startsWith("You received a request to become an owner of the production company \"")
                || message.contains(" requested to appoint you as owner of the production company \"");
    }

    private String buildAssignmentMessage(String rawMessage, boolean ownerAssignment) {
        String companyName = extractBetween(rawMessage, "production company \"", "\"");
        String appointerName = extractBefore(rawMessage, " requested to appoint you as");

        String safeCompanyName = companyName == null || companyName.isBlank()
                ? "החברה"
                : companyName;

        String roleText = ownerAssignment ? "לבעלים" : "למנהל";

        if (appointerName == null || appointerName.isBlank()) {
            return "קיבלת בקשה להתמנות " + roleText + " בחברת ההפקה \"" + safeCompanyName + "\".";
        }

        return "מנוי " + appointerName + " מבקש למנות אותך " + roleText
                + " בחברת ההפקה \"" + safeCompanyName + "\".";
    }

    private String resolveMessage(Notification notification) {
        String message = safeRawMessage(notification);

        if (message.isBlank()) {
            return "התקבלה התראה חדשה";
        }

        return MessageTranslator.translate(message);
    }

    private String safeRawMessage(Notification notification) {
        if (notification == null || notification.getMessage() == null) {
            return "";
        }

        return notification.getMessage().trim();
    }

    private String resolveErrorMessage(Exception exception, String fallback) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return fallback;
        }

        return exception.getMessage();
    }

    private String extractBefore(String value, String delimiter) {
        if (value == null || delimiter == null) {
            return null;
        }

        int end = value.indexOf(delimiter);
        if (end <= 0) {
            return null;
        }

        return value.substring(0, end).trim();
    }

    private String extractBetween(String value, String prefix, String suffix) {
        if (value == null || prefix == null || suffix == null) {
            return null;
        }

        int start = value.indexOf(prefix);
        if (start < 0) {
            return null;
        }

        start += prefix.length();

        int end = value.indexOf(suffix, start);
        if (end < 0 || end <= start) {
            return null;
        }

        return value.substring(start, end);
    }

    private void showDeactivationAndLogout() {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.add("חשבונך נחסם והוסר על ידי מנהל המערכת. הנך מנותק כעת.");

        com.vaadin.flow.component.button.Button okButton = new com.vaadin.flow.component.button.Button("אישור", event -> {
            dialog.close();
            
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.close();
            }
            
            UI.getCurrent().getPage().setLocation("/login");
        });

        dialog.add(okButton);
        dialog.setCloseOnEsc(false); 
        dialog.setCloseOnOutsideClick(false); 
        dialog.open();
    }
}