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
import ticketsystem.PresentationLayer.Layouts.AdminLayout;

@PageTitle("Admin Preview")
@Route(value = UiRoutes.UI_PREVIEW_ADMIN, layout = AdminLayout.class)
public class AdminPreview extends PageContainer {

    public AdminPreview() {
        addClassName("preview-page");

        Button suspendUser = new Button("השהיית משתמש");
        suspendUser.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(
                new ViewHeader(
                        "לוח ניהול מערכת",
                        "תצוגה מקדימה למסך מנהל מערכת.",
                        suspendUser
                ),
                createMetrics(),
                createAdminActions()
        );
    }

    private Div createMetrics() {
        Div grid = new Div();
        grid.addClassName("preview-grid");

        grid.add(
                new MetricCard("משתמשים פעילים", "2,341", "128 התחברו היום"),
                new MetricCard("משתמשים מושהים", "12", "3 השעיות זמניות"),
                new MetricCard("התראות מערכת", "46", "8 דורשות טיפול")
        );

        return grid;
    }

    private FormCard createAdminActions() {
        Button viewSuspensions = new Button("צפייה בהשהיות");
        Button releaseUser = new Button("ביטול השהייה");

        return new FormCard(
                "פעולות מנהל מערכת",
                "בדיקה של כרטיס פעולות תחת AdminLayout.",
                new StatusBadge("מערכת פעילה", StatusBadge.Type.SUCCESS),
                viewSuspensions,
                releaseUser
        );
    }
}