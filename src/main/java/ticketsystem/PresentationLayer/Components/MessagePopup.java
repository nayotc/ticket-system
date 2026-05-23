package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

public class MessagePopup extends Dialog {

    public enum Type {
        SUCCESS,
        ERROR
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
        setCloseOnEsc(true);
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
        card.addClassName(type == Type.SUCCESS ? "message-popup-card-success" : "message-popup-card-error");

        if (type == Type.SUCCESS) {
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

    private Span createIcon(Type type) {
        Span icon = new Span(type == Type.SUCCESS ? "✓" : "!");
        icon.addClassName("message-popup-icon");
        icon.addClassName(type == Type.SUCCESS ? "message-popup-icon-success" : "message-popup-icon-error");
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
            button.addClassName(type == Type.SUCCESS ? "message-popup-success-button" : "message-popup-error-button");
        }

        button.addClickListener(event -> {
            close();
            if (action != null) {
                action.run();
            }
        });

        return button;
    }
}
