package InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import DomainLayer.order.Order;
import DomainLayer.order.Ticket;
import DomainLayer.IRepository.IOrderRepository;

public class OrderRepository implements IOrderRepository {

    private OrderRepository instance;
    private ConcurrentHashMap<Integer, Order> orders;

    private OrderRepository() {
        orders = new ConcurrentHashMap<Integer, Order>();
    }

    public OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }
    
    public synchronized void addOrder(int orderId, List<Ticket> tickets) {

    }

    public Order findOrderById(int orderId) {

    }

    public void updateOrder() {

    }

    public synchronized void deleteOrder(int orderId) {

    }

    public List<Order> getAll() {

    }
}
