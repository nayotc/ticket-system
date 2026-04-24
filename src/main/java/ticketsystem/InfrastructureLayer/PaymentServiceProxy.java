package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.IPaymentService;

public class PaymentServiceProxy implements IPaymentService {

    @Override
    public boolean connect() {
        System.out.println("Payment Proxy Successfully connected to external Payment Service.");
        return true;
    }

}
