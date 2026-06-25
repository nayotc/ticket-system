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
                throw new PresentationException("עדכון שם המשתמש נכשל. אנא בדקו את הפרטים נסו שוב.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateUserError(msg,
                    "עדכון שם המשתמש נכשל. אנא נסו שוב."
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateUserError(msg,
                    "אירעה שגיאה במהלך עדכון שם המשתמש. אנא נסו שוב."
                ));
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
            throw PresentationException.dispatch(e, 
                msg -> translateUserError(msg,
                    "עדכון הסיסמה נכשל. אנא נסו שוב."
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateUserError(msg,
                    "אירעה שגיאה במהלך עדכון הסיסמה. אנא נסו שוב."
                ));
        }
    }

    private String translateUserError(String message, String fallbackMessage) {
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        String cleanMessage = message.trim();

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
}
