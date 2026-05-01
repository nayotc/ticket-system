package ticketsystem.DTO;

public class PaymentDetails {

    private final String paymentMethodId; // Stripe / Fake
    private final String payerName;

    public PaymentDetails(String paymentMethodId, String payerName) {
        this.paymentMethodId = paymentMethodId;
        this.payerName = payerName;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getPayerName() {
        return payerName;
    }
}