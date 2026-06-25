package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.UserService;

/**
 * Presenter for member profile actions.
 *
 * Authentication and session actions such as visitSystem, login, signUp,
 * logOut and exit should be handled by AuthPresenter.
 *
 * This presenter translates UserService failures into PresentationException,
 * so the View can display user-facing messages without interpreting service
 * return values directly.
 */
@Component
public class UserPresenter {
    private final UserService userService;

    public UserPresenter(UserService userService) {
        this.userService = userService;
    }

    public boolean updateMemberUsername(String sessionToken, String password, String username, String newUsername) {
        try {
            boolean updated = userService.updateMemberUsername(sessionToken, password, username, newUsername);

            if (!updated) {
                throw new PresentationException("עדכון שם המשתמש נכשל. אנא בדקו את הפרטים ונסו שוב.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));

        } catch (Exception e) {
            String fallback = "עדכון שם המשתמש נכשל. אנא נסו שוב.";
            throw new PresentationException(translateError(extractUsefulMessage(e), fallback));
        }
    }

    public boolean updateMemberPassword(String sessionToken, String password, String username, String newPassword) {
        try {
            boolean updated = userService.updateMemberPassword(sessionToken, password, username, newPassword);

            if (!updated) {
                throw new PresentationException("עדכון הסיסמה נכשל. אנא בדקו את הפרטים ונסו שוב.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));

        } catch (Exception e) {
            String fallback = "עדכון הסיסמה נכשל. אנא נסו שוב.";
            throw new PresentationException(translateError(extractUsefulMessage(e), fallback));
        }
    }

    private String translateError(String message, String fallbackMessage) {
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        String cleanMessage = message.trim();

        if (PresentationException.isDbDisconnectMessage(cleanMessage)) {
            return PresentationException.DB_DISCONNECT_HEBREW_MSG;
        }

        if (PresentationException.isSessionTimeoutMessage(cleanMessage)) {
            return PresentationException.SESSION_TOKEN_EXPIRED;
        }

        return switch (cleanMessage) {
            case "Username update failed. Please check your details and try again." ->
                    "עדכון שם המשתמש נכשל. אנא בדקו את הפרטים ונסו שוב.";

            case "Password update failed. Please check your details and try again." ->
                    "עדכון הסיסמה נכשל. אנא בדקו את הפרטים ונסו שוב.";

            case "Incorrect password", "Incorrect current password." ->
                    "הסיסמה הנוכחית שהוזנה שגויה.";

            case "Username already taken", "New username is already taken." ->
                    "שם המשתמש החדש כבר תפוס על ידי משתמש אחר.";

            case "New username cannot be the same as the old one." ->
                    "שם המשתמש החדש זהה לשם המשתמש הנוכחי.";

            case "New password cannot be the same as the old one." ->
                    "הסיסמה החדשה אינה יכולה להיות זהה לסיסמה הנוכחית.";

            case "User not found.", "Member not found." ->
                    "המשתמש לא נמצא במערכת.";

            default -> fallbackMessage;
        };
    }

    /**
     * חולצת את השגיאה המקורית ממעמקי ה-Stack Trace כדי לזהות ניתוקי DB.
     */
    private String extractUsefulMessage(Exception exception) {
        if (exception == null) {
            return "";
        }

        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }

        return exception.getMessage() != null ? exception.getMessage() : "";
    }
}
