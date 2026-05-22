package ticketsystem.PresentationLayer.Session;

import com.vaadin.flow.server.VaadinSession;

public final class UiSession {

    private UiSession() {
    }

    public static final String MEMBER_TOKEN = "memberToken";

    public static boolean isLoggedIn() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && session.getAttribute(MEMBER_TOKEN) != null;
    }

    public static void login(String memberToken) {
        if (memberToken == null || memberToken.isBlank()) {
            return;
        }

        VaadinSession.getCurrent().setAttribute(MEMBER_TOKEN, memberToken);
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session != null) {
            session.setAttribute(MEMBER_TOKEN, null);
        }
    }

    public static String getMemberToken() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return null;
        }

        Object token = session.getAttribute(MEMBER_TOKEN);
        return token instanceof String ? (String) token : null;
    }
}