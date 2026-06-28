package ticketsystem.PresentationLayer.Notifications;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class MessageTranslator {

    private static final Map<String, String> EXACT_TRANSLATIONS = new LinkedHashMap<>();
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        EXACT_TRANSLATIONS.put(
                "Payment failed. No purchase was completed.",
                "התשלום נכשל. הרכישה לא הושלמה."
        );

        EXACT_TRANSLATIONS.put(
                "Your purchase was completed successfully. Your tickets are now available.",
                "הרכישה הושלמה בהצלחה. הכרטיסים שלך זמינים כעת."
        );

        EXACT_TRANSLATIONS.put(
                "The purchase was canceled because ticket issuing failed. A refund was issued.",
                "הרכישה בוטלה כי הנפקת הכרטיסים נכשלה. בוצע זיכוי."
        );

        EXACT_TRANSLATIONS.put(
                "Your active order has expired. The reserved tickets were released back to the inventory.",
                "תוקף ההזמנה הפעילה שלך פג. הכרטיסים ששוריינו שוחררו חזרה למלאי."
        );

        EXACT_TRANSLATIONS.put(
                "Your active order is about to expire. Please complete your purchase soon.",
                "ההזמנה הפעילה שלך עומדת לפוג. יש להשלים את הרכישה בהקדם."
        );

        EXACT_TRANSLATIONS.put(
                "Your account suspension has been revoked. You now have access to your account.",
                "השעיית החשבון שלך בוטלה. ההגבלה על ביצוע פעולות הוסרה."
        );
    }

    private MessageTranslator() {
    }

    public static String translate(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String trimmed = message.trim();

        String exactTranslation = EXACT_TRANSLATIONS.get(trimmed);
        if (exactTranslation != null) {
            return exactTranslation;
        }

        String translated = translateSuspensionMessage(trimmed);
        if (translated != null) {
            return translated;
        }

        translated = translateProductionCompanyMessage(trimmed);
        if (translated != null) {
            return translated;
        }

        translated = translateEventMessage(trimmed);
        if (translated != null) {
            return translated;
        }

        translated = translateRoleMessage(trimmed);
        if (translated != null) {
            return translated;
        }

        return message;
    }

    private static String translateProductionCompanyMessage(String message) {
        String companyName = extractBetween(message, "The production company \"", "\"");

        if (companyName == null) {
            return null;
        }

        if (message.endsWith("\" has been closed and is no longer active.")) {
            return "חברת ההפקה \"" + companyName + "\" נסגרה ואינה פעילה יותר.";
        }

        if (message.endsWith("\" has been reopened and is now active.")) {
            return "חברת ההפקה \"" + companyName + "\" נפתחה מחדש וכעת היא פעילה.";
        }

        return null;
    }

    private static String translateEventMessage(String message) {
        String eventName = extractBetween(message, "The event \"", "\"");

        if (eventName != null && message.endsWith("\" is now sold out.")) {
            return "האירוע \"" + eventName + "\" אזל מהמלאי.";
        }
        
        if (eventName != null && message.endsWith("\" that you purchased tickets for was canceled.")) {
            return "האירוע \"" + eventName + "\" אליו רכשת כרטיסים, בוטל על ידי המארגנים.";
        }
        return null;
    }

    private static String translateRoleMessage(String message) {
        String companyName = extractBetween(message, "production company \"", "\"");

        if (companyName == null) {
            return null;
        }

        if (message.startsWith("You received a request to become a manager of the production company \"")) {
            return "קיבלת בקשה להפוך למנהל בחברת ההפקה \"" + companyName + "\".";
        }

        if (message.startsWith("You received a request to become an owner of the production company \"")) {
            return "קיבלת בקשה להפוך לבעלים בחברת ההפקה \"" + companyName + "\".";
        }

        if (message.startsWith("Your owner role in the production company \"")
                && message.endsWith("\" was removed.")) {
            return "תפקיד הבעלים שלך בחברת ההפקה \"" + companyName + "\" הוסר.";
        }

        if (message.startsWith("Your manager role in the production company \"")
                && message.endsWith("\" was removed.")) {
            return "תפקיד המנהל שלך בחברת ההפקה \"" + companyName + "\" הוסר.";
        }

        if (message.startsWith("Your management permissions in the production company \"")
                && message.endsWith("\" were updated.")) {
            return "הרשאות הניהול שלך בחברת ההפקה \"" + companyName + "\" עודכנו.";
        }

        if (message.contains(" approved the assignment request for the production company \"")) {
            String memberName = message.substring(0, message.indexOf(" approved the assignment request"));
            return memberName + " אישר את בקשת המינוי לחברת ההפקה \"" + companyName + "\".";
        }

        if (message.contains(" rejected the assignment request for the production company \"")) {
            String memberName = message.substring(0, message.indexOf(" rejected the assignment request"));
            return memberName + " דחה את בקשת המינוי לחברת ההפקה \"" + companyName + "\".";
        }

        if (message.contains(" resigned from the owner role in the production company \"")) {
            String memberName = message.substring(0, message.indexOf(" resigned from the owner role"));
            return memberName + " ויתר על תפקיד הבעלים בחברת ההפקה \"" + companyName + "\".";
        }

        if (message.contains(" requested to appoint you as manager of the production company \"")) {
            String memberName = message.substring(0, message.indexOf(" requested to appoint you as manager"));
            return "מנוי " + memberName + " מבקש למנות אותך למנהל בחברת ההפקה \"" + companyName + "\".";
        }

        if (message.contains(" requested to appoint you as owner of the production company \"")) {
            String memberName = message.substring(0, message.indexOf(" requested to appoint you as owner"));
            return "מנוי " + memberName + " מבקש למנות אותך לבעלים בחברת ההפקה \"" + companyName + "\".";
        }

        return null;
    }

    private static String extractBetween(String value, String prefix, String suffix) {
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

    /**
     * Translates dynamic account suspension notification messages into a Hebrew user-facing message.
     *
     * <p>The suspension notification is currently created as a dynamic English message that
     * includes the suspension start date, end date, and reason. Since these values are dynamic,
     * the message cannot be translated using the exact translation map.</p>
     *
     * <p>Expected source format:</p>
     * <pre>
     * Your account has been suspended from {startDate} to {endDate} for the following reason: {reason}
     * </pre>
     *
     * @param message the original notification message after trimming
     * @return a Hebrew suspension notification message, or {@code null} if this message is not
     *         an account suspension notification
     */
    private static String translateSuspensionMessage(String message) {
        String prefix = "Your account has been suspended from ";
        String middle = " to ";
        String reasonPrefix = " for the following reason: ";

        if (!message.startsWith(prefix) || !message.contains(middle) || !message.contains(reasonPrefix)) {
            return null;
        }

        int startIndex = prefix.length();
        int endStartIndex = message.indexOf(middle, startIndex);
        int reasonStartIndex = message.indexOf(reasonPrefix, endStartIndex + middle.length());

        if (endStartIndex < 0 || reasonStartIndex < 0) {
            return null;
        }

        String startDate = message.substring(startIndex, endStartIndex).trim();
        String endDate = message.substring(endStartIndex + middle.length(), reasonStartIndex).trim();
        String reason = message.substring(reasonStartIndex + reasonPrefix.length()).trim();

        String formattedStartDate = formatDateTimeForDisplay(startDate);
        String formattedEndDate = formatDateTimeForDisplay(endDate);

        StringBuilder translated = new StringBuilder();

        translated.append("חשבונך הושעה.");

        if (!formattedStartDate.isBlank()) {
            translated.append("\nמ־").append(formattedStartDate);
        }

        if (!formattedEndDate.isBlank()) {
            translated.append(" עד ").append(formattedEndDate);
        } else {
            translated.append(" ללא תאריך סיום");
        }

        translated.append(".");

        if (!reason.isBlank() && !"null".equalsIgnoreCase(reason)) {
            translated.append("\nמהסיבה הבאה: ").append(reason).append(".");
        }

        translated.append("\nבזמן הזה אפשר לצפות במידע במערכת, אך לא לבצע פעולות.");

        return translated.toString();
    }

    /**
     * Formats a technical {@link LocalDateTime} value for display in user-facing notifications.
     *
     * <p>For example, the value {@code 2026-06-26T18:44:00} is displayed as
     * {@code 26/06/2026 18:44}.</p>
     *
     * <p>If the value cannot be parsed as a standard {@link LocalDateTime}, the method falls
     * back to a readable cleanup by removing fractions/seconds where possible and replacing
     * {@code T} with a space.</p>
     *
     * @param value the raw date-time value from the notification message
     * @return a readable date-time string, or an empty string if the value is missing
     */
    private static String formatDateTimeForDisplay(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            return "";
        }

        String normalized = value.trim();

        try {
            return LocalDateTime.parse(normalized).format(DISPLAY_DATE_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            // Fall back to a readable representation if the value is not a standard LocalDateTime.
        }

        int dotIndex = normalized.indexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(0, dotIndex);
        }

        normalized = normalized.replace('T', ' ');

        if (normalized.length() >= 16) {
            normalized = normalized.substring(0, 16);
        }

        return normalized;
    }
}