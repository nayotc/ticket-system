package ticketsystem.DTO;

import java.time.LocalDate;

public class PaymentDetails {
    
    
    private String paymentMethodId; // Stripe / Fake
    private String payerName;
    private LocalDate birthDate;

    public PaymentDetails() {
    }

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
    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }
     public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}