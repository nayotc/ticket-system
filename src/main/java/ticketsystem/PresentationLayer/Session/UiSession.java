package ticketsystem.PresentationLayer.Session;

import com.vaadin.flow.server.VaadinSession;

/**
 * Holds UI-level session data for the current Vaadin session.
 *
 * This class does not create or validate tokens. Token creation, validation,
 * and invalidation are handled by the Application Layer through TokenService.
 *
 * UiSession only stores the token that the Presentation Layer should use when
 * calling application services:
 * - guestToken: used before login, for guest flows such as sign-up/login.
 * - memberToken: used after successful login, for member actions.
 */
public final class UiSession {

    private UiSession() {
    }

    public static final String GUEST_TOKEN = "guestToken";
    public static final String MEMBER_TOKEN = "memberToken";
    public static final String NOTIFICATION_TARGET_ID = "notificationTargetId";

    /**
     * Starts a guest UI session.
     *
     * Used after visitSystem creates a guest token. A guest session clears any
     * existing member token because the current UI state is no longer logged-in.
     *
     * @param guestToken active guest session token returned from UserService
     */
    public static void startGuestSession(String guestToken) {
        if (guestToken == null || guestToken.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(GUEST_TOKEN, guestToken);
        session.setAttribute(MEMBER_TOKEN, null);
        session.setAttribute(NOTIFICATION_TARGET_ID, guestToken);
    }

    /**
     * Marks the current UI session as logged-in.
     *
     * Used after a successful login. The member token becomes the active token,
     * and the old guest token is cleared.
     *
     * @param memberToken active member session token returned from UserService
     */
    public static void login(String memberToken, String notificationTargetId) {
        if (memberToken == null || memberToken.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(MEMBER_TOKEN, memberToken);
        session.setAttribute(GUEST_TOKEN, null);

        if (notificationTargetId == null || notificationTargetId.isBlank()) {
            session.setAttribute(NOTIFICATION_TARGET_ID, null);
        } else {
            session.setAttribute(NOTIFICATION_TARGET_ID, notificationTargetId);
        }
    }

    /**
     * Converts the current UI session back to a guest session after logout.
     *
     * Used after UserService.logOut returns a new guest token. The member token
     * is cleared and the new guest token is stored.
     *
     * @param guestToken new guest token returned after logout
     */
    public static void logoutToGuest(String guestToken) {
        if (guestToken == null || guestToken.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(MEMBER_TOKEN, null);
        session.setAttribute(GUEST_TOKEN, guestToken);
        session.setAttribute(NOTIFICATION_TARGET_ID, guestToken);
    }

    /**
     * Backward-compatible logout helper.
     *
     * This method only clears the member token and does not store a new guest
     * token. Prefer logoutToGuest when UserService returns a new guest token.
     */
    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session != null) {
            session.setAttribute(MEMBER_TOKEN, null);
            session.setAttribute(NOTIFICATION_TARGET_ID, null);
        }
    }

    /**
     * Clears all UI session tokens.
     *
     * Used when the user exits the system completely.
     */
    public static void exit() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session != null) {
            session.setAttribute(MEMBER_TOKEN, null);
            session.setAttribute(GUEST_TOKEN, null);
            session.setAttribute(NOTIFICATION_TARGET_ID, null);
        }
    }


    /**
     * @return true if the current UI session has a member token
     */
    public static boolean isLoggedIn() {
        return getMemberToken() != null;
    }

    /**
     * Returns the token that should be used for application-service calls.
     *
     * Member token has priority because a logged-in member should act as a
     * member. If there is no member token, the guest token is returned.
     *
     * @return member token if logged in, otherwise guest token, or null if none exists
     */
    public static String getCurrentToken() {
        String memberToken = getMemberToken();

        if (memberToken != null) {
            return memberToken;
        }

        return getGuestToken();
    }

    /**
     * @return current guest token, or null if no valid guest token is stored
     */
    public static String getGuestToken() {
        return getToken(GUEST_TOKEN);
    }

    /**
     * @return current member token, or null if no valid member token is stored
     */
    public static String getMemberToken() {
        return getToken(MEMBER_TOKEN);
    }

    /**
     * Reads a non-blank String token from the current Vaadin session.
     *
     * @param key session attribute key
     * @return stored token, or null if missing/blank/not a String
     */
    private static String getToken(String key) {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return null;
        }

        Object token = session.getAttribute(key);
        return token instanceof String value && !value.isBlank() ? value : null;
    }

    public static void setNotificationTargetId(String targetId) {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        if (targetId == null || targetId.isBlank()) {
            session.setAttribute(NOTIFICATION_TARGET_ID, null);
            return;
        }

        session.setAttribute(NOTIFICATION_TARGET_ID, targetId);
    }

    public static String getNotificationTargetId() {
        return getToken(NOTIFICATION_TARGET_ID);
    }

    public static boolean hasGuestSession() {
        return getGuestToken() != null;
    }
}
