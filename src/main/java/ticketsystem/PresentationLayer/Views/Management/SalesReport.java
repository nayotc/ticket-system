package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.SalesReportDTO;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Presenters.SalesReportPresenter;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("TixNow | Sales Report")
@Route(value = UiRoutes.SALES_REPORT, layout = ManagementLayout.class)
public class SalesReport extends PageContainer implements BeforeEnterObserver {

    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("he", "IL"));

    private final Div metricsGrid = new Div();
    private final Div chartCard = new Div();
    private final Grid<OrderDTO> transactionsGrid = new Grid<>(OrderDTO.class, false);
    private final Div emptyStateContainer = new Div();

    private SalesReportPresenter presenter;
    private Long companyId = 1L;
    private SalesReportDTO currentReport = new SalesReportDTO(0, BigDecimal.ZERO, "לא נטענו נתונים");
    private List<OrderDTO> currentTransactions = new ArrayList<>();

    @Autowired
    public SalesReport(SalesReportPresenter salesReportPresenter) {
        this.presenter = salesReportPresenter;

        addClassName("sales-report-page");
        setSpacing(false);

        Button refreshButton = createHeaderButton("רענון", VaadinIcon.REFRESH, false);
        refreshButton.addClickListener(event -> refreshData());

        Anchor exportButton = createExportAnchor();

        ViewHeader header = new ViewHeader(
                "דוח מכירות",
                "סקירה של הכנסות, כרטיסים שנמכרו ועסקאות של חברת ההפקה.",
                exportButton, // Pass the returned anchor to the header
                refreshButton
        );

        metricsGrid.addClassName("sales-report-metrics-grid");

        Div contentGrid = new Div();
        contentGrid.addClassName("sales-report-content-grid");

        chartCard.addClassName("sales-report-chart-card");
        chartCard.addClassName("app-card");

        //AppCard summaryCard = createSummaryCard();
        contentGrid.add(chartCard/*, summaryCard*/);

        AppCard transactionsCard = createTransactionsCard();

        add(header, metricsGrid, contentGrid, transactionsCard, emptyStateContainer);

        configureTransactionsGrid();
    }

    /**
     * Creates the export button wrapped in an Anchor tag to trigger the CSV download.
     * Uses the official Vaadin StreamResource approach for dynamic, lazy-loaded file generation.
     */
    private Anchor createExportAnchor() {
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            String csv = buildSalesReportCsv();

            return new DownloadResponse(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    "sales-report.csv",
                    "text/csv;charset=UTF-8",
                    -1
            );
        });

        Button exportButton = createHeaderButton("ייצוא", VaadinIcon.DOWNLOAD, false);

        Anchor anchor = new Anchor(handler, "");
        anchor.add(exportButton);
        anchor.getElement().setAttribute("download", true);

        return anchor;
    }

    /**
     * Builds a CSV string from the current transactions' data.
     * This method is called lazily when the user clicks the export button, ensuring that the latest data is included in the download.
     */
    private String buildSalesReportCsv() {
        StringBuilder csv = new StringBuilder();

        // Helps Excel detect UTF-8 correctly
        csv.append('\uFEFF');

        csv.append("Purchase ID,Event,Location,Member ID,Tickets,Amount\r\n");

        for (OrderDTO order : currentTransactions) {
            appendCsvValue(csv, order.getPurchaseId());
            appendCsvValue(csv, safeText(order.getEventName()));
            appendCsvValue(csv, safeText(order.getLocation()));

            // Force Excel to keep long ID as text
            appendCsvValue(csv, excelText(order.getMemberId()));

            appendCsvValue(csv, countSoldTickets(order));
            appendCsvValue(csv, calculateOrderTotal(order));
            csv.append("\r\n");
        }

        return csv.toString();
    }

    private String excelText(Long value) {
        if (value == null) {
            return "";
        }

        return "=\"" + value + "\"";
    }

    private void appendCsvValue(StringBuilder csv, Object value) {
        if (csv.length() > 0 && csv.charAt(csv.length() - 1) != '\n') {
            csv.append(",");
        }

        String text = value == null ? "" : value.toString();
        text = text.replace("\"", "\"\"");

        csv.append("\"").append(text).append("\"");
    }

    /**
     * Entry point for the future presenter.
     * The view keeps a defensive company filter so the table never shows transactions from another company.
     */
    public void bindSalesReport(SalesReportDTO report, List<OrderDTO> companyTransactions) {
        currentReport = report != null ? report : new SalesReportDTO(0, BigDecimal.ZERO, "אין נתוני מכירות");
        currentTransactions = filterTransactionsByCompany(companyTransactions);
        refreshView();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters()
                .get("companyId")
                .flatMap(this::tryParseLong)
                .ifPresent(id -> companyId = id);

        String token = UiSession.getMemberToken();

        // 1. Unauthenticated user - redirect to the login page before the view loads
        if (token == null || token.isBlank()) {
            showError("יש להתחבר כדי לצפות בדוח המכירות.");
            event.forwardTo(UiRoutes.LOGIN);
            return;
        }

        // 2. Authenticated user - attempt to fetch data
        try {
            SalesReportDTO report = presenter.generateSalesReport(token, companyId);
            List<OrderDTO> transactions = presenter.getCompanyTransactions(token, companyId);
            bindSalesReport(report, transactions);
        } catch (PresentationException e) {
            showError(e.getMessage());
            event.forwardTo(UiRoutes.HOME);
        }
    }

    // This function is triggered only by clicking the "Refresh" button while already on the page.
    private void refreshData() {
        String token = UiSession.getMemberToken();
        
        if (token == null || token.isBlank()) {
            showError("פג תוקף החיבור, אנא התחבר מחדש.");
            UI.getCurrent().navigate(UiRoutes.LOGIN);
            return;
        }

        try {
            SalesReportDTO report = presenter.generateSalesReport(token, companyId);
            List<OrderDTO> transactions = presenter.getCompanyTransactions(token, companyId);
            bindSalesReport(report, transactions);
            Notifications.success("הנתונים רעננו בהצלחה");
        } catch (PresentationException e) {
            showError(e.getMessage());
            UI.getCurrent().navigate(UiRoutes.HOME);
        }
    }

