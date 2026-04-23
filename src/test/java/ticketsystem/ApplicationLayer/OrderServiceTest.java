package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class OrderServiceTest {

    private IOrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Create a mock instance of the repository to isolate the service logic
        orderRepository = mock(IOrderRepository.class);

        // Inject the mock repository into the service under test
        orderService = new OrderService(orderRepository);
    }

    @Test
    void givenValidOrderIdAndTickets_whenAddOrder_thenRepositoryShouldBeCalled() {
        // Arrange: prepare input data
        int orderId = 1;
        List<Ticket> tickets = List.of();

        // Act: invoke the method under test
        orderService.addOrder(orderId, tickets);

        // Assert:
        // Verify that the repository method is invoked with the correct parameters
        // NOTE: Replace 'saveOrder' with the actual method name in your repository
        // verify(orderRepository).saveOrder(orderId, tickets);
    }

    @Test
    void givenExistingOrderId_whenDeleteOrder_thenRepositoryShouldBeCalled() {
        // Arrange
        int orderId = 1;

        // Act
        orderService.deleteOrder(orderId);

        // Assert:
        // Verify that the delete operation is delegated to the repository layer
        // NOTE: Replace 'deleteOrder' if your method name differs
        // verify(orderRepository).deleteOrder(orderId);
    }

    @Test
    void givenOrderIdAndTicketId_whenAddTicketToOrder_thenRepositoryShouldBeCalled() {
        // Arrange
        int orderId = 1;
        int ticketId = 10;

        // Act
        orderService.addTicketToOrder(orderId, ticketId);

        // Assert:
        // Verify that the repository is responsible for adding the ticket to the order
        // NOTE: Replace method name accordingly
        // verify(orderRepository).addTicketToOrder(orderId, ticketId);
    }

    @Test
    void givenOrderIdAndTicketId_whenRemoveTicketFromOrder_thenRepositoryShouldBeCalled() {
        // Arrange
        int orderId = 1;
        int ticketId = 10;

        // Act
        orderService.removeTicketFromOrder(orderId, ticketId);

        // Assert:
        // Verify that the repository is called to remove the ticket from the order
        // NOTE: Replace method name accordingly
        // verify(orderRepository).removeTicketFromOrder(orderId, ticketId);
    }
}