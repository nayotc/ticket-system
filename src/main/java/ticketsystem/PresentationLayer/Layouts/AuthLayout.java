package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class AuthLayout extends PublicLayout {

    @Autowired
    public AuthLayout(UiVisitCoordinator visitCoordinator) {
        super(visitCoordinator);
    }

    @Override
    protected boolean shouldShowAuthAction() {
        return false;
    }
}