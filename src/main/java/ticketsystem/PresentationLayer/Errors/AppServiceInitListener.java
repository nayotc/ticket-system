package ticketsystem.PresentationLayer.Errors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import org.springframework.stereotype.Component;

import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

/**
 * A Vaadin service initialization listener that automatically registers the
 * CustomErrorHandler for every newly created user session and wires browser-exit
 * cleanup for the active UI session token.
 */
@Component
public class AppServiceInitListener implements VaadinServiceInitListener {

    private final CustomErrorHandler customErrorHandler;
    private final UiVisitCoordinator visitCoordinator;

    public AppServiceInitListener(CustomErrorHandler customErrorHandler,
                                 UiVisitCoordinator visitCoordinator) {
        this.customErrorHandler = customErrorHandler;
        this.visitCoordinator = visitCoordinator;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(initEvent -> {
            initEvent.getSession().setErrorHandler(customErrorHandler);
        });

        event.getSource().addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();
            visitCoordinator.registerBrowserExitHook(ui);
            visitCoordinator.syncBrowserExitToken(ui);
        });
    }
}
