package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.PoliciesEditorPresenter;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = UiRoutes.POLICIES_EDITOR, layout = ManagementLayout.class)
public class PoliciesEditor extends Div implements BeforeEnterObserver {

    private String companyId;

    private final PoliciesEditorPresenter presenter;

    private final List<PurchaseRuleDTO> purchaseRules = new ArrayList<>();
    private final List<DiscountDTO> discounts = new ArrayList<>();

    private final ComboBox<LogicalOperator> purchaseRootOperator = new ComboBox<>();
    private DiscountCompositionStrategy discountCompositionStrategy = DiscountCompositionStrategy.MAXIMUM;

    private final Div purchaseRulesContainer = new Div();
    private final Div discountsContainer = new Div();

    private final Button maximumDiscountButton = new Button("מקסימום");
    private final Button sumDiscountButton = new Button("סכום");

    @Autowired
    public PoliciesEditor(PoliciesEditorPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("policy-editor-page");

        Button resetButton = createSecondaryButton("בטל שינויים", "↺");
        resetButton.addClickListener(event -> resetDraft());

        Button saveButton = createPrimaryButton("שמור מדיניות", "✓");
        saveButton.addClickListener(event -> saveDraft());

        ViewHeader header = new ViewHeader(
                "עורך מדיניות",
                "הגדרת כללי רכישה, מגבלות רכישה, הנחות והרכבת הנחות עבור חברת ההפקה.",
                resetButton,
                saveButton
        );

        Div grid = new Div(createPurchasePolicySection(), createDiscountPolicySection());
        grid.addClassName("policy-editor-grid");

        add(new PageContainer(header, grid));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyId = event.getRouteParameters()
                .get("companyId")
                .orElse("1");

        loadDraftForCompany(companyId);
    }

    private Component createPurchasePolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-purchase");

        card.add(
                createPolicyAccent("purchase"),
                createPolicyTitle("⚖", "מדיניות רכישה"),
                paragraph("הגדר תנאים שחייבים להתקיים כדי לאשר רכישה. ההרכבה תומכת ב־AND ו־OR ומוכנה לעומק נוסף בהמשך."),
                createPurchaseRuleBuilder(),
                createAddRuleButton()
        );

