package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;

public class PaymentServiceProxy implements IPaymentService {

    public static boolean isConnectionSuccessful = true; // for testing purposes
    public static boolean wasConnectCalled = false; // for testing purposes

    @Override
    public boolean connect() {
        wasConnectCalled = true;
        return isConnectionSuccessful;
    }

    @Override
    public boolean pay(OrderDTO order, PaymentDetails details) {
      //  System.out.println("Payment Proxy: Processing payment for Order ID: " + order.getOrderId() + " with amount: $" + details.getAmount());
        // Simulate payment processing logic here   
        return true; // Simulate successful payment    
        }



}