//    private AppCard createSummaryCard() {
//        Div content = new Div();
//        content.addClassName("sales-report-summary-card");
//
//        H3 title = new H3("תשתית לחיבור Presenter");
//        title.addClassName("sales-report-card-title");
//
//        Span text = new Span("ה-View מקבל SalesReportDTO לריכוז הדוח ורשימת OrderDTO להצגת העסקאות. הרשימה מסוננת שוב לפי companyId לפני הצגה.");
//        text.addClassName("sales-report-summary-text");
//
//        Div steps = new Div();
//        steps.addClassName("sales-report-steps");
//        steps.add(
//                createStep("1", "Presenter קורא ל-HistoryService.generateSalesReport(token, companyId)."),
//                createStep("2", "Presenter טוען עסקאות חברה דרך HistoryService.getHistoryForCompany(token, companyId)."),
//                createStep("3", "View מציג רק עסקאות שבהן order.companyId שווה ל-companyId שבנתיב.")
//        );
//
//        content.add(title, text, steps);
//        return new AppCard(content);
//    }

//    private Div createStep(String number, String text) {
//        Div row = new Div();
//        row.addClassName("sales-report-step");
//
//        Span badge = new Span(number);
//        badge.addClassName("sales-report-step-badge");
//
//        Span label = new Span(text);
//        label.addClassName("sales-report-step-text");
//
//        row.add(badge, label);
//        return row;
//    }

    private AppCard createTransactionsCard() {
        Div header = new Div();
        header.addClassName("sales-report-card-header");

        H3 title = new H3("עסקאות החברה");
        title.addClassName("sales-report-card-title");

        header.add(title);

        Div tableWrapper = new Div(transactionsGrid);
        tableWrapper.addClassName("sales-report-table-wrapper");

        AppCard card = new AppCard(header, tableWrapper);
        card.addClassName("sales-report-transactions-card");
        return card;
    }

    private void configureTransactionsGrid() {
        transactionsGrid.addClassName("sales-report-grid");
        transactionsGrid.setWidthFull();
        transactionsGrid.setAllRowsVisible(true);

        transactionsGrid.addColumn(order -> formatPurchaseId(order.getPurchaseId()))
                .setHeader("מס' עסקה")
                .setAutoWidth(true)
                .setFlexGrow(0);

        transactionsGrid.addColumn(order -> safeText(order.getEventName()))
                .setHeader("אירוע")
                .setAutoWidth(true)
                .setFlexGrow(1);

        transactionsGrid.addColumn(order -> safeText(order.getLocation()))
                .setHeader("מיקום")
                .setAutoWidth(true);

        transactionsGrid.addColumn(order -> formatNullableId(order.getMemberId()))
                .setHeader("משתמש")
                .setAutoWidth(true);

        transactionsGrid.addColumn(this::countSoldTickets)
                .setHeader("כרטיסים")
                .setAutoWidth(true);

        transactionsGrid.addColumn(order -> formatCurrency(calculateOrderTotal(order)))
                .setHeader("סכום")
                .setAutoWidth(true);

        transactionsGrid.addComponentColumn(this::createOrderStatusBadge)
                .setHeader("סטטוס")
                .setAutoWidth(true);
    }

    private void refreshView() {
        renderMetrics();
        renderChart();
        renderTransactions();
        renderEmptyState();
    }

    private void renderMetrics() {
        metricsGrid.removeAll();

        BigDecimal totalRevenue = currentReport.getTotalRevenue() != null
                ? currentReport.getTotalRevenue()
                : BigDecimal.ZERO;

        int totalTicketsSold = currentReport.getTotalTicketsSold();
        int transactionsCount = currentTransactions.size();
        long eventsCount = currentTransactions.stream()
                .map(OrderDTO::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal averageOrder = transactionsCount == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(transactionsCount), 2, RoundingMode.HALF_UP);

        metricsGrid.add(
                createMetricCard("סה\"כ הכנסות", formatCurrency(totalRevenue), currentReport.getMessage(), VaadinIcon.MONEY),
                createMetricCard("כרטיסים שנמכרו", String.valueOf(totalTicketsSold), "לא כולל כרטיסים שבוטלו", VaadinIcon.TICKET),
                createMetricCard("עסקאות", String.valueOf(transactionsCount), "עסקאות של החברה הנוכחית", VaadinIcon.CART),
                createMetricCard("ממוצע לעסקה", formatCurrency(averageOrder), eventsCount + " אירועים בדוח", VaadinIcon.LINE_CHART)
        );
    }

    private AppCard createMetricCard(String label, String value, String helper, VaadinIcon icon) {
        Div top = new Div();
        top.addClassName("sales-report-metric-top");

        Span labelElement = new Span(label);
        labelElement.addClassName("sales-report-metric-label");

        Span iconElement = new Span();
        iconElement.addClassName("sales-report-metric-icon");
        iconElement.add(icon.create());

        top.add(labelElement, iconElement);

        Span valueElement = new Span(value);
        valueElement.addClassName("sales-report-metric-value");

        Span helperElement = new Span(helper == null || helper.isBlank() ? " " : helper);
        helperElement.addClassName("sales-report-metric-helper");

        AppCard card = new AppCard(top, valueElement, helperElement);
        card.addClassName("sales-report-metric-card");
        return card;
    }

    private void renderChart() {
        chartCard.removeAll();

        H3 title = new H3("הכנסות לפי אירוע");
        title.addClassName("sales-report-card-title");
        Div chart = new Div();
        chart.addClassName("sales-report-chart");

        List<EventRevenue> eventRevenues = calculateRevenueByEvent();
        BigDecimal maxRevenue = eventRevenues.stream()
                .map(EventRevenue::revenue)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        if (eventRevenues.isEmpty() || maxRevenue.compareTo(BigDecimal.ZERO) == 0) {
            chart.add(new EmptyState("₪", "אין נתוני הכנסות", "ברגע שיהיו עסקאות משויכות לחברה, הדוח יציג אותן כאן.", null));
        } else {
            for (EventRevenue eventRevenue : eventRevenues) {
                chart.add(createEventRevenueRow(eventRevenue, maxRevenue));
            }
        }

        chartCard.add(title, chart);
    }

    private Div createEventRevenueRow(EventRevenue eventRevenue, BigDecimal maxRevenue) {
        Div row = new Div();
        row.addClassName("sales-report-chart-row");

        Div labels = new Div();
        labels.addClassName("sales-report-chart-labels");

        Span eventName = new Span(eventRevenue.eventName());
        eventName.addClassName("sales-report-chart-event");

        Span revenue = new Span(formatCurrency(eventRevenue.revenue()));
        revenue.addClassName("sales-report-chart-value");

        labels.add(eventName, revenue);

        Div track = new Div();
        track.addClassName("sales-report-chart-track");

        Div fill = new Div();
        fill.addClassName("sales-report-chart-fill");

        int width = eventRevenue.revenue()
                .multiply(BigDecimal.valueOf(100))
                .divide(maxRevenue, 0, RoundingMode.HALF_UP)
                .intValue();

        fill.getStyle().set("width", Math.max(6, width) + "%");
        track.add(fill);

        row.add(labels, track);
        return row;
    }

    private void renderTransactions() {
        transactionsGrid.setItems(currentTransactions);
    }

    private void renderEmptyState() {
        emptyStateContainer.removeAll();

        if (!currentTransactions.isEmpty()) {
            emptyStateContainer.setVisible(false);
            return;
        }

        emptyStateContainer.setVisible(true);
        emptyStateContainer.add(new EmptyState(
                "₪",
                "אין עסקאות להצגה",
                "לא נמצאו עסקאות שמקושרות לחברה הנוכחית או שאין הרשאה לצפות בדוח.",
                null
        ));
    }

    private List<EventRevenue> calculateRevenueByEvent() {
        Map<String, BigDecimal> revenueByEvent = new LinkedHashMap<>();

        for (OrderDTO order : currentTransactions) {
            String eventName = safeText(order.getEventName());
            BigDecimal total = calculateOrderTotal(order);
            revenueByEvent.merge(eventName, total, BigDecimal::add);
        }

        return revenueByEvent.entrySet()
                .stream()
                .map(entry -> new EventRevenue(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EventRevenue::revenue).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private List<OrderDTO> filterTransactionsByCompany(List<OrderDTO> transactions) {
        if (transactions == null) {
            return List.of();
        }

        return transactions.stream()
                .filter(Objects::nonNull)
                .filter(order -> Objects.equals(order.getCompanyId(), companyId))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateOrderTotal(OrderDTO order) {
        if (order == null || order.getTickets() == null) {
            return BigDecimal.ZERO;
        }

        return order.getTickets()
                .stream()
                .filter(Objects::nonNull)
                .filter(ticket -> !isCanceled(ticket.getStatus()))
                .map(PurchaseDTO::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countSoldTickets(OrderDTO order) {
        if (order == null || order.getTickets() == null) {
            return 0;
        }

        return (int) order.getTickets()
                .stream()
                .filter(Objects::nonNull)
                .filter(ticket -> !isCanceled(ticket.getStatus()))
                .count();
    }

    private StatusBadge createOrderStatusBadge(OrderDTO order) {
        if (order == null || order.getTickets() == null || order.getTickets().isEmpty()) {
            return new StatusBadge("ריק", StatusBadge.Type.NEUTRAL);
        }

        boolean allCanceled = order.getTickets()
                .stream()
                .filter(Objects::nonNull)
                .allMatch(ticket -> isCanceled(ticket.getStatus()));

        if (allCanceled) {
            return new StatusBadge("בוטל", StatusBadge.Type.ERROR);
        }

        boolean hasPending = order.getTickets()
                .stream()
                .filter(Objects::nonNull)
                .map(PurchaseDTO::getStatus)
                .filter(Objects::nonNull)
                .anyMatch(status -> status.equalsIgnoreCase("PENDING") || status.equalsIgnoreCase("PROCESSING"));

        if (hasPending) {
            return new StatusBadge("בטיפול", StatusBadge.Type.INFO);
        }

        return new StatusBadge("הושלם", StatusBadge.Type.SUCCESS);
    }

    private boolean isCanceled(String status) {
        if (status == null) {
            return false;
        }

        return STATUS_CANCELED.equalsIgnoreCase(status) || STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    private Button createHeaderButton(String text, VaadinIcon icon, boolean subtle) {
        Button button = new Button(text, icon.create());
        button.addClassName("sales-report-header-button");

        if (!subtle) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        return button;
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return currencyFormat.format(safeAmount);
    }

    private String formatPurchaseId(Long purchaseId) {
        return purchaseId == null ? "-" : "#TX-" + purchaseId;
    }

    private String formatNullableId(Long id) {
        return id == null ? "-" : "#" + id;
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private java.util.Optional<Long> tryParseLong(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private void showInfo(String message) {
        Notifications.info(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }

    private record EventRevenue(String eventName, BigDecimal revenue) {
    }

    // public interface SalesReportPresenter {
    //     SalesReportDTO generateSalesReport(String token, long companyId);

    //     List<OrderDTO> getCompanyTransactions(String token, long companyId);
    // }
}
