package ticketsystem.PresentationLayer.Errors;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * A Vaadin service initialization listener that automatically registers the
 * CustomErrorHandler for every newly created user session. This ensures the
 * global error handling mechanism is active throughout the application lifecycle
 * for all users.
 */
@Component
public class AppServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(initEvent -> {
            initEvent.getSession().setErrorHandler(new CustomErrorHandler());
        });
    }
}