package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;

public interface IPaymentService {

    boolean connect();
    boolean pay(BigDecimal amount, PaymentDetails details);
    
}
