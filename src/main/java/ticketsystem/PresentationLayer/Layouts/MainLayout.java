package ticketsystem.PresentationLayer.Layouts;

import org.springframework.beans.factory.annotation.Autowired;

import ticketsystem.PresentationLayer.Components.PublicHeader;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class MainLayout extends PublicLayout {

    @Autowired
    public MainLayout(UiVisitCoordinator visitCoordinator, PublicHeader.HeaderPresenter headerPresenter) {
        super(visitCoordinator, headerPresenter);
        addClassName("main-layout");
    }
}