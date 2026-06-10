package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import ticketsystem.DTO.PaymentDetails;

public interface IPaymentService {

    boolean handshake();

    Integer pay(BigDecimal amount, PaymentDetails details);
    
    boolean refund(Integer transactionId);
}