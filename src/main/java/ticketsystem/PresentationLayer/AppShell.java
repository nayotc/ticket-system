package ticketsystem.PresentationLayer;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;


@Theme(value = "ticket-system", variant = Lumo.DARK)
@Push
public class AppShell implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.setViewport("width=device-width, initial-scale=1.0");
        settings.setPageTitle("TixNow");
    }
}