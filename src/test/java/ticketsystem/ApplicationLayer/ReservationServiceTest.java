// package ticketsystem.ApplicationLayer;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import java.lang.reflect.Field;

// import ticketsystem.ApplicationLayer.ReservationService;
// import ticketsystem.ApplicationLayer.TokenService;
// import ticketsystem.ApplicationLayer.IPaymentService;
// import ticketsystem.DTO.OrderDTO;
// import ticketsystem.DTO.PaymentDetails;
// import ticketsystem.DomainLayer.Reservation;
// import ticketsystem.DomainLayer.IRepository.IEventRepository;
// import ticketsystem.DomainLayer.IRepository.IOrderRepository;
// import ticketsystem.DomainLayer.event.Event;
// import ticketsystem.DomainLayer.order.ActiveOrder;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;

// public class ReservationServiceTest {

//     private IOrderRepository orderRepository;
//     private IEventRepository eventRepository;
//     private TokenService tokenService;
//     private IPaymentService paymentService;
//     private Reservation reservation;

//     private ReservationService service;

//     private final String token = "valid-token";
//     private final int eventId = 1;

//     private ActiveOrder order;
//     private Event event;
//     private PaymentDetails paymentDetails;

//     @BeforeEach
//     void setUp() throws Exception {
//         orderRepository = mock(IOrderRepository.class);
//         eventRepository = mock(IEventRepository.class);
//         tokenService = mock(TokenService.class);
//         paymentService = mock(IPaymentService.class);
//         reservation = mock(Reservation.class);

//         order = mock(ActiveOrder.class);
//         event = mock(Event.class);
//         paymentDetails = mock(PaymentDetails.class);

//         service = new ReservationService(
//                 orderRepository,
//                 eventRepository,
//                 tokenService,
//                 paymentService
//         );

//         injectReservationMock();

//         when(tokenService.validateToken(token)).thenReturn(true);
//         when(tokenService.isGuestToken(token)).thenReturn(false);
//         when(tokenService.isMemberToken(token)).thenReturn(true);
//         when(tokenService.extractUserId(token)).thenReturn(7L);

//         when(orderRepository.getActiveOrderByUserIdAndEventId(7L, eventId))
//                 .thenReturn(order);

//         when(eventRepository.getEventById(eventId)).thenReturn(event);
//     }

//     private void injectReservationMock() throws Exception {
//         Field field = ReservationService.class.getDeclaredField("reservation");
//         field.setAccessible(true);
//         field.set(service, reservation);
//     }

//     @Test
//     void selectSeatTicket_success_updatesOrderAndEvent() {
//         service.selectSeatTicket(token, eventId, 2, 5);

//         verify(reservation).selectSeatTicket(order, event, 2, 5);
//         verify(orderRepository).updateOrder(order);
//         verify(eventRepository).updateEvent(event);
//     }

//     @Test
//     void selectStandingTicket_success_updatesOrderAndEvent() {
//         service.selectStandingTicket(token, eventId, 3);

//         verify(reservation).selectStandingTicket(order, event, 3);
//         verify(orderRepository).updateOrder(order);
//         verify(eventRepository).updateEvent(event);
//     }

//     @Test
//     void removeTicketFromActiveOrder_success_updatesOrderAndEvent() {
//         service.removeTicketFromActiveOrder(token, eventId, 10);

//         verify(reservation).removeTicketFromActiveOrder(order, event, 10);
//         verify(orderRepository).updateOrder(order);
//         verify(eventRepository).updateEvent(event);
//     }

//     @Test
//     void submitActiveOrderForCheckout_success_updatesOrderAndEvent() {
//         service.submitActiveOrderForCheckout(token, eventId);

//         verify(reservation).submitActiveOrderForCheckout(order, event);
//         verify(orderRepository).updateOrder(order);
//         verify(eventRepository).updateEvent(event);
//     }

//     // @Test
//     // void checkout_paymentSuccess_completesCheckoutUpdatesAndNotifies() {
//     //     OrderDTO orderDTO = mock(OrderDTO.class);

//     //     doReturn(100.0).when(order).calculateTotalPrice();
//     //     doReturn(orderDTO).when(order).toDTO();

//     //     when(paymentService.pay(eq(orderDTO), eq(paymentDetails))).thenReturn(true);

//     //     service.checkout(token, eventId, paymentDetails);

//     //     verify(paymentService).pay(eq(orderDTO), eq(paymentDetails));
//     //     verify(reservation).completeCheckout(order, event);
//     //     verify(orderRepository).updateOrder(order);
//     //     verify(eventRepository).updateEvent(event);
//     // }

//     // @Test
//     // void checkout_paymentFails_doesNotCompleteCheckoutAndDoesNotUpdate() {
//     //     OrderDTO orderDTO = mock(OrderDTO.class);

//     //     doReturn(100.0).when(order).calculateTotalPrice();
//     //     doReturn(orderDTO).when(order).toDTO();

//     //     when(paymentService.pay(eq(orderDTO), eq(paymentDetails))).thenReturn(false);

//     //     assertThrows(IllegalStateException.class, () ->
//     //             service.checkout(token, eventId, paymentDetails)
//     //     );

//     //     verify(paymentService).pay(eq(orderDTO), eq(paymentDetails));
//     //     verify(reservation, never()).completeCheckout(any(), any());
//     //     verify(orderRepository, never()).updateOrder(order);
//     //     verify(eventRepository, never()).updateEvent(event);
//     // }

//     @Test
//     void invalidToken_doesNotUpdateAnything() {
//         when(tokenService.validateToken(token)).thenReturn(false);

//         assertThrows(IllegalArgumentException.class, () ->
//                 service.selectStandingTicket(token, eventId, 2)
//         );

//         verify(reservation, never()).selectStandingTicket(any(), any(), anyInt());
//         verify(orderRepository, never()).updateOrder(any());
//         verify(eventRepository, never()).updateEvent(any());
//     }

//     @Test
//     void eventNotFound_doesNotUpdateAnything() {
//         when(eventRepository.getEventById(eventId)).thenReturn(null);

//         assertThrows(IllegalArgumentException.class, () ->
//                 service.selectSeatTicket(token, eventId, 1, 1)
//         );

//         verify(reservation, never()).selectSeatTicket(any(), any(), anyInt(), anyInt());
//         verify(orderRepository, never()).updateOrder(order);
//         verify(eventRepository, never()).updateEvent(any());
//     }

//     @Test
//     void existingOrderNotFound_removeTicket_throwsAndDoesNotUpdate() {
//         when(orderRepository.getActiveOrderByUserIdAndEventId(7L, eventId))
//                 .thenReturn(null);

//         assertThrows(IllegalArgumentException.class, () ->
//                 service.removeTicketFromActiveOrder(token, eventId, 10)
//         );

//         verify(reservation, never()).removeTicketFromActiveOrder(any(), any(), anyInt());
//         verify(orderRepository, never()).updateOrder(any());
//         verify(eventRepository, never()).updateEvent(any());
//     }
// }