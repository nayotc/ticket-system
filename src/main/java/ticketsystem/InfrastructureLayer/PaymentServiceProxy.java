package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.IPaymentService;

public class PaymentServiceProxy implements IPaymentService {

    @Override
    public boolean connect() {
        System.out.println("Payment Proxy Successfully connected to external Payment Service.");
        return true;
    }

    @Override
    public void pay(OrderDTO order, PaymentDetails details) {
        System.out.println("Payment Proxy: Processing payment for Order ID: " + order.getOrderId() + " with amount: $" + amount);
        // Simulate payment processing logic here       
        }



}
