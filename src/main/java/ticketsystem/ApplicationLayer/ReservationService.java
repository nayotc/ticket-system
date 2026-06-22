package ticketsystem.ApplicationLayer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import org.springframework.stereotype.Service;

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
          
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
             Event event = eventRepository.getEventById(eventId);
            if (eventRepository.getEventById(eventId) == null) {
                throw new IllegalArgumentException("Event not found");
            }

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
            eventRepository.updateSeatStatus(eventId, areaId, position.getRow(), position.getChair(),SeatStatus.RESERVED);
            logger.logEvent("Seat ticket selected: orderId=" + order.getOrderId() + ", eventId=" + eventId + ", areaId="
                    + areaId + ", position=" + position, LogLevel.INFO);
            if (event != null && !event.isSoldOut()) {
            soldOutNotificationSentEventIds.remove(event.getId());
            }
            return true;

        } catch (Exception e) {
            logger.logEvent("selectSeatTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity, String lotteryCode) {
          
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
            
            if (event != null && !event.isSoldOut()) {
            soldOutNotificationSentEventIds.remove(event.getId());
            }
            return true;
        } catch (Exception e) {
            logger.logEvent("selectStandingTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    // UC 2.7
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
          
        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));
            ActiveOrder order = findActiveOrder(token, eventId);
            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            Ticket ticket = reservationDomeinService.removeTicketFromActiveOrder(order, event, ticketId);
           
            updateRemoveTicket(eventId, ticket.getAreaId(), ticket, 1);
            saveOrder(order);
            
            logger.logEvent("Ticket removed from active order: orderId=" + order.getOrderId() + ", eventId=" + eventId
                    + ", ticketId=" + ticketId, LogLevel.INFO);
            if (event != null && !event.isSoldOut()) {
            soldOutNotificationSentEventIds.remove(event.getId());
            }
            return true;

        } catch (Exception e) {
            logger.logEvent("removeTicketFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean removeSeatTicketFromActiveOrder(String token, Long eventId, Long areaId, seatPositionDTO position) {
          

        try {
            tokenService.validateToken(token);
            userAccessService.validateCanPerformNonViewAction(tokenService.extractUserId(token));

            if (eventId == null || areaId == null || position == null) {
                throw new IllegalArgumentException("Seat removal details are incomplete");
            }

            ActiveOrder order = findActiveOrder(token, eventId);

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            Event event = eventRepository.getEventById(eventId);

            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }

            Long ticketId = order.getTickets().stream()
                    .filter(ticket -> areaId.equals(ticket.getAreaId()))
                    .filter(ticket -> ticket.getRow() == position.getRow())
                    .filter(ticket -> ticket.getChair() == position.getChair())
                    .map(ticket -> ticket.getTicketId())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found in active order"));

           Ticket ticket= reservationDomeinService.removeTicketFromActiveOrder(order, event, ticketId);
            updateRemoveTicket(eventId, areaId, ticket, 1);
            saveOrder(order);
          

            logger.logEvent(
                    "Seat ticket removed from active order: orderId=" + order.getOrderId()
                            + ", eventId=" + eventId
                            + ", areaId=" + areaId
                            + ", row=" + position.getRow()
                            + ", chair=" + position.getChair(),
                    LogLevel.INFO);
            if (event != null && !event.isSoldOut()) {
                soldOutNotificationSentEventIds.remove(event.getId());
            }
            return true;

        } catch (Exception e) {
            logger.logEvent("removeSeatTicketFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean removeStandingTicketsFromActiveOrder(String token, Long eventId, Long areaId, int quantity) {
          
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
            
            if (event != null && !event.isSoldOut()) {
                soldOutNotificationSentEventIds.remove(event.getId());
            }
            return true;

        } catch (Exception e) {
            logger.logEvent("removeStandingTicketsFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public ActiveOrderDTO viewActiveOrder(String token, Long orderId) {
          

        try {
            tokenService.validateToken(token);

            ActiveOrder order = orderRepository.findOrderById(orderId);

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found");
            }

            if (!isOrderOwnedByToken(order, token)) {
                throw new SecurityException("User is not allowed to view this order");
            }

            ActiveOrderDTO activeOrderDTO = toDTO(order);

            logger.logEvent(
                    "Active order viewed: orderId=" + order.getOrderId()
                            + ", eventId=" + order.getEventId(),
                    LogLevel.INFO);

            return activeOrderDTO;

        } catch (Exception e) {
            logger.logEvent("viewActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Returns the current active order for the given UI session token.
     * This method is intended for presentation-layer flows such as the active order
     * cart, where the UI needs to display the current user's active order without
     * receiving an order id in the route.
     *
     * The lookup is based on the token type:
     * - guest token: finds the active order by session token
     * - member token: extracts the member id and finds the active order by user id
     *
     * If no active order exists, or if the found order is no longer ACTIVE, this
     * method returns null so the UI can render an empty cart state.
     *
     * @param token active guest/member session token
     * @return active order DTO for the current session, or null if none exists
     */
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

    /**
     * Calculates the pricing quote DTO for the current active order.
     *
     * This method calculates the original active-order amount, applies the
     * company-level and event-level discount policies through the domain layer,
     * and returns an application-layer DTO with the subtotal, total discount
     * amount, final total, and the discounts that were actually applied.
     *
     * This method only calculates pricing. It does not complete checkout, does not
     * charge payment, and does not validate purchase-policy rules that require
     * buyer details, such as minimum age. Purchase-policy validation is still done
     * before payment.
     *
     * The domain layer returns a PricingQuote object, but this service converts it
     * to PricingQuoteDTO before returning it so upper layers do not receive
     * domain-layer objects directly.
     *
     * @param token      active guest/member session token
     * @param eventId    event identifier of the active order
     * @param couponCode coupon code entered by the user, if any
     * @return full pricing DTO for the active order
     */
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

    @Transactional
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
            BigDecimal amount = reservationDomeinService.calculatePrice(order, event);
            BigDecimal amountAfterDiscount = eventCatalogDomainService.calculateFinalPrice(event.getCompanyId(), event,
                    amount, order.getTickets().size(), couponCode);
            
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
                expireCurrentOrder(token, order, event);
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

    private void expireCurrentOrder(String token, ActiveOrder order, Event event) {
        notifyOrderOwner(
                order,
                "Your active order has expired. Please select tickets again."
        );

        expirationWarningSentOrderIds.remove(order.getOrderId());
        //eventRepository.updateEvent(event);
        releaseTicketInEvent(order.getTickets());
        orderRepository.deleteOrder(order.getOrderId());

        logger.logEvent(
                "Active order expired during checkout: orderId="
                        + order.getOrderId() + ", eventId=" + event.getId(),
                LogLevel.INFO
        );
    }

    // listener
    public void addOrderListener(OrderCompletedListener listener) {
        listeners.add(listener);
    }
    @Transactional
    public void sweepExpiredAndExpiringOrders() {
        expireOldOrders();
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

    // refund
    /**
     * Handles failures that happen after the payment was already approved but
     * before the checkout flow was fully completed.
     *
     * At this stage, the system must refund the payment because the purchase
     * cannot be safely completed. The method also logs the original failure cause
     * before throwing a user-facing checkout failure exception.
     *
     * Logging the original exception is important because the public exception
     * message intentionally stays general, while the internal cause may explain
     * whether the failure came from ticket issuing, order status validation,
     * seat status validation, persistence, or another checkout-completion step.
     *
     * @param order                active order that failed during checkout
     *                             completion
     * @param event                event related to the active order
     * @param amount               payment amount that should be refunded
     * @param originalException    original exception that caused checkout
     *                             completion to fail
     * @param refundSuccessMessage message used when refund succeeds
     * @param refundFailureMessage message used when refund itself fails
     */
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
        OrderDTO orderDTO = toDTO(order, event,total, transactionId);

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

    // Helper methods
    private boolean isOrderOwnedByToken(ActiveOrder order, String token) {
        if (tokenService.isGuestToken(token)) {
            return order.getSessionToken().equals(token);
        }

        Long userId = tokenService.extractUserId(token);
        return order.getUserId() != null && order.getUserId().equals(userId);
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
    

    private void expireOldOrders() {
        List<ActiveOrder> allOrders = orderRepository.getAll();

        for (ActiveOrder order : allOrders) {
            try{
            Event event = eventRepository.getEventById(order.getEventId());

            if (event == null || order == null) {
                continue;
            }
            String token= order.getSessionToken();
            boolean tokenExpired =false;
            try{
                tokenService.validateToken(token);
            }
            catch (Exception e){
                tokenExpired = true;
            }
            boolean guestOrder = order.getUserId() == null;
            if (reservationDomeinService.timeExpire(event, order) ||  (tokenExpired &&guestOrder)) {
                List<Ticket> tickets= reservationDomeinService.expire(event, order);
                
                notifyOrderOwner(
                        order,
                        "Your active order has expired. The reserved tickets were released back to the inventory.");
                
                expirationWarningSentOrderIds.remove(order.getOrderId());
                releaseTicketInEvent(tickets);
                orderRepository.deleteOrder(order.getOrderId());

                logger.logEvent(
                        "Expired order cancelled: " + order.getOrderId(),
                        LogLevel.WARN);

                continue;
            }

            if (reservationDomeinService.timeAboutToExpire(event, order) && expirationWarningSentOrderIds.add(order.getOrderId())) {

                notifyOrderOwner(
                        order,
                        "Your active order is about to expire. Please complete your purchase soon.");

                logger.logEvent(
                        "Active order expiration warning sent: " + order.getOrderId(),
                        LogLevel.INFO);
            }
        }
        catch (Exception e){
                          logger.logEvent(
                    "Failed to expire orderId=" + order.getOrderId()
                            + ", eventId=" + order.getEventId()
                            + ", reason=" + e.getMessage(),
                    LogLevel.WARN
            );
        }
    }
}
 

    private void releaseTicketInEvent(List<Ticket> tickets) {
		for(Ticket ticket: tickets){
            updateRemoveTicket(ticket.getEventId(), ticket.getAreaId(), ticket, 1);
        }
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
                ""
        ));
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
            false
    );
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
                    ticket.getPrice()
            ));
        }

    return new ActiveOrderDTO(
            order.getOrderId(),
            order.getUserId(),
            order.getEventId(),
            ticketDTOs,
            order.getExpiresAtEpochMillis()
    );
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
                    seating
                            ? TicketIssueRequest.TicketZoneType.SEATING
                            : TicketIssueRequest.TicketZoneType.STANDING,
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
