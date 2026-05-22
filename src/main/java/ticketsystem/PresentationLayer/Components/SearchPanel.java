package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

public class SearchPanel extends AppCard {

    private final TextField freeText = new TextField("חיפוש חופשי");
    private final ComboBox<String> location = new ComboBox<>("אזור");
    private final ComboBox<String> date = new ComboBox<>("תאריך");
    private final ComboBox<String> category = new ComboBox<>("קטגוריה");
    private final Button searchButton = new Button("חפש אירועים");

    public SearchPanel() {
        super();

        addClassName("search-panel");

        freeText.setPlaceholder("שם אירוע או אמן...");
        freeText.setPrefixComponent(VaadinIcon.SEARCH.create());

        location.setItems("כל האזורים", "תל אביב והמרכז", "ירושלים", "צפון", "דרום");
        location.setValue("כל האזורים");

        date.setItems("מתי?", "היום", "בסופ\"ש הקרוב", "החודש");
        date.setValue("מתי?");

        category.setItems("כל הקטגוריות", "הופעות חיות", "תיאטרון וסטנדאפ", "ספורט");
        category.setValue("כל הקטגוריות");

        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.setIcon(VaadinIcon.SEARCH.create());

        HorizontalLayout fields = new HorizontalLayout(freeText, location, date, category);
        fields.addClassName("search-fields");
        fields.setWidthFull();

        Div actions = new Div(searchButton);
        actions.addClassName("search-actions");

        add(fields, actions);
    }

    public TextField getFreeText() {
        return freeText;
    }

    public ComboBox<String> getLocation() {
        return location;
    }

    public ComboBox<String> getDate() {
        return date;
    }

    public ComboBox<String> getCategory() {
        return category;
    }

    public Button getSearchButton() {
        return searchButton;
    }
}
