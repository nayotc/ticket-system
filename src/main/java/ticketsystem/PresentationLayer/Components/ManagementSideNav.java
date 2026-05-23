package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class ManagementSideNav extends Div {

    private String companyId = "1";
    private final Div links = new Div();

    public ManagementSideNav() {
        addClassName("management-side-nav");

        add(createHeader(), createCreateEventButton(), links);

        links.addClassName("management-side-nav-links");
        rebuildLinks();
    }

    public void setCompanyId(String companyId) {
        if (companyId != null && !companyId.isBlank()) {
            this.companyId = companyId;
            rebuildLinks();
        }
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("management-side-nav-header");

        Span title = new Span("Producer Hub");
        title.addClassName("management-side-nav-title");

        Span subtitle = new Span("Manage your events");
        subtitle.addClassName("management-side-nav-subtitle");

        header.add(title, subtitle);
        return header;
    }

    private Button createCreateEventButton() {
        Button button = new Button("יצירת אירוע חדש");
        button.addClassName("management-create-button");
        button.addClickListener(event -> navigate(UiRoutes.CREATE_EVENT));
        return button;
    }

    private void rebuildLinks() {
        links.removeAll();

        links.add(
                navButton("ניהול חברה", UiRoutes.COMPANY_MANAGEMENT),
                navButton("עורך מדיניות", UiRoutes.POLICIES_EDITOR),
                navButton("דוח מכירות", UiRoutes.SALES_REPORT),
                navButton("עץ תפקידים והרשאות", UiRoutes.ROLES_AND_PERMISSIONS_TREE),
                navButton("יצירת אירוע", UiRoutes.CREATE_EVENT)
        );
    }

    private Button navButton(String text, String routeTemplate) {
        Button button = new Button(text);
        button.addClassName("management-nav-item");
        button.addClickListener(event -> navigate(routeTemplate));
        return button;
    }

    private void navigate(String routeTemplate) {
        String route = routeTemplate.replace(":companyId", companyId);
        UI.getCurrent().navigate(route);
    }
}