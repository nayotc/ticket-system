package ticketsystem.PresentationLayer.Views;

// Java Standard Library
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
// Spring Framework (הזרקת תלויות מול השרת)
import org.springframework.beans.factory.annotation.Autowired;
// Vaadin - Core & Server
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.component.UI;
// Vaadin - UI Components
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
// Project - DTOs
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
// Project - Presentation Layer
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.AdminLayout;
import ticketsystem.PresentationLayer.Presenters.SystemAdminPresenter;
import ticketsystem.PresentationLayer.Session.UiSession;


@PageTitle("TixNow | Admin Dashboard")
@Route(value = UiRoutes.ADMIN_DASHBOARD, layout = AdminLayout.class)
public class SystemAdminDashboard extends Div {

    private final SystemAdminPresenter presenter;

    private final List<AdminUserRow> allUsers = new ArrayList<>();
    private final List<CompanyTableRow> allCompanies = new ArrayList<>();
    private final List<PurchaseByCompanyRow> companyHistoryRows = new ArrayList<>();
    private final List<PurchaseByUserRow> userHistoryRows = new ArrayList<>();

    private final Grid<AdminUserRow> usersGrid = new Grid<>(AdminUserRow.class, false);
    private final Grid<CompanyTableRow> companiesGrid = new Grid<>(CompanyTableRow.class, false);
    private final Grid<PurchaseByCompanyRow> companyHistoryGrid = new Grid<>(PurchaseByCompanyRow.class, false);
    private final Grid<PurchaseByUserRow> userHistoryGrid = new Grid<>(PurchaseByUserRow.class, false);

    private final TextField userEmailSearch = new TextField();
    private final TextField companyHistorySearch = new TextField();
    private final TextField userHistorySearch = new TextField();

    private Long currentLoggedInAdminId = null;
    private final Div metricsContainer = new Div();

    public SystemAdminDashboard() {
        this(null);
    }

    @Autowired
    public SystemAdminDashboard(SystemAdminPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("system-admin-page");

        loadInitialData();

        metricsContainer.setId("admin-overview");
        metricsContainer.addClassName("system-admin-metrics-grid");
        
        refreshMetrics();

        add(
                createHeader(),
                metricsContainer,
                createDashboardGrid()
        );
    }

    private ViewHeader createHeader() {
        final String token = UiSession.getMemberToken();

        DownloadHandler eventLogsHandler = DownloadHandler.fromInputStream(event -> {
            InputStream stream = downloadEventLogsStream(token);
            
            return new DownloadResponse(
                    stream,
                    "events_log.txt",
                    "text/plain;charset=UTF-8",
                    -1
            );
        });

        Button eventLogsBtn = new Button("הורד לוג אירועים", VaadinIcon.DOWNLOAD.create());
        Anchor eventLogsAnchor = new Anchor(eventLogsHandler, "");
        eventLogsAnchor.getElement().setAttribute("download", true);
        eventLogsAnchor.add(eventLogsBtn);

        DownloadHandler errorLogsHandler = DownloadHandler.fromInputStream(event -> {
            InputStream stream = downloadErrorLogsStream(token);
            
            return new DownloadResponse(
                    stream,
                    "errors_log.txt",
                    "text/plain;charset=UTF-8",
                    -1
            );
        });

        Button errorLogsBtn = new Button("הורד לוג שגיאות", VaadinIcon.DOWNLOAD.create());
        errorLogsBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Anchor errorLogsAnchor = new Anchor(errorLogsHandler, "");
        errorLogsAnchor.getElement().setAttribute("download", true);
        errorLogsAnchor.add(errorLogsBtn);

        return new ViewHeader(
                "לוח בקרה ראשי",
                "ניהול משתמשים, חברות פעילות והיסטוריית רכישות במערכת.",
                eventLogsAnchor,
                errorLogsAnchor
        );
    }

