package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.PaymentDetails;

public interface IPaymentService {

    boolean connect();
    void pay(OrderDTO order, PaymentDetails details);
    
}