        return card;
    }

    private Component createDiscountPolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-discount");

        card.add(
                createPolicyAccent("discount"),
                createPolicyTitle("%", "מדיניות הנחות"),
                createDiscountCompositionSelector(),
                discountsContainer,
                createAddDiscountButton()
        );

        discountsContainer.addClassName("discounts-list");
        return card;
    }

    private Component createPurchaseRuleBuilder() {
        Div builder = new Div();
        builder.addClassName("purchase-rule-builder");

        purchaseRootOperator.setItems(LogicalOperator.values());
        purchaseRootOperator.setItemLabelGenerator(LogicalOperator::getLabel);
        purchaseRootOperator.setValue(LogicalOperator.AND);
        purchaseRootOperator.addClassName("purchase-root-operator");
        purchaseRootOperator.addValueChangeListener(event -> {
            // TODO: When the Presenter is connected, update the root operator in the policy draft DTO.
        });

        Div operatorWrapper = new Div(purchaseRootOperator);
        operatorWrapper.addClassName("purchase-root-operator-wrapper");

        Div line = new Div();
        line.addClassName("policy-tree-line");

        purchaseRulesContainer.addClassName("purchase-rules-list");

        builder.add(operatorWrapper, line, purchaseRulesContainer);
        return builder;
    }

    private Component createDiscountCompositionSelector() {
        Div wrapper = new Div();
        wrapper.addClassName("discount-composition-selector");

        Div text = new Div();
        text.addClassName("discount-composition-text");
        text.add(new Span("לוגיקת שילוב הנחות"), smallText("כיצד לחשב כשיש מספר הנחות חופפות"));

        maximumDiscountButton.addClassName("discount-composition-button");
        sumDiscountButton.addClassName("discount-composition-button");

        maximumDiscountButton.addClickListener(event -> setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM));
        sumDiscountButton.addClickListener(event -> setDiscountCompositionStrategy(DiscountCompositionStrategy.SUM));

        Div actions = new Div(maximumDiscountButton, sumDiscountButton);
        actions.addClassName("discount-composition-actions");

        wrapper.add(text, actions);
        setDiscountCompositionStrategy(discountCompositionStrategy);
        return wrapper;
    }

    private Component createAddRuleButton() {
        Button button = createDashedButton("הוסף חוק חדש", "+");
        button.addClassName("policy-add-purchase-button");
        button.addClickListener(event -> openPurchaseRuleDialog(null));
        return button;
    }

    private Component createAddDiscountButton() {
        Button button = createDashedButton("הוסף הנחה חדשה", "+");
        button.addClassName("policy-add-discount-button");
        button.addClickListener(event -> openDiscountDialog(null));
        return button;
    }

    private Component createPolicyTitle(String iconText, String title) {
        Div header = new Div();
        header.addClassName("policy-card-title-row");

        Span icon = new Span(iconText);
        icon.addClassName("policy-card-icon");

        H2 heading = new H2(title);
        heading.addClassName("policy-card-title");

        header.add(icon, heading);
        return header;
    }

    private Component createPolicyAccent(String type) {
        Div accent = new Div();
        accent.addClassName("policy-card-accent");
        accent.addClassName("policy-card-accent-" + type);
        return accent;
    }

    private Paragraph paragraph(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.addClassName("policy-card-description");
        return paragraph;
    }

    private Span smallText(String text) {
        Span span = new Span(text);
        span.addClassName("policy-small-text");
        return span;
    }

    private void refreshPurchaseRules() {
        purchaseRulesContainer.removeAll();

        if (purchaseRules.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("policy-empty-state-inline");
            empty.add(new Span("לא הוגדרו מגבלות רכישה. ברירת המחדל היא ללא מגבלות."));
            purchaseRulesContainer.add(empty);
            return;
        }

        purchaseRules.forEach(rule -> purchaseRulesContainer.add(createPurchaseRuleRow(rule)));
    }

    private Component createPurchaseRuleRow(PurchaseRuleDTO rule) {
        Div row = new Div();
        row.addClassName("purchase-rule-row");

        Span drag = new Span("⋮⋮");
        drag.addClassName("policy-drag-handle");

        Div text = new Div();
        text.addClassName("purchase-rule-text");
        text.add(new Span(rule.toDisplayText()), smallText(rule.toTechnicalText()));

        Button edit = createIconButton("עריכה", "✎");
        edit.addClickListener(event -> openPurchaseRuleDialog(rule));

        Button delete = createDangerIconButton("מחיקה", "×");
        delete.addClickListener(event -> {
            purchaseRules.remove(rule);
            refreshPurchaseRules();
        });

        Div actions = new Div(edit, delete);
        actions.addClassName("policy-row-actions");

        row.add(drag, text, actions);
        return row;
    }

    private void refreshDiscounts() {
        discountsContainer.removeAll();

        if (discounts.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("policy-empty-state-inline");
            empty.add(new Span("לא הוגדרו הנחות. ברירת המחדל היא ללא הנחה."));
            discountsContainer.add(empty);
            return;
        }

        discounts.forEach(discount -> discountsContainer.add(createDiscountRow(discount)));
    }

    private Component createDiscountRow(DiscountDTO discount) {
        Div row = new Div();
        row.addClassName("discount-row");

        Div top = new Div();
        top.addClassName("discount-row-top");

        Span icon = new Span(discount.type().getIconText());
        icon.addClassName("discount-row-icon");

        Div titleBlock = new Div();
        titleBlock.addClassName("discount-row-title-block");

        H3 title = new H3(discount.name());
        title.addClassName("discount-row-title");

        titleBlock.add(title, smallText(discount.type().getDescription()));

        Div labels = new Div();
        labels.addClassName("discount-row-labels");

        if (discount.type() == DiscountType.COUPON && !discount.couponCode().isBlank()) {
            Span coupon = new Span(discount.couponCode());
            coupon.addClassName("discount-coupon-badge");
            labels.add(coupon);
        }

        if (discount.type() == DiscountType.CONDITIONAL && !discount.conditionText().isBlank()) {
            Span condition = new Span(discount.conditionText());
            condition.addClassName("discount-condition-badge");
            labels.add(condition);
        }

        Button edit = createIconButton("עריכה", "✎");
        edit.addClickListener(event -> openDiscountDialog(discount));
        labels.add(edit);

        top.add(icon, titleBlock, labels);

        Div data = new Div(
                createDiscountDataBox("סוג הנחה", discount.valueType().getLabel()),
                createDiscountDataBox("ערך", discount.formattedValue())
        );
        data.addClassName("discount-data-grid");

        row.add(top, data);
        return row;
    }

    private Component createDiscountDataBox(String label, String value) {
        Div box = new Div();
        box.addClassName("discount-data-box");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("discount-data-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("discount-data-value");

        box.add(labelSpan, valueSpan);
        return box;
    }

    private void openPurchaseRuleDialog(PurchaseRuleDTO existingRule) {
        boolean editing = existingRule != null;
        PurchaseRuleDTO draft = editing ? existingRule.copy() : PurchaseRuleDTO.defaultRule();

        Dialog dialog = new Dialog();
        dialog.addClassName("policy-editor-dialog");
        dialog.setHeaderTitle(editing ? "עריכת חוק רכישה" : "הוספת חוק רכישה");

        ComboBox<PurchaseRuleField> field = new ComboBox<>("שדה");
        field.setItems(PurchaseRuleField.values());
        field.setItemLabelGenerator(PurchaseRuleField::getLabel);
        field.setValue(draft.field());

        ComboBox<ComparisonOperator> operator = new ComboBox<>("אופרטור");
        operator.setItems(ComparisonOperator.values());
        operator.setItemLabelGenerator(ComparisonOperator::getLabel);
        operator.setValue(draft.operator());

        NumberField value = new NumberField("ערך");
        value.setMin(0);
        value.setStep(1);
        value.setValue((double) draft.value());

        TextField unit = new TextField("יחידת תצוגה");
        unit.setValue(draft.unit());
        unit.setPlaceholder("לדוגמה: שנים, לרוכש");

        Div form = new Div(field, operator, value, unit);
        form.addClassName("policy-dialog-form");

        Button cancel = createSecondaryButton("ביטול", null);
        cancel.addClickListener(event -> dialog.close());

        Button save = createPrimaryButton(editing ? "עדכן חוק" : "הוסף חוק", null);
        save.addClickListener(event -> {
            if (field.isEmpty() || operator.isEmpty() || value.isEmpty()) {
                showError("יש למלא שדה, אופרטור וערך.");
                return;
            }

            PurchaseRuleDTO updated = new PurchaseRuleDTO(
                    draft.id(),
                    field.getValue(),
                    operator.getValue(),
                    value.getValue().intValue(),
                    unit.getValue() == null ? "" : unit.getValue().trim()
            );

            if (editing) {
                int index = purchaseRules.indexOf(existingRule);
                purchaseRules.set(index, updated);
            } else {
                purchaseRules.add(updated);
            }

            refreshPurchaseRules();
            dialog.close();
        });

        dialog.getFooter().add(cancel, save);
        dialog.add(form);
        dialog.open();
    }

    private void openDiscountDialog(DiscountDTO existingDiscount) {
        boolean editing = existingDiscount != null;
        DiscountDTO draft = editing ? existingDiscount : DiscountDTO.defaultCoupon();

        Dialog dialog = new Dialog();
        dialog.addClassName("policy-editor-dialog");
        dialog.setHeaderTitle(editing ? "עריכת הנחה" : "הוספת הנחה");

        TextField name = new TextField("שם ההנחה");
        name.setValue(draft.name());

        ComboBox<DiscountType> type = new ComboBox<>("סוג");
        type.setItems(DiscountType.values());
        type.setItemLabelGenerator(DiscountType::getLabel);
        type.setValue(draft.type());

        ComboBox<DiscountValueType> valueType = new ComboBox<>("אופן חישוב");
        valueType.setItems(DiscountValueType.values());
        valueType.setItemLabelGenerator(DiscountValueType::getLabel);
        valueType.setValue(draft.valueType());

        NumberField value = new NumberField("ערך");
        value.setMin(0);
        value.setValue(draft.value());

        TextField couponCode = new TextField("קוד קופון");
        couponCode.setValue(draft.couponCode());
        couponCode.setPlaceholder("לדוגמה: EARLYBIRD20");

        DatePicker validUntil = new DatePicker("תוקף עד");
        validUntil.setValue(draft.validUntil());

        TextField conditionText = new TextField("תנאי להפעלה");
        conditionText.setValue(draft.conditionText());
        conditionText.setPlaceholder("לדוגמה: כמות כרטיסים >= 2");

        Div form = new Div(name, type, valueType, value, couponCode, validUntil, conditionText);
        form.addClassName("policy-dialog-form");

        Button delete = createDangerButton("מחיקה", null);
        delete.setVisible(editing);
        delete.addClickListener(event -> {
            discounts.remove(existingDiscount);
            refreshDiscounts();
            dialog.close();
        });

        Button cancel = createSecondaryButton("ביטול", null);
        cancel.addClickListener(event -> dialog.close());

        Button save = createPrimaryButton(editing ? "עדכן הנחה" : "הוסף הנחה", null);
        save.addClickListener(event -> {
            if (name.isEmpty() || type.isEmpty() || valueType.isEmpty() || value.isEmpty()) {
                showError("יש למלא שם, סוג, אופן חישוב וערך.");
                return;
            }

            DiscountDTO updated = new DiscountDTO(
                    draft.id(),
                    name.getValue().trim(),
                    type.getValue(),
                    valueType.getValue(),
                    value.getValue(),
                    couponCode.getValue() == null ? "" : couponCode.getValue().trim(),
                    validUntil.getValue(),
                    conditionText.getValue() == null ? "" : conditionText.getValue().trim()
            );

            if (editing) {
                int index = discounts.indexOf(existingDiscount);
                discounts.set(index, updated);
            } else {
                discounts.add(updated);
            }

            refreshDiscounts();
            dialog.close();
        });

        dialog.getFooter().add(delete, cancel, save);
        dialog.add(form);
        dialog.open();
    }

    private void setDiscountCompositionStrategy(DiscountCompositionStrategy strategy) {
        discountCompositionStrategy = Objects.requireNonNullElse(strategy, DiscountCompositionStrategy.MAXIMUM);

        maximumDiscountButton.removeClassName("discount-composition-button-selected");
        sumDiscountButton.removeClassName("discount-composition-button-selected");

        if (discountCompositionStrategy == DiscountCompositionStrategy.MAXIMUM) {
            maximumDiscountButton.addClassName("discount-composition-button-selected");
        } else {
            sumDiscountButton.addClassName("discount-composition-button-selected");
        }

        // TODO: When the Presenter is connected, update the discount composition strategy in the policy draft DTO.
    }

    private void loadDraftForCompany(String companyId) {
        try {
            Long parsedCompanyId = Long.parseLong(companyId);
            
            PoliciesEditorPresenter.PoliciesDraftData data = 
                    presenter.loadPolicies(UiSession.getMemberToken(), parsedCompanyId);

            this.purchaseRules.clear();
            this.purchaseRules.addAll(data.purchaseDraft().rules());
            this.purchaseRootOperator.setValue(data.purchaseDraft().rootOperator());

            this.discounts.clear();
            this.discounts.addAll(data.discountDraft().discounts());
            setDiscountCompositionStrategy(data.discountDraft().compositionStrategy());

            refreshPurchaseRules();
            refreshDiscounts();

        } catch (Exception e) {
            showError("שגיאה בטעינת המדיניות מהשרת: " + e.getMessage());
            resetDraft();
        }
    }

    private void resetDraft() {
        purchaseRules.clear();
        discounts.clear();

        purchaseRootOperator.setValue(LogicalOperator.AND);
        discountCompositionStrategy = DiscountCompositionStrategy.MAXIMUM;

        purchaseRules.add(new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.AGE, ComparisonOperator.GREATER_OR_EQUALS, 18, "שנים"));
        purchaseRules.add(new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, ComparisonOperator.LESS_OR_EQUALS, 5, "לרוכש"));

        discounts.add(new DiscountDTO(
                UUID.randomUUID().toString(),
                "הנחת קופון",
                DiscountType.COUPON,
                DiscountValueType.PERCENTAGE,
                20,
                "EARLYBIRD20",
                LocalDate.now().plusMonths(1),
                ""
        ));

        discounts.add(new DiscountDTO(
                UUID.randomUUID().toString(),
                "הנחה מותנית",
                DiscountType.CONDITIONAL,
                DiscountValueType.PERCENTAGE,
                10,
                "",
                null,
                "כמות כרטיסים >= 2"
        ));

        setDiscountCompositionStrategy(discountCompositionStrategy);
        refreshPurchaseRules();
        refreshDiscounts();
    }

    private void saveDraft() {
        PurchasePolicyDraftDTO purchasePolicyDraft = getPurchasePolicyDraft();
        DiscountPolicyDraftDTO discountPolicyDraft = getDiscountPolicyDraft();

        try {
            Long parsedCompanyId = Long.parseLong(companyId);
            
            presenter.savePolicies(
                    UiSession.getMemberToken(), 
                    parsedCompanyId, 
                    purchasePolicyDraft, 
                    discountPolicyDraft
            );
            
            showSuccess("המדיניות נשמרה והתעדכנה בהצלחה במערכת!");
        } catch (Exception e) {
            showError("שגיאה בשמירת המדיניות: " + e.getMessage());
        }
    }

    public PurchasePolicyDraftDTO getPurchasePolicyDraft() {
        return new PurchasePolicyDraftDTO(
                companyId,
                purchaseRootOperator.getValue(),
                new ArrayList<>(purchaseRules)
        );
    }

    public DiscountPolicyDraftDTO getDiscountPolicyDraft() {
        return new DiscountPolicyDraftDTO(
                companyId,
                discountCompositionStrategy,
                new ArrayList<>(discounts)
        );
    }

    private Button createPrimaryButton(String text, String iconText) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClassName("policy-primary-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createSecondaryButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-secondary-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createDangerButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-danger-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createDashedButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-dashed-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createIconButton(String title, String iconText) {
        Button button = new Button(iconText);
        button.addClassName("policy-icon-button");
        button.getElement().setAttribute("title", title);
        return button;
    }

    private Button createDangerIconButton(String title, String iconText) {
        Button button = new Button(iconText);
        button.addClassNames("policy-icon-button", "policy-icon-danger-button");
        button.getElement().setAttribute("title", title);
        return button;
    }

    private void addIcon(Button button, String iconText) {
        if (iconText == null || iconText.isBlank()) {
            return;
        }

        Span icon = new Span(iconText);
        icon.addClassName("policy-button-icon");
        button.setIcon(icon);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public enum LogicalOperator {
        AND("וגם (AND)"),
        OR("או (OR)");

        private final String label;

        LogicalOperator(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum PurchaseRuleField {
        AGE("גיל"),
        MIN_TICKETS("מינימום כרטיסים"),
        MAX_TICKETS("מקסימום כרטיסים");

        private final String label;

        PurchaseRuleField(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ComparisonOperator {
        GREATER_OR_EQUALS("גדול או שווה", ">="),
        LESS_OR_EQUALS("קטן או שווה", "<="),
        EQUALS("שווה", "=");

        private final String label;
        private final String symbol;

        ComparisonOperator(String label, String symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        public String getLabel() {
            return label + " (" + symbol + ")";
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public enum DiscountCompositionStrategy {
        MAXIMUM("מקסימום"),
        SUM("סכום");

        private final String label;

        DiscountCompositionStrategy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum DiscountType {
        SIMPLE("הנחה פשוטה", "כרוכה רק בחישוב מחיר", "%"),
        CONDITIONAL("הנחה מותנית", "מוחלת אוטומטית לפי תנאי", "?"),
        COUPON("הנחת קופון", "מופעלת בעת הזנת קוד", "#");

        private final String label;
        private final String description;
        private final String iconText;

        DiscountType(String label, String description, String iconText) {
            this.label = label;
            this.description = description;
            this.iconText = iconText;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getIconText() {
            return iconText;
        }
    }

    public enum DiscountValueType {
        PERCENTAGE("אחוזים (%)"),
        FIXED_AMOUNT("סכום קבוע (₪)");

        private final String label;

        DiscountValueType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record PurchasePolicyDraftDTO(
            String companyId,
            LogicalOperator rootOperator,
            List<PurchaseRuleDTO> rules
    ) {
    }

    public record DiscountPolicyDraftDTO(
            String companyId,
            DiscountCompositionStrategy compositionStrategy,
            List<DiscountDTO> discounts
    ) {
    }

    public record PurchaseRuleDTO(
            String id,
            PurchaseRuleField field,
            ComparisonOperator operator,
            int value,
            String unit
    ) {
        public static PurchaseRuleDTO defaultRule() {
            return new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, ComparisonOperator.LESS_OR_EQUALS, 5, "לרוכש");
        }

        public PurchaseRuleDTO copy() {
            return new PurchaseRuleDTO(id, field, operator, value, unit);
        }

        public String toDisplayText() {
            return field.getLabel() + " " + operator.getSymbol() + " " + value + (unit == null || unit.isBlank() ? "" : " " + unit);
        }

        public String toTechnicalText() {
            return field.name() + " " + operator.getSymbol() + " " + value;
        }
    }

    public record DiscountDTO(
            String id,
            String name,
            DiscountType type,
            DiscountValueType valueType,
            double value,
            String couponCode,
            LocalDate validUntil,
            String conditionText
    ) {
        public static DiscountDTO defaultCoupon() {
            return new DiscountDTO(
                    UUID.randomUUID().toString(),
                    "הנחה חדשה",
                    DiscountType.SIMPLE,
                    DiscountValueType.PERCENTAGE,
                    10,
                    "",
                    null,
                    ""
            );
        }

        public String formattedValue() {
            if (valueType == DiscountValueType.PERCENTAGE) {
                return removeTrailingZero(value) + "%";
            }
            return "₪" + removeTrailingZero(value);
        }

        private static String removeTrailingZero(double number) {
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
    }
}
