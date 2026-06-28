package ticketsystem.UnitTesting;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import ticketsystem.PresentationLayer.Components.MessagePopup;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessagePopupTest {

    @Test
    void GivenMultilineMessage_WhenCreateNotificationPopup_ThenPreserveLineBreaks() {
        MessagePopup popup = MessagePopup.notification(
                "התראה חדשה",
                "שורה ראשונה\nשורה שנייה",
                null
        );

        Paragraph messageParagraph = findComponent(
                popup,
                Paragraph.class
        );

        assertNotNull(messageParagraph);
        assertEquals(
                "pre-line",
                messageParagraph.getStyle().get("white-space")
        );
    }

    @Test
    void GivenMultilineErrorMessage_WhenCreateErrorPopup_ThenPreserveLineBreaks() {
        MessagePopup popup = MessagePopup.error(
                "הפעולה נכשלה",
                "פרטי השגיאה\nיש לנסות שוב"
        );

        Paragraph messageParagraph = findComponent(
                popup,
                Paragraph.class
        );

        assertNotNull(messageParagraph);
        assertEquals(
                "pre-line",
                messageParagraph.getStyle().get("white-space")
        );
    }

    private static <T extends Component> T findComponent(
            Component root,
            Class<T> componentType
    ) {
        if (componentType.isInstance(root)) {
            return componentType.cast(root);
        }

        return root.getChildren()
                .map(child -> findComponent(child, componentType))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}