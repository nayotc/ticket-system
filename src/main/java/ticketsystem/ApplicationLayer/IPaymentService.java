package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;

public interface IPaymentService {

    boolean connect();
    void pay(OrderDTO order, PaymentDetails details);
    
}
