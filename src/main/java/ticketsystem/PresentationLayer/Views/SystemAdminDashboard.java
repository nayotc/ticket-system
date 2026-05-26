package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.MetricCard;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.AdminLayout;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.DTO.PurchaseDTO;

import java.math.BigDecimal;
import java.util.Comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public SystemAdminDashboard() {
        this(null);
    }

    public SystemAdminDashboard(SystemAdminPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("system-admin-page");

        loadInitialData();

        add(
                createHeader(),
                createMetrics(),
                createDashboardGrid()
        );
    }

    private ViewHeader createHeader() {
        return new ViewHeader(
                "לוח בקרה ראשי",
                "ניהול משתמשים, חברות פעילות והיסטוריית רכישות במערכת."
        );
    }

    private Div createMetrics() {
        Div metrics = new Div();
        metrics.setId("admin-overview");
        metrics.addClassName("system-admin-metrics-grid");

        metrics.add(
                new MetricCard("משתמשים פעילים", String.valueOf(allUsers.size()), "מחוברים או זמינים במערכת"),
                new MetricCard("חברות פעילות", String.valueOf(allCompanies.size()), "חברות שאפשר לסגור"),
                new MetricCard("רכישות לפי חברה", String.valueOf(companyHistoryRows.size()), "מקובץ לפי חברה ואירוע"),
                new MetricCard("רכישות לפי משתמש", String.valueOf(userHistoryRows.size()), "מקובץ לפי מזהה משתמש")
        );

        return metrics;
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
                "משתמשים פעילים",
                "חיפוש לפי מייל וביצוע פעולות מנהל מערכת."
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

        usersGrid.addColumn(AdminUserRow::lastSeen)
                .setHeader("פעילות אחרונה")
                .setAutoWidth(true);

        usersGrid.addComponentColumn(this::createUserActions)
                .setHeader("פעולות")
                .setAutoWidth(true)
                .setFlexGrow(0);

        usersGrid.setItems(allUsers);
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

        companiesGrid.addColumn(CompanyTableRow::founderId)
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

    private void configureCompanyHistoryGrid() {
        companyHistoryGrid.addClassName("system-admin-data-grid");
        companyHistoryGrid.setHeight("420px");

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::companyName)
                .setHeader("שם חברה")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::eventName)
                .setHeader("שם אירוע")
                .setAutoWidth(true);

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::eventDate)
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

        companyHistoryGrid.addColumn(PurchaseByCompanyRow::purchaseDate)
                .setHeader("תאריך רכישה")
                .setAutoWidth(true);

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

        userHistoryGrid.addColumn(PurchaseByUserRow::eventDate)
                .setHeader("תאריך אירוע")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::purchaseId)
                .setHeader("מס רכישה")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::purchaseDate)
                .setHeader("תאריך רכישה")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::ticketCount)
                .setHeader("מספר כרטיסים")
                .setAutoWidth(true);

        userHistoryGrid.addColumn(PurchaseByUserRow::amount)
                .setHeader("סכום")
                .setAutoWidth(true);

        userHistoryGrid.setItems(userHistoryRows);
    }

    private HorizontalLayout createUserActions(AdminUserRow user) {
        Button removeFromCompanies = new Button("הסר מחברות", VaadinIcon.UNLINK.create());
        removeFromCompanies.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeFromCompanies.addClassName("system-admin-secondary-action");
        removeFromCompanies.addClickListener(event -> confirm(
                "הסרה מכל החברות",
                "המשתמש יוסר מכל התפקידים שלו בכל חברות ההפקה.",
                () -> removeUserFromAllCompanies(user)
        ));

        Button delete = new Button("מחיקה", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        delete.addClassName("system-admin-danger-action");
        delete.addClickListener(event -> confirm(
                "מחיקת משתמש",
                "המשתמש יוסר מהמערכת וההזמנות הפעילות שלו ינוקו.",
                () -> deleteUser(user)
        ));

        HorizontalLayout actions = new HorizontalLayout(removeFromCompanies, delete);
        actions.addClassName("system-admin-row-actions");
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

    private void deleteUser(AdminUserRow user) {
        try {
            if (presenter != null) {
                presenter.deleteUser(UiSession.getMemberToken(), user.id());
            }

            allUsers.removeIf(row -> Objects.equals(row.id(), user.id()));
            filterUsers(userEmailSearch.getValue());
            showSuccess("המשתמש נמחק בהצלחה");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void removeUserFromAllCompanies(AdminUserRow user) {
        try {
            if (presenter != null) {
                presenter.removeUserFromAllCompanies(UiSession.getMemberToken(), user.id());
            }

            showSuccess("המשתמש הוסר מכל החברות");
        } catch (Exception exception) {
            showError(exception.getMessage());
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
        } catch (Exception exception) {
            showError(exception.getMessage());
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

            allUsers.clear();
            allUsers.addAll(presenter.loadActiveUsers(token));

            allCompanies.clear();
            allCompanies.addAll(toCompanyRows(presenter.loadActiveCompanies(token)));

            companyHistoryRows.clear();
            companyHistoryRows.addAll(toCompanyHistoryRows(presenter.loadPurchaseHistoryByCompanyAndEvent(token)));

            userHistoryRows.clear();
            userHistoryRows.addAll(toUserHistoryRows(presenter.loadPurchaseHistoryByBuyer(token)));
        } catch (Exception exception) {
            showError(exception.getMessage());
            loadDemoData();
        }
    }

    private void loadDemoData() {
        allUsers.clear();
        allUsers.add(new AdminUserRow(101L, "noam@test.com", "נועם כהן", "פעיל", "עכשיו"));
        allUsers.add(new AdminUserRow(102L, "maya@test.com", "מאיה לוי", "פעיל", "לפני 4 דקות"));
        allUsers.add(new AdminUserRow(103L, "admin-watch@test.com", "חשבון בבדיקה", "פעיל", "לפני 12 דקות"));

        allCompanies.clear();
        allCompanies.add(new CompanyTableRow(11L, "LiveNation Israel", 1L, "פעילה"));
        allCompanies.add(new CompanyTableRow(12L, "Zappa Group", 2L, "פעילה"));
        allCompanies.add(new CompanyTableRow(13L, "Open Stage Productions", 7L, "פעילה"));

        companyHistoryRows.clear();
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "LiveNation Israel",
                "רוק בפארק",
                "20/07/2026",
                "תל אביב",
                9001L,
                "נועם כהן",
                "25/05/2026",
                "₪640",
                2
        ));
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "LiveNation Israel",
                "רוק בפארק",
                "20/07/2026",
                "תל אביב",
                9002L,
                "מאיה לוי",
                "25/05/2026",
                "₪320",
                1
        ));
        companyHistoryRows.add(new PurchaseByCompanyRow(
                "Zappa Group",
                "סטנדאפ לילה",
                "04/08/2026",
                "ירושלים",
                9003L,
                "נועם כהן",
                "25/05/2026",
                "₪450",
                3
        ));

        userHistoryRows.clear();
        userHistoryRows.add(new PurchaseByUserRow(
                "noam@test.com",
                "נועם כהן",
                "רוק בפארק",
                "תל אביב",
                "20/07/2026",
                9001L,
                "25/05/2026",
                2,
                "₪640"
        ));
        userHistoryRows.add(new PurchaseByUserRow(
                "noam@test.com",
                "נועם כהן",
                "סטנדאפ לילה",
                "ירושלים",
                "04/08/2026",
                9003L,
                "25/05/2026",
                3,
                "₪450"
        ));
        userHistoryRows.add(new PurchaseByUserRow(
                "maya@test.com",
                "מאיה לוי",
                "רוק בפארק",
                "תל אביב",
                "20/07/2026",
                9002L,
                "25/05/2026",
                1,
                "₪320"
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
                            unavailableDate(),
                            order.getLocation(),
                            order.getPurchaseId(),
                            findUserName(order.getMemberId()),
                            unavailableDate(),
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
                        unavailableDate(),
                        order.getPurchaseId(),
                        unavailableDate(),
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

    public interface SystemAdminPresenter {
        List<AdminUserRow> loadActiveUsers(String sessionToken) throws Exception;

        List<CompanyDTO> loadActiveCompanies(String sessionToken) throws Exception;

        Map<Long, Map<String, List<OrderDTO>>> loadPurchaseHistoryByCompanyAndEvent(String sessionToken) throws Exception;

        Map<Long, List<OrderDTO>> loadPurchaseHistoryByBuyer(String sessionToken) throws Exception;

        void deleteUser(String sessionToken, long memberId) throws Exception;

        void removeUserFromAllCompanies(String sessionToken, long memberId) throws Exception;

        void closeCompany(String sessionToken, long companyId) throws Exception;
    }

    public record AdminUserRow(
            Long id,
            String email,
            String displayName,
            String status,
            String lastSeen
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
            String eventDate,
            String location,
            Long purchaseId,
            String buyerName,
            String purchaseDate,
            String amount,
            int ticketCount
    ) {
    }

    private record PurchaseByUserRow(
            String buyerEmail,
            String buyerName,
            String eventName,
            String location,
            String eventDate,
            Long purchaseId,
            String purchaseDate,
            int ticketCount,
            String amount
    ) {
    }


}