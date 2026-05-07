package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;

public interface IPaymentService {

    boolean connect();
    boolean pay(OrderDTO order, PaymentDetails details);
    
}
