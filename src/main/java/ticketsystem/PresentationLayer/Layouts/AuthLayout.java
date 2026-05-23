package ticketsystem.PresentationLayer.Layouts;

public class AuthLayout extends PublicLayout {

    public AuthLayout() {
        addClassName("auth-layout");
        getContentContainer().addClassName("auth-content");
    }

    @Override
    protected boolean shouldShowAuthAction() {
        return false;
    }
}