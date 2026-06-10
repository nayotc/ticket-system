package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.PaymentDetails;

@Component
public class PaymentServiceProxy implements IPaymentService {

    public static boolean isConnectionSuccessful = true; // for testing
    public static boolean isPaymentSuccessful = true;    // for testing
    public static boolean isRefundSuccessful = true;     // for testing

    public static boolean wasConnectCalled = false;
    public static boolean wasPayCalled = false;
    public static boolean wasRefundCalled = false;

    @Override
    public boolean handshake() {
        wasConnectCalled = true;
        return isConnectionSuccessful;
    }


    // public boolean pay(BigDecimal amount, PaymentDetails details) {
    //     wasPayCalled = true;

    //     if (!isConnectionSuccessful) {
    //         return false;
    //     }

    //     return isPaymentSuccessful;
    // }

    
    public boolean refund(BigDecimal amount, PaymentDetails details) {
        wasRefundCalled = true;

        if (!isConnectionSuccessful) {
            return false;
        }

        return isRefundSuccessful;
    }

	@Override
	public boolean refund(Integer transactionId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'refund'");
	}

    @Override
      public Integer pay(BigDecimal amount, PaymentDetails details) {
        wasPayCalled = true;

        if (!isConnectionSuccessful) {
            return -1;
        }

        return 1;
    }
}