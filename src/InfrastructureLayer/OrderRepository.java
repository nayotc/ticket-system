package InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import DomainLayer.order.Order;
import DomainLayer.IRepository.IOrderRepository;

public class OrderRepository implements IOrderRepository {

    private int counter;
    private OrderRepository instance;
    private ConcurrentHashMap<Integer, Order> orders;

    private OrderRepository() {
        this.counter = 1;
        this.orders = new ConcurrentHashMap<Integer, Order>();
    }

    public OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }
    
    public synchronized void addOrder() {

    }

    public Order findOrderById(int orderId) {

    }

    public void updateOrder(Order order) {

    }

    public synchronized void deleteOrder(int orderId) {

    }

    public List<Order> getAll() {

    }
}
