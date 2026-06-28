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

    @Test
    void GivenAccountDeactivationMessage_WhenTranslate_ThenReturnHebrewMessage() {
        String message =
                "Your account has been deactivated by a system administrator.\n"
                        + "You will no longer have access to the system.";

        String translated = MessageTranslator.translate(message);

        assertEquals(
                "החשבון שלך הושבת על ידי מנהל מערכת. "
                        + "לא ניתן עוד להתחבר למערכת.",
                translated
        );
    }

    @Test
    void GivenSystemAdminPromotionMessage_WhenTranslate_ThenReturnHebrewMessage() {
        String message =
                "Congratulations! You have been promoted to System Admin.";

        String translated = MessageTranslator.translate(message);

        assertEquals(
                "קיבלת הרשאת מנהל מערכת.",
                translated
        );
    }

    @Test
    void GivenCompanyClosedBySystemAdmin_WhenTranslate_ThenReturnHebrewMessage() {
        String message =
                "The production company \"הפקות הדרום\" "
                        + "was closed by a system administrator, "
                        + "and your role in this company was removed.";

        String translated = MessageTranslator.translate(message);

        assertEquals(
                "חברת ההפקה \"הפקות הדרום\" נסגרה על ידי מנהל מערכת, "
                        + "והתפקיד שלך בחברה הוסר.",
                translated
        );
    }

    @Test
    void GivenUnknownTechnicalError_WhenTranslateOrFallback_ThenReturnFallback() {
        String translated = MessageTranslator.translateOrFallback(
                "SQL constraint violation: users_email_key",
                "עדכון הפרטים נכשל. נסו שוב."
        );

        assertEquals(
                "עדכון הפרטים נכשל. נסו שוב.",
                translated
        );
    }

    @Test
    void GivenKnownError_WhenTranslateOrFallback_ThenReturnTranslation() {
        String translated = MessageTranslator.translateOrFallback(
                "minimum tickets cannot be greater than maximum tickets",
                "שמירת המדיניות נכשלה. נסו שוב."
        );

        assertEquals(
                "מינימום הכרטיסים לא יכול להיות גדול מהמקסימום.",
                translated
        );
    }

    @Test
    void GivenNullError_WhenTranslateOrFallback_ThenReturnFallback() {
        String translated = MessageTranslator.translateOrFallback(
                null,
                "טעינת המידע נכשלה. נסו שוב."
        );

        assertEquals(
                "טעינת המידע נכשלה. נסו שוב.",
                translated
        );
    }
}