package ticketsystem.PresentationLayer.Constants;

public final class UiRoutes {

    private UiRoutes() {
    }

    // Public
    public static final String HOME = "";
    public static final String EVENTS = "events";
    public static final String EVENT_SEARCH = "events/search";
    public static final String SEARCH_RESULTS = "events/results";

    // Auth
    public static final String LOGIN = "login";
    public static final String REGISTRATION = "register";

    // Buyer flow
    public static final String EVENT_MAP = "events/:eventId/map";
    public static final String TICKET_SELECTION = "events/:eventId/tickets";
    public static final String ACTIVE_ORDER_CART = "orders/active";
    public static final String CHECKOUT = "checkout/:eventId";

    // User account
    public static final String MY_ACCOUNT = "account";
    public static final String CREATE_PRODUCTION_COMPANY = "companies/create";

    // Lottery
    public static final String LOTTERY_REGISTRATION = "events/:eventId/lottery";
    public static final String LOTTERY_RESULT_CODE = "lottery/:lotteryId/result";

    // Company management
    public static final String COMPANY_MANAGEMENT = "companies/:companyId/manage";
    public static final String POLICIES_EDITOR = "companies/:companyId/policies";
    public static final String SALES_REPORT = "companies/:companyId/sales";
    public static final String ROLES_AND_PERMISSIONS_TREE = "companies/:companyId/roles";

    // Event management
    public static final String CREATE_EVENT = "companies/:companyId/events/create";
    public static final String EDIT_EVENT = "companies/:companyId/events/:eventId/edit";
    public static final String HALL_MAP_BUILDER = "companies/:companyId/events/:eventId/hall-map";

    // Admin
    public static final String ADMIN_DASHBOARD = "admin";

    // Existing compatibility
    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    public static final String HELP = "help";

    // UI preview routes
    public static final String UI_PREVIEW = "ui-preview";
    public static final String UI_PREVIEW_AUTH = "ui-preview/auth";
    public static final String UI_PREVIEW_MANAGEMENT = "ui-preview/management";
    public static final String UI_PREVIEW_ADMIN = "ui-preview/admin";
}