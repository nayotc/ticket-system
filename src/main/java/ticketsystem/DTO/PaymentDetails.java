package ticketsystem.DTO;

import java.time.LocalDate;

public class PaymentDetails {

    private String paymentMethodId;

    private String payerName;

    private LocalDate birthDate;

    private String cardNumber;

    private Integer expirationMonth;

    private Integer expirationYear;

    private String cvv;

    private String holderId;

    private String currency;

    public PaymentDetails() {
    }

    public PaymentDetails(String paymentMethodId,
                          String payerName,
                          LocalDate birthDate,
                          String cardNumber,
                          Integer expirationMonth,
                          Integer expirationYear,
                          String cvv,
                          String holderId,
                          String currency) {

        this.paymentMethodId = paymentMethodId;
        this.payerName = payerName;
        this.birthDate = birthDate;
        this.cardNumber = cardNumber;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cvv = cvv;
        this.holderId = holderId;
        this.currency = currency;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(Integer expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    public Integer getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(Integer expirationYear) {
        this.expirationYear = expirationYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}