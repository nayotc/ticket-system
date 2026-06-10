package ticketsystem.InfrastructureLayer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.PaymentDetails;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalPaymentService implements IPaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    //("${external.system.url:https://damp-lynna-wsep-1984852e.koyeb.app/}") 
    @Value("${external.system.url}")

    private String externalSystemUrl;

    @Override
    public boolean handshake() {
        try {
            Map<String, String> request = Map.of(
                    "action_type", "handshake"
            );

            String response = restTemplate.postForObject(
                    externalSystemUrl,
                    request,
                    String.class
            );

            return response != null
                    && response.trim().equalsIgnoreCase("OK");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
public Integer pay(BigDecimal amount, PaymentDetails details) {
    try {
        Map<String, String> request = new HashMap<>();

        request.put("action_type", "pay");
        request.put("amount", amount.toPlainString());
        request.put("currency", details.getCurrency());
        request.put("card_number", details.getCardNumber());
        request.put("month", String.valueOf(details.getExpirationMonth()));
        request.put("year", String.valueOf(details.getExpirationYear()));
        request.put("holder", details.getPayerName());
        request.put("cvv", details.getCvv());
        request.put("id", details.getHolderId());

        String response = restTemplate.postForObject(
                externalSystemUrl,
                request,
                String.class
        );

        if (response == null) {
            return -1;
        }

        return Integer.parseInt(response.trim());

    } catch (Exception e) {
        return -1;
    }
}

    @Override
    public boolean refund(Integer transactionId) {
        try {
            if (transactionId == null) {
                return false;
            }

            Map<String, String> request = Map.of(
                    "action_type", "refund",
                    "transaction_id", String.valueOf(transactionId)
            );

            String response = restTemplate.postForObject(
                    externalSystemUrl,
                    request,
                    String.class
            );

            return response != null && response.trim().equals("1");

        } catch (Exception e) {
            return false;
        }
    }
}