package ticketsystem.UnitTesting;

import org.junit.jupiter.api.Test;
import ticketsystem.PresentationLayer.Notifications.MessageTranslator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MessageTranslatorTest {

    @Test
    void GivenNullMessage_WhenTranslate_ThenReturnNull() {
        String translated = MessageTranslator.translate(null);

        assertNull(translated);
    }

    @Test
    void GivenBlankMessage_WhenTranslate_ThenReturnOriginalMessage() {
        String message = "   ";

        String translated = MessageTranslator.translate(message);

        assertEquals(message, translated);
    }

    @Test
    void GivenSuspensionRevokedMessage_WhenTranslate_ThenReturnHebrewMessage() {
        String message =
                "Your account suspension has been revoked. "
                        + "You now have access to your account.";

        String translated = MessageTranslator.translate(message);

        assertEquals(
                "השעיית החשבון שלך בוטלה. ההגבלה על ביצוע פעולות הוסרה.",
                translated
        );
    }

    @Test
    void GivenTemporarySuspensionWithReason_WhenTranslate_ThenReturnFormattedHebrewMessage() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:00"
                        + " to "
                        + "2026-06-30T18:45:00"
                        + " for the following reason: "
                        + "פעילות חריגה";

        String translated = MessageTranslator.translate(message);

        String expected =
                "חשבונך הושעה."
                        + "\nמ\u05BE28/06/2026 10:30 עד 30/06/2026 18:45."
                        + "\nמהסיבה הבאה: פעילות חריגה."
                        + "\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.";

        assertEquals(expected, translated);
    }

    @Test
    void GivenPermanentSuspensionWithReason_WhenTranslate_ThenIndicateNoEndDate() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:00"
                        + " to null"
                        + " for the following reason: "
                        + "הפרת תנאי שימוש";

        String translated = MessageTranslator.translate(message);

        String expected =
                "חשבונך הושעה."
                        + "\nמ\u05BE28/06/2026 10:30 ללא תאריך סיום."
                        + "\nמהסיבה הבאה: הפרת תנאי שימוש."
                        + "\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.";

        assertEquals(expected, translated);
    }

    @Test
    void GivenSuspensionWithNullReason_WhenTranslate_ThenDoNotShowReasonLine() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:00"
                        + " to "
                        + "2026-06-29T10:30:00"
                        + " for the following reason: null";

        String translated = MessageTranslator.translate(message);

        String expected =
                "חשבונך הושעה."
                        + "\nמ\u05BE28/06/2026 10:30 עד 29/06/2026 10:30."
                        + "\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.";

        assertEquals(expected, translated);
    }

    @Test
    void GivenSuspensionDatesWithFractionalSeconds_WhenTranslate_ThenRemoveSecondsAndFractions() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:45.123456"
                        + " to "
                        + "2026-06-28T12:05:59.999"
                        + " for the following reason: "
                        + "בדיקה";

        String translated = MessageTranslator.translate(message);

        String expected =
                "חשבונך הושעה."
                        + "\nמ\u05BE28/06/2026 10:30 עד 28/06/2026 12:05."
                        + "\nמהסיבה הבאה: בדיקה."
                        + "\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.";

        assertEquals(expected, translated);
    }

    @Test
    void GivenUnsupportedDateFormat_WhenTranslate_ThenUseReadableDateFallback() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:45Z"
                        + " to "
                        + "2026-06-30T18:45:10Z"
                        + " for the following reason: "
                        + "בדיקה";

        String translated = MessageTranslator.translate(message);

        String expected =
                "חשבונך הושעה."
                        + "\nמ\u05BE2026-06-28 10:30 עד 2026-06-30 18:45."
                        + "\nמהסיבה הבאה: בדיקה."
                        + "\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.";

        assertEquals(expected, translated);
    }

    @Test
    void GivenMalformedSuspensionMessageWithoutReasonSection_WhenTranslate_ThenReturnOriginalMessage() {
        String message =
                "Your account has been suspended from "
                        + "2026-06-28T10:30:00"
                        + " to "
                        + "2026-06-30T18:45:00";

        String translated = MessageTranslator.translate(message);

        assertEquals(message, translated);
    }

    @Test
    void GivenUnrecognizedMessage_WhenTranslate_ThenReturnOriginalMessage() {
        String message = "A message that does not have a translation.";

        String translated = MessageTranslator.translate(message);

        assertEquals(message, translated);
    }
}