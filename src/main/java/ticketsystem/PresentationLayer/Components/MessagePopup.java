package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

public class MessagePopup extends Dialog {

    public enum Type {
        SUCCESS,
        ERROR,
        NOTIFICATION,
        ASSIGNMENT
    }

    private MessagePopup(
            Type type,
            String title,
            String message,
            String primaryText,
            Runnable primaryAction,
            String secondaryText,
            Runnable secondaryAction
    ) {
        getElement().setAttribute("dir", "rtl");
        getElement().getThemeList().add("message-popup");
        addClassName("message-popup-dialog");

        setModal(true);
        setDraggable(false);
        setResizable(false);
        setCloseOnEsc(type != Type.ASSIGNMENT);
        setCloseOnOutsideClick(false);

        add(createCard(type, title, message, primaryText, primaryAction, secondaryText, secondaryAction));
    }

    public static void showSuccess(String message) {
        showSuccess(message, null);
    }

    public static void showSuccess(String message, Runnable onConfirm) {
        success("הפעולה בוצעה בהצלחה", message, onConfirm).open();
    }

    public static void showError(String message) {
        error("אופס, משהו השתבש", message).open();
    }

    public static void showError(String message, Runnable onRetry) {
        error("אופס, משהו השתבש", message, onRetry).open();
    }

    public static void showNotification(String message) {
        notification("התראה חדשה", message, null).open();
    }

    public static void showNotification(String message, Runnable onConfirm) {
        notification("התראה חדשה", message, onConfirm).open();
    }

    public static MessagePopup notification(String title, String message, Runnable onConfirm) {
        return new MessagePopup(
                Type.NOTIFICATION,
                title,
                message,
                "אישור",
                onConfirm,
                null,
                null
        );
    }

    public static MessagePopup success(String title, String message, Runnable onConfirm) {
        return new MessagePopup(
                Type.SUCCESS,
                title,
                message,
                "אישור",
                onConfirm,
                null,
                null
        );
    }

    public static MessagePopup error(String title, String message) {
        return new MessagePopup(
                Type.ERROR,
                title,
                message,
                "אישור",
                null,
                null,
                null
        );
    }

    public static MessagePopup error(String title, String message, Runnable onRetry) {
        return new MessagePopup(
                Type.ERROR,
                title,
                message,
                "נסה שוב",
                onRetry,
                "ביטול",
                null
        );
    }

    private Div createCard(
            Type type,
            String title,
            String message,
            String primaryText,
            Runnable primaryAction,
            String secondaryText,
            Runnable secondaryAction
    ) {
        Div card = new Div();
        card.addClassName("message-popup-card");
        card.addClassName(resolveCardClass(type));

        if (type == Type.SUCCESS || type == Type.NOTIFICATION) {
            Div glow = new Div();
            glow.addClassName("message-popup-glow");
            card.add(glow);
        }

        card.add(
                createIcon(type),
                createTitle(title),
                createMessage(message),
                createActions(type, primaryText, primaryAction, secondaryText, secondaryAction)
        );

        return card;
    }

    private String resolveCardClass(Type type) {
        if (type == Type.SUCCESS) {
            return "message-popup-card-success";
        }

        if (type == Type.ERROR) {
            return "message-popup-card-error";
        }

        if (type == Type.ASSIGNMENT) {
            return "message-popup-card-assignment";
        }

        return "message-popup-card-notification";
    }

    private Span createIcon(Type type) {
        Span icon = new Span();

        if (type == Type.SUCCESS) {
            icon.setText("✓");
            icon.addClassName("message-popup-icon-success");
        } else if (type == Type.ERROR) {
            icon.setText("!");
            icon.addClassName("message-popup-icon-error");
        } else if (type == Type.ASSIGNMENT) {
            icon.add(VaadinIcon.USER.create());
            icon.addClassName("message-popup-icon-assignment");
        } else {
            icon.add(VaadinIcon.BELL.create());
            icon.addClassName("message-popup-icon-notification");
        }

        icon.addClassName("message-popup-icon");
        return icon;
    }

    private H2 createTitle(String title) {
        H2 titleElement = new H2(title);
        titleElement.addClassName("message-popup-title");
        return titleElement;
    }

    private Paragraph createMessage(String message) {
        Paragraph messageElement = new Paragraph(message);
        messageElement.addClassName("message-popup-message");
        return messageElement;
    }

    private Div createActions(
            Type type,
            String primaryText,
            Runnable primaryAction,
            String secondaryText,
            Runnable secondaryAction
    ) {
        Div actions = new Div();
        actions.addClassName("message-popup-actions");

        boolean hasSecondary = secondaryText != null && !secondaryText.isBlank();
        actions.addClassName(hasSecondary ? "message-popup-actions-double" : "message-popup-actions-single");

        Button primary = createButton(primaryText, type, true, primaryAction);
        actions.add(primary);

        if (hasSecondary) {
            Button secondary = createButton(secondaryText, type, false, secondaryAction);
            actions.add(secondary);
        }

        return actions;
    }

    private Button createButton(String text, Type type, boolean primary, Runnable action) {
        Button button = new Button(text);
        button.addClassName("message-popup-button");
        button.addClassName(primary ? "message-popup-primary-button" : "message-popup-secondary-button");

        if (primary) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            if (type == Type.ERROR) {
                button.addClassName("message-popup-error-button");
            } else {
                button.addClassName("message-popup-success-button");
            }
        }

        button.addClickListener(event -> {
            close();
            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    public static void showAssignmentRequest(
            String title,
            String message,
            Runnable onAccept,
            Runnable onDecline
    ) {
        assignmentRequest(title, message, onAccept, onDecline).open();
    }

    public static MessagePopup assignmentRequest(
            String title,
            String message,
            Runnable onAccept,
            Runnable onDecline
    ) {
        return new MessagePopup(
                Type.ASSIGNMENT,
                title,
                message,
                "קבל",
                onAccept,
                "סרב",
                onDecline
        );
    }
}
