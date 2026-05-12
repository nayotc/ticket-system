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

public class ReservationService{

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final TokenService tokenService;
    private final IPaymentService paymentService;
    private final ISecureBarcode secureBarcode;
    private final ILotteryRepository lotteryRepository;
    private final Reservation reservation;
    private final ISystemLogger logger;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();




    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            TokenService tokenService,IPaymentService paymentService, ISecureBarcode secureBarcode, ILotteryRepository lotteryRepository,ISystemLogger logger) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.paymentService=paymentService;
        this.secureBarcode=secureBarcode;
        this.lotteryRepository=lotteryRepository;
        this.logger=logger;
        this.reservation=new Reservation();
        
    }

//UC 2.5,2.4
     public boolean selectSeatTicket(String token, Long eventId, Long areaId, seatPositionDTO position,String lotteryCode) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);

            if(eventRepository.getEventById(eventId)==null) {
                throw new IllegalArgumentException("Event not found");
            }
            Lottery lottery = lotteryRepository.findByEventId(eventId);

            if (lottery != null) {
                Long userId = tokenService.extractUserId(token);
                reservation.checkLottery(lottery, userId, lotteryCode);
            }

            ActiveOrder order = getOrCreateOrder(token, eventId);
            if(order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }

            Event event = eventRepository.getEventById(eventId);
            reservation.selectSeatTicket(order, event, areaId, position);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            logger.logEvent("selectSeatTicket failed: " + e.getMessage(),LogLevel.WARN);
            throw e;
        }
    }
    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity,String lotteryCode) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            if(eventRepository.getEventById(eventId)==null) {
                    throw new IllegalArgumentException("Event not found");
            }

            Lottery lottery = lotteryRepository.findByEventId(eventId);

            if (lottery != null) {
                Long userId = tokenService.extractUserId(token);
                reservation.checkLottery(lottery, userId, lotteryCode);
            }

            ActiveOrder order = getOrCreateOrder(token, eventId);
            if(order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.selectStandingTicket(order, event, areaId, quantity);
            saveAll(order, event);
            return true;
        } catch (Exception e) {
            logger.logEvent("selectStandingTicket failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }


    //UC 2.7
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);
            if (order==null|| order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.removeTicketFromActiveOrder(order, event, ticketId);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            logger.logEvent("removeTicketFromActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

       //uc 2.7

    public ActiveOrderDTO viewActiveOrder(String token, Long orderId) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            ActiveOrder order = orderRepository.findOrderById(orderId);
            if (order == null|| order.getStatus() != ActiveOrder.OrderStatus.ACTIVE) {
                throw new IllegalStateException("No active order found for this event");
            }
            ObjectMapper objectMapper = new ObjectMapper();
           ActiveOrderDTO activeOrderDTO = objectMapper.convertValue(
                order, 
                ActiveOrderDTO.class
            );
            return activeOrderDTO;
        } 
        catch (Exception e) {
            logger.logEvent("viewActiveOrder failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }
    }

    // UC 2.8
    private void submitActiveOrderForCheckout(String token, Long eventId) {
            ActiveOrder order = findActiveOrder(token, eventId);
            if(order==null) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.submitActiveOrderForCheckout(order, event);
            saveAll(order, event);
    }
    //for testing purposes
    public boolean enterUserDetails(String name, String Email){
        return true;
    }
   
    public boolean checkout(String token, Long eventId, PaymentDetails details) {
        expireOldOrders();
        try {    
            tokenService.validateToken(token);
            submitActiveOrderForCheckout(token, eventId);
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            BigDecimal amount = reservation.calculateTotalPrice(order, event);           

            boolean paymentResult = paymentService.pay(amount, details);

            if (!paymentResult) {
                order.paymentFailed();
                throw new IllegalStateException("Payment failed");
            }
    
            try {
                OrderDTO orderDTO = order.toDTO(event.getName(),event.getLocation().toString(), event.getCompanyId() );
                for(PurchaseDTO purchesDTO : orderDTO.getTickets()) {
                    String barcode = secureBarcode.generateSecureBarcode(purchesDTO.getTicketId(),order.getEventId(),order.getUserId());
                    purchesDTO.setSecureBarcode(barcode);
                
                }
                reservation.completeCheckout(order, event);
                saveAll(order, event);
                notifyListeners(orderDTO);
                return true;

            } catch (Exception issuingException) {
                boolean refundResult = paymentService.refund(amount, details);
                order.paymentFailed();
                saveAll(order, event);

                if (!refundResult) {
                    throw new IllegalStateException(
                            "Ticket issuing failed and refund failed",
                            issuingException
                    );
                }

                throw new IllegalStateException(
                        "Ticket issuing failed. Payment was refunded.",
                        issuingException
                );
            }
        } catch (Exception e) {
            logger.logEvent("checkout failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }

    }
    //listener
    public void addOrderListener(OrderCompletedListener listener) {
            listeners.add(listener);
    }

    private void notifyListeners(OrderDTO order) {
        for (OrderCompletedListener listener : listeners) {
            listener.onOrderCompleted(order);
        }
    }

    //Helper methods

    private ActiveOrder getOrCreateOrder(String token, Long eventId) {
         ActiveOrder order = findActiveOrder(token, eventId);

    if (order == null) {
        Long userId = tokenService.isMemberToken(token)
                ? tokenService.extractUserId(token)
                : null;

        order = new ActiveOrder(
                orderRepository.getNextId(),token,
                userId,
                eventId
        );

        orderRepository.addOrder(order);
    }

        return order;
    }
    

    private ActiveOrder findActiveOrder(String token, Long eventId) {

        ActiveOrder order;

        if (tokenService.isGuestToken(token)) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    token,
                    eventId
            );
        } else {
            Long userId = tokenService.extractUserId(token);

            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    userId,
                    eventId
            );
        }

        return order;
    }
    private void saveAll(ActiveOrder order, Event event) {
        eventRepository.updateEvent(event);

        if (order.getStatus() == ActiveOrder.OrderStatus.CANCELLED
                || order.getStatus() == ActiveOrder.OrderStatus.COMPLETED) {
            orderRepository.deleteOrder(order.getOrderId());
        } else {
            orderRepository.updateOrder(order);
        }
    }


    private void expireOldOrders() {
        List<ActiveOrder> allOrders = orderRepository.getAll();
        for (ActiveOrder order : allOrders) {
            if ((order.getStatus() != ActiveOrder.OrderStatus.PENDING_CHECKOUT && order.isExpired())) {
                Event event = eventRepository.getEventById(order.getEventId());
                reservation.expire(event,order);
                orderRepository.deleteOrder(order.getOrderId());
                //logger.logEvent("Expired order cancelled: " + order.getOrderId(), LogLevel.WARN);
         }
    }
    }

}