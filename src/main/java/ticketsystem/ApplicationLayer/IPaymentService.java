package ticketsystem.ApplicationLayer;

public interface IPaymentService {

    boolean connect();
    void pay(OrderDTO order, PaymentDetails details);
    
}
