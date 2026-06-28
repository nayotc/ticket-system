package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SalesReportDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;
import ticketsystem.DomainLayer.user.Permission;
/**
 * Application service for purchase-history use cases.
 *
 * <p>Read operations execute in read-only transactions. Operations that
 * create or update history override this setting with a writable transaction.</p>
 */
@Service
@Transactional(readOnly = true)
public class HistoryService
        implements OrderCompletedListener, EventUpdatesListener {

    private final IHistoryRepository historyRepository;
    private final ITokenService tokenService;
    private final MembershipDomainService membershipDomainService;
    private final ISystemLogger logger;
    private final UserAccessService userAccessService;
    private final INotifier notificationsService;
    private final IPaymentService paymentService;
    private final ITicketIssuingService ticketIssuingService;

    /**
     * Creates the history application service.
     */
    @Autowired
    public HistoryService(
            IHistoryRepository historyRepository,
            ITokenService tokenService,
            MembershipDomainService membershipDomainService,
            ISystemLogger logger,
            UserAccessService userAccessService,
            INotifier notificationsService,
            IPaymentService paymentService,
            ITicketIssuingService ticketIssuingService
    ) {
        this.historyRepository = historyRepository;
        this.tokenService = tokenService;
        this.membershipDomainService = membershipDomainService;
        this.logger = logger;
        this.userAccessService = userAccessService;
        this.notificationsService = notificationsService;
        this.paymentService = paymentService;
        this.ticketIssuingService = ticketIssuingService;
    }

    /**
     * Persists a completed order as a purchase-history snapshot.
     *
     * <p>The purchase starts without an identifier. The repository flushes the
     * insert, the database assigns an ID, and the generated value is then
     * copied back to the completed order DTO.</p>
     *
     * @param order completed order
     */
    @Override
    @Transactional
    public void onOrderCompleted(OrderDTO order) {
        try {
            Purchase purchase =
                    HistoryMapper.toPurchase(order);

            historyRepository.addPurchase(purchase);

            order.setPurchaseId(
                    purchase.getPurchaseId()
            );
        } catch (RuntimeException exception) {
            logger.logEvent(
                    "Failed to process completed order: "
                            + exception.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );

            throw exception;
        }
    }

    /**
     * Returns the personal purchase history of the authenticated member.
     */
    public List<OrderDTO> getHistoryForUser(String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException(
                        "Invalid or expired token"
                );
            }

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException(
                        "Only members can view personal purchase history"
                );
            }

            Long memberId =
                    tokenService.extractUserId(token);

            if (memberId == null) {
                throw new IllegalArgumentException(
                        "Could not extract user id from token"
                );
            }

            return historyRepository
                    .getPurchasesByMemberId(memberId)
                    .stream()
                    .map(HistoryMapper::toOrderDTO)
                    .toList();

        } catch (IllegalArgumentException exception) {
            logger.logEvent(
                    "Failed to retrieve personal purchase history: "
                            + exception.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );

            throw exception;
        }
    }

    /**
     * Returns purchase history for a production company.
     */
    public List<OrderDTO> getHistoryForCompany(
            String token,
            long companyId
    ) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException(
                        "Invalid or expired token"
                );
            }

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException(
                        "Only members can view personal purchase history"
                );
            }

            Long memberId =
                    tokenService.extractUserId(token);

            if (memberId == null) {
                throw new IllegalArgumentException(
                        "Could not extract user id from token"
                );
            }

            if (!membershipDomainService.validatePermission(
                    memberId,
                    companyId,
                    Permission.VIEW_PURCHASE_HISTORY
            )) {
                throw new IllegalArgumentException(
                        "Insufficient permissions to view company purchase history"
                );
            }

            return historyRepository
                    .getPurchasesByCompanyId(companyId)
                    .stream()
                    .map(HistoryMapper::toOrderDTO)
                    .toList();

        } catch (IllegalArgumentException exception) {
            logger.logEvent(
                    "Failed to retrieve company purchase history: "
                            + exception.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );

            throw exception;
        }
    }

    /**
     * Returns company history filtered according to the requesting manager's
     * management subtree.
     */
    public List<OrderDTO> getManagerFilteredHistoryForCompany(
            String token,
            long companyId
    ) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException(
                        "Invalid or expired token"
                );
            }

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException(
                        "Only members can generate sales reports"
                );
            }

            Long memberId =
                    tokenService.extractUserId(token);

            if (memberId == null) {
                throw new IllegalArgumentException(
                        "Could not extract user id from token"
                );
            }

            if (!membershipDomainService.validatePermission(
                    memberId,
                    companyId,
                    Permission.GENERATE_SALES_REPORT
            )) {
                throw new IllegalArgumentException(
                        "Insufficient permissions to generate sales report"
                );
            }

            Set<Long> allowedManagers = new HashSet<>(
                    membershipDomainService
                            .getManagementSubTreeMemberIds(
                                    memberId,
                                    companyId
                            )
            );

            allowedManagers.add(memberId);

            return historyRepository
                    .getPurchasesByCompanyId(companyId)
                    .stream()
                    .filter(purchase ->
                            allowedManagers.contains(
                                    purchase.getManagedByMemberId()
                            )
                    )
                    .map(HistoryMapper::toOrderDTO)
                    .toList();

        } catch (IllegalArgumentException exception) {
            logger.logEvent(
                    "Failed to retrieve filtered sales history: "
                            + exception.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );

            throw exception;
        }
    }

    /**
     * Generates a sales report for the requesting manager's management
     * subtree.
     */
    public SalesReportDTO generateSalesReport(
            String token,
            long companyId
    ) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException(
                        "Invalid or expired token"
                );
            }

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException(
                        "Only members can generate sales reports"
                );
            }

            Long memberId =
                    tokenService.extractUserId(token);

            if (memberId == null) {
                throw new IllegalArgumentException(
                        "Could not extract user id from token"
                );
            }

            userAccessService.validateCanPerformNonViewAction(memberId);

            if (!membershipDomainService.validatePermission(
                    memberId,
                    companyId,
                    Permission.GENERATE_SALES_REPORT
            )) {
                throw new IllegalArgumentException(
                        "Insufficient permissions to generate sales report"
                );
            }

            Set<Long> allowedManagers =
                    membershipDomainService
                            .getManagementSubTreeMemberIds(
                                    memberId,
                                    companyId
                            );

            List<Purchase> companyPurchases =
                    historyRepository
                            .getPurchasesByCompanyId(companyId);

            int totalTicketsSold = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Purchase purchase : companyPurchases) {
                if (!allowedManagers.contains(
                        purchase.getManagedByMemberId()
                )) {
                    continue;
                }

                for (PurchasedTicket ticket :
                        purchase.getTickets()) {

                    if (ticket.getStatus()
                            != TicketStatus.CANCELED) {

                        totalRevenue =
                                totalRevenue.add(
                                        ticket.getPrice()
                                );

                        totalTicketsSold++;
                    }
                }
            }

            if (totalTicketsSold == 0) {
                return new SalesReportDTO(
                        0,
                        BigDecimal.ZERO,
                        "No sales data was found"
                );
            }

            return new SalesReportDTO(
                    totalTicketsSold,
                    totalRevenue,
                    "Sales report generated successfully"
            );

        } catch (IllegalArgumentException exception) {
            logger.logEvent(
                    "Failed to generate sales report: "
                            + exception.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );

            throw exception;
        }
    }

    /**
     * Notifies buyers after an event has been canceled.
     */
    @Override
    public void onEventCanceled(Long eventId) {
        List<Purchase> purchases =
                historyRepository
                        .getPurchasesByEventId(eventId);

        if (!purchases.isEmpty()) {
            notifyPurchasedBuyers(
                    purchases,
                    "The event \""
                            + purchases.get(0).getEventName()
                            + "\" that you purchased tickets for was canceled."
            );
        }
    }

    /**
     * Attempts to refund and cancel every purchase associated with an event.
     *
     * <p>The loaded purchases remain managed for the duration of this
     * transaction. Changes to the refund flag and embedded ticket statuses are
     * persisted by Hibernate dirty checking when the transaction commits.</p>
     */
    @Override
    @Transactional
    public boolean onEventCancellationRequested(Long eventId) {
        boolean allSucceeded = true;

        if (!paymentService.handshake()
                || !ticketIssuingService.handshake()) {
            return false;
        }

        List<Purchase> purchases =
                historyRepository
                        .getPurchasesByEventId(eventId);

        for (Purchase purchase : purchases) {
            if (purchase.isRefunded()) {
                continue;
            }

            boolean refunded =
                    paymentService.refund(
                            purchase.getTransactionId()
                    );

            if (!refunded) {
                allSucceeded = false;
                continue;
            }

            notifyRefundCompleted(purchase);

            purchase.setRefunded(true);

            for (PurchasedTicket ticket :
                    purchase.getTickets()) {
                ticket.setStatus(TicketStatus.CANCELED);
            }
            historyRepository.updatePurchase(purchase);
        }

        return allSucceeded;
    }

    /**
     * Notifies buyers about an event update.
     */
    @Override
    @Transactional(readOnly = true)
    public void onEventUpdated(
            Long eventId,
            LocalDateTime date,
            String location,
            String updateMessage
    ) {
        if (eventId == null) {
            return;
        }

        List<Purchase> purchases =
                historyRepository
                        .getPurchasesByEventId(eventId);

        notifyPurchasedBuyers(
                purchases,
                updateMessage
        );
    }

    /**
     * Sends one event notification to every distinct member buyer.
     */
    private void notifyPurchasedBuyers(
            List<Purchase> purchases,
            String message
    ) {
        if (notificationsService == null
                || purchases == null
                || purchases.isEmpty()
                || message == null
                || message.isBlank()) {
            return;
        }

        List<Long> buyerMemberIds =
                purchases.stream()
                        .map(Purchase::getMemberId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (buyerMemberIds.isEmpty()) {
            return;
        }

        notificationsService.notifyMembers(
                buyerMemberIds,
                message
        );
    }

    /**
     * Notifies one member that their purchase was refunded.
     */
    private void notifyRefundCompleted(Purchase purchase) {
        if (notificationsService == null
                || purchase == null
                || purchase.getMemberId() == null) {
            return;
        }

        notificationsService.notifyMembers(
                List.of(purchase.getMemberId()),
                "בוצע החזר כספי עבור רכישת הכרטיסים לאירוע \""
                        + purchase.getEventName()
                        + "\" בעקבות ביטול האירוע."
        );
    }
}