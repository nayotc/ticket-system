package ticketsystem.DomainLayer.history;

import java.time.LocalDate;

public class PaymentDetails {
    
    
    private final String paymentMethodId; // Stripe / Fake
    private final String payerName;
    private final LocalDate birthDate;

    public PaymentDetails(String paymentMethodId, String payerName, LocalDate birthDate) {
        this.paymentMethodId = paymentMethodId;
        this.payerName = payerName;
        this.birthDate = birthDate;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getPayerName() {
        return payerName;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
}
