package ticketsystem.PresentationLayer.Session;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import ticketsystem.PresentationLayer.Components.Notifications;

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
    private static final String LOTTERY_CODE_PREFIX = "lotteryCode:";
    private static final String COUPON_CODE_PREFIX = "couponCode:";

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

    public static void setLotteryCode(Long eventId, String lotteryCode) {
        if (eventId == null || lotteryCode == null || lotteryCode.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(LOTTERY_CODE_PREFIX + eventId, lotteryCode.trim());
    }

    public static String getLotteryCode(Long eventId) {
        if (eventId == null) {
            return null;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(LOTTERY_CODE_PREFIX + eventId);
        return value instanceof String code && !code.isBlank() ? code : null;
    }

    public static void clearLotteryCode(Long eventId) {
        if (eventId == null) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(LOTTERY_CODE_PREFIX + eventId, null);
    }

    /**
     * Stores the coupon code associated with a specific active order.
     *
     * <p>The code is stored only in the current Vaadin UI session. This method
     * does not validate whether the coupon exists or was applied; callers should
     * store the code only after a successful pricing calculation confirms that a
     * coupon discount was applied.</p>
     *
     * @param orderId    active order identifier
     * @param couponCode coupon code confirmed by the pricing calculation
     */
    public static void setCouponCode(Long orderId, String couponCode) {
        if (orderId == null || couponCode == null || couponCode.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(
                COUPON_CODE_PREFIX + orderId,
                couponCode.trim()
        );
    }

    /**
     * Returns the coupon code stored for a specific active order.
     *
     * @param orderId active order identifier
     * @return stored non-blank coupon code, or {@code null} when none exists
     */
    public static String getCouponCode(Long orderId) {
        if (orderId == null) {
            return null;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(COUPON_CODE_PREFIX + orderId);

        return value instanceof String code && !code.isBlank()
                ? code
                : null;
    }

    /**
     * Removes the coupon code stored for a specific active order.
     *
     * <p>This should be called when the user clears the coupon input and applies
     * the change, or when the associated order is completed and should no longer
     * retain pricing input in the UI session.</p>
     *
     * @param orderId active order identifier
     */
    public static void clearCouponCode(Long orderId) {
        if (orderId == null) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        session.setAttribute(COUPON_CODE_PREFIX + orderId, null);
    }

    /**
     * Handles the redirection or state reset when a session/token timeout occurs.
     *
     * - For logged-in members: Logs them out (clears UI session) and redirects them
     * to the login page with a timeout parameter to display an appropriate message.
     * - For guests: Clears the stale guest token from the UI session but DOES NOT
     * reload the page. This prevents data loss (e.g., in registration forms).
     * Instead, it displays a gentle notification prompting the user to try again.
     */
    public static void handleTimeoutRedirect() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                boolean wasLoggedIn = isLoggedIn();

                // Clear all tokens from the Vaadin session
                exit();

                if (wasLoggedIn) {
                    // Member: redirect to login to secure their account
                    ui.getPage().setLocation("/" + ticketsystem.PresentationLayer.Constants.UiRoutes.LOGIN + "?timeout=true");
                } else {
                    // Guest: keep them on the page to prevent losing typed data,
                    // just show a notification. The next button click will
                    // generate a fresh guest token dynamically.
                    Notifications.info("תוקף החיבור פג. אנא נסו ללחוץ שוב כדי להמשיך.");
                }
            });
        }
    }
}