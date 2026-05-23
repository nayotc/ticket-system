package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Route(value = UiRoutes.ROLES_AND_PERMISSIONS_TREE, layout = ManagementLayout.class)
public class RolesTree extends Div implements BeforeEnterObserver {

    private Long companyId = 1L;

    private final Span liveBadge = new Span("LIVE ROLE TREE");
    private final H1 title = new H1("עץ תפקידים והרשאות");
    private final Paragraph description = new Paragraph();

    private final Span companyName = new Span("חברת הפקה");
    private final Span companyStatus = new Span("פעילה");

    private final TextField searchField = new TextField();
    private final ComboBox<String> roleFilter = new ComboBox<>();
    private final ComboBox<String> permissionFilter = new ComboBox<>();

    private final Div treeCanvas = new Div();
    private final Div metricsGrid = new Div();

    private RoleNode rootNode;

    public RolesTree() {
        getElement().setAttribute("dir", "rtl");
        addClassName("role-tree-view");

        configureFilters();
        rootNode = createPreviewTree();

        add(
                createHeader(),
                createMetrics(),
                createToolbar(),
                createTreePanel()
        );

        renderTree();
        updateMetrics();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters()
                .get("companyId")
                .ifPresent(this::setCompanyIdFromRoute);

        updateHeaderText();
    }

    private void setCompanyIdFromRoute(String routeCompanyId) {
        try {
            this.companyId = Long.parseLong(routeCompanyId);
        } catch (NumberFormatException ignored) {
            this.companyId = 1L;
        }
    }

    private Component createHeader() {
        Div header = new Div();
        header.addClassName("role-tree-header");

        Div textBlock = new Div();
        textBlock.addClassName("role-tree-header-text");

        liveBadge.addClassName("role-tree-live-badge");

        updateHeaderText();

        textBlock.add(liveBadge, title, description);

        Div companyBlock = new Div();
        companyBlock.addClassName("role-tree-company-card");

        Span companyLabel = new Span("חברה נוכחית");
        companyLabel.addClassName("role-tree-company-label");

        companyName.addClassName("role-tree-company-name");
        companyStatus.addClassName("role-tree-company-status");

        companyBlock.add(companyLabel, companyName, companyStatus);

        header.add(textBlock, companyBlock);
        return header;
    }

    private Component createMetrics() {
        metricsGrid.addClassName("role-tree-metrics");

        metricsGrid.add(
                new MetricCard("סה״כ בעלי תפקידים", "6", "כולל מייסד, בעלים ומנהלים"),
                new MetricCard("בעלים", "3", "כולל מייסד החברה"),
                new MetricCard("מנהלים", "3", "לפי הרשאות פרטניות"),
                new MetricCard("הרשאות פעילות", "6", "הרשאות ניהול זמינות")
        );

        return metricsGrid;
    }

    private Component createToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("role-tree-toolbar");

        Div filters = new Div();
        filters.addClassName("role-tree-filters");
        filters.add(searchField, roleFilter, permissionFilter);

        Div actions = new Div();
        actions.addClassName("role-tree-actions");

        Button refreshButton = new Button("רענון");
        refreshButton.addClassName("role-tree-action-button");
        refreshButton.addClickListener(event -> {
            renderTree();
            Notification.show("עץ התפקידים רוענן", 2500, Notification.Position.TOP_CENTER);
        });

        Button exportButton = new Button("ייצוא PDF");
        exportButton.addClassName("role-tree-action-button");
        exportButton.addClickListener(event ->
                Notification.show("ייצוא PDF יחובר בהמשך דרך ה־Presenter", 3000, Notification.Position.TOP_CENTER)
        );

        actions.add(refreshButton, exportButton);

