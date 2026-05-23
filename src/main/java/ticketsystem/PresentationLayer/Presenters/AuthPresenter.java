package ticketsystem.PresentationLayer.Presenters;

import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.PresentationLayer.Session.UiSession;

/**
 * Presenter for authentication and session actions.
 *
 * Handles visit, sign-up, login, logout, and exit flows.
 * This presenter translates UserService failures into PresentationException,
 * and updates UiSession when the active UI token changes.
 */
public class AuthPresenter {
    private final UserService userService;

    public AuthPresenter(UserService userService) {
        this.userService = userService;
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

    public boolean signUp(String username, String password) {
        try {
            String guestToken = UiSession.getGuestToken();

            if (guestToken == null) {
                throw new PresentationException("Guest session is missing. Please refresh and try again.");
            }

            userService.signUp(guestToken, username, password);
            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Registration failed. Please try again.");
        }
    }

    public String login(String username, String password) {
        try {
            String guestToken = UiSession.getGuestToken();

            if (guestToken == null) {
                throw new PresentationException("Guest session is missing. Please refresh and try again.");
            }

            String memberToken = userService.login(guestToken, username, password);

            if (memberToken == null || memberToken.isBlank()) {
                throw new IllegalStateException("Login failed. Please try again.");
            }

            UiSession.login(memberToken);
            return memberToken;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Login failed. Please try again.");
        }
    }

    public String logOut() {
        try {
            String memberToken = UiSession.getMemberToken();

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
}
