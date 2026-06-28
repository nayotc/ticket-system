package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.PoliciesEditorPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Route(value = UiRoutes.POLICIES_EDITOR, layout = ManagementLayout.class)
public class PoliciesEditor extends Div implements BeforeEnterObserver {

    private static final String DATE_PICKER_DISPLAY_FORMAT = "dd/MM/yy";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern(DATE_PICKER_DISPLAY_FORMAT);
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private String companyId;

    private final PoliciesEditorPresenter presenter;

    /*
     * Step 1 only:
     * These flags are temporary UI flags.
     * In the next step they should be loaded from MembershipService.hasPermission(...)
     * through the presenter.
     */
    private boolean canEditPurchasePolicy = true;
    private boolean canEditDiscountPolicy = true;

    private PurchaseExpressionNode purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
    private final List<DiscountDTO> discounts = new ArrayList<>();

    private DiscountCompositionStrategy discountCompositionStrategy = DiscountCompositionStrategy.MAXIMUM;

    private final Div policiesTabsShell = new Div();
    private final Div policyTabContent = new Div();
    private final Div purchaseExpressionContainer = new Div();
    private final Div discountsContainer = new Div();

    private Tabs policyTabs;
    private Tab purchasePolicyTab;
    private Tab discountPolicyTab;
    private PolicyTab activePolicyTab = PolicyTab.PURCHASE;

    private final Button maximumDiscountButton = new Button("מקסימום");
    private final Button sumDiscountButton = new Button("סכום");

    @Autowired
    public PoliciesEditor(PoliciesEditorPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("policy-editor-page");

        Button resetButton = createSecondaryButton("בטל שינויים", "↺");
        resetButton.addClickListener(event -> {
            if (companyId == null || companyId.isBlank()) {
                resetDraft();
                return;
            }

            loadDraftForCompany(companyId);
        });

        ViewHeader header = new ViewHeader(
                "עורך מדיניות",
                "הגדרת כללי רכישה, מגבלות רכישה, הנחות והרכבת הנחות עבור חברת ההפקה.",
                resetButton
        );

        policiesTabsShell.addClassName("policy-editor-tabs-shell");
        policyTabContent.addClassName("policy-editor-tab-content");

        resetDraft();
        refreshVisiblePolicySections();

        add(new PageContainer(header, policiesTabsShell));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyId = event.getRouteParameters()
                .get("companyId")
                .orElse("1");

        loadDraftForCompany(companyId);
    }

    private void refreshVisiblePolicySections() {
        policiesTabsShell.removeAll();
        policyTabContent.removeAll();

        if (!canEditPurchasePolicy && !canEditDiscountPolicy) {
            policiesTabsShell.add(createNoAccessiblePoliciesState());
            return;
        }

        if (!canAccessActiveTab()) {
            activePolicyTab = getFirstAccessibleTab();
        }

        policiesTabsShell.add(createPolicyTabsHeader(), policyTabContent);
        refreshActivePolicyTabContent();
    }

    private Component createPolicyTabsHeader() {
        Div wrapper = new Div();
        wrapper.addClassName("policy-editor-tabs-wrapper");

        policyTabs = new Tabs();
        policyTabs.addClassName("policy-editor-tabs");

        if (canEditPurchasePolicy) {
            purchasePolicyTab = createPolicyTab("⚖", "מדיניות רכישה", "תנאים וקבוצות AND / OR");
            policyTabs.add(purchasePolicyTab);
        } else {
            purchasePolicyTab = null;
        }

        if (canEditDiscountPolicy) {
            discountPolicyTab = createPolicyTab("%", "מדיניות הנחות", "סוגי הנחות ושילוב הנחות");
            policyTabs.add(discountPolicyTab);
        } else {
            discountPolicyTab = null;
        }

        if (activePolicyTab == PolicyTab.PURCHASE && purchasePolicyTab != null) {
            policyTabs.setSelectedTab(purchasePolicyTab);
        } else if (activePolicyTab == PolicyTab.DISCOUNT && discountPolicyTab != null) {
            policyTabs.setSelectedTab(discountPolicyTab);
        }

        policyTabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == purchasePolicyTab) {
                activePolicyTab = PolicyTab.PURCHASE;
            } else if (event.getSelectedTab() == discountPolicyTab) {
                activePolicyTab = PolicyTab.DISCOUNT;
            }

