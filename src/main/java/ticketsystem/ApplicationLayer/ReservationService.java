package ticketsystem.ApplicationLayer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DTO.TicketIssueRequest;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.discount.PricingQuote;
import ticketsystem.DTO.PricingQuoteDTO;

@Service
public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final ICompanyRepository companyRepository;
    private final MembershipDomainService membershipDomain;
    private final TokenService tokenService;
    private final IPaymentService paymentService;
    private final ITicketIssuingService ticketIssuingService;
    private final ILotteryRepository lotteryRepository;
    private final Reservation reservationDomeinService;
    private final EventCatalogDomainService eventCatalogDomainService;
    private final ISystemLogger logger;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();
    private final INotifier notificationsService;
    private final UserAccessService userAccessService;
    private final Set<Long> expirationWarningSentOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> soldOutNotificationSentEventIds = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            ICompanyRepository companyRepository,
            MembershipDomainService membershipDomain,
            TokenService tokenService,
            IPaymentService paymentService,
            ITicketIssuingService secureBarcode,
            ILotteryRepository lotteryRepository,
            EventCatalogDomainService eventCatalogDomainService,
            ISystemLogger logger,
            INotifier notifier, UserAccessService userAccessService) {

        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.companyRepository = companyRepository;
        this.membershipDomain = membershipDomain;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.ticketIssuingService = secureBarcode;
        this.lotteryRepository = lotteryRepository;
        this.eventCatalogDomainService = eventCatalogDomainService;
        this.logger = logger;
        this.reservationDomeinService = new Reservation();
        this.notificationsService = notifier;
        this.userAccessService = userAccessService;
    }

    // UC 2.5,2.4
    public boolean selectSeatTicket(String token, Long eventId, Long areaId, seatPositionDTO position,
            String lotteryCode) {
        expireOldOrdersForEvent(eventId);
        ;
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
            if (eventRepository.getEventById(eventId) == null) {
                throw new IllegalArgumentException("Event not found");
            }
            Event event = eventRepository.getEventById(eventId);
            if (event.getSaleStatus().equals(SaleStatus.PRE_SALE)) {
                Lottery lottery = lotteryRepository.findByEventId(eventId);

                if (lottery != null && event.getSaleStatus().equals(SaleStatus.PRE_SALE)) {
                    Long userId = tokenService.extractUserId(token);
                    reservationDomeinService.checkLottery(lottery, userId, lotteryCode);
                }
            }
            ActiveOrder order = getOrCreateOrder(token, eventId);
            if (order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            reservationDomeinService.selectSeatTicket(order, event, areaId, position);
            saveOrder(order);
            eventRepository.updateSeatStatus(eventId, areaId, position.getRow(), position.getChair(),
                    SeatStatus.RESERVED);

            logger.logEvent("Seat ticket selected: orderId=" + order.getOrderId() + ", eventId=" + eventId + ", areaId="
                    + areaId + ", position=" + position, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("selectSeatTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity, String lotteryCode) {
        expireOldOrdersForEvent(eventId);
        ;
        try {
            tokenService.validateToken(token);
            Long memberId = tokenService.extractUserId(token);
            userAccessService.validateCanPerformNonViewAction(memberId);
            if (eventRepository.getEventById(eventId) == null) {
                throw new IllegalArgumentException("Event not found");
            }

            Event event = eventRepository.getEventById(eventId);
            if (event.getSaleStatus().equals(SaleStatus.PRE_SALE)) {
                Lottery lottery = lotteryRepository.findByEventId(eventId);
                if (lottery != null && event.getSaleStatus().equals(SaleStatus.PRE_SALE)) {
                    Long userId = tokenService.extractUserId(token);
                    reservationDomeinService.checkLottery(lottery, userId, lotteryCode);
                }
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            ActiveOrder order = getOrCreateOrder(token, eventId);
            if (order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            reservationDomeinService.selectStandingTicket(order, event, areaId, quantity);
            saveOrder(order);
            eventRepository.updateStandingAreaReservedCount(eventId, areaId, quantity);
            logger.logEvent("Standing ticket selected: orderId=" + order.getOrderId() + ", eventId=" + eventId
                    + ", areaId=" + areaId + ", quantity=" + quantity, LogLevel.INFO);
            return true;
        } catch (Exception e) {
            logger.logEvent("selectStandingTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    // UC 2.7
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
        expireOldOrdersForEvent(eventId);
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
            ActiveOrder order = findActiveOrder(token, eventId);
            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            Ticket ticket = reservationDomeinService.removeTicketFromActiveOrder(order, event, ticketId);
            saveOrder(order);
            updateRemoveTicket(eventId, ticket.getAreaId(), ticket, 1);
            logger.logEvent("Ticket removed from active order: orderId=" + order.getOrderId() + ", eventId=" + eventId
                    + ", ticketId=" + ticketId, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("removeTicketFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean removeStandingTicketsFromActiveOrder(String token, Long eventId, Long areaId, int quantity) {
        expireOldOrdersForEvent(eventId);
        ;
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
            ActiveOrder order = findActiveOrder(token, eventId);

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            Event event = eventRepository.getEventById(eventId);
            reservationDomeinService.removeStandingTicketsFromActiveOrder(order, event, areaId, quantity);
            saveOrder(order);
            updateRemoveTicket(eventId, areaId, null, quantity);
            logger.logEvent("Standing tickets removed from active order: orderId=" + order.getOrderId() + ", eventId="
                    + eventId + ", areaId=" + areaId + ", quantity=" + quantity, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("removeStandingTicketsFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public ActiveOrderDTO viewCurrentActiveOrder(String token) {
        try {
            tokenService.validateToken(token);

            ActiveOrder order;

            if (tokenService.isGuestToken(token)) {
                order = orderRepository.getActiveOrderBySessionToken(token);
            } else {
                Long userId = tokenService.extractUserId(token);
                order = orderRepository.getActiveOrderByUserId(userId);
            }

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                return null;
            }

            ActiveOrderDTO activeOrderDTO = toDTO(order);

            logger.logEvent(
                    "Current active order viewed: orderId=" + order.getOrderId()
                            + ", eventId=" + order.getEventId(),
                    LogLevel.INFO);

            return activeOrderDTO;

        } catch (Exception e) {
            logger.logEvent("viewCurrentActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PricingQuoteDTO calculateActiveOrderPricing(String token, Long eventId, String couponCode) {
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));

            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);

            if (order == null || event == null) {
                throw new IllegalStateException("No active order or event found");
            }

            BigDecimal subtotal = reservationDomeinService.calculateTotalPrice(order, event);

            PricingQuote quote = eventCatalogDomainService.calculatePricingQuote(
                    event.getCompanyId(),
                    event,
                    subtotal,
                    order.getTickets().size(),
                    couponCode);

            return objectMapper.convertValue(quote, PricingQuoteDTO.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to calculate active order pricing: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean validateActiveOrderPolicy(String token, Long eventId, PaymentDetails details, String couponCode) {
        // Implementation for validating active order policy
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            if (details == null || details.getBirthDate() == null || details.getPayerName() == null
                    || details.getPaymentMethodId() == null) {
                throw new IllegalArgumentException("Payment details are incomplete");
            }
            int buyerAge = Period.between(details.getBirthDate(), LocalDate.now()).getYears();
            eventCatalogDomainService.canPurchaseByCompanyPolicy(event.getCompanyId(), order.getTickets().size(),
                    buyerAge);
            reservationDomeinService.canPurchaseByEventPolicy(event, order.getTickets().size(), buyerAge);
            // BigDecimal amount = reservationDomeinService.calculatePrice(order, event);
            // BigDecimal amountAfterDiscount =
            // eventCatalogDomainService.calculateFinalPrice(event.getCompanyId(), event,
            // amount, order.getTickets().size(), couponCode);
            // saveOrder(order);
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate active order policy: " + e.getMessage());

        }
    }

    // 2.8 checkout
    @Transactional
    public boolean checkout(String token, Long eventId, PaymentDetails details, String couponCode) {
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));

            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            if (order == null || event == null) {
                throw new IllegalStateException("No active order or event found");
            }

            if (reservationDomeinService.timeExpire(event, order)) {
                expireSingleOrder(order);
                throw new IllegalStateException("Active order has expired");
            }

            if (!paymentService.handshake()) {
                throw new IllegalStateException("Payment service is unavailable");
            }

            if (details == null || details.getBirthDate() == null || details.getPayerName() == null
                    || details.getPaymentMethodId() == null) {
                throw new IllegalArgumentException("Payment details are incomplete");
            }

            BigDecimal amount = reservationDomeinService.submitActiveOrderForCheckout(order, event);
            BigDecimal amountAfterDiscount = eventCatalogDomainService.calculateFinalPrice(event.getCompanyId(), event,
                    amount, order.getTickets().size(), couponCode);
            Integer transactionId = paymentService.pay(amountAfterDiscount, details);

            if (transactionId == -1) {
                order.paymentFailed();
                saveOrder(order);
                notifyOrderOwner(
                        order,
                        "Payment failed. No purchase was completed.");
                throw new IllegalStateException("Payment failed");
            }

            OrderDTO orderDTO;

            try {
                orderDTO = creaOrderDTOwithBarcode(order, event, amountAfterDiscount, transactionId);
            } catch (Exception barcodeException) {
                handleRefundAfterCheckoutFailure(order, event, amountAfterDiscount, transactionId, barcodeException,
                        "Ticket issuing failed. Payment was refunded.",
                        "Ticket issuing failed and refund failed.");
                return false; // unreachable
            }

            try {
                boolean wasSoldOutBeforeCheckout = event.isSoldOut();

                reservationDomeinService.completeCheckout(order, event);
                completeOrderInventory(order);
                saveOrder(order);
                notifyOrderOwner(
                        order,
                        "Your purchase was completed successfully. Your tickets are now available.");

                notifyEventManagersIfBecameSoldOut(event, wasSoldOutBeforeCheckout);

            } catch (Exception completeCheckoutException) {
                handleRefundAfterCheckoutFailure(order, event, amountAfterDiscount, transactionId,
                        completeCheckoutException,
                        "Complete checkout failed. Payment was refunded.",
                        "Complete checkout failed and refund failed.");
                return false; // unreachable
            }

            boolean listenersNotified = notifyListeners(orderDTO);
            if (!listenersNotified) {
                logger.logEvent(
                        "Order completed but notifying listeners failed: orderId="
                                + order.getOrderId() + ", eventId=" + eventId,
                        LogLevel.WARN);
            }

            logger.logEvent(
                    "Checkout completed successfully: orderId="
                            + order.getOrderId() + ", eventId=" + eventId,
                    LogLevel.INFO);

            return true;

        } catch (Exception e) {
            logger.logEvent("checkout failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    // listener
    public void addOrderListener(OrderCompletedListener listener) {
        listeners.add(listener);
    }

    @Transactional
    public void sweepExpiredAndExpiringOrders() {

        logger.logEvent(
                "Running expired orders cleanup job",
                LogLevel.INFO);

        expireOldOrdersAndNotifyUsers();
    }

    private void expireOldOrdersAndNotifyUsers() {
        List<ActiveOrder> expiredOrders = orderRepository.findExpiredActiveOrders();

        for (ActiveOrder order : expiredOrders) {
            expireSingleOrder(order);
        }

        List<ActiveOrder> expiringOrders = orderRepository.findOrdersAboutToExpire();

        for (ActiveOrder order : expiringOrders) {
            if (expirationWarningSentOrderIds.add(order.getOrderId())) {
                notifyOrderOwner(
                        order,
                        "Your active order is about to expire. Please complete your purchase soon.");

                logger.logEvent(
                        "Active order expiration warning sent: " + order.getOrderId(),
                        LogLevel.INFO);
            }
        }
    }

    private void expireSingleOrder(ActiveOrder order) {
        if (order == null) {
            return;
        }

        try {
            if (order.getStatus() == ActiveOrder.OrderStatus.ACTIVE
                    || order.getStatus() == ActiveOrder.OrderStatus.CANCELLED) {
                releaseOrderReservationsSafely(order);
                System.out.print("עבר את השחרור");
            }

            expirationWarningSentOrderIds.remove(order.getOrderId());
            orderRepository.deleteOrder(order.getOrderId());

            logger.logEvent(
                    "Expired order cleaned: " + order.getOrderId(),
                    LogLevel.INFO);

        } catch (Exception e) {
            logger.logEvent(
                    "Failed to expire orderId=" + order.getOrderId()
                            + ", eventId=" + order.getEventId()
                            + ", reason=" + e.getMessage(),
                    LogLevel.WARN);
        }
    }

    private void releaseOrderReservationsSafely(ActiveOrder order) {
        Map<Long, Integer> standingByArea = new HashMap<>();

        for (Ticket ticket : order.getTickets()) {
            if (ticket.isSeat()) {
                try {
                    eventRepository.updateSeatStatus(
                            ticket.getEventId(),
                            ticket.getAreaId(),
                            ticket.getRow(),
                            ticket.getChair(),
                            SeatStatus.AVAILABLE);
                } catch (Exception ignored) {
                    System.out.print("בודקת עם הכיסא שוחרר");
                }
            } else {
                standingByArea.merge(ticket.getAreaId(), 1, Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> entry : standingByArea.entrySet()) {
            try {
                eventRepository.updateStandingAreaReservedCount(
                        order.getEventId(),
                        entry.getKey(),
                        -entry.getValue());
            } catch (Exception ignored) {
                // already released / changed
            }
        }
    }

    private void expireOldOrdersForEvent(Long eventId) {
        List<ActiveOrder> expiredOrders = orderRepository.findExpiredActiveOrdersByEventId(eventId);

        for (ActiveOrder order : expiredOrders) {
            expireSingleOrder(order);
        }
    }

    private boolean notifyListeners(OrderDTO order) {
        try {
            for (OrderCompletedListener listener : listeners) {
                listener.onOrderCompleted(order);
            }
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private void handleRefundAfterCheckoutFailure(
            ActiveOrder order,
            Event event,
            BigDecimal amount,
            Integer transactionId,
            Exception originalException,
            String refundSuccessMessage, String refundFailureMessage) {

        Long eventId = event.getId();
        logger.logError(
                refundSuccessMessage
                        + " Original checkout failure cause: "
                        + originalException.getClass().getSimpleName()
                        + " - "
                        + originalException.getMessage()
                        + ". orderId=" + order.getOrderId()
                        + ", eventId=" + eventId,
                originalException);
        reservationDomeinService.expire(event, order);
        releaseOrderReservationsSafely(order);
        boolean refundResult = paymentService.refund(transactionId);

        order.paymentFailed();
        saveOrder(order);

        notifyOrderOwner(
                order,
                "The purchase was canceled because ticket issuing failed. A refund was issued.");

        if (refundResult) {
            logger.logEvent(
                    refundSuccessMessage + " orderId=" + order.getOrderId() + ", eventId=" + eventId,
                    LogLevel.INFO);

            throw new IllegalStateException(refundSuccessMessage, originalException);
        }

        logger.logError(
                refundFailureMessage + " orderId=" + order.getOrderId() + ", eventId=" + eventId,
                originalException);

        throw new IllegalStateException(refundFailureMessage, originalException);
    }

    // secure barcode logic
    private OrderDTO creaOrderDTOwithBarcode(ActiveOrder order, Event event, BigDecimal total, Integer transactionId) {
        OrderDTO orderDTO = toDTO(order, event, total, transactionId);

        for (PurchaseDTO purchesDTO : orderDTO.getTickets()) {
            TicketIssueRequest request = createTicketIssueRequest(purchesDTO, orderDTO);
            String barcode = ticketIssuingService.issueTicket(request);
            if (barcode == null || barcode.isBlank() || barcode.equals("-1")) {
                throw new IllegalStateException("Ticket issuing failed");
            }
            purchesDTO.setSecureBarcode(barcode);
        }
        return orderDTO;
    }

    private void updateRemoveTicket(Long eventId, Long areaId, Ticket ticket, Integer quantity) {
        if (ticket != null && ticket.isSeat()) {
            eventRepository.updateSeatStatus(eventId, ticket.getAreaId(), ticket.getRow(), ticket.getChair(),
                    SeatStatus.AVAILABLE);
        } else {
            if (quantity != null)
                eventRepository.updateStandingAreaReservedCount(eventId, areaId, -quantity);
        }
    }

    private void completeOrderInventory(ActiveOrder order) {
        Map<Long, Integer> standingByArea = new HashMap<>();

        for (Ticket ticket : order.getTickets()) {
            if (ticket.isSeat()) {
                eventRepository.updateSeatStatus(order.getEventId(), ticket.getAreaId(), ticket.getRow(),
                        ticket.getChair(), SeatStatus.SOLD);
            } else {
                standingByArea.merge(ticket.getAreaId(), 1, Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> entry : standingByArea.entrySet()) {
            eventRepository.markStandingTicketsAsSold(order.getEventId(), entry.getKey(), entry.getValue());
        }
    }

    private ActiveOrder getOrCreateOrder(String token, Long eventId) {
        ActiveOrder order = findActiveOrder(token, eventId);

        if (order == null) {
            Long userId = tokenService.isMemberToken(token)
                    ? tokenService.extractUserId(token)
                    : null;

            order = new ActiveOrder(
                    null, token,
                    userId,
                    eventId);

            orderRepository.addOrder(order);
            logger.logEvent(
                    "Active order created: orderId=" + order.getOrderId()
                            + ", eventId=" + eventId,
                    LogLevel.INFO);
        }

        return order;
    }

    private ActiveOrder findActiveOrder(String token, Long eventId) {

        ActiveOrder order;

        if (tokenService.isGuestToken(token)) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    token,
                    eventId);
        } else {
            Long userId = tokenService.extractUserId(token);

            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    userId,
                    eventId);
        }

        return order;
    }

    private void saveOrder(ActiveOrder order) {
        if (order.getStatus() == ActiveOrder.OrderStatus.COMPLETED) {
            expirationWarningSentOrderIds.remove(order.getOrderId());
            orderRepository.deleteOrder(order.getOrderId());
        } else {
            orderRepository.updateOrder(order);
        }
        // eventRepository.updateMap(event);
        // if (event != null && !event.isSoldOut()) {
        // soldOutNotificationSentEventIds.remove(event.getId());
        // }
    }

    private void notifyEventManagersIfBecameSoldOut(Event event, boolean wasSoldOutBefore) {
        if (event == null || notificationsService == null || companyRepository == null || membershipDomain == null) {
            return;
        }

        if (wasSoldOutBefore || !event.isSoldOut()) {
            return;
        }

        if (!soldOutNotificationSentEventIds.add(event.getId())) {
            return;
        }

        try {
            Company company = companyRepository.findById(event.getCompanyId()).orElse(null);
            if (company == null) {
                return;
            }

            Set<Long> staffMemberIds = membershipDomain.getManagementSubTreeMemberIds(
                    company.getFounderId(),
                    company.getId());

            if (staffMemberIds == null || staffMemberIds.isEmpty()) {
                return;
            }

            Set<Long> recipients = new HashSet<>();

            for (Long memberId : staffMemberIds) {
                if (memberId != null
                        && membershipDomain.validatePermission(
                                memberId,
                                company.getId(),
                                Permission.MANAGE_EVENT_INVENTORY)) {
                    recipients.add(memberId);
                }
            }

            notificationsService.notifyMembers(
                    recipients,
                    "The event \"" + event.getName() + "\" is now sold out.");

            logger.logEvent(
                    "Sold out notification sent for eventId=" + event.getId()
                            + ", companyId=" + event.getCompanyId(),
                    LogLevel.INFO);

        } catch (Exception e) {
            soldOutNotificationSentEventIds.remove(event.getId());

            logger.logEvent(
                    "Failed to send sold out notification for eventId="
                            + event.getId() + ". reason=" + e.getMessage(),
                    LogLevel.WARN);
        }
    }

    private void notifyOrderOwner(ActiveOrder order, String message) {
        if (order == null || message == null || message.isBlank()) {
            return;
        }

        try {
            if (order.getUserId() != null) {
                notificationsService.notifyMember(order.getUserId(), message);
                return;
            }

            String sessionToken = order.getSessionToken();
            if (sessionToken == null) {
                return;
            }

            try {
                tokenService.validateToken(sessionToken);
                notificationsService.notifyGuest(sessionToken, message);
            } catch (Exception ignored) {
                // אורח לא מחובר / טוקן פג — אין למי לשלוח התראה
            }

        } catch (Exception e) {
            logger.logEvent("Failed to notify order owner. reason=" + e.getMessage(), LogLevel.WARN);
        }
    }

    public OrderDTO toDTO(ActiveOrder order, Event event, BigDecimal total, Integer transactionId) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        List<PurchaseDTO> ticketDTOs = new ArrayList<>();

        for (Ticket ticket : order.getTickets()) {
            ticketDTOs.add(new PurchaseDTO(
                    ticket.getTicketId(),
                    ticket.getRow(),
                    ticket.getChair(),
                    event.getAreaName(ticket.getAreaId()),
                    ticket.getPrice(),
                    "ACTIVE",
                    ""));
        }

        Long eventId = order.getEventId();
        Long memberId = order.getUserId();

        String eventName = event == null ? "אירוע" : event.getName();
        String location = event == null ? "" : event.getLocation().toString();

        Long companyId = event == null ? null : event.getCompanyId();

        return new OrderDTO(
                0L,
                ticketDTOs,
                eventName,
                location,
                memberId,
                companyId,
                null,
                eventId,
                total == null ? BigDecimal.ZERO : total,
                transactionId,
                false);
    }

    public ActiveOrderDTO toDTO(ActiveOrder order) {
        List<TicketDTO> ticketDTOs = new ArrayList<>();

        for (Ticket ticket : order.getTickets()) {
            ticketDTOs.add(new TicketDTO(
                    ticket.getTicketId(),
                    ticket.getEventId(),
                    ticket.getAreaId(),
                    ticket.getRow(),
                    ticket.getChair(),
                    ticket.getPrice()));
        }

        return new ActiveOrderDTO(
                order.getOrderId(),
                order.getUserId(),
                order.getEventId(),
                ticketDTOs,
                order.getExpiresAtEpochMillis());
    }

    private TicketIssueRequest createTicketIssueRequest(PurchaseDTO purchesDTO, OrderDTO orderDTO) {
        {

            boolean seating = isSeating(purchesDTO);
            String seatsJson = seating
                    ? buildSeatsJson(purchesDTO)
                    : null;

            return new TicketIssueRequest(
                    String.valueOf(orderDTO.getMemberId()),
                    String.valueOf(orderDTO.getEventId()),
                    purchesDTO.getAreaName(),
                    seating ? null : 1,
                    seating,
                    seatsJson);
        }
    }

    private boolean isSeating(PurchaseDTO purchesDTO) {
        if (purchesDTO.getRow() != 0 && purchesDTO.getChair() != 0) {
            return true;
        }
        return false;
    }

    private String buildSeatsJson(PurchaseDTO purchaseDTO) {
        return String.format(
                "[{\"row\":%d,\"seat\":%d}]",
                purchaseDTO.getRow(),
                purchaseDTO.getChair());
    }
}
