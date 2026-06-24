package ticketsystem.InfrastructureLayer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.PaymentDetails;

import java.math.BigDecimal;

@Service
@Primary
public class ExternalPaymentService implements IPaymentService {

    private final RestTemplate restTemplate;

    public ExternalPaymentService() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);

        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${external.system.url}")
    private String externalSystemUrl;

    @Override
    public boolean handshake() {
        try {
           System.out.println(externalSystemUrl+"!!!");
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("action_type", "handshake");

            String response = postForm(body);
            
            return response != null
                    && response.trim().equalsIgnoreCase("OK");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
public Integer pay(BigDecimal amount, PaymentDetails details) {
    try {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("action_type", "pay");
        body.add("amount", amount.toPlainString());
        body.add("currency", details.getCurrency());
        body.add("card_number", details.getCardNumber());
        body.add("month", String.valueOf(details.getExpirationMonth()));
        body.add("year", String.valueOf(details.getExpirationYear()));
        body.add("holder", details.getPayerName());
        body.add("cvv", details.getCvv());
        body.add("id", details.getHolderId());

        String response = postForm(body);

        if (response == null || response.isBlank()) {
            return -1;
        }

        int transactionId = Integer.parseInt(response.trim());

        if (transactionId == -1) {
            return -1;
        }

        if (transactionId < 10000 || transactionId > 100000) {
            return -1;
        }

        return transactionId;

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

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            body.add("action_type", "refund");
            body.add("transaction_id", String.valueOf(transactionId));

            String response = postForm(body);

            return response != null && response.trim().equals("1");

        } catch (Exception e) {
            return false;
        }
    }

    private String postForm(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                externalSystemUrl,
                request,
                String.class
        );
    }
}