    private void refreshMetrics() {
            metricsContainer.removeAll();
            
            metricsContainer.add(
                    new MetricCard("סך הכל מנויים", String.valueOf(allUsers.size()), "פעילים ומושעים במערכת"),
                    new MetricCard("חברות פעילות", String.valueOf(allCompanies.size()), "חברות שאפשר לסגור"),
                    new MetricCard("רכישות לפי חברה", String.valueOf(companyHistoryRows.size()), "מקובץ לפי חברה ואירוע"),
                    new MetricCard("רכישות לפי משתמש", String.valueOf(userHistoryRows.size()), "מקובץ לפי מזהה משתמש")
            );
        }

    private Div createDashboardGrid() {
        Div grid = new Div();
        grid.addClassName("system-admin-dashboard-grid");

        grid.add(
                createUsersPanel(),
                createCompaniesPanel(),
                createCompanyHistoryPanel(),
                createUserHistoryPanel()
        );

        return grid;
    }

    private AppCard createUsersPanel() {
        AppCard card = new AppCard();
        card.setId("admin-users");
        card.addClassName("system-admin-panel");
        card.addClassName("system-admin-users-panel");

        Div header = createPanelHeader(
                VaadinIcon.USERS,
                "כל המנויים במערכת",
                "רשימת כל המשתמשים. חיפוש לפי מייל וביצוע פעולות ניהול."
        );

        userEmailSearch.setPlaceholder("חיפוש לפי מייל...");
        userEmailSearch.setPrefixComponent(VaadinIcon.SEARCH.create());
        userEmailSearch.setValueChangeMode(ValueChangeMode.EAGER);
        userEmailSearch.addClassName("system-admin-search-field");
        userEmailSearch.addValueChangeListener(event -> filterUsers(event.getValue()));

        configureUsersGrid();

        card.add(header, userEmailSearch, usersGrid);
        return card;
    }

    private AppCard createCompaniesPanel() {
        AppCard card = new AppCard();
        card.setId("admin-companies");
        card.addClassName("system-admin-panel");

        Div header = createPanelHeader(
                VaadinIcon.BUILDING,
                "חברות פעילות",
                "רשימת חברות פעילות עם אפשרות סגירה."
        );

        configureCompaniesGrid();

        card.add(header, companiesGrid);
        return card;
    }

    private AppCard createCompanyHistoryPanel() {
        AppCard card = new AppCard();
        card.setId("admin-company-history");
        card.addClassName("system-admin-panel");
        card.addClassName("system-admin-wide-panel");

        Div header = createPanelHeader(
                VaadinIcon.ARCHIVE,
                "היסטוריית רכישה לפי חברה ואירוע",
                "תצוגה מקובצת לפי מזהה חברה ושם אירוע."
        );

        companyHistorySearch.setPlaceholder("סינון לפי שם חברה או שם אירוע...");
        companyHistorySearch.setPrefixComponent(VaadinIcon.SEARCH.create());
        companyHistorySearch.setValueChangeMode(ValueChangeMode.EAGER);
        companyHistorySearch.addClassName("system-admin-search-field");
        companyHistorySearch.addValueChangeListener(event -> filterCompanyHistory(event.getValue()));

        configureCompanyHistoryGrid();

        card.add(header, companyHistorySearch, companyHistoryGrid);
        return card;
    }

    private AppCard createUserHistoryPanel() {
        AppCard card = new AppCard();
        card.setId("admin-user-history");
        card.addClassName("system-admin-panel");
        card.addClassName("system-admin-wide-panel");

        Div header = createPanelHeader(
                VaadinIcon.USER_CARD,
                "היסטוריית רכישה לפי משתמשים",
                "תצוגה לפי מזהה משתמש והרכישות שביצע."
        );

        userHistorySearch.setPlaceholder("סינון לפי מייל משתמש...");
        userHistorySearch.setPrefixComponent(VaadinIcon.SEARCH.create());
        userHistorySearch.setValueChangeMode(ValueChangeMode.EAGER);
        userHistorySearch.addClassName("system-admin-search-field");
        userHistorySearch.addValueChangeListener(event -> filterUserHistory(event.getValue()));

        configureUserHistoryGrid();

        card.add(header, userHistorySearch, userHistoryGrid);
        return card;
    }

