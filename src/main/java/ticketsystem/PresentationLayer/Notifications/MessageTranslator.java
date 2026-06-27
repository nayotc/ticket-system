package ticketsystem.PresentationLayer.Notifications;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageTranslator {

    private static final Map<String, String> EXACT_TRANSLATIONS = new LinkedHashMap<>();

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
                "פג תוקף שריון הכרטיסים. הכרטיסים שוחררו וניתן לבחור אותם מחדש."
        );

        EXACT_TRANSLATIONS.put(
                "Your active order has expired. Please select tickets again.",
                "פג תוקף שריון הכרטיסים. יש לבחור כרטיסים מחדש."
        );

        EXACT_TRANSLATIONS.put(
                "Your active order is about to expire. Please complete your purchase soon.",
                "ההזמנה הפעילה שלך עומדת לפוג. יש להשלים את הרכישה בהקדם."
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

        String translated = translateProductionCompanyMessage(trimmed);
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
}