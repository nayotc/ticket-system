package ticketsystem.PresentationLayer.Views.Preview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.PresentationLayer.Components.ActionBar;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;

@PageTitle("UI Preview")
@Route(value = UiRoutes.UI_PREVIEW, layout = MainLayout.class)

public class MainPreview extends PageContainer {
    public MainPreview() {
        addClassName("preview-page");

        Button primaryButton = new Button("פעולה ראשית");
        primaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button secondaryButton = new Button("פעולה משנית");

        add(
                new ViewHeader(
                        "תצוגה מקדימה לרכיבי UI",
                        "עמוד זמני לבדיקת רכיבים משותפים, צבעים, ריווחים, כפתורים וטפסים.",
                        primaryButton,
                        secondaryButton
                ),
                createMetricSection(),
                createStatusSection(),
                createFormExample(),
                createEmptyStateExample()
        );
    }

    private Div createMetricSection() {
        Div grid = new Div();
        grid.addClassName("preview-grid");

        grid.add(
                new MetricCard("הכנסות", "₪42,300", "עלייה של 12% מהחודש הקודם"),
                new MetricCard("כרטיסים שנמכרו", "1,284", "312 השבוע"),
                new MetricCard("אירועים פעילים", "18", "3 אירועים חדשים")
        );

        return grid;
    }

    private Div createStatusSection() {
        Div card = new FormCard(
                "תגיות סטטוס",
                "בדיקה של כל המצבים האפשריים.",
                new StatusBadge("פעיל", StatusBadge.Type.SUCCESS),
                new StatusBadge("ממתין", StatusBadge.Type.WARNING),
                new StatusBadge("נכשל", StatusBadge.Type.ERROR),
                new StatusBadge("מידע", StatusBadge.Type.INFO),
                new StatusBadge("רגיל", StatusBadge.Type.NEUTRAL)
        );

        card.addClassName("preview-section");
        return card;
    }

    private FormCard createFormExample() {
        TextField name = new TextField("שם אירוע");
        name.setPlaceholder("לדוגמה: פסטיבל קיץ");

        ComboBox<String> category = new ComboBox<>("קטגוריה");
        category.setItems("הופעה", "ספורט", "סטנדאפ", "כנס");
        category.setPlaceholder("בחר קטגוריה");

        TextField location = new TextField("מיקום");
        location.setPlaceholder("לדוגמה: תל אביב");

        Button save = new Button("שמור");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("ביטול");

        return new FormCard(
                "דוגמת טופס",
                "כאן רואים שדות, קומבו בוקס וכפתורי פעולה.",
                name,
                category,
                location,
                new ActionBar(save, cancel)
        );
    }

    private EmptyState createEmptyStateExample() {
        Button action = new Button("צור אירוע חדש");
        action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return new EmptyState(
                "🎟️",
                "אין עדיין אירועים",
                "ברגע שתיצור אירוע חדש, הוא יופיע כאן.",
                action
        );
    }
}