    private Div createPanelHeader(VaadinIcon icon, String title, String description) {
        Div header = new Div();
        header.addClassName("system-admin-panel-header");

        Div iconBox = new Div(icon.create());
        iconBox.addClassName("system-admin-panel-icon");

        Div text = new Div();
        text.addClassName("system-admin-panel-text");

        H3 titleElement = new H3(title);
        titleElement.addClassName("system-admin-panel-title");

        Paragraph descriptionElement = new Paragraph(description);
        descriptionElement.addClassName("system-admin-panel-description");

        text.add(titleElement, descriptionElement);
        header.add(iconBox, text);

        return header;
    }

    private void configureUsersGrid() {
        usersGrid.addClassName("system-admin-data-grid");
        usersGrid.setHeight("360px");

        usersGrid.addColumn(AdminUserRow::id)
                .setHeader("מזהה")
                .setAutoWidth(true)
                .setFlexGrow(0);

        usersGrid.addColumn(AdminUserRow::email)
                .setHeader("מייל")
                .setAutoWidth(true);

        usersGrid.addColumn(AdminUserRow::displayName)
                .setHeader("שם")
                .setAutoWidth(true);

        usersGrid.addComponentColumn(user -> new StatusBadge(user.status(), StatusBadge.Type.SUCCESS))
                .setHeader("סטטוס")
                .setAutoWidth(true)
                .setFlexGrow(0);

        usersGrid.addComponentColumn(this::createUserActions)
                .setHeader("פעולות")
                .setAutoWidth(true)
                .setFlexGrow(0);

        usersGrid.setItems(allUsers);
    }

    private void refreshUsersGrid() {
        try {
            String token = UiSession.getMemberToken();
            
            allUsers.clear();
            allUsers.addAll(presenter.loadActiveUsers(token));
            usersGrid.getDataProvider().refreshAll();
            refreshMetrics();
            
        } catch (Exception e) {
            showError("שגיאה ברענון רשימת המשתמשים: " + e.getMessage());
        }
    }

    private void configureCompaniesGrid() {
        companiesGrid.addClassName("system-admin-data-grid");
        companiesGrid.setHeight("360px");

        companiesGrid.addColumn(CompanyTableRow::id)
                .setHeader("מזהה")
                .setAutoWidth(true)
                .setFlexGrow(0);

        companiesGrid.addColumn(CompanyTableRow::name)
                .setHeader("שם חברה")
                .setAutoWidth(true);

        companiesGrid.addColumn(company -> findUserEmail(company.founderId()))
                .setHeader("מייסד")
                .setAutoWidth(true);

        companiesGrid.addComponentColumn(company -> new StatusBadge(company.status(), StatusBadge.Type.SUCCESS))
                .setHeader("סטטוס")
                .setAutoWidth(true)
                .setFlexGrow(0);

        companiesGrid.addComponentColumn(this::createCompanyActions)
                .setHeader("פעולות")
                .setAutoWidth(true)
                .setFlexGrow(0);

        companiesGrid.setItems(allCompanies);
    }

    private void refreshCompaniesGrid() {
        try {
            String token = UiSession.getMemberToken();
            
            allCompanies.clear();
            allCompanies.addAll(toCompanyRows(presenter.loadActiveCompanies(token)));
            companiesGrid.getDataProvider().refreshAll();
            refreshMetrics();
            
        } catch (Exception e) {
            showError("שגיאה ברענון רשימת החברות: " + e.getMessage());
        }
    }

