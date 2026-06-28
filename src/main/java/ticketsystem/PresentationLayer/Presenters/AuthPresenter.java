package ticketsystem.PresentationLayer.Presenters;

import java.time.LocalDate;

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
    private static final String VISIT_ERROR = "לא ניתן להתחיל ביקור במערכת. נסו לרענן את העמוד.";
    private static final String SIGN_UP_ERROR = "ההרשמה נכשלה. בדקו את הפרטים ונסו שוב.";
    private static final String LOGIN_ERROR = "ההתחברות נכשלה. בדקו את פרטי ההתחברות ונסו שוב.";
    private static final String LOGOUT_ERROR = "ההתנתקות נכשלה. נסו לרענן את העמוד.";
    private static final String EXIT_ERROR = "לא ניתן לסיים את הביקור כרגע. נסו לרענן את העמוד.";

    public AuthPresenter(UserService userService, ITokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    public String visitSystem() {
        try {
            String guestToken = userService.visitSystem();

            if (guestToken == null || guestToken.isBlank()) {
                throw new PresentationException(VISIT_ERROR);
            }

            UiSession.startGuestSession(guestToken);
            return guestToken;
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException(VISIT_ERROR);
        }
    }

    public void ensureGuestSession() {
        if (UiSession.isLoggedIn()) {
            return;
        }

        String guestToken = UiSession.getGuestToken();
        if (guestToken == null
                || guestToken.isBlank()
                || !tokenService.isActiveSession(guestToken)) {
            visitSystem();
        }
    }

    public boolean signUp(String username, String password, String fullName, String phone, LocalDate birthDate) {
        try {
            String guestToken = UiSession.getGuestToken();

            if (guestToken == null || guestToken.isBlank()) {
                throw new PresentationException("החיבור למערכת פג. נא לרענן את העמוד ולנסות שוב.");
            }

            userService.signUp(guestToken, username, password, fullName, phone, birthDate);

            return true;
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateSignUpError(e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(SIGN_UP_ERROR);
        }
    }



    public String login(String guestToken, String username, String password) {
        try {
            if (guestToken == null || guestToken.isBlank()) {
                throw new PresentationException("החיבור למערכת פג. נא לרענן את העמוד ולנסות שוב.");
            }

            String memberToken = userService.login(guestToken, username, password);

            if (memberToken == null || memberToken.isBlank()) {
                throw new PresentationException(LOGIN_ERROR);
            }

            Long memberId = tokenService.extractUserId(memberToken);

            if (memberId == null) {
                throw new PresentationException(LOGIN_ERROR);
            }

            UiSession.login(memberToken, memberId.toString());
            return memberToken;
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateLoginError(e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(LOGIN_ERROR);
        }
    }

    public String logOut(String memberToken) {
        try {
            if (memberToken == null || memberToken.isBlank()) {
                throw new PresentationException("אינכם מחוברים למערכת.");
            }

            String guestToken = userService.logOut(memberToken);

            if (guestToken == null || guestToken.isBlank()) {
                throw new PresentationException(LOGOUT_ERROR);
            }

            UiSession.logoutToGuest(guestToken);
            return guestToken;
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateSessionError(e.getMessage(), LOGOUT_ERROR));
        } catch (Exception e) {
            throw new PresentationException(LOGOUT_ERROR);
        }
    }

    public boolean exit() {
        try {
            String currentToken = UiSession.getCurrentToken();

            if (currentToken == null || currentToken.isBlank()) {
                throw new PresentationException("לא נמצא ביקור פעיל במערכת.");
            }

            userService.exit(currentToken);
            UiSession.exit();

            return true;
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateSessionError(e.getMessage(), EXIT_ERROR)
            );
        } catch (Exception e) {
            throw new PresentationException(EXIT_ERROR);
        }
    }

    private String translateSignUpError(String message) {
        if (message == null || message.isBlank()) {
            return SIGN_UP_ERROR;
        }

        return switch (message) {
            case "Username and password are required." -> "יש להזין אימייל וסיסמה.";
            case "Only guests can sign up." -> "לא ניתן להירשם כאשר כבר מחוברים למערכת.";
            case "Username is already taken." -> "האימייל כבר קיים במערכת.";
            case "Password must be at least 5 characters long." -> "הסיסמה חייבת להכיל לפחות 5 תווים.";
            case "Birth date is required." -> "יש להזין תאריך לידה.";
            case "Full name is required." -> "יש להזין שם מלא.";
            case "Full name must be between 2 and 100 characters." -> "השם המלא חייב להכיל בין 2 ל־100 תווים.";
            case "Phone number is required." -> "יש להזין מספר טלפון.";
            case "Phone number must contain digits only." -> "מספר הטלפון יכול להכיל ספרות בלבד.";
            case "Phone number must be 9 or 10 digits long." -> "מספר הטלפון חייב להכיל 9 או 10 ספרות.";
            default -> SIGN_UP_ERROR;
        };
    }

    private String translateLoginError(String message) {
        if (message == null || message.isBlank()) {
            return LOGIN_ERROR;
        }

        return switch (message) {
            case "Username and password are required." -> "יש להזין אימייל וסיסמה.";
            case "Invalid username or password." -> "האימייל או הסיסמה שגויים.";
            case "Only guests can log in." -> "כבר מחוברים למערכת.";
            case "This account has been deactivated.\n" + "Please contact support." -> "החשבון הושבת. לא ניתן להתחבר למערכת.";
            case "Login failed.\nPlease try again.", "Login failed. Please try again." -> LOGIN_ERROR;
            default -> translateSessionError(message, LOGIN_ERROR);
        };
    }

    private String translateSessionError(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        String normalized = message.toLowerCase();

        if (normalized.contains("token")
                || normalized.contains("session")
                || normalized.contains("expired")
                || normalized.contains("not active")) {
            return "החיבור למערכת פג. נא לרענן את העמוד ולהתחבר מחדש.";
        }

        if (message.equals("Only logged-in members can log out.")) {
            return "אינכם מחוברים למערכת.";
        }

        return fallback;
    }
}
