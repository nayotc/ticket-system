package ticketsystem.PresentationLayer.Views.Preview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;

@PageTitle("Management Preview")
@Route(value = UiRoutes.UI_PREVIEW_MANAGEMENT, layout = ManagementLayout.class)
public class ManagementPreview extends PageContainer {

    public ManagementPreview() {
        addClassName("preview-page");

        Button createEvent = new Button("יצירת אירוע");
        createEvent.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(
                new ViewHeader(
                        "ניהול חברה",
                        "תצוגה מקדימה ל־ManagementLayout, SideNav ורכיבי ניהול.",
                        createEvent
                ),
                createMetrics(),
                createCompanyActions()
        );
    }

    private Div createMetrics() {
        Div grid = new Div();
        grid.addClassName("preview-grid");

        grid.add(
                new MetricCard("מכירות היום", "₪8,420", "34 רכישות"),
                new MetricCard("אירועים פעילים", "7", "2 נמכרו כמעט לגמרי"),
                new MetricCard("מנהלים", "4", "כולל בעלים אחד")
        );

        return grid;
    }

    private FormCard createCompanyActions() {
        Button editPolicies = new Button("עריכת מדיניות");
        Button openRoles = new Button("עץ תפקידים");
        Button salesReport = new Button("דוח מכירות");

        return new FormCard(
                "פעולות ניהול",
                "כאן אפשר לראות איך כפתורים וכרטיסים נראים בתוך אזור הניהול.",
                new StatusBadge("חברה פעילה", StatusBadge.Type.SUCCESS),
                editPolicies,
                openRoles,
                salesReport
        );
    }
}