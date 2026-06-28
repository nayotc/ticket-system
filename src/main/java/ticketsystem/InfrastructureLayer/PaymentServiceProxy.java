package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.PaymentDetails;

@Component
public class PaymentServiceProxy implements IPaymentService {

    /** Default successful transaction id per WSEP external payment API ([10000, 100000]). */
    public static final int DEFAULT_SUCCESS_TRANSACTION_ID = 50_000;

    public static boolean isConnectionSuccessful = true; // for testing
    public static boolean isPaymentSuccessful = true;    // for testing
    public static boolean isRefundSuccessful = true;     // for testing
    public static Integer forcedPayTransactionId = null;   // for testing invalid external pay responses

    public static boolean wasConnectCalled = false;
    public static boolean wasPayCalled = false;
    public static boolean wasRefundCalled = false;

    @Override
    public boolean handshake() {
        wasConnectCalled = true;
        return isConnectionSuccessful;
    }

    @Override
    public Integer pay(BigDecimal amount, PaymentDetails details) {
        wasPayCalled = true;

        if (forcedPayTransactionId != null) {
            return forcedPayTransactionId;
        }

        if (!isPaymentSuccessful) {
            return -1;
        }

        return DEFAULT_SUCCESS_TRANSACTION_ID;
    }

    @Override
    public boolean refund(Integer transactionId) {
        wasRefundCalled = true;

        if (transactionId == null || transactionId == -1) {
            return false;
        }

        return isRefundSuccessful;
    }

    public static void reset() {
        isConnectionSuccessful = true;
        isPaymentSuccessful = true;
        isRefundSuccessful = true;
        forcedPayTransactionId = null;

        wasConnectCalled = false;
        wasPayCalled = false;
        wasRefundCalled = false;
    }
}