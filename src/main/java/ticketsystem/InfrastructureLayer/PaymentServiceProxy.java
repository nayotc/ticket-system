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
    public boolean connect() {
        wasConnectCalled = true;
        return isConnectionSuccessful;
    }

    @Override
    public boolean pay(BigDecimal amount, PaymentDetails details) {
        wasPayCalled = true;

        if (!isConnectionSuccessful) {
            return false;
        }

        return isPaymentSuccessful;
    }

    @Override
    public boolean refund(BigDecimal amount, PaymentDetails details) {
        wasRefundCalled = true;

        if (!isConnectionSuccessful) {
            return false;
        }

        return isRefundSuccessful;
    }
}