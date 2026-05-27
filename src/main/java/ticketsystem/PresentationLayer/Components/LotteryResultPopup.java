package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class LotteryResultPopup extends Dialog {

    private LotteryResultPopup(String eventName, String purchaseCode) {
        getElement().setAttribute("dir", "rtl");
        getElement().getThemeList().add("message-popup");
        addClassName("message-popup-dialog");

        setModal(true);
        setDraggable(false);
        setResizable(false);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        add(createCard(eventName, purchaseCode));
    }

    public static void show(String eventName, String purchaseCode) {
        new LotteryResultPopup(eventName, purchaseCode ).open();
    }

    private Div createCard(String eventName, String purchaseCode) {
        Div card = new Div();
        card.addClassName("message-popup-card");
        card.addClassName("message-popup-card-success");
        card.addClassName("message-popup-card-lottery");

        Div glow = new Div();
        glow.addClassName("message-popup-glow");

        Span icon = new Span("🏆");
        icon.addClassName("message-popup-icon");
        icon.addClassName("message-popup-icon-success");
        icon.addClassName("message-popup-icon-lottery");

        H2 title = new H2("זכית בהגרלה!");
        title.addClassName("message-popup-title");

        Paragraph message = new Paragraph("הקוד הבא מאפשר לך לרכוש כרטיסים לאירוע:");
        message.addClassName("message-popup-message");

        Span eventLabel = new Span("אירוע");
        eventLabel.addClassName("lottery-popup-event-label");

        Span eventNameElement = new Span(safeText(eventName, "האירוע שלך"));
        eventNameElement.addClassName("lottery-popup-event-name");

        Div eventBlock = new Div(eventLabel, eventNameElement);
        eventBlock.addClassName("lottery-popup-event-block");

        Div codeBlock = createCodeBlock(purchaseCode);
        Paragraph note = createNote();
        Div actions = createActions();

        card.add(glow, icon, title, message, eventBlock, codeBlock, note, actions);
        return card;
    }

    private Div createCodeBlock(String purchaseCode) {
        String normalizedCode = safeText(purchaseCode, "");

        Span code = new Span(normalizedCode.isBlank() ? "קוד רכישה יופיע כאן" : normalizedCode);
        code.addClassName("lottery-popup-code");

        Button copy = new Button("העתקה");
        copy.setIcon(VaadinIcon.COPY.create());
        copy.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        copy.addClassName("lottery-popup-copy-button");
        copy.setEnabled(!normalizedCode.isBlank());
        copy.addClickListener(event -> copyCodeToClipboard(normalizedCode));

        Div codeBlock = new Div(code, copy);
        codeBlock.addClassName("lottery-popup-code-block");
        return codeBlock;
    }

    private Div createActions() {
        Button confirm = new Button("אישור");
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClassName("message-popup-button");
        confirm.addClassName("message-popup-primary-button");
        confirm.addClassName("lottery-popup-confirm-button");
        confirm.addClickListener(event -> close());

        Div actions = new Div(confirm);
        actions.addClassName("message-popup-actions");
        actions.addClassName("message-popup-actions-single");
        actions.addClassName("lottery-popup-actions");
        return actions;
    }

    private void copyCodeToClipboard(String code) {
        if (code == null || code.isBlank()) {
            return;
        }

        UI current = UI.getCurrent();
        if (current == null) {
            return;
        }

        current.getPage().executeJs("navigator.clipboard.writeText($0)", code);

        Notification notification = Notification.show("קוד הרכישה הועתק", 2200, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private Paragraph createNote() {
        Paragraph note = new Paragraph("בעת פתיחת המכירה המוקדמת ניתן יהיה להירשם עם הקוד שהתקבל.");
        note.addClassName("lottery-popup-note");
        return note;
    }
}
