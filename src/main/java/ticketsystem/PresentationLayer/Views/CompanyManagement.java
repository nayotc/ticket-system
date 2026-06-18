package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Session.UiSession;

import ticketsystem.PresentationLayer.Presenters.CompanyPresenter;
import ticketsystem.PresentationLayer.Presenters.MembershipPresenter;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.CompanyStats;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.EventManagementItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.ManagedCompanyItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.PolicySummary;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.RoleType;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.TeamMemberItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("TixNow | Company Management")
@Route(value = UiRoutes.COMPANY_MANAGEMENT, layout = ManagementLayout.class)
public class CompanyManagement extends Div implements BeforeEnterObserver {

    private final MembershipPresenter membershipPresenter;
    private final CompanyPresenter companyPresenter;
    private CompanyManagementState state;

    private final TextField managerNameInput = new TextField("שם משתמש למינוי או עדכון מנהל");
    private final TextField ownerNameInput = new TextField("שם משתמש למינוי בעלים");

    // private final TextField managerMemberId = new TextField("מזהה מנוי למינוי מנהל");
    // private final TextField ownerMemberId = new TextField("מזהה מנוי למינוי בעלים");

    private final CheckboxGroup<Permission> managerPermissions = new CheckboxGroup<>("הרשאות מנהל");

    public CompanyManagement() {
        this(null, null);
    }

    @Autowired
    public CompanyManagement(MembershipPresenter membershipPresenter, CompanyPresenter companyPresenter) {
        this.membershipPresenter = membershipPresenter;
        this.companyPresenter = companyPresenter;
        
        addClassName("company-management-page");
        getElement().setAttribute("dir", "rtl");
        configureFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Long companyId = event.getRouteParameters()
                .get("companyId")
                .map(this::safeParseLong)
                .orElse(null);

        loadState(companyId);
    }

    private void configureFields() {
        managerNameInput.setPlaceholder("לדוגמה: user@test.com");
        managerNameInput.setWidthFull();

        ownerNameInput.setPlaceholder("לדוגמה: owner@test.com");
        ownerNameInput.setWidthFull();

        managerPermissions.setItems(Permission.values());
        managerPermissions.setItemLabelGenerator(this::permissionLabel);
        managerPermissions.addClassName("manager-permissions-group");
    }

    private void loadState(Long requestedCompanyId) {
        try {
            if (membershipPresenter == null) {
                state = demoState(requestedCompanyId);
            } else {
                state = membershipPresenter.loadCompanyManagement(UiSession.getMemberToken(), requestedCompanyId);
            }
            render();
        } catch (Exception e) {
            renderLoadError(e.getMessage());
        }
    }

    private void render() {
        removeAll();

        if (state == null || state.companies().isEmpty()) {
            add(new EmptyState(
                    "🏢",
                    "אין חברות לניהול",
                    "כאשר המשתמש ינהל לפחות חברת הפקה אחת, עמוד הניהול יציג כאן את הפעולות הרלוונטיות.",
                    createNavigateButton("פתיחת חברת הפקה", UiRoutes.CREATE_PRODUCTION_COMPANY, VaadinIcon.PLUS)
            ));
            return;
        }

        PageContainer page = new PageContainer(
                createHeader(),
                createMetricsGrid(),
                createMainGrid()
        );

        add(page);
    }

    private Component createHeader() {
        return new ViewHeader(
                "ניהול חברה",
                "נהל אירועים, צוות, הרשאות ופעולות בעלים עבור חברת ההפקה שנבחרה.",
                createCompanySelector()
        );
    }