        toolbar.add(filters, actions);
        return toolbar;
    }

    private Component createTreePanel() {
        Div panel = new Div();
        panel.addClassName("role-tree-panel");

        treeCanvas.addClassName("role-tree-canvas");

        panel.add(treeCanvas);
        return panel;
    }

    private void configureFilters() {
        searchField.setLabel("חיפוש");
        searchField.setPlaceholder("חיפוש לפי שם, תפקיד או הרשאה");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addClassName("role-tree-search");
        searchField.addValueChangeListener(event -> renderTree());

        roleFilter.setLabel("סוג תפקיד");
        roleFilter.setItems("כל התפקידים", "מייסד", "בעלים", "מנהל");
        roleFilter.setValue("כל התפקידים");
        roleFilter.addClassName("role-tree-filter");
        roleFilter.addValueChangeListener(event -> renderTree());

        permissionFilter.setLabel("הרשאה");
        permissionFilter.setItems(
                "כל ההרשאות",
                "ניהול אירועים",
                "מפת אולם",
                "מדיניות רכישה",
                "מדיניות הנחות",
                "פניות",
                "דוח מכירות"
        );
        permissionFilter.setValue("כל ההרשאות");
        permissionFilter.addClassName("role-tree-filter");
        permissionFilter.addValueChangeListener(event -> renderTree());
    }

    private void renderTree() {
        treeCanvas.removeAll();

        RoleNode filteredRoot = filterTree(rootNode);

        if (filteredRoot == null) {
            Div empty = new Div();
            empty.addClassName("role-tree-empty");
            empty.setText("לא נמצאו בעלי תפקידים שמתאימים לסינון.");
            treeCanvas.add(empty);
            return;
        }

        Div tree = new Div();
        tree.addClassName("role-tree-structure");
        tree.add(createBranch(filteredRoot, 0));

        treeCanvas.add(tree);
    }

    private Component createBranch(RoleNode node, int depth) {
        Div branch = new Div();
        branch.addClassName("role-tree-branch");
        branch.addClassName("role-tree-depth-" + depth);

        branch.add(createNodeCard(node, depth));

        if (!node.children().isEmpty()) {
            Div children = new Div();
            children.addClassName("role-tree-children");

            for (RoleNode child : node.children()) {
                children.add(createBranch(child, depth + 1));
            }

            branch.add(children);
        }

        return branch;
    }

    private Component createNodeCard(RoleNode node, int depth) {
        Div card = new Div();
        card.addClassName("role-tree-node-card");
        card.addClassName("role-tree-node-" + node.kind().name().toLowerCase(Locale.ROOT));

        Div top = new Div();
        top.addClassName("role-tree-node-top");

        Span avatar = new Span(createInitials(node.name()));
        avatar.addClassName("role-tree-node-avatar");

        Div identity = new Div();
        identity.addClassName("role-tree-node-identity");

        H3 name = new H3(node.name());
        name.addClassName("role-tree-node-name");

        Span role = new Span(node.roleTitle());
        role.addClassName("role-tree-node-role");

        identity.add(name, role);
        top.add(avatar, identity);

        Paragraph helper = new Paragraph(node.description());
        helper.addClassName("role-tree-node-description");

        Div meta = new Div();
        meta.addClassName("role-tree-node-meta");

        Span appointedBy = new Span(node.appointedByText());
        appointedBy.addClassName("role-tree-node-appointed-by");

        Span childCount = new Span(node.children().size() + " כפיפים ישירים");
        childCount.addClassName("role-tree-node-child-count");

        meta.add(appointedBy, childCount);

        Div permissions = new Div();
        permissions.addClassName("role-tree-permissions");

        for (String permission : node.permissions()) {
            Span chip = new Span(permission);
            chip.addClassName("role-tree-permission-chip");
            permissions.add(chip);
        }

        card.add(top, helper, meta, permissions);
        return card;
    }

    private RoleNode filterTree(RoleNode node) {
        if (node == null) {
            return null;
        }

        List<RoleNode> filteredChildren = new ArrayList<>();

        for (RoleNode child : node.children()) {
            RoleNode filteredChild = filterTree(child);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        boolean matches = matchesSearch(node) && matchesRole(node) && matchesPermission(node);
        boolean filtersAreEmpty = filtersAreEmpty();

        if (filtersAreEmpty || matches || !filteredChildren.isEmpty()) {
            return new RoleNode(
                    node.memberId(),
                    node.name(),
                    node.roleTitle(),
                    node.kind(),
                    node.description(),
                    node.appointedByText(),
                    node.permissions(),
                    filteredChildren
            );
        }

        return null;
    }

    private boolean filtersAreEmpty() {
        return isBlank(searchField.getValue())
                && "כל התפקידים".equals(roleFilter.getValue())
                && "כל ההרשאות".equals(permissionFilter.getValue());
    }

    private boolean matchesSearch(RoleNode node) {
        String value = searchField.getValue();

        if (isBlank(value)) {
            return true;
        }

        String normalized = normalize(value);

        return normalize(node.name()).contains(normalized)
                || normalize(node.roleTitle()).contains(normalized)
                || normalize(node.description()).contains(normalized)
                || node.permissions().stream().anyMatch(permission -> normalize(permission).contains(normalized));
    }

    private boolean matchesRole(RoleNode node) {
        String selected = roleFilter.getValue();

        if (selected == null || "כל התפקידים".equals(selected)) {
            return true;
        }

        return switch (selected) {
            case "מייסד" -> node.kind() == RoleKind.FOUNDER;
            case "בעלים" -> node.kind() == RoleKind.OWNER;
            case "מנהל" -> node.kind() == RoleKind.MANAGER;
            default -> true;
        };
    }

    private boolean matchesPermission(RoleNode node) {
        String selected = permissionFilter.getValue();

        if (selected == null || "כל ההרשאות".equals(selected)) {
            return true;
        }

        return node.permissions().contains(selected);
    }

    private void updateHeaderText() {
        description.setText(
                "תצוגה היררכית של בעלי החברה, מנהלים והרשאות ניהול עבור חברה מספר " + companyId + "."
        );
    }

    private void updateMetrics() {
        metricsGrid.removeAll();

        int total = countNodes(rootNode);
        int owners = countByKind(rootNode, RoleKind.FOUNDER) + countByKind(rootNode, RoleKind.OWNER);
        int managers = countByKind(rootNode, RoleKind.MANAGER);
        int permissions = countDistinctPermissions(rootNode);

        metricsGrid.add(
                new MetricCard("סה״כ בעלי תפקידים", String.valueOf(total), "כולל כל העץ"),
                new MetricCard("בעלים", String.valueOf(owners), "כולל מייסד החברה"),
                new MetricCard("מנהלים", String.valueOf(managers), "לפי הרשאות פרטניות"),
                new MetricCard("הרשאות פעילות", String.valueOf(permissions), "הרשאות ניהול זמינות")
        );
    }

    public void showCompany(CompanyDTO company) {
        if (company == null) {
            return;
        }

        this.companyId = company.getId();
        companyName.setText(company.getName());
        companyStatus.setText(company.isActive() ? "פעילה" : "לא פעילה");
        updateHeaderText();
    }

    public void showRoleTree(RoleNode rootNode) {
        if (rootNode == null) {
            return;
        }

        this.rootNode = rootNode;
        renderTree();
        updateMetrics();
    }

    public void showError(String message) {
        Notification.show(message, 4000, Notification.Position.TOP_CENTER);
    }

    private RoleNode createPreviewTree() {
        RoleNode backendManager = new RoleNode(
                201L,
                "תומר לוי",
                "מנהל Backend",
                RoleKind.MANAGER,
                "אחראי על שירותי ליבה, הזמנות ומלאי.",
                "מונה על ידי יעל רון",
                List.of("ניהול אירועים", "דוח מכירות"),
                List.of()
        );

        RoleNode policyManager = new RoleNode(
                202L,
                "שירה גל",
                "מנהלת מדיניות",
                RoleKind.MANAGER,
                "אחראית על מדיניות רכישה והנחות.",
                "מונתה על ידי יעל רון",
                List.of("מדיניות רכישה", "מדיניות הנחות"),
                List.of()
        );

        RoleNode operationsManager = new RoleNode(
                203L,
                "נועה ברק",
                "מנהלת תפעול אירועים",
                RoleKind.MANAGER,
                "אחראית על מפות אולם, פניות ותפעול שוטף.",
                "מונתה על ידי אדם שוורץ",
                List.of("מפת אולם", "פניות", "ניהול אירועים"),
                List.of()
        );

        RoleNode ownerTech = new RoleNode(
                101L,
                "יעל רון",
                "בעלת חברה",
                RoleKind.OWNER,
                "בעלת הרשאות ניהול מלאות בחברה.",
                "מונתה על ידי מייסד החברה",
                List.of("ניהול אירועים", "מפת אולם", "מדיניות רכישה", "מדיניות הנחות", "פניות", "דוח מכירות"),
                List.of(backendManager, policyManager)
        );

        RoleNode ownerProduct = new RoleNode(
                102L,
                "אדם שוורץ",
                "בעל חברה",
                RoleKind.OWNER,
                "בעל הרשאות ניהול מלאות בחברה.",
                "מונה על ידי מייסד החברה",
                List.of("ניהול אירועים", "מפת אולם", "פניות", "דוח מכירות"),
                List.of(operationsManager)
        );

        return new RoleNode(
                1L,
                "דוד כהן",
                "מייסד החברה",
                RoleKind.FOUNDER,
                "פתח את חברת ההפקה ולכן אינו ממונה על ידי משתמש אחר.",
                "ללא ממנה",
                List.of("ניהול אירועים", "מפת אולם", "מדיניות רכישה", "מדיניות הנחות", "פניות", "דוח מכירות"),
                List.of(ownerTech, ownerProduct)
        );
    }

    private int countNodes(RoleNode node) {
        if (node == null) {
            return 0;
        }

        int count = 1;

        for (RoleNode child : node.children()) {
            count += countNodes(child);
        }

        return count;
    }

    private int countByKind(RoleNode node, RoleKind kind) {
        if (node == null) {
            return 0;
        }

        int count = node.kind() == kind ? 1 : 0;

        for (RoleNode child : node.children()) {
            count += countByKind(child, kind);
        }

        return count;
    }

    private int countDistinctPermissions(RoleNode node) {
        List<String> permissions = new ArrayList<>();
        collectPermissions(node, permissions);
        return (int) permissions.stream().distinct().count();
    }

    private void collectPermissions(RoleNode node, List<String> permissions) {
        if (node == null) {
            return;
        }

        permissions.addAll(node.permissions());

        for (RoleNode child : node.children()) {
            collectPermissions(child, permissions);
        }
    }

    private String createInitials(String name) {
        if (isBlank(name)) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1);
        }

        return parts[0].substring(0, 1) + parts[1].substring(0, 1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum RoleKind {
        FOUNDER,
        OWNER,
        MANAGER
    }

    public record RoleNode(
            Long memberId,
            String name,
            String roleTitle,
            RoleKind kind,
            String description,
            String appointedByText,
            List<String> permissions,
            List<RoleNode> children
    ) {
    }
}