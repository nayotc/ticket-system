package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.IPaymentService;

public class PaymentServiceProxy implements IPaymentService {

    public static boolean isConnectionSuccessful = true; // for testing purposes
    public static boolean wasConnectCalled = false; // for testing purposes

    @Override
    public boolean connect() {
        wasConnectCalled = true;
        return isConnectionSuccessful;
    }

    @Override
    public void pay(OrderDTO order, PaymentDetails details) {
        System.out.println("Payment Proxy: Processing payment for Order ID: " + order.getOrderId() + " with amount: $" + amount);
        // Simulate payment processing logic here       
        }



}