            refreshActivePolicyTabContent();
        });

        wrapper.add(policyTabs);
        return wrapper;
    }

    private Tab createPolicyTab(String iconText, String title, String subtitle) {
        Tab tab = new Tab(createPolicyTabLabel(iconText, title, subtitle));
        tab.addClassName("policy-editor-tab");
        return tab;
    }

    private Component createPolicyTabLabel(String iconText, String title, String subtitle) {
        Div label = new Div();
        label.addClassName("policy-tab-label");

        Span icon = new Span(iconText);
        icon.addClassName("policy-tab-icon");

        Div text = new Div();
        text.addClassName("policy-tab-text");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("policy-tab-title");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.addClassName("policy-tab-subtitle");

        text.add(titleSpan, subtitleSpan);
        label.add(icon, text);
        return label;
    }

    private boolean canAccessActiveTab() {
        return (activePolicyTab == PolicyTab.PURCHASE && canEditPurchasePolicy)
                || (activePolicyTab == PolicyTab.DISCOUNT && canEditDiscountPolicy);
    }

    private PolicyTab getFirstAccessibleTab() {
        if (canEditPurchasePolicy) {
            return PolicyTab.PURCHASE;
        }

        return PolicyTab.DISCOUNT;
    }

    private void refreshActivePolicyTabContent() {
        policyTabContent.removeAll();

        if (activePolicyTab == PolicyTab.PURCHASE && canEditPurchasePolicy) {
            policyTabContent.add(createPurchasePolicySection());
            return;
        }

        if (activePolicyTab == PolicyTab.DISCOUNT && canEditDiscountPolicy) {
            policyTabContent.add(createDiscountPolicySection());
        }
    }

    private Component createNoAccessiblePoliciesState() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-no-access-card");

        card.add(
                createPolicyTitle("!", "אין הרשאות לעריכת מדיניות"),
                paragraph("אין לך הרשאה לערוך מדיניות רכישה או מדיניות הנחות בחברה הזו.")
        );

        return card;
    }

    private Component createPurchasePolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-purchase");

        card.add(
                createPolicyAccent("purchase"),
                createPolicyTitle("⚖", "מדיניות רכישה"),
                paragraph("בנה תנאי רכישה מורכב באמצעות קבוצות AND ו־OR. לדוגמה: ((A OR B) AND (C OR D))."),
                createPurchaseExpressionBuilder(),
                createPurchasePolicyActions()
        );

        return card;
    }

    private Component createDiscountPolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-discount");

        discountsContainer.addClassName("discounts-list");

        card.add(
                createPolicyAccent("discount"),
                createPolicyTitle("%", "מדיניות הנחות"),
                paragraph("בחר סוג הנחה. רק השדות הרלוונטיים לסוג שנבחר יוצגו בטופס."),
                createDiscountCompositionSelector(),
                discountsContainer,
                createAddDiscountButton(),
                createDiscountPolicyActions()
        );

        return card;
    }

    private Component createPurchaseExpressionBuilder() {
        Div builder = new Div();
        builder.addClassName("purchase-rule-builder");

        purchaseExpressionContainer.addClassName("purchase-expression-tree");

        builder.add(purchaseExpressionContainer);
        return builder;
    }

    private Component createPurchasePolicyActions() {
        Button savePurchaseButton = createPrimaryButton("שמור מדיניות רכישה", "✓");
        savePurchaseButton.addClickListener(event -> savePurchaseDraft());

        Div actions = new Div(savePurchaseButton);
        actions.addClassName("policy-section-actions");
        return actions;
    }

    private Component createDiscountPolicyActions() {
        Button saveDiscountButton = createPrimaryButton("שמור מדיניות הנחות", "✓");
        saveDiscountButton.addClickListener(event -> saveDiscountDraft());

        Div actions = new Div(saveDiscountButton);
        actions.addClassName("policy-section-actions");
        return actions;
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

    private void refreshPurchaseExpression() {
        purchaseExpressionContainer.removeAll();
        purchaseExpressionContainer.add(createPurchaseExpressionNode(purchasePolicyRoot, null, 0));
    }

    private Component createPurchaseExpressionNode(
            PurchaseExpressionNode node,
            PurchaseExpressionNode parent,
            int depth
    ) {
        if (node.isRule()) {
            return createPurchaseExpressionRuleRow(node, parent);
        }

        Div group = new Div();
        group.addClassNames("purchase-expression-group", "purchase-expression-depth-" + Math.min(depth, 3));

        Div header = new Div();
        header.addClassName("purchase-expression-group-header");

        Div titleBlock = new Div();
        titleBlock.addClassName("purchase-expression-group-title-block");

        Span badge = new Span(depth == 0 ? "שורש" : "קבוצה");
        badge.addClassName("purchase-expression-group-badge");

        Div titleText = new Div();
        titleText.addClassName("purchase-expression-group-title-text");

        Span title = new Span(depth == 0 ? "הביטוי הראשי" : "קבוצת תנאים");
        title.addClassName("purchase-expression-group-title");

        Span summary = new Span(createGroupSummary(node));
        summary.addClassName("purchase-expression-group-summary");

        titleText.add(title, summary);
        titleBlock.add(badge, titleText);

        ComboBox<LogicalOperator> operator = new ComboBox<>();
        operator.setItems(LogicalOperator.values());
        operator.setItemLabelGenerator(LogicalOperator::getLabel);
        operator.setValue(node.operator());
        operator.addClassName("purchase-expression-operator");
        operator.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                node.setOperator(event.getValue());
                refreshPurchaseExpression();
            }
        });

        Div operatorBlock = new Div();
        operatorBlock.addClassName("purchase-expression-operator-block");

        Span operatorLabel = new Span("חיבור בין הילדים");
        operatorLabel.addClassName("purchase-expression-operator-label");
        operatorBlock.add(operatorLabel, operator);

        Div actions = new Div();
        actions.addClassName("purchase-expression-group-actions");

        Button addRule = createSecondaryButton("תנאי", "+");
        addRule.addClassName("purchase-expression-action-button");
        addRule.addClickListener(event -> openPurchaseRuleDialog(null, rule -> {
            node.children().add(PurchaseExpressionNode.rule(rule));
            refreshPurchaseExpression();
        }));

        Button addGroup = createSecondaryButton("קבוצה", "+");
        addGroup.addClassName("purchase-expression-action-button");
        addGroup.addClickListener(event -> {
            node.children().add(PurchaseExpressionNode.group(LogicalOperator.AND));
            refreshPurchaseExpression();
        });

        actions.add(addRule, addGroup);

        if (parent != null) {
            Button deleteGroup = createDangerIconButton("מחיקת קבוצה", "×");
            deleteGroup.addClassName("purchase-expression-delete-group-button");
            deleteGroup.addClickListener(event -> {
                parent.children().remove(node);
                refreshPurchaseExpression();
            });
            actions.add(deleteGroup);
        }

        header.add(titleBlock, operatorBlock, actions);

        Div children = new Div();
        children.addClassName("purchase-expression-children");

        if (node.children().isEmpty()) {
            Div empty = new Div();
            empty.addClassName("policy-empty-state-inline");
            empty.add(new Span("אין תנאים בקבוצה הזו."));
            children.add(empty);
        } else {
            for (int i = 0; i < node.children().size(); i++) {
                if (i > 0) {
                    children.add(createLogicalConnector(node.operator()));
                }

                PurchaseExpressionNode child = node.children().get(i);
                children.add(createPurchaseExpressionNode(child, node, depth + 1));
            }
        }

        group.add(header, children);
        return group;
    }

    private Component createLogicalConnector(LogicalOperator operator) {
        Div connector = new Div();
        connector.addClassName("purchase-expression-connector");

        Span label = new Span(operator == LogicalOperator.OR ? "או" : "וגם");
        label.addClassName("purchase-expression-connector-label");

        connector.add(label);
        return connector;
    }

    private String createGroupSummary(PurchaseExpressionNode node) {
        int groups = 0;
        int rules = 0;

        for (PurchaseExpressionNode child : node.children()) {
            if (child.isRule()) {
                rules++;
            } else {
                groups++;
            }
        }

        if (groups == 0 && rules == 0) {
            return "הקבוצה ריקה";
        }

        List<String> parts = new ArrayList<>();

        if (rules > 0) {
            parts.add(rules + " תנאים");
        }

        if (groups > 0) {
            parts.add(groups + " קבוצות");
        }

        return String.join(" · ", parts);
    }

    private Component createPurchaseExpressionRuleRow(
            PurchaseExpressionNode node,
            PurchaseExpressionNode parent
    ) {
        PurchaseRuleDTO rule = node.rule();

        Div row = new Div();
        row.addClassName("purchase-rule-row");

        Span drag = new Span("תנאי");
        drag.addClassName("policy-condition-badge");

        Div text = new Div();
        text.addClassName("purchase-rule-text");
        text.add(new Span(rule.toDisplayText()), smallText(rule.toTechnicalText()));

        Button edit = createIconButton("עריכה", "✎");
        edit.addClickListener(event -> openPurchaseRuleDialog(rule, updated -> {
            node.setRule(updated);
            refreshPurchaseExpression();
        }));

        Button delete = createDangerIconButton("מחיקה", "×");
        delete.addClickListener(event -> {
            if (parent != null) {
                parent.children().remove(node);
                refreshPurchaseExpression();
            }
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

    if (discount.type() == DiscountType.COUPON
            && discount.couponCode() != null
            && !discount.couponCode().isBlank()) {
        Span coupon = new Span(discount.couponCode());
        coupon.addClassName("discount-coupon-badge");
        labels.add(coupon);
    }

    if (discount.type() == DiscountType.CONDITIONAL
            && discount.conditionText() != null
            && !discount.conditionText().isBlank()) {
        Span condition = new Span(discount.conditionText());
        condition.addClassName("discount-condition-badge");
        labels.add(condition);
    }

    Button edit = createIconButton("עריכה", "✎");
    edit.addClickListener(event -> openDiscountDialog(discount));

    Button delete = createDangerIconButton("מחיקה", "×");
    delete.addClickListener(event -> {
        discounts.remove(discount);
        refreshDiscounts();
    });

    if (discount.type() == DiscountType.CONDITIONAL) {
        Button addCondition = createIconButton("הוסף תנאי", "+");
        addCondition.addClickListener(event -> openAddConditionDialog(discount));
        labels.add(addCondition);
    }

    labels.add(edit, delete);

    top.add(icon, titleBlock, labels);

    Div data = new Div(
            createDiscountDataBox("אופן חישוב", discount.valueType().getLabel()),
            createDiscountDataBox("ערך", discount.formattedValue())
    );

    if (discount.type() == DiscountType.COUPON && discount.validUntil() != null) {
        data.add(createDiscountDataBox("תוקף קופון", formatDate(discount.validUntil())));
    }

    if (discount.type() == DiscountType.CONDITIONAL) {
        int conditionCount = discount.conditions() == null ? 0 : discount.conditions().size();

        data.add(createDiscountDataBox("מספר תנאים", String.valueOf(conditionCount)));

        if (conditionCount > 0) {
            data.add(createDiscountDataBox("שילוב תנאים", "AND"));
        }
    }

    data.addClassName("discount-data-grid");

    row.add(top, data);
    return row;
}

private void openAddConditionDialog(DiscountDTO discount) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("הוספת תנאי להנחה");

    ComboBox<DiscountConditionType> conditionType = new ComboBox<>("סוג תנאי");
    conditionType.setItems(DiscountConditionType.values());
    conditionType.setItemLabelGenerator(DiscountConditionType::getLabel);
    conditionType.setWidthFull();

    IntegerField ticketThreshold = new IntegerField("מספר כרטיסים");
    ticketThreshold.setMin(1);
    ticketThreshold.setWidthFull();

    DateTimePicker startTime = new DateTimePicker("מתאריך");
    startTime.setWidthFull();

    DateTimePicker endTime = new DateTimePicker("עד תאריך");
    endTime.setWidthFull();

    Button save = new Button("הוסף תנאי", event -> {
        DiscountConditionType selectedType = conditionType.getValue();

        if (selectedType == null) {
            throw new PresentationException("יש לבחור סוג תנאי.");
        }

        DiscountConditionDTO newCondition = new DiscountConditionDTO(
                selectedType,
                ticketThreshold.getValue(),
                startTime.getValue(),
                endTime.getValue()
        );

        List<DiscountConditionDTO> updatedConditions = new ArrayList<>();
        if (discount.conditions() != null) {
            updatedConditions.addAll(discount.conditions());
        }
        updatedConditions.add(newCondition);

        DiscountDTO updated = discount.withConditions(updatedConditions);

        int index = discounts.indexOf(discount);
        if (index >= 0) {
            discounts.set(index, updated);
        }

        dialog.close();
        refreshDiscounts();
    });

    Button cancel = new Button("ביטול", event -> dialog.close());

    VerticalLayout layout = new VerticalLayout(
            conditionType,
            ticketThreshold,
            startTime,
            endTime,
            new HorizontalLayout(save, cancel)
    );

    dialog.add(layout);
    dialog.open();
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

    private void openPurchaseRuleDialog(PurchaseRuleDTO existingRule, Consumer<PurchaseRuleDTO> onSave) {
        boolean editing = existingRule != null;
        PurchaseRuleDTO draft = editing ? existingRule.copy() : PurchaseRuleDTO.defaultRule();

        Dialog dialog = new Dialog();
        dialog.addClassName("policy-editor-dialog");
        dialog.setHeaderTitle(editing ? "עריכת תנאי רכישה" : "הוספת תנאי רכישה");

        ComboBox<PurchaseRuleField> field = new ComboBox<>("שדה");
        field.setItems(PurchaseRuleField.values());
        field.setItemLabelGenerator(PurchaseRuleField::getLabel);
        field.setValue(draft.field());

        NumberField value = new NumberField("ערך");
        value.setMin(0);
        value.setStep(1);
        value.setValue((double) draft.value());

        TextField unit = new TextField("יחידת תצוגה");
        unit.setValue(draft.unit());
        unit.setPlaceholder("לדוגמה: שנים, לרוכש");

        Div form = new Div(field, value, unit);
        form.addClassName("policy-dialog-form");

        Button cancel = createSecondaryButton("ביטול", null);
        cancel.addClickListener(event -> dialog.close());

        Button save = createPrimaryButton(editing ? "עדכן תנאי" : "הוסף תנאי", null);
        save.addClickListener(event -> {
            if (field.isEmpty() || value.isEmpty()) {
                showError("יש למלא שדה וערך.");
                return;
            }

            PurchaseRuleDTO updated = new PurchaseRuleDTO(
                    draft.id(),
                    field.getValue(),
                    value.getValue().intValue(),
                    unit.getValue() == null ? "" : unit.getValue().trim()
            );

            onSave.accept(updated);
            dialog.close();
        });

        dialog.getFooter().add(cancel, save);
        dialog.add(form);
        dialog.open();
    }

    private void openDiscountDialog(DiscountDTO existingDiscount) {
    boolean editing = existingDiscount != null;
    DiscountDTO draft = editing ? existingDiscount : DiscountDTO.defaultDiscount();

    Dialog dialog = new Dialog();
    dialog.addClassName("policy-editor-dialog");
    dialog.setHeaderTitle(editing ? "עריכת הנחה" : "הוספת הנחה");

    TextField name = new TextField("שם ההנחה");
    name.setValue(draft.name());

    ComboBox<DiscountType> type = new ComboBox<>("סוג הנחה");
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

    DatePicker validUntil = createPolicyDatePicker("תוקף קופון עד", draft.validUntil());

    List<DiscountConditionDTO> conditionDrafts = new ArrayList<>();
    if (draft.conditions() != null) {
        conditionDrafts.addAll(draft.conditions());
    }

    Div conditionsContainer = new Div();
    conditionsContainer.addClassName("discount-conditions-container");

    Runnable refreshConditions = () -> {
        conditionsContainer.removeAll();

        if (conditionDrafts.isEmpty()) {
            conditionsContainer.add(smallText("לא הוגדרו תנאים עדיין."));
            return;
        }

        for (DiscountConditionDTO condition : new ArrayList<>(conditionDrafts)) {
            Div conditionRow = new Div();
            conditionRow.addClassName("discount-condition-row");

            Span text = new Span(conditionText(condition));
            text.addClassName("discount-condition-badge");

            Button remove = createDangerIconButton("מחיקת תנאי", "×");
            remove.addClickListener(e -> {
                conditionDrafts.remove(condition);
                conditionsContainer.removeAll();
                // קריאה מחדש ידנית
                for (DiscountConditionDTO c : new ArrayList<>(conditionDrafts)) {
                    Div row = new Div();
                    row.addClassName("discount-condition-row");

                    Span t = new Span(conditionText(c));
                    t.addClassName("discount-condition-badge");

                    Button r = createDangerIconButton("מחיקת תנאי", "×");
                    r.addClickListener(ev -> {
                        conditionDrafts.remove(c);
                        openDiscountDialog(new DiscountDTO(
                                draft.id(),
                                name.getValue().trim(),
                                type.getValue(),
                                valueType.getValue(),
                                value.getValue(),
                                couponCode.getValue(),
                                validUntil.getValue(),
                                conditionDrafts
                        ));
                        dialog.close();
                    });

                    row.add(t, r);
                    conditionsContainer.add(row);
                }

                if (conditionDrafts.isEmpty()) {
                    conditionsContainer.add(smallText("לא הוגדרו תנאים עדיין."));
                }
            });

            conditionRow.add(text, remove);
            conditionsContainer.add(conditionRow);
        }
    };

    Button addCondition = createSecondaryButton("הוסף תנאי", null);
    addCondition.addClickListener(event -> openAddConditionDialog(
            name.getValue().trim(),
            conditionDrafts,
            conditionsContainer
    ));

    Div conditionalBox = new Div(addCondition, conditionsContainer);
    conditionalBox.addClassName("discount-conditional-box");

    Div form = new Div(
            name,
            type,
            valueType,
            value,
            couponCode,
            validUntil,
            conditionalBox
    );
    form.addClassName("policy-dialog-form");

    Runnable applyVisibility = () -> {
        DiscountType selectedType = type.getValue();

        couponCode.setVisible(selectedType == DiscountType.COUPON);
        validUntil.setVisible(selectedType == DiscountType.COUPON);

        conditionalBox.setVisible(selectedType == DiscountType.CONDITIONAL);
    };

    refreshConditions.run();
    applyVisibility.run();

    type.addValueChangeListener(event -> applyVisibility.run());

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

        DiscountType selectedType = type.getValue();

        if (selectedType == DiscountType.COUPON && safeTrim(couponCode.getValue()).isBlank()) {
            showError("בהנחת קופון יש למלא קוד קופון.");
            return;
        }

        if (selectedType == DiscountType.CONDITIONAL && conditionDrafts.isEmpty()) {
            showError("בהנחה מותנית יש להוסיף לפחות תנאי אחד.");
            return;
        }

        if (selectedType == DiscountType.CONDITIONAL) {
            String ticketConditionError = invalidDiscountTicketConditionMessage(conditionDrafts);
            if (ticketConditionError != null) {
                showError(ticketConditionError);
                return;
            }
        }

        DiscountDTO updated = new DiscountDTO(
                draft.id(),
                name.getValue().trim(),
                selectedType,
                valueType.getValue(),
                value.getValue(),
                selectedType == DiscountType.COUPON ? safeTrim(couponCode.getValue()) : "",
                selectedType == DiscountType.COUPON ? validUntil.getValue() : null,
                selectedType == DiscountType.CONDITIONAL ? new ArrayList<>(conditionDrafts) : new ArrayList<>()
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

private void openAddConditionDialog(String discountName,
                                    List<DiscountConditionDTO> conditionDrafts,
                                    Div conditionsContainer) {
    Dialog dialog = new Dialog();
    dialog.addClassName("policy-editor-dialog");
    dialog.setHeaderTitle("הוספת תנאי");

    ComboBox<DiscountConditionType> conditionType = new ComboBox<>("סוג תנאי");
    conditionType.setItems(DiscountConditionType.values());
    conditionType.setItemLabelGenerator(DiscountConditionType::getLabel);
    conditionType.setValue(DiscountConditionType.MIN_TICKET);

    NumberField ticketThreshold = new NumberField("כמות כרטיסים");
    ticketThreshold.setMin(1);
    ticketThreshold.setStep(1);
    ticketThreshold.setPlaceholder("לדוגמה: 2");

    DatePicker startDate = createPolicyDatePicker("מתאריך", null);
    DatePicker endDate = createPolicyDatePicker("עד תאריך", null);

    Runnable applyVisibility = () -> {
        DiscountConditionType selected = conditionType.getValue();

        boolean needsTickets = selected != null && selected.requiresTicketThreshold();
        boolean needsDate = selected != null && selected.requiresDateRange();

        ticketThreshold.setVisible(needsTickets);
        startDate.setVisible(needsDate);
        endDate.setVisible(needsDate);
    };

    applyVisibility.run();
    conditionType.addValueChangeListener(event -> applyVisibility.run());

    Button cancel = createSecondaryButton("ביטול", null);
    cancel.addClickListener(event -> dialog.close());

    Button save = createPrimaryButton("הוסף תנאי", null);
    save.addClickListener(event -> {
        DiscountConditionType selected = conditionType.getValue();

        if (selected == null) {
            showError("יש לבחור סוג תנאי.");
            return;
        }

        Integer normalizedThreshold = null;
        LocalDateTime normalizedStartTime = null;
        LocalDateTime normalizedEndTime = null;

        if (selected.requiresTicketThreshold()) {
            if (ticketThreshold.isEmpty() || ticketThreshold.getValue() <= 0) {
                showError("בתנאי לפי כמות כרטיסים יש להזין כמות חיובית.");
                return;
            }

            normalizedThreshold = ticketThreshold.getValue().intValue();
        }

        if (selected.requiresDateRange()) {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                showError("בתנאי לפי תאריך יש למלא תאריך התחלה ותאריך סיום.");
                return;
            }

            if (endDate.getValue().isBefore(startDate.getValue())) {
                showError("תאריך הסיום לא יכול להיות לפני תאריך ההתחלה.");
                return;
            }

            normalizedStartTime = startDate.getValue().atStartOfDay();
            normalizedEndTime = endDate.getValue().atTime(23, 59);
        }

        DiscountConditionDTO newCondition = new DiscountConditionDTO(
                selected,
                normalizedThreshold,
                normalizedStartTime,
                normalizedEndTime
        );

        List<DiscountConditionDTO> candidateConditions = new ArrayList<>(conditionDrafts);
        candidateConditions.add(newCondition);

        String ticketConditionError = invalidDiscountTicketConditionMessage(candidateConditions);
        if (ticketConditionError != null) {
            showError(ticketConditionError);
            return;
        }

        conditionDrafts.add(newCondition);

        conditionsContainer.removeAll();
        for (DiscountConditionDTO condition : conditionDrafts) {
            Span conditionLabel = new Span(conditionText(condition));
            conditionLabel.addClassName("discount-condition-badge");
            conditionsContainer.add(conditionLabel);
        }

        dialog.close();
    });

    Div form = new Div(conditionType, ticketThreshold, startDate, endDate);
    form.addClassName("policy-dialog-form");

    dialog.getFooter().add(cancel, save);
    dialog.add(form);
    dialog.open();
}
private String conditionText(DiscountConditionDTO condition) {
    if (condition == null || condition.conditionType() == null) {
        return "";
    }

    DiscountConditionType type = condition.conditionType();

    if (type.requiresTicketThreshold()) {
        if (condition.ticketThreshold() == null) {
            return "";
        }

        return type.getDisplayPrefix() + " " + condition.ticketThreshold();
    }

    if (type.requiresDateRange()) {
        if (condition.startTime() != null && condition.endTime() != null) {
            return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME)
                    + " עד " + condition.endTime().format(DISPLAY_DATE_TIME);
        }

        return "";
    }

    return "";
}


    private void applyDiscountTypeVisibility(
            DiscountType selectedType,
            DiscountConditionType selectedCondition,
            TextField couponCode,
            DatePicker validUntil,
            ComboBox<DiscountConditionType> conditionType,
            NumberField ticketThreshold,
            DatePicker startDate,
            DatePicker endDate
    ) {
        DiscountType safeType = Objects.requireNonNullElse(selectedType, DiscountType.SIMPLE);
        DiscountConditionType safeCondition = Objects.requireNonNullElse(selectedCondition, DiscountConditionType.MIN_TICKET);

        boolean coupon = safeType == DiscountType.COUPON;
        boolean conditional = safeType == DiscountType.CONDITIONAL;
        boolean ticketCondition = conditional && safeCondition.requiresTicketThreshold();
        boolean dateCondition = conditional && safeCondition.requiresDateRange();

        couponCode.setVisible(coupon);
        validUntil.setVisible(coupon);
        conditionType.setVisible(conditional);
        ticketThreshold.setVisible(ticketCondition);
        startDate.setVisible(dateCondition);
        endDate.setVisible(dateCondition);
    }

    private DatePicker createPolicyDatePicker(String label, LocalDate value) {
        DatePicker datePicker = new DatePicker(label);
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
        i18n.setDateFormat(DATE_PICKER_DISPLAY_FORMAT);
        i18n.setFirstDayOfWeek(0);
        datePicker.setI18n(i18n);
        datePicker.setLocale(Locale.forLanguageTag("he-IL"));
        datePicker.setClearButtonVisible(true);
        datePicker.setPlaceholder("DD/MM/YY");
        datePicker.setValue(value);
        return datePicker;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
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
    }

    private void loadDraftForCompany(String companyId) {
        try {
            Long parsedCompanyId = Long.parseLong(companyId);
            String token = UiSession.getMemberToken();

            PurchasePolicyExpressionDraftDTO purchaseDraft = presenter.loadPurchasePolicy(
                    token,
                    parsedCompanyId
            );

            DiscountPolicyDraftDTO discountDraft = presenter.loadDiscountPolicy(
                    token,
                    parsedCompanyId
            );

            applyPurchasePolicyDraft(purchaseDraft);
            applyDiscountPolicyDraft(discountDraft);
            refreshVisiblePolicySections();
        
        } catch (PresentationException e) {
            if (PresentationException.isSessionTimeoutMessage(e.getMessage())) {
                UiSession.handleTimeoutRedirect();
                return;
            }
            showError(e.getMessage());
            resetDraft();
            refreshVisiblePolicySections();

        } catch (Exception e) {
            showError("שגיאה בטעינת המדיניות: " + e.getMessage());
            resetDraft();
            refreshVisiblePolicySections();
        }
    }

    private void applyPurchasePolicyDraft(PurchasePolicyExpressionDraftDTO draft) {
        if (draft == null || draft.root() == null) {
            purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
            refreshPurchaseExpression();
            return;
        }

        purchasePolicyRoot = PurchaseExpressionNode.fromDraft(draft.root());
        refreshPurchaseExpression();
    }

    private void applyDiscountPolicyDraft(DiscountPolicyDraftDTO draft) {
        discounts.clear();

        if (draft == null) {
            setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM);
            refreshDiscounts();
            return;
        }

        setDiscountCompositionStrategy(draft.compositionStrategy());

        if (draft.discounts() != null) {
            discounts.addAll(draft.discounts());
        }

        refreshDiscounts();
    }

    private void resetDraft() {
        purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);

        PurchaseExpressionNode firstGroup = PurchaseExpressionNode.group(LogicalOperator.OR);
        firstGroup.children().add(PurchaseExpressionNode.rule(
                new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.AGE, 18, "שנים")
        ));
        firstGroup.children().add(PurchaseExpressionNode.rule(
                new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, 5, "לרוכש")
        ));

        PurchaseExpressionNode secondGroup = PurchaseExpressionNode.group(LogicalOperator.OR);
        secondGroup.children().add(PurchaseExpressionNode.rule(
                new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MIN_TICKETS, 2, "בהזמנה")
        ));
        secondGroup.children().add(PurchaseExpressionNode.rule(
                new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, 8, "בהזמנה")
        ));

        purchasePolicyRoot.children().add(firstGroup);
        purchasePolicyRoot.children().add(secondGroup);

        discounts.clear();

        setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM);
        refreshPurchaseExpression();
        refreshDiscounts();
    }

    private void savePurchaseDraft() {
        try {
            Long parsedCompanyId = Long.parseLong(companyId);
            
            presenter.savePurchasePolicy(
                    UiSession.getMemberToken(),
                    parsedCompanyId,
                    getPurchasePolicyExpressionDraft()
            );
            
            showSuccess("מדיניות הרכישה נשמרה והתעדכנה בהצלחה בחברה.");
            
        } catch (PresentationException e) {
            if (PresentationException.isSessionTimeoutMessage(e.getMessage())) {
                UiSession.handleTimeoutRedirect();
                return;
            }
            showError(e.getMessage());
            
        } catch (Exception e) {
            showError("שגיאה בשמירת מדיניות הרכישה: " + e.getMessage());
        }
    }

    private void saveDiscountDraft() {
        try {
            Long parsedCompanyId = Long.parseLong(companyId);

            presenter.saveCompanyDiscountPolicy(
                    UiSession.getMemberToken(),
                    parsedCompanyId,
                    getDiscountPolicyDraft()
            );

            showSuccess("מדיניות ההנחות נשמרה והתעדכנה בהצלחה בחברה.");
        
        } catch (PresentationException e) {
            if (PresentationException.isSessionTimeoutMessage(e.getMessage())) {
                UiSession.handleTimeoutRedirect();
                return;
            }
            showError(e.getMessage());
        
        } catch (Exception e) {
            showError("שגיאה בשמירת מדיניות ההנחות: " + e.getMessage());
        }
    }

    public PurchasePolicyDraftDTO getPurchasePolicyDraft() {
        return new PurchasePolicyDraftDTO(
                companyId,
                purchasePolicyRoot.operator(),
                collectPurchaseRules(purchasePolicyRoot)
        );
    }

    public PurchasePolicyExpressionDraftDTO getPurchasePolicyExpressionDraft() {
        return new PurchasePolicyExpressionDraftDTO(
                companyId,
                purchasePolicyRoot.toDraft()
        );
    }

    public DiscountPolicyDraftDTO getDiscountPolicyDraft() {
        return new DiscountPolicyDraftDTO(
                companyId,
                discountCompositionStrategy,
                new ArrayList<>(discounts)
        );
    }

    private List<PurchaseRuleDTO> collectPurchaseRules(PurchaseExpressionNode node) {
        List<PurchaseRuleDTO> rules = new ArrayList<>();

        if (node.isRule()) {
            rules.add(node.rule());
            return rules;
        }

        for (PurchaseExpressionNode child : node.children()) {
            rules.addAll(collectPurchaseRules(child));
        }

        return rules;
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

    private String invalidDiscountTicketConditionMessage(List<DiscountConditionDTO> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }

        Integer minTickets = null;
        Integer maxTickets = null;

        for (DiscountConditionDTO condition : conditions) {
            if (condition == null || condition.conditionType() == null || condition.ticketThreshold() == null) {
                continue;
            }

            if (condition.conditionType() == DiscountConditionType.MIN_TICKET) {
                minTickets = minTickets == null
                        ? condition.ticketThreshold()
                        : Math.max(minTickets, condition.ticketThreshold());
            }

            if (condition.conditionType() == DiscountConditionType.MAX_TICKET) {
                maxTickets = maxTickets == null
                        ? condition.ticketThreshold()
                        : Math.min(maxTickets, condition.ticketThreshold());
            }
        }

        if (minTickets != null && maxTickets != null && minTickets > maxTickets) {
            return "מינימום הכרטיסים לא יכול להיות גדול מהמקסימום.";
        }

        return null;
    }

    public enum PolicyTab {
        PURCHASE,
        DISCOUNT
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
        AGE("גיל מינימום"),
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

    public enum DiscountConditionType {
        MIN_TICKET("מינימום כרטיסים", "כמות כרטיסים >=", true, false),
        MAX_TICKET("מקסימום כרטיסים", "כמות כרטיסים <=", true, false),
        DATE("טווח תאריכים", "פעיל בין תאריך התחלה לתאריך סיום", false, true);

        private final String label;
        private final String displayPrefix;
        private final boolean requiresTicketThreshold;
        private final boolean requiresDateRange;

        DiscountConditionType(
                String label,
                String displayPrefix,
                boolean requiresTicketThreshold,
                boolean requiresDateRange
        ) {
            this.label = label;
            this.displayPrefix = displayPrefix;
            this.requiresTicketThreshold = requiresTicketThreshold;
            this.requiresDateRange = requiresDateRange;
        }

        public String getLabel() {
            return label;
        }

        public String getDisplayPrefix() {
            return displayPrefix;
        }

        public boolean requiresTicketThreshold() {
            return requiresTicketThreshold;
        }

        public boolean requiresDateRange() {
            return requiresDateRange;
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

    public enum PurchaseNodeType {
        GROUP,
        RULE
    }

    private static class PurchaseExpressionNode {
        private final String id;
        private final PurchaseNodeType type;
        private LogicalOperator operator;
        private PurchaseRuleDTO rule;
        private final List<PurchaseExpressionNode> children = new ArrayList<>();

        private PurchaseExpressionNode(
                String id,
                PurchaseNodeType type,
                LogicalOperator operator,
                PurchaseRuleDTO rule
        ) {
            this.id = id;
            this.type = type;
            this.operator = operator;
            this.rule = rule;
        }

        static PurchaseExpressionNode group(LogicalOperator operator) {
            return new PurchaseExpressionNode(UUID.randomUUID().toString(), PurchaseNodeType.GROUP, operator, null);
        }

        static PurchaseExpressionNode rule(PurchaseRuleDTO rule) {
            PurchaseRuleDTO safeRule = rule == null ? PurchaseRuleDTO.defaultRule() : rule;
            return new PurchaseExpressionNode(safeRule.id(), PurchaseNodeType.RULE, null, safeRule);
        }

        boolean isRule() {
            return type == PurchaseNodeType.RULE;
        }

        LogicalOperator operator() {
            return operator;
        }

        void setOperator(LogicalOperator operator) {
            this.operator = operator;
        }

        PurchaseRuleDTO rule() {
            return rule;
        }

        void setRule(PurchaseRuleDTO rule) {
            this.rule = rule;
        }

        List<PurchaseExpressionNode> children() {
            return children;
        }

        static PurchaseExpressionNode fromDraft(PurchaseExpressionNodeDTO draft) {
            if (draft == null) {
                return PurchaseExpressionNode.group(LogicalOperator.AND);
            }

            if (draft.type() == PurchaseNodeType.RULE) {
                return PurchaseExpressionNode.rule(draft.rule());
            }

            PurchaseExpressionNode node = new PurchaseExpressionNode(
                    draft.id() == null || draft.id().isBlank() ? UUID.randomUUID().toString() : draft.id(),
                    PurchaseNodeType.GROUP,
                    Objects.requireNonNullElse(draft.operator(), LogicalOperator.AND),
                    null
            );

            if (draft.children() != null) {
                for (PurchaseExpressionNodeDTO child : draft.children()) {
                    node.children().add(PurchaseExpressionNode.fromDraft(child));
                }
            }

            return node;
        }

        PurchaseExpressionNodeDTO toDraft() {
            List<PurchaseExpressionNodeDTO> childDrafts = new ArrayList<>();
            for (PurchaseExpressionNode child : children) {
                childDrafts.add(child.toDraft());
            }

            return new PurchaseExpressionNodeDTO(id, type, operator, rule, childDrafts);
        }
    }

    public record PurchasePolicyDraftDTO(
            String companyId,
            LogicalOperator rootOperator,
            List<PurchaseRuleDTO> rules
    ) {
    }

    public record PurchasePolicyExpressionDraftDTO(
            String companyId,
            PurchaseExpressionNodeDTO root
    ) {
    }

    public record PurchaseExpressionNodeDTO(
            String id,
            PurchaseNodeType type,
            LogicalOperator operator,
            PurchaseRuleDTO rule,
            List<PurchaseExpressionNodeDTO> children
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
            int value,
            String unit
    ) {
        public static PurchaseRuleDTO defaultRule() {
            return new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, 5, "לרוכש");
        }

        public PurchaseRuleDTO copy() {
            return new PurchaseRuleDTO(id, field, value, unit);
        }

        public String toDisplayText() {
            return field.getLabel() + ": " + value + (unit == null || unit.isBlank() ? "" : " " + unit);
        }

        public String toTechnicalText() {
            return field.name() + " = " + value;
        }
    }
     public record DiscountConditionDTO(

        DiscountConditionType conditionType,

        Integer ticketThreshold,

        LocalDateTime startTime,

        LocalDateTime endTime

) {}

    
    public record DiscountDTO(
            String id,
            String name,
            DiscountType type,
            DiscountValueType valueType,
            double value,
            String couponCode,
            LocalDate validUntil,
            List<DiscountConditionDTO> conditions
    ) {
        public static DiscountDTO defaultDiscount() {
            return new DiscountDTO(
                    UUID.randomUUID().toString(),
                    "הנחה חדשה",
                    DiscountType.SIMPLE,
                    DiscountValueType.PERCENTAGE,
                    10,
                    "",
                    null,null
            );
        }
        public DiscountDTO withConditions(List<DiscountConditionDTO> newConditions) {
            return new DiscountDTO(
                    id,
                    name,
                    type,
                    valueType,
                    value,
                    couponCode,
                    validUntil,
                    newConditions
            );
        }
        public String conditionText() {
            if (type != DiscountType.CONDITIONAL || conditions == null || conditions.isEmpty()) {
                return "";
            }

            return conditions.stream()
                    .map(this::singleConditionText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(java.util.stream.Collectors.joining(" וגם "));
        }

        private String singleConditionText(DiscountConditionDTO condition) {
            if (condition == null || condition.conditionType() == null) {
                return "";
            }

            DiscountConditionType conditionType = condition.conditionType();

            if (conditionType.requiresTicketThreshold()) {
                if (condition.ticketThreshold() == null) {
                    return "";
                }

                return conditionType.getDisplayPrefix() + " " + condition.ticketThreshold();
            }

            if (conditionType.requiresDateRange()) {
                if (condition.startTime() != null && condition.endTime() != null) {
                    return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME)
                            + " עד " + condition.endTime().format(DISPLAY_DATE_TIME);
                }

                if (condition.endTime() != null) {
                    return "תאריך עד " + condition.endTime().format(DISPLAY_DATE_TIME);
                }

                if (condition.startTime() != null) {
                    return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME);
                }

                return "";
            }

            return "";
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