    private Div createCompanySelector() {
        Div wrapper = new Div();
        wrapper.addClassName("company-selector-wrapper");

        if (state.companies().size() > 1) {
            ComboBox<ManagedCompanyItem> selector = new ComboBox<>("בחר חברה");
            selector.setItemLabelGenerator(ManagedCompanyItem::name);
            selector.addClassName("company-selector");
            selector.setItems(state.companies());
            selector.setValue(state.selectedCompany());
            selector.addValueChangeListener(event -> {
                ManagedCompanyItem selected = event.getValue();
                if (selected != null && selected.id() != state.selectedCompany().id()) {
                    UI.getCurrent().navigate(routeFor(UiRoutes.COMPANY_MANAGEMENT, selected.id()));
                }
            });
            wrapper.add(selector);
        } else {
            Span label = new Span("חברה נבחרת");
            label.addClassName("company-single-label");
            Span name = new Span(state.selectedCompany().name());
            name.addClassName("company-single-name");
            wrapper.add(label, name);
        }

        return wrapper;
    }

    private Component createMetricsGrid() {
        Div grid = new Div();
        grid.addClassName("company-management-metrics-grid");

        StatusBadge.Type statusType = state.selectedCompany().active()
                ? StatusBadge.Type.SUCCESS
                : StatusBadge.Type.ERROR;

        MetricCard status = new MetricCard(
                "סטטוס חברה",
                state.selectedCompany().active() ? "פעילה" : "מושהית",
                "מייסד: " + state.selectedCompany().founderEmailOrName()
        );
        status.add(new StatusBadge(state.selectedCompany().active() ? "פתוחה למכירה" : "לא פעילה", statusType));

        grid.add(
                status,
                new MetricCard("אירועים פעילים", String.valueOf(state.stats().activeEvents()), "אירועים שפתוחים לניהול"),
                new MetricCard("חברי צוות", String.valueOf(state.teamMembers().size()), "בעלים ומנהלים בחברה"),
                new MetricCard("בקשות מינוי", String.valueOf(state.stats().pendingAssignments()), "בקשות שממתינות לאישור")
        );

        return grid;
    }

    private Component createMainGrid() {
        Div grid = new Div();
        grid.addClassName("company-management-grid");

        grid.add(
                createPolicyCard(),
                createEventManagementCard(),
                createTeamManagementCard()
        );

        if (state.owner()) {
            grid.add(createOwnerZoneCard());
        }

        return grid;
    }

    private Component createPolicyCard() {
        AppCard card = managementCard(
                "מדיניות רכישה והנחות",
                "המדיניות נערכות בעמוד ייעודי כדי להשאיר את עמוד ניהול החברה נקי וברור.",
                VaadinIcon.CLIPBOARD_CHECK
        );
        card.addClassName("company-card-half");

        Div policyList = new Div();
        policyList.addClassName("policy-summary-list");

        policyList.add(
                summaryRow("מדיניות רכישה", state.policySummary().purchasePolicySummary()),
                summaryRow("מדיניות הנחות", state.policySummary().discountPolicySummary())
        );

        card.add(policyList);

        // הכפתור ייווצר ויתווסף אך ורק אם למשתמש יש הרשאת ניהול מדיניות
        if (state.canManagePolicies()) {
            Button openEditor = createNavigateButton("מעבר לעורך מדיניות", routeFor(UiRoutes.POLICIES_EDITOR), VaadinIcon.ARROW_FORWARD);
            openEditor.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            card.add(openEditor);
        }
        
        return card;
    }

    private Component createEventManagementCard() {
        AppCard card = managementCard("ניהול אירועים", "כל אירוע מוצג עם פעולה ישירה לעריכה או ביטול.", VaadinIcon.TICKET);
        card.addClassName("company-card-half");

        if (state.events().isEmpty()) {
            card.add(new EmptyState("🎫", "אין אירועים להצגה", "לא נמצאו אירועים זמינים לניהול עבור החברה שנבחרה.", null));
            return card;
        }

        Div list = new Div();
        list.addClassName("event-management-list");

        for (EventManagementItem eventItem : state.events()) {
            list.add(eventManagementRow(eventItem));
        }

        card.add(list);
        return card;
    }

