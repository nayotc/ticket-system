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
                throw new PresentationException("Username update failed. Please check your details and try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Username update failed. Please try again.");
        }
    }

    public boolean updateMemberPassword(String sessionToken, String password, String username, String newPassword) {
        try {
            boolean updated = userService.updateMemberPassword(sessionToken, password, username, newPassword);

            if (!updated) {
                throw new PresentationException("Password update failed. Please check your details and try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Password update failed. Please try again.");
        }
    }
}
