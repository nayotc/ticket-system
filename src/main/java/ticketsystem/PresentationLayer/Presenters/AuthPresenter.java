package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.PresentationLayer.Session.UiSession;

/**
 * Presenter for authentication and session actions.
 *
 * Handles visit, sign-up, login, logout, and exit flows.
 * This presenter translates UserService failures into PresentationException,
 * and updates UiSession when the active UI token changes.
 */
@Component
public class AuthPresenter {
    private final UserService userService;
    private final ITokenService tokenService;

    public AuthPresenter(UserService userService, ITokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    public String visitSystem() {
        try {
            String guestToken = userService.visitSystem();

            if (guestToken == null || guestToken.isBlank()) {
                throw new IllegalStateException("Failed to start guest session. Please try again.");
            }

            UiSession.startGuestSession(guestToken);
            return guestToken;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Failed to start guest session. Please try again.");
        }
    }

    public boolean signUp(String username, String password, String fullName, String phone) {
        try {
            String guestToken = UiSession.getGuestToken();

            if (guestToken == null) {
                throw new PresentationException("Guest session is missing. Please refresh and try again.");
            }

            userService.signUp(guestToken, username, password, fullName, phone);
            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
        throw new PresentationException(translateSignUpError(e.getMessage()));
    } catch (Exception e) {
            throw new PresentationException("Registration failed. Please try again.");
        }
    }



    public String login(String guestToken, String username, String password) {
        try {

            if (guestToken == null) {
                throw new PresentationException("Guest session is missing. Please refresh and try again.");
            }

            String memberToken = userService.login(guestToken, username, password);

            if (memberToken == null || memberToken.isBlank()) {
                throw new IllegalStateException("Login failed. Please try again.");
            }

            UiSession.login(memberToken, tokenService.extractUserId(memberToken).toString());
            return memberToken;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Login failed. Please try again.");
        }
    }

    public String logOut(String memberToken) {
        try {

            if (memberToken == null) {
                throw new PresentationException("You are not logged in.");
            }

            String guestToken = userService.logOut(memberToken);

            if (guestToken == null || guestToken.isBlank()) {
                throw new IllegalStateException("Logout failed. Please try again.");
            }

            UiSession.logoutToGuest(guestToken);
            return guestToken;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Logout failed. Please try again.");
        }
    }

    public boolean exit() {
        try {
            String currentToken = UiSession.getCurrentToken();

            if (currentToken == null) {
                throw new PresentationException("No active session found.");
            }

            userService.exit(currentToken);
            UiSession.exit();
            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Exit failed. Please try again.");
        }
    }

    private String translateSignUpError(String message) {
        if (message == null || message.isBlank()) {
            return "ההרשמה נכשלה. נסו שוב.";
        }

        return switch (message) {
            case "Username and password are required." -> "יש להזין אימייל וסיסמה.";
            case "Only guests can sign up." -> "לא ניתן להירשם כאשר כבר מחוברים למערכת.";
            case "Username is already taken." -> "האימייל כבר קיים במערכת.";
            case "Full name is required." -> "יש להזין שם מלא.";
            case "Full name must be between 2 and 100 characters." -> "השם המלא חייב להכיל בין 2 ל־100 תווים.";
            case "Phone number is required." -> "יש להזין מספר טלפון.";
            case "Phone number must contain digits only." -> "מספר הטלפון יכול להכיל ספרות בלבד.";
            case "Phone number must be 9 or 10 digits long." -> "מספר הטלפון חייב להכיל 9 או 10 ספרות.";
            default -> message;
        };
    }
}