    private void configureCompanyHistoryGrid() {
        companyHistoryGrid.addClassName("system-admin-data-grid");
        companyHistoryGrid.setHeight("420px");

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::companyName)
                .setHeader("שם חברה")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::eventName)
                .setHeader("שם אירוע")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(row -> presenter.getEventDateFormatted(UiSession.getMemberToken(), row.eventId()))
                .setHeader("תאריך אירוע")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::location)
                .setHeader("מיקום אירוע")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::purchaseId)
                .setHeader("מספר רכישה")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::buyerName)
                .setHeader("שם משתמש")
                .setAutoWidth(true);

        // companyHistoryGrid.addColumn(PurchaseByCompanyRow::purchaseDate)
        //         .setHeader("תאריך רכישה")
        //         .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::amount)
                .setHeader("סכום")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::ticketCount)
                .setHeader("מס כרטיסים")
                .setAutoWidth(true);

        companyHistoryGrid.setItems(companyHistoryRows);
    }

    private void configureUserHistoryGrid() {
        userHistoryGrid.addClassName("system-admin-data-grid");
        userHistoryGrid.setHeight("420px");

        userHistoryGrid.addColumn(PurchaseByUserRow::buyerEmail)
                .setHeader("מייל משתמש")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::buyerName)
                .setHeader("שם מלא")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::eventName)
                .setHeader("שם אירוע")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::location)
                .setHeader("מיקום")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(row -> presenter.getEventDateFormatted(UiSession.getMemberToken(), row.eventId()))
            .setHeader("תאריך אירוע")
            .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::purchaseId)
                .setHeader("מס רכישה")
                .setAutoWidth(true);

        // userHistoryGrid.addColumn(PurchaseByUserRow::purchaseDate)
        //         .setHeader("תאריך רכישה")
        //         .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::ticketCount)
                .setHeader("מספר כרטיסים")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::amount)
                .setHeader("סכום")
                .setAutoWidth(true);

        userHistoryGrid.setItems(userHistoryRows);
    }

    private HorizontalLayout createUserActions(AdminUserRow user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("system-admin-row-actions");

        if (currentLoggedInAdminId != null && currentLoggedInAdminId.equals(user.id())) {
            Span meLabel = new Span("משתמש נוכחי (את/ה)");
            meLabel.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            meLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
            meLabel.getStyle().set("font-weight", "500");
            actions.add(meLabel);
            return actions;
        }

        if (!user.isActive()) {
            Span deletedLabel = new Span("נמחק");
            deletedLabel.getStyle().set("color", "var(--lumo-error-text-color)");
            deletedLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
            deletedLabel.getStyle().set("font-weight", "500");
            actions.add(deletedLabel);
            
            return actions; 
        }

    // Dynamic button (toggles between suspend and return to activity)
        Button suspendAction = new Button();
        if ("מושעה".equals(user.status())) {
            suspendAction.setText("החזר לפעילות");
            suspendAction.setIcon(VaadinIcon.PLAY.create());
            suspendAction.addClickListener(event -> confirm(
                    "החזרת משתמש לפעילות",
                    "האם לבטל את השעיית המשתמש ולהחזיר לו את הגישה למערכת?",
                    () -> {
                        try {
                            revokeSuspension(user);
                        } catch (Exception e) {
                            showError(e.getMessage());
                        }
                    }
            ));
        } else {
            suspendAction.setText("השעה משתמש");
            suspendAction.setIcon(VaadinIcon.PAUSE.create());
            suspendAction.addClassName("system-admin-danger-action");
            suspendAction.addClickListener(event -> openSuspendDialog(user));
        }

        boolean isFounder = allCompanies.stream()
                .anyMatch(company -> Objects.equals(company.founderId(), user.id()));

        Button removeFromCompanies = new Button("הסר מכל החברות", VaadinIcon.UNLINK.create());
        removeFromCompanies.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeFromCompanies.addClassName("system-admin-secondary-action");

        if (isFounder) {
            removeFromCompanies.setEnabled(false);
            removeFromCompanies.getElement().setProperty("title", "לא ניתן להסיר מייסד מחברות. יש לסגור את החברה בבעלותו קודם.");
        } else {
            removeFromCompanies.addClickListener(event -> confirm(
                    "הסרה מכל החברות",
                    "המשתמש יוסר מכל התפקידים שלו בכל חברות ההפקה.",
                    () -> removeUserFromAllCompanies(user)
            ));
        }

        Button delete = new Button("מחיקה", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        delete.addClassName("system-admin-danger-action");
        delete.addClickListener(event -> confirm(
                "מחיקת משתמש לצמיתות",
                "המשתמש יוסר מהמערכת לחלוטין וההזמנות הפעילות שלו ינוקו. האם להמשיך?",
                () -> deleteUser(user)
        ));

        actions.add(suspendAction, removeFromCompanies, delete);
        return actions;
    }

    private HorizontalLayout createCompanyActions(CompanyTableRow company) {
        Button close = new Button("סגירת חברה", VaadinIcon.CLOSE_CIRCLE.create());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        close.addClassName("system-admin-danger-action");
        close.addClickListener(event -> confirm(
                "סגירת חברה",
                "החברה תיסגר על ידי מנהל מערכת וכל התפקידים שלה יבוטלו.",
                () -> closeCompany(company)
        ));

        HorizontalLayout actions = new HorizontalLayout(close);
        actions.addClassName("system-admin-row-actions");
        return actions;
    }

    private void filterUsers(String emailQuery) {
        String query = safeLower(emailQuery);

        usersGrid.setItems(
                allUsers.stream()
                        .filter(user -> safeLower(user.email()).contains(query))
                        .toList()
        );
    }

    private void filterCompanyHistory(String queryText) {
        String query = safeLower(queryText);

        companyHistoryGrid.setItems(
                companyHistoryRows.stream()
                        .filter(row ->
                                safeLower(row.companyName()).contains(query)
                                        || safeLower(row.eventName()).contains(query)
                        )
                        .toList()
        );
    }

    private void filterUserHistory(String queryText) {
        String query = safeLower(queryText);

        userHistoryGrid.setItems(
                userHistoryRows.stream()
                        .filter(row -> safeLower(row.buyerEmail()).contains(query))
                        .toList()
        );
    }

private void openSuspendDialog(AdminUserRow user) {
        Dialog dialog = new Dialog();
        dialog.addClassName("admin-confirm-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        Div card = new Div();
        card.addClassName("admin-confirm-card");

        H3 titleElement = new H3("השעיית משתמש: " + user.displayName());
        titleElement.addClassName("admin-confirm-title");

        DateTimePicker endPicker = new DateTimePicker("סיום השעיה (אופציונלי)");
        endPicker.setValue(LocalDateTime.now().plusDays(7)); 
        endPicker.setWidthFull();

        Checkbox permanentCheckbox = new Checkbox("השעיה לצמיתות");
        permanentCheckbox.addValueChangeListener(event -> {
            boolean isPermanent = event.getValue();
            endPicker.setEnabled(!isPermanent); 
            if (isPermanent) {
                endPicker.clear();
            } else if (endPicker.isEmpty()) {
                endPicker.setValue(LocalDateTime.now().plusDays(7)); 
            }
        });

        TextField reasonField = new TextField("סיבת השעיה");
        reasonField.setRequiredIndicatorVisible(true); 
        reasonField.setErrorMessage("חובה להזין סיבה להשעיה");
        reasonField.setWidthFull();

        Button cancel = new Button("ביטול", event -> dialog.close());
        cancel.addClassName("system-admin-secondary-action");

        Button approve = new Button("ביצוע השעיה", VaadinIcon.PAUSE.create());
        approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        approve.addClassName("system-admin-confirm-button");
        
        approve.addClickListener(event -> {
            if (reasonField.isEmpty()) {
                reasonField.setInvalid(true);
                return;
            }

            LocalDateTime endDate = null;
            if (!permanentCheckbox.getValue()) { 
                endDate = endPicker.getValue();
                if (endDate == null) {
                    endDate = LocalDateTime.now().plusDays(7); 
                }
            }

            dialog.close();
            suspendMember(user, endDate, reasonField.getValue());
        });

        HorizontalLayout actions = new HorizontalLayout(cancel, approve);
        actions.addClassName("admin-confirm-actions");

        card.add(titleElement, endPicker, permanentCheckbox, reasonField, actions);
        dialog.add(card);
        dialog.open();
    }

    private void suspendMember(AdminUserRow user, LocalDateTime endDate, String reason) {
        try {
            if (presenter != null) {
                presenter.suspendMember(UiSession.getMemberToken(), user.id(), LocalDateTime.now(), endDate, reason);
            }
            showSuccess("המשתמש הושעה בהצלחה");
            refreshUsersGrid();
        } catch (Exception e) {
            handleAdminActionError(e);
        }
    }

    private void revokeSuspension(AdminUserRow user) {
        try {
            if (presenter != null) {
                presenter.revokeSuspension(UiSession.getMemberToken(), user.id());
            }
            showSuccess("המשתמש הוחזר לפעילות בהצלחה");
            refreshUsersGrid();
        } catch (Exception e) {
            handleAdminActionError(e);
        }
    }

    private void removeUserFromAllCompanies(AdminUserRow user) {
        try {
            if (presenter != null) {
                presenter.removeUserFromAllCompanies(UiSession.getMemberToken(), user.id());
            }
            showSuccess("המשתמש הוסר מכל החברות");
            refreshUsersGrid();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            
            if (msg.contains("Failed to remove user")) {
                showError("שגיאה: לא ניתן להסיר את המשתמש. ייתכן שיש לו תפקיד ניהולי שלא ניתן לביטול כרגע.");
            } else {
                handleAdminActionError(e);
            }
        }
    }

    private void deleteUser(AdminUserRow user) {
        try {
            if (presenter != null) {
                presenter.deleteUser(UiSession.getMemberToken(), user.id());
            }
            showSuccess("המשתמש נמחק מהמערכת בהצלחה");
            refreshUsersGrid();
        } catch (Exception e) {
            handleAdminActionError(e);
        }
    }

    private void closeCompany(CompanyTableRow company) {
        try {
            if (presenter != null) {
                presenter.closeCompany(UiSession.getMemberToken(), company.id());
            }
            allCompanies.removeIf(row -> Objects.equals(row.id(), company.id()));
            companiesGrid.setItems(allCompanies);
            showSuccess("החברה נסגרה בהצלחה");
            refreshCompaniesGrid();
        } catch (Exception e) {
            handleAdminActionError(e);
        }
    }

    private InputStream downloadEventLogsStream(String token) {
        try {
            if (presenter != null) {
                List<String> logs = presenter.viewEventLogs(token);
                String fileContent = String.join("\n", logs);
                return new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
            }
            return new ByteArrayInputStream("Error: Presenter is not initialized.".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ByteArrayInputStream(("שגיאה בשליפת הלוגים: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private InputStream downloadErrorLogsStream(String token) {
        try {
            if (presenter != null) {
                List<String> logs = presenter.viewErrorLogs(token);
                String fileContent = String.join("\n", logs);
                return new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
            }
            return new ByteArrayInputStream("Error: Presenter is not initialized.".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ByteArrayInputStream(("שגיאה בשליפת הלוגים: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void confirm(String title, String message, Runnable action) {
        Dialog dialog = new Dialog();
        dialog.addClassName("admin-confirm-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        Div card = new Div();
        card.addClassName("admin-confirm-card");

        H3 titleElement = new H3(title);
        titleElement.addClassName("admin-confirm-title");

        Paragraph messageElement = new Paragraph(message);
        messageElement.addClassName("admin-confirm-message");

        Button cancel = new Button("ביטול", event -> dialog.close());
        cancel.addClassName("system-admin-secondary-action");

        Button approve = new Button("אישור", VaadinIcon.CHECK.create());
        approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        approve.addClassName("system-admin-confirm-button");
        approve.addClickListener(event -> {
            dialog.close();
            action.run();
        });

        HorizontalLayout actions = new HorizontalLayout(cancel, approve);
        actions.addClassName("admin-confirm-actions");

        card.add(titleElement, messageElement, actions);
        dialog.add(card);
        dialog.open();
    }

    private void loadInitialData() {
        if (presenter == null) {
            loadDemoData();
            return;
        }

        try {
            String token = UiSession.getMemberToken();
            if (token == null || token.isBlank()) {
                UI.getCurrent().navigate(UiRoutes.HOME);
                return;
            }

            currentLoggedInAdminId = presenter.getCurrentAdminId(token);
            
            allUsers.clear();
            allUsers.addAll(presenter.loadActiveUsers(token));

            allCompanies.clear();
            allCompanies.addAll(toCompanyRows(presenter.loadActiveCompanies(token)));

            companyHistoryRows.clear();
            companyHistoryRows.addAll(toCompanyHistoryRows(presenter.loadPurchaseHistoryByCompanyAndEvent(token)));

            userHistoryRows.clear();
            userHistoryRows.addAll(toUserHistoryRows(presenter.loadPurchaseHistoryByBuyer(token)));
            
        } catch (Exception exception) {

            allUsers.clear();
            allCompanies.clear();
            companyHistoryRows.clear();
            userHistoryRows.clear();

            showError("גישה נדחתה: אין לך הרשאות של מנהל מערכת. הנך מועבר/ת לעמוד הראשי.");

            if (UI.getCurrent() != null) {
                UI.getCurrent().getPage().executeJs("setTimeout(() => { window.location.href = '/' }, 2500);");
            }
        }
    }

    private void loadDemoData() {
        allUsers.clear();
        allUsers.add(new AdminUserRow(101L, "noam@test.com", "נועם כהן", "פעיל", true, null, null, null));
        allUsers.add(new AdminUserRow(102L, "maya@test.com", "מאיה לוי", "פעיל", true, null, null, null));
        allUsers.add(new AdminUserRow(103L, "admin-watch@test.com", "חשבון בבדיקה", "פעיל", true, null, null, null));

        allCompanies.clear();
        allCompanies.add(new CompanyTableRow(11L, "LiveNation Israel", 1L, "פעילה"));
        allCompanies.add(new CompanyTableRow(12L, "Zappa Group", 2L, "פעילה"));
        allCompanies.add(new CompanyTableRow(13L, "Open Stage Productions", 7L, "פעילה"));

        companyHistoryRows.clear();
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "LiveNation Israel", "רוק בפארק", "תל אביב", 101L, 9001L, "נועם כהן", "₪640", 2
        ));
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "LiveNation Israel", "רוק בפארק", "תל אביב", 101L, 9002L, "מאיה לוי", "₪320", 1
        ));
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "Zappa Group", "סטנדאפ לילה", "ירושלים", 102L, 9003L, "נועם כהן", "₪450", 3
        ));

        userHistoryRows.clear();
        userHistoryRows.add(new PurchaseByUserRow(
                "noam@test.com", "נועם כהן", "רוק בפארק", "תל אביב", 101L, 9001L, 2, "₪640"
        ));
        userHistoryRows.add(new PurchaseByUserRow(
                "noam@test.com", "נועם כהן", "סטנדאפ לילה", "ירושלים", 102L, 9003L, 3, "₪450"
        ));
        userHistoryRows.add(new PurchaseByUserRow(
                "maya@test.com", "מאיה לוי", "רוק בפארק", "תל אביב", 101L, 9002L, 1, "₪320"
        ));
    }

    private List<CompanyTableRow> toCompanyRows(List<CompanyDTO> companies) {
        if (companies == null) {
            return List.of();
        }

        return companies.stream()
                .filter(CompanyDTO::isActive)
                .map(company -> new CompanyTableRow(
                        company.getId(),
                        company.getName(),
                        company.getFounderId(),
                        company.isActive() ? "פעילה" : "סגורה"
                ))
                .toList();
    }

    private List<PurchaseByCompanyRow> toCompanyHistoryRows(Map<Long, Map<String, List<OrderDTO>>> history) {
        if (history == null) {
            return List.of();
        }

        List<PurchaseByCompanyRow> rows = new ArrayList<>();

        history.forEach((companyId, events) -> {
            if (events == null) {
                return;
            }

            events.forEach((eventName, orders) -> {
                if (orders == null) {
                    return;
                }

                for (OrderDTO order : orders) {
                    rows.add(new PurchaseByCompanyRow(
                            findCompanyName(companyId),
                            eventName,
                            order.getLocation(),
                            order.getEventId(), // מושכים את המזהה מה-DTO
                            order.getPurchaseId(),
                            findUserName(order.getMemberId()),
                            formatMoney(totalAmount(order)),
                            ticketCount(order)
                    ));
                }
            });
        });

        return rows;
    }

    private List<PurchaseByUserRow> toUserHistoryRows(Map<Long, List<OrderDTO>> history) {
        if (history == null) {
            return List.of();
        }

        List<PurchaseByUserRow> rows = new ArrayList<>();

        history.forEach((buyerId, orders) -> {
            if (orders == null) {
                return;
            }

            for (OrderDTO order : orders) {
                rows.add(new PurchaseByUserRow(
                        findUserEmail(buyerId),
                        findUserName(buyerId),
                        order.getEventName(),
                        order.getLocation(),
                        order.getEventId(), // מושכים את המזהה מה-DTO
                        order.getPurchaseId(),
                        ticketCount(order),
                        formatMoney(totalAmount(order))
                ));
            }
        });

        return rows;
    }

    private String findCompanyName(Long companyId) {
        if (companyId == null) {
            return "לא זמין";
        }

        return allCompanies.stream()
                .filter(company -> Objects.equals(company.id(), companyId))
                .map(CompanyTableRow::name)
                .findFirst()
                .orElse("חברה " + companyId);
    }

    private AdminUserRow findUser(Long memberId) {
        if (memberId == null) {
            return null;
        }

        return allUsers.stream()
                .filter(user -> Objects.equals(user.id(), memberId))
                .findFirst()
                .orElse(null);
    }

    private String findUserName(Long memberId) {
        AdminUserRow user = findUser(memberId);
        return user == null ? "משתמש " + memberId : user.displayName();
    }


    private String findUserEmail(Long memberId) {
        AdminUserRow user = findUser(memberId);
        return user == null ? "לא זמין" : user.email();
    }

    private int ticketCount(OrderDTO order) {
        return order.getTickets() == null ? 0 : order.getTickets().size();
    }

    private BigDecimal totalAmount(OrderDTO order) {
        if (order.getTotalPrice() != null) {
            return order.getTotalPrice(); 
        }
        if (order.getTickets() == null) {
            return BigDecimal.ZERO;
        }
        return order.getTickets().stream()
                .map(PurchaseDTO::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "₪0";
        }

        return "₪" + amount.stripTrailingZeros().toPlainString();
    }

    private String unavailableDate() {
        return "לא זמין";
    }

    private String ticketsText(OrderDTO order) {
        int count = order.getTickets() == null ? 0 : order.getTickets().size();
        return count == 1 ? "1 כרטיס" : count + " כרטיסים";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message == null || message.isBlank() ? "הפעולה נכשלה" : message,
                4000,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void handleAdminActionError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";

        if (e instanceof ticketsystem.PresentationLayer.Presenters.PresentationException pe && pe.isSessionTimeout()) {
            UiSession.handleTimeoutRedirect();
            return;
        }

        if (msg.contains("Unauthorized") || msg.contains("Invalid admin credentials") || msg.contains("ERROR: Unauthorized")) {
            UiSession.handleTimeoutRedirect();
            return;
        }

        showError(msg.isBlank() ? "הפעולה נכשלה" : msg);
    }

    public record AdminUserRow(
            Long id,
            String email,
            String displayName,
            String status,
            boolean isActive,
            LocalDateTime suspensionStartDate,
            LocalDateTime suspensionEndDate,
            String suspensionReason
    ) {
    }

    private record CompanyTableRow(
            Long id,
            String name,
            Long founderId,
            String status
    ) {
    }

    private record PurchaseByCompanyRow(
            String companyName,
            String eventName,
            String location,
            Long eventId,
            Long purchaseId,
            String buyerName,
            String amount,
            int ticketCount
    ) {
    }

    private record PurchaseByUserRow(
            String buyerEmail,
            String buyerName,
            String eventName,
            String location,
            Long eventId,
            Long purchaseId,
            int ticketCount,
            String amount
    ) {
    }

}