    private Component eventManagementRow(EventManagementItem eventItem) {
        Div row = new Div();
        row.addClassName("event-management-row");

        boolean cancelled = isCancelledStatus(eventItem.status());

        if (cancelled) {
            row.addClassName("event-management-row-cancelled");
        }

        Div details = new Div();
        details.addClassName("event-management-details");

        Span title = new Span(eventItem.title());
        title.addClassName("event-management-title");

        Span meta = new Span("סטטוס: " + eventItem.status());
        meta.addClassName("event-management-meta");

        details.add(title, meta);

        Div actions = new Div();
        actions.addClassName("event-management-actions");

        if (state.canManageEvents()) {
            Button edit = new Button("ניהול", VaadinIcon.COG.create());
            edit.addClassName("company-secondary-button");
            edit.addClickListener(event -> navigateToEventEditor(eventItem));

            Button cancel = new Button(
                    cancelled ? "מבוטל" : "בטל",
                    cancelled ? VaadinIcon.LOCK.create() : VaadinIcon.CLOSE_CIRCLE.create()
            );

            cancel.addClassName("company-danger-soft-button");

            if (cancelled) {
                cancel.addClassName("tn-disabled-action-button");
                cancel.setEnabled(false);
                cancel.getElement().setAttribute("title", "האירוע כבר מבוטל");
                cancel.getElement().setAttribute("aria-label", "האירוע כבר מבוטל");
            } else {
                cancel.addClickListener(event -> confirmCancelEvent(eventItem));
            }

            actions.add(edit, cancel);
        }

        row.add(details, actions);
        return row;
    }

