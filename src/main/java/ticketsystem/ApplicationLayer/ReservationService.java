package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.slf4j.ILoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;
import java.time.LocalDate;
import java.time.Period;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final TokenService tokenService;
    private final IPaymentService paymentService;
    private final ISecureBarcode secureBarcode;
    private final ILotteryRepository lotteryRepository;
    private final Reservation reservationDomeinService;
    private final ISystemLogger logger;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();

    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            TokenService tokenService, IPaymentService paymentService, ISecureBarcode secureBarcode,
            ILotteryRepository lotteryRepository, ISystemLogger logger) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.secureBarcode = secureBarcode;
        this.lotteryRepository = lotteryRepository;
        this.logger = logger;
        this.reservationDomeinService = new Reservation();

    }

    // UC 2.5,2.4
    public boolean selectSeatTicket(String token, Long eventId, Long areaId, seatPositionDTO position,
            String lotteryCode) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);

            if (eventRepository.getEventById(eventId) == null) {
                throw new IllegalArgumentException("Event not found");
            }
            Lottery lottery = lotteryRepository.findByEventId(eventId);

            if (lottery != null) {
                Long userId = tokenService.extractUserId(token);
                reservationDomeinService.checkLottery(lottery, userId, lotteryCode);
            }

            ActiveOrder order = getOrCreateOrder(token, eventId);
            if (order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            Event event = eventRepository.getEventById(eventId);
            reservationDomeinService.selectSeatTicket(order, event, areaId, position);

            saveAll(order, event);
            logger.logEvent("Seat ticket selected: orderId=" + order.getOrderId() + ", eventId=" + eventId + ", areaId="
                    + areaId + ", position=" + position, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("selectSeatTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity, String lotteryCode) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            if (eventRepository.getEventById(eventId) == null) {
                throw new IllegalArgumentException("Event not found");
            }

            Lottery lottery = lotteryRepository.findByEventId(eventId);

            if (lottery != null) {
                Long userId = tokenService.extractUserId(token);
                reservationDomeinService.checkLottery(lottery, userId, lotteryCode);
            }

            ActiveOrder order = getOrCreateOrder(token, eventId);
            if (order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            Event event = eventRepository.getEventById(eventId);
            reservationDomeinService.selectStandingTicket(order, event, areaId, quantity);
            saveAll(order, event);
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
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);
            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            reservationDomeinService.removeTicketFromActiveOrder(order, event, ticketId);

            saveAll(order, event);
            logger.logEvent("Ticket removed from active order: orderId=" + order.getOrderId() + ", eventId=" + eventId
                    + ", ticketId=" + ticketId, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("removeTicketFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public boolean removeStandingTicketsFromActiveOrder(String token, Long eventId, Long areaId, int quantity) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            Event event = eventRepository.getEventById(eventId);
            reservationDomeinService.removeStandingTicketsFromActiveOrder(order, event, areaId, quantity);

            saveAll(order, event);
            logger.logEvent("Standing tickets removed from active order: orderId=" + order.getOrderId() + ", eventId="
                    + eventId + ", areaId=" + areaId + ", quantity=" + quantity, LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logEvent("removeStandingTicketsFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    public ActiveOrderDTO viewActiveOrder(String token, Long orderId) {
        expireOldOrders();

        try {
            tokenService.validateToken(token);

            ActiveOrder order = orderRepository.findOrderById(orderId);

            if (order == null || order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found");
            }

            if (!isOrderOwnedByToken(order, token)) {
                throw new SecurityException("User is not allowed to view this order");
            }

            ActiveOrderDTO activeOrderDTO = order.toDTO();

            logger.logEvent(
                    "Active order viewed: orderId=" + order.getOrderId()
                            + ", eventId=" + order.getEventId(),
                    LogLevel.INFO
            );

            return activeOrderDTO;

        } catch (Exception e) {
            logger.logEvent("viewActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    // 2.8 checkout
   
        public boolean checkout(String token, Long eventId, PaymentDetails details) {
        expireOldOrders();

        try {
            tokenService.validateToken(token);

            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);

            if (order == null || event == null) {
                throw new IllegalStateException("No active order or event found");
            }

            BigDecimal amount = reservationDomeinService.submitActiveOrderForCheckout(order, event, 
                Period.between(details.getBirthDate(), LocalDate.now()).getYears());

            boolean paymentResult = paymentService.pay(amount, details);

            if (!paymentResult) {
                order.paymentFailed();
                saveAll(order, event);
                throw new IllegalStateException("Payment failed");
            }

            OrderDTO orderDTO;

            try {
                orderDTO = creaOrderDTOwithBarcode(order, event);
            } catch (Exception barcodeException) {
                handleRefundAfterCheckoutFailure(order, event, amount, details, eventId, barcodeException,
                        "Ticket issuing failed. Payment was refunded.",
                        "Ticket issuing failed and refund failed.");
                return false; // unreachable
            }

            try {
                reservationDomeinService.completeCheckout(order, event);
                saveAll(order, event);
            } catch (Exception completeCheckoutException) {
                handleRefundAfterCheckoutFailure(order, event, amount, details, eventId, completeCheckoutException,
                        "Complete checkout failed. Payment was refunded.",
                        "Complete checkout failed and refund failed.");
                return false; // unreachable
            }

            boolean listenersNotified = notifyListeners(orderDTO);
            if (!listenersNotified) {
                logger.logEvent(
                        "Order completed but notifying listeners failed: orderId="
                                + order.getOrderId() + ", eventId=" + eventId,
                        LogLevel.WARN
                );
            }

            logger.logEvent(
                    "Checkout completed successfully: orderId="
                            + order.getOrderId() + ", eventId=" + eventId,
                    LogLevel.INFO
            );

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

    private boolean notifyListeners(OrderDTO order) {
        try{
              for (OrderCompletedListener listener : listeners) {
            listener.onOrderCompleted(order);
        }
        return true;
        }
        catch(Exception e){
            return false;
        }
      
    }
    //refund
        private void handleRefundAfterCheckoutFailure(
            ActiveOrder order,
            Event event,
            BigDecimal amount,
            PaymentDetails details,
            Long eventId,
            Exception originalException,
            String refundSuccessMessage,
            String refundFailureMessage) {

        boolean refundResult = paymentService.refund(amount, details);

        order.paymentFailed();
        saveAll(order, event);

        if (refundResult) {
            logger.logEvent(
                    refundSuccessMessage + " orderId=" + order.getOrderId() + ", eventId=" + eventId,
                    LogLevel.INFO
            );

            throw new IllegalStateException(refundSuccessMessage, originalException);
        }

        logger.logError(
                refundFailureMessage + " orderId=" + order.getOrderId() + ", eventId=" + eventId,
                originalException
        );

        throw new IllegalStateException(refundFailureMessage, originalException);
    }

    //secure barcode logic
    private OrderDTO creaOrderDTOwithBarcode(ActiveOrder order, Event event){
          OrderDTO orderDTO = order.toDTO(event.getName(), event.getLocation().toString(), event.getCompanyId(),
                        event.getOpenedBy(), event.getId());
                for (PurchaseDTO purchesDTO : orderDTO.getTickets()) {
                    String barcode = secureBarcode.generateSecureBarcode(purchesDTO.getTicketId(), order.getEventId(),
                            order.getOrderId());
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
                    orderRepository.getNextId(), token,
                    userId,
                    eventId);

            orderRepository.addOrder(order);
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

    private void saveAll(ActiveOrder order, Event event) {
        
        if (order.getStatus() == ActiveOrder.OrderStatus.COMPLETED) {
            orderRepository.deleteOrder(order.getOrderId());
        } else {
            orderRepository.updateOrder(order);
        }
        eventRepository.updateEvent(event);
    }

    private void expireOldOrders() {
        List<ActiveOrder> allOrders = orderRepository.getAll();
        for (ActiveOrder order : allOrders) {
                Event event = eventRepository.getEventById(order.getEventId());
                if(reservationDomeinService.timeExpire(event, order))
                    orderRepository.deleteOrder(order.getOrderId());
                // logger.logEvent("Expired order cancelled: " + order.getOrderId(),
                // LogLevel.WARN);
            }
        }
}