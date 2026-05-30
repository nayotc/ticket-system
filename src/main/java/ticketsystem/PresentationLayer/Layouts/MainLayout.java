package ticketsystem.PresentationLayer.Layouts;

import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class MainLayout extends PublicLayout {

    @Autowired
    public MainLayout(UiVisitCoordinator visitCoordinator) {
        super(visitCoordinator);
        addClassName("main-layout");
    }
}