    private boolean isCancelledStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }

        String normalized = status.trim().toUpperCase();
        return normalized.equals("CANCELLED")
                || normalized.equals("CANCELLATION_PENDING");
    }

    private Component createTeamManagementCard() {
        AppCard card = managementCard("מינוי צוות", "מינוי מנהל מתבצע לפי מזהה מנוי והרשאות. מינוי בעלים נמצא באזור הבעלים.", VaadinIcon.GROUP);
        card.addClassName("company-card-team");

        if (!state.canManageTeam()) {
            card.add(new EmptyState(
                    "🔒",
                    "אין הרשאת מינוי צוות",
                    "רק בעלים או משתמש שקיבל הרשאה מתאימה יוכל לבצע מינוי וניהול הרשאות.",
                    null
            ));
            return card;
        }

        Div layout = new Div();
        layout.addClassName("team-management-layout");
        layout.add(createManagerAssignmentForm(), createTeamList());

        card.add(layout);
        return card;
    }

    private Component createManagerAssignmentForm() {
        Div form = new Div();
        form.addClassName("team-form-card");

        H4 title = new H4("מינוי או עדכון מנהל");
        title.addClassName("team-form-title");

        Button assign = new Button("שלח בקשת מינוי", VaadinIcon.USER_CHECK.create());
        assign.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        assign.addClickListener(event -> requestManagerAssignment());

        Button update = new Button("עדכן הרשאות", VaadinIcon.EDIT.create());
        update.addClassName("company-secondary-button");
        update.addClickListener(event -> updateManagerPermissions());

        HorizontalLayout actions = new HorizontalLayout(assign, update);
        actions.addClassName("company-form-actions");

        form.add(title, managerNameInput, managerPermissions, actions);
        return form;
    }

    private Component createTeamList() {
        Div list = new Div();
        list.addClassName("team-list");

        H4 title = new H4("צוות נוכחי");
        title.addClassName("team-form-title");
        list.add(title);

        for (TeamMemberItem member : state.teamMembers()) {
            list.add(teamMemberRow(member));
        }

        return list;
    }

    private Component teamMemberRow(TeamMemberItem member) {
        Div row = new Div();
        row.addClassName("team-member-row");

        Div details = new Div();
        details.addClassName("team-member-details");

        Span name = new Span(member.userName());
        name.addClassName("team-member-name");

        Span meta = new Span("תפקיד: " + member.roleLabel());
        meta.addClassName("team-member-meta");

        String permsText = member.permissionLabels(this::permissionLabel);
        Span permissions = new Span(permsText.isBlank() ? "ללא הרשאות פרטניות" : permsText);
        permissions.addClassName("team-member-permissions");

        details.add(name, meta, permissions);

        Div actions = new Div();
        actions.addClassName("team-member-actions");

        if (member.roleType() == RoleType.MANAGER) {
            Button fill = new Button("ערוך", VaadinIcon.EDIT.create());
            fill.addClassName("company-secondary-button");
            fill.addClickListener(event -> fillManagerForm(member));
            actions.add(fill);
        }

        if (member.removable()) {
            Button remove = new Button("הסר", VaadinIcon.TRASH.create());
            remove.addClassName("company-danger-soft-button");
            remove.addClickListener(event -> confirmRemoveTeamMember(member));
            actions.add(remove);
        }

        row.add(details, actions);
        return row;
    }

    private Component createOwnerZoneCard() {
        AppCard card = managementCard("אזור בעלים", "פעולות שמשפיעות על מצב החברה ועל תפקיד הבעלות שלך.", VaadinIcon.SHIELD);
        card.addClassName("company-card-owner-zone");

        Div content = new Div();
        content.addClassName("owner-zone-content");
        content.add(createOwnerAssignmentSection(), createOwnerActionsSection());

        card.add(content);
        return card;
    }

    private Component createOwnerAssignmentSection() {
        Div section = new Div();
        section.addClassName("owner-assignment-section");

        H4 title = new H4("מינוי בעלים נוסף");
        title.addClassName("team-form-title");

        Paragraph description = new Paragraph("בעלים מקבל הרשאות ניהול מלאות בחברה. המשתמש הממונה יצטרך לאשר את המינוי.");
        description.addClassName("muted-text");

        Button assign = new Button("שלח בקשת מינוי", VaadinIcon.STAR.create());
        assign.addClassName("company-danger-soft-button");
        assign.setWidthFull();
        assign.addClickListener(event -> requestOwnerAssignment());

        section.add(title, description, ownerNameInput, assign);
        return section;
    }

    private Component createOwnerActionsSection() {
        Div actions = new Div();
        actions.addClassName("owner-actions");

        // 1. כפתורי השהייה ופתיחה מחדש - מוגבלים למייסד בלבד
        if (state.founder()) {
            Button close = new Button("השהה חברה", VaadinIcon.BAN.create());
            close.addClassName("company-secondary-button");
            close.setWidthFull();
            close.setEnabled(state.selectedCompany().active());
            close.addClickListener(event -> confirmOwnerAction(
                    "השהיית חברה",
                    "החברה תהפוך ללא פעילה ולא תופיע בתוצאות החיפוש הכללי.",
                    () -> runPresenterAction(
                            () -> companyPresenter.closeProductionCompany(UiSession.getMemberToken(), state.selectedCompany().id()),
                            "החברה הושהתה בהצלחה"
                    )
            ));

            Button reopen = new Button("פתח מחדש חברה", VaadinIcon.PLAY.create());
            reopen.addClassName("company-secondary-button");
            reopen.setWidthFull();
            reopen.setEnabled(!state.selectedCompany().active());
            reopen.addClickListener(event -> confirmOwnerAction(
                    "פתיחת חברה מחדש",
                    "החברה תחזור להיות פעילה ותוכל להופיע בתוצאות החיפוש.",
                    () -> runPresenterAction(
                            () -> companyPresenter.reopenProductionCompany(UiSession.getMemberToken(), state.selectedCompany().id()),
                            "החברה נפתחה מחדש"
                    )
            ));

            // הוספת כפתורי המייסד לאזור הפעולות
            actions.add(close, reopen);
        }

        // 2. כפתור ויתור בעלות - זמין לכל מי שיש לו בעלות (Founder או Owner)
        Button resign = new Button("ויתור בעלות", VaadinIcon.WARNING.create());
        resign.addClassName("company-danger-button");
        resign.setWidthFull();
        resign.addClickListener(event -> confirmOwnerAction(
                "ויתור על בעלות",
                "הפעולה תסיר ממך את תפקיד הבעלים בהתאם ללוגיקת המינויים במערכת.",
                () -> runPresenterAction(
                        () -> membershipPresenter.giveUpOwnership(UiSession.getMemberToken(), state.selectedCompany().id()),
                        "ויתור הבעלות בוצע בהצלחה"
                )
        ));

        // הוספת כפתור ויתור הבעלות לאזור הפעולות
        actions.add(resign);
        
        return actions;
    }

    private AppCard managementCard(String titleText, String descriptionText, VaadinIcon icon) {
        AppCard card = new AppCard();
        card.addClassName("company-management-card");

        Div titleRow = new Div();
        titleRow.addClassName("company-card-title-row");

        Span iconElement = new Span();
        iconElement.add(icon.create());
        iconElement.addClassName("company-card-icon");

        Div text = new Div();
        H3 title = new H3(titleText);
        title.addClassName("company-card-title");
        Paragraph description = new Paragraph(descriptionText);
        description.addClassName("company-card-description");

        text.add(title, description);
        titleRow.add(iconElement, text);
        card.add(titleRow);

        return card;
    }

    private Component summaryRow(String title, String value) {
        Div row = new Div();
        row.addClassName("policy-summary-row");
        row.add(new Span(title), new Span(value));
        return row;
    }

    private Button createNavigateButton(String text, String route, VaadinIcon icon) {
        Button button = new Button(text, icon.create());
        button.addClassName("company-secondary-button");
        button.addClickListener(event -> UI.getCurrent().navigate(route));
        return button;
    }

    private String routeFor(String routeTemplate) {
        return routeFor(routeTemplate, state.selectedCompany().id());
    }

    private String routeFor(String routeTemplate, long companyId) {
        return routeTemplate.replace(":companyId", String.valueOf(companyId));
    }

    private void navigateToEventEditor(EventManagementItem eventItem) {
        String route = UiRoutes.EDIT_EVENT
                .replace(":companyId", String.valueOf(state.selectedCompany().id()))
                .replace(":eventId", String.valueOf(eventItem.eventId()));
        UI.getCurrent().navigate(route);
    }

    private void confirmCancelEvent(EventManagementItem eventItem) {
        confirmOwnerAction(
                "ביטול אירוע",
                "האירוע " + eventItem.title() + " יבוטל לאחר אישור הפעולה.",
                () -> runPresenterAction(
                        () -> membershipPresenter.cancelEvent(UiSession.getMemberToken(), state.selectedCompany().id(), eventItem.eventId()),
                        "האירוע בוטל בהצלחה"
                )
        );
        
    }

    private String readEmailInput(TextField field, String label) {
        String value = field.getValue();
        if (value == null || value.isBlank()) {
            showWarning(label + " לא יכול להיות ריק");
            return null;
        }
        return value.trim();
    }

    private void requestManagerAssignment() {
        String name = readEmailInput(managerNameInput, "שם משתמש");
        if (name == null) return;
        Set<Permission> permissions = managerPermissions.getSelectedItems();
        if (permissions == null || permissions.isEmpty()) {
            showWarning("בחר לפחות הרשאה אחת למנהל");
            return;
        }
        runPresenterAction(
                () -> membershipPresenter.requestManagerAssignment(UiSession.getMemberToken(), state.selectedCompany().id(), name, permissions),
                "בקשת מינוי המנהל נשלחה לאישור"
        );
    }

    private void updateManagerPermissions() {
        String name = readEmailInput(managerNameInput, "שם משתמש");
        if (name == null) return;
        Set<Permission> permissions = managerPermissions.getSelectedItems();
        runPresenterAction(
                () -> membershipPresenter.updateManagerPermissions(UiSession.getMemberToken(), state.selectedCompany().id(), name, permissions),
                "הרשאות המנהל עודכנו"
        );
    }

    private void requestOwnerAssignment() {
        String name = readEmailInput(ownerNameInput, "שם משתמש");
        if (name == null) return;
        runPresenterAction(
                () -> membershipPresenter.requestOwnerAssignment(UiSession.getMemberToken(), state.selectedCompany().id(), name),
                "בקשת מינוי הבעלים נשלחה לאישור"
        );
    }

    private void fillManagerForm(TeamMemberItem member) {
        managerNameInput.setValue(member.userName());
        managerPermissions.setValue(member.permissions());
    }

    private void confirmRemoveTeamMember(TeamMemberItem member) {
        String actionText = member.roleType() == RoleType.OWNER ? "הסרת בעלים" : "הסרת מנהל";
        confirmOwnerAction(
                actionText,
                "המשתמש " + member.userName() + " יוסר מתפקידו בחברה.",
                () -> runPresenterAction(
                        () -> {
                            if (member.roleType() == RoleType.OWNER) {
                                membershipPresenter.removeOwnerAssignment(UiSession.getMemberToken(), state.selectedCompany().id(), member.memberId());
                            } else {
                                membershipPresenter.removeManagerAssignment(UiSession.getMemberToken(), state.selectedCompany().id(), member.memberId());
                            }
                        },
                        "התפקיד הוסר בהצלחה"
                )
        );
    }

    private Long readMemberId(TextField field, String label) {
        Long value = safeParseLong(field.getValue());
        if (value == null || value <= 0) {
            showWarning(label + " חייב להיות מספר חיובי");
            return null;
        }
        return value;
    }

    // private void runPresenterAction(ThrowingRunnable action, String successMessage) {
    //     try {
    //         if (membershipPresenter == null || companyPresenter == null) {
    //             showWarning("הפעולה מוכנה לחיבור Presenter. כרגע מוצג מידע דמו בלבד.");
    //             return;
    //         }
    //         action.run();
    //         showSuccess(successMessage);
            
    //         // ניקוי הטפסים כדי להשלים את חוויית הריפרש בממשק
    //         managerNameInput.clear();
    //         managerPermissions.deselectAll();
    //         ownerNameInput.clear();
            
    //         loadState(state.selectedCompany().id());
    //     } catch (Exception e) {
    //         showError(e.getMessage());
    //     }
    // }
    private void runPresenterAction(ThrowingRunnable action, String successMessage) {
    Long currentCompanyId = state == null || state.selectedCompany() == null
            ? null
            : state.selectedCompany().id();

    try {
        if (membershipPresenter == null || companyPresenter == null) {
            showWarning("הפעולה מוכנה לחיבור Presenter. כרגע מוצג מידע דמו בלבד.");
            return;
        }

        action.run();
        showSuccess(successMessage);

        managerNameInput.clear();
        managerPermissions.deselectAll();
        ownerNameInput.clear();

    } catch (Exception e) {
        showError(e.getMessage());

    } finally {
        if (currentCompanyId != null) {
            loadState(currentCompanyId);
        }
    }
}

    private void confirmOwnerAction(String title, String text, Runnable action) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setCancelText("ביטול");
        dialog.setConfirmText("אישור");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> action.run());
        dialog.open();
    }

    private String permissionLabel(Permission permission) {
        if (permission == null) return "";
        String name = permission.name().toLowerCase();
        
        if (name.contains("inventory") || name.contains("event")) return "ניהול מלאי ואירועים";
        if (name.contains("map") || name.contains("hall")) return "הגדרת אולם ומפת אירוע";
        
        // הפרדנו את ההרשאות כדי למנוע כפילויות במסך
        if (name.contains("discount")) return "עריכת מדיניות הנחות";
        if (name.contains("purchasing_policy")) return "עריכת מדיניות רכישה";
        if (name.contains("policy")) return "ניהול מדיניות כללית"; 
        
        // תיקון הבאג: חיפוש של החלק המשותף למילה כדי לתפוס גם inquiries וגם inquiry
        if (name.contains("inquir") || name.contains("message") || name.contains("support")) return "טיפול בפניות";
        
        if (permission == Permission.VIEW_PURCHASE_HISTORY) {
            return "צפייה בהיסטוריית רכישות";
        }
        if (permission == Permission.GENERATE_SALES_REPORT) {
            return "הפקת דוחות מכירה";
        }
        // במקרה שנוספה הרשאה חדשה שלא הכרנו, נציג אותה בצורה נקייה יותר
        return "הרשאת " + name.replace('_', ' ');
    }

    private Long safeParseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void renderLoadError(String message) {
        removeAll();
        add(new EmptyState(
                "⚠",
                "טעינת עמוד ניהול החברה נכשלה",
                message == null || message.isBlank() ? "אירעה שגיאה לא צפויה." : message,
                null
        ));
    }

    private void showSuccess(String message) {
        showNotification(message, NotificationVariant.LUMO_SUCCESS);
    }

    private void showWarning(String message) {
        showNotification(message, NotificationVariant.LUMO_CONTRAST);
    }

    private void showError(String message) {
        showNotification(message == null || message.isBlank() ? "הפעולה נכשלה" : message, NotificationVariant.LUMO_ERROR);
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    private CompanyManagementState demoState(Long requestedCompanyId) {
        List<ManagedCompanyItem> companies = List.of(
                new ManagedCompanyItem(1L, "הפקות לייב שואו בע\"מ", 101L, "ליב שואו", true),
                new ManagedCompanyItem(2L, "פסטיבלים ישראל", 101L, "נועה כהן", true),
                new ManagedCompanyItem(3L, "מועדון הבלוק", 301L, "עומר בלוק", false)
        );

        ManagedCompanyItem selected = companies.stream()
                .filter(company -> Objects.equals(company.id(), requestedCompanyId))
                .findFirst()
                .orElse(companies.get(0));

        Set<Permission> firstPermissions = permissionsByIndex(0, 1, 2);
        Set<Permission> secondPermissions = permissionsByIndex(0, 3);

        List<TeamMemberItem> team = new ArrayList<>();
        team.add(new TeamMemberItem(101L, "ליב שואו", "Founder", RoleType.FOUNDER, EnumSet.noneOf(Permission.class), false));
        team.add(new TeamMemberItem(205L, "נועה כהן", "Owner", RoleType.OWNER, EnumSet.noneOf(Permission.class), true));
        team.add(new TeamMemberItem(311L, "דניאל לוי", "Manager", RoleType.MANAGER, firstPermissions, true));
        team.add(new TeamMemberItem(412L, "מאיה ישראלי", "Manager", RoleType.MANAGER, secondPermissions, true));

        List<EventManagementItem> events = List.of(
                new EventManagementItem(501L, "Live Night 2026", "פעיל"),
                new EventManagementItem(502L, "Summer Festival", "פעיל"),
                new EventManagementItem(503L, "Standup Weekend", "מושהה")
        );

        return new CompanyManagementState(
                companies,
                selected,
                true,
                true,
                true,
                true,
                true,
                team,
                events,
                new CompanyStats(7, 2),
                new PolicySummary("עד 5 כרטיסים לרוכש, גיל מינימום לפי אירוע", "הנחת קופון ומכירה מוקדמת")
        );
    }

    private Set<Permission> permissionsByIndex(int... indexes) {
        Permission[] values = Permission.values();
        Set<Permission> result = EnumSet.noneOf(Permission.class);

        for (int index : indexes) {
            if (values.length > index) {
                result.add(values[index]);
            }
        }

        return result;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

}
