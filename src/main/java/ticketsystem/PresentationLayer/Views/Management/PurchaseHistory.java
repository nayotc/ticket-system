package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.SalesReportPresenter; 
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("TixNow | Purchase History")
@Route(value = UiRoutes.PURCHASE_HISTORY, layout = ManagementLayout.class)
public class PurchaseHistory extends PageContainer implements BeforeEnterObserver {
    private final Grid<OrderDTO> transactionsGrid = new Grid<>(OrderDTO.class, false);
    private final Div emptyStateContainer = new Div();

    private Long companyId;
    private final SalesReportPresenter presenter;
    private List<OrderDTO> currentTransactions = new ArrayList<>();
    private final java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("he", "IL"));

    @Autowired
    public PurchaseHistory(SalesReportPresenter salesReportPresenter) {
        this.presenter = salesReportPresenter;

        addClassName("purchase-history-page");
        setSpacing(false);

        ViewHeader header = new ViewHeader(
                "היסטוריית רכישות והזמנות",
                "צפייה בכלל הכרטיסים והעסקאות שנמכרו לאירועי החברה (נתונים קבועים שאינם משתנים)."
        );

        AppCard transactionsCard = createTransactionsCard();
        add(header, transactionsCard, emptyStateContainer);
        configureTransactionsGrid();
    }

    private AppCard createTransactionsCard() {
        Div header = new Div();
        header.addClassName("sales-report-card-header");
        H3 title = new H3("כלל עסקאות החברה");
        header.add(title);

        Div tableWrapper = new Div(transactionsGrid);
        tableWrapper.addClassName("sales-report-table-wrapper");

        return new AppCard(header, tableWrapper);
    }

    private void configureTransactionsGrid() {
        transactionsGrid.removeAllColumns(); 

        transactionsGrid.addColumn(order -> order.getEventName() == null ? "-" : order.getEventName())
                .setHeader("אירוע")
                .setAutoWidth(true);

        transactionsGrid.addColumn(order -> order.getLocation() == null ? "-" : order.getLocation())
                .setHeader("מיקום")
                .setAutoWidth(true);

        transactionsGrid.addColumn(order -> presenter.getBuyerName(order.getMemberId()))
                .setHeader("משתמש שם")
                .setAutoWidth(true);



        transactionsGrid.addColumn(order -> {
            if (order.getTickets() == null || order.getTickets().isEmpty()) return "-";
            
            return order.getTickets().stream()
                    .map(t -> {
                        // Check for standing tickets (row 0, chair 0)
                        if (t.getRow() != null && t.getChair() != null && t.getRow() == 0 && t.getChair() == 0) {
                            return "[כרטיס עמידה]";
                        }
                        //simple format for seat tickets
                        return String.format("[ש׳ %d, כ׳ %d]", t.getRow(), t.getChair());
                    })
                    .collect(Collectors.joining(", "));
        }).setHeader("מקומות ישיבה / עמידה").setAutoWidth(true);


        transactionsGrid.addColumn(order -> formatCurrency(order.getTotalPrice()))
                .setHeader("סכום ששולם")
                .setAutoWidth(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("companyId")
                .map(Long::parseLong)
                .ifPresent(id -> companyId = id);

        String token = UiSession.getMemberToken();
        if (token == null || token.isBlank()) {
            event.forwardTo(UiRoutes.LOGIN);
            return;
        }

        try {
            List<OrderDTO> allTransactions = presenter.getCompanyHistoryUnfiltered(token, companyId); 
            
            currentTransactions = allTransactions.stream()
                    .filter(order -> Objects.equals(order.getCompanyId(), companyId))
                    .collect(Collectors.toList());


            transactionsGrid.setItems(currentTransactions);
            
            if (currentTransactions.isEmpty()) {
                emptyStateContainer.add(new EmptyState("₪", "אין עסקאות להצגה", "לא נמצאו עסקאות משויכות לחברה זו.", null));
            }
        } catch (Exception e) {
            event.forwardTo(UiRoutes.HOME);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return currencyFormat.format(safeAmount);
    }
}