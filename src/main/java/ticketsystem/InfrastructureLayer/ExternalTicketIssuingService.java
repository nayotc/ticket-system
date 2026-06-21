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

import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.DTO.TicketIssueRequest;

@Service
@Primary
public class ExternalTicketIssuingService implements ITicketIssuingService {

    private final RestTemplate restTemplate;

        public ExternalTicketIssuingService() {
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
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("action_type", "handshake");

            String response = postForm(body);

            return response != null && response.trim().equalsIgnoreCase("OK");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String issueTicket(TicketIssueRequest requestDto) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            body.add("action_type", "issue_ticket");
            body.add("customer_id", requestDto.getCustomerId());
            body.add("event_id", requestDto.getEventId());
            body.add("zone", requestDto.getZoneType());

            if (requestDto.isSeating()) {
                body.add("is_seating", "true");
                body.add("seats", requestDto.getSeatsJson());
            } else {
                body.add("quantity", String.valueOf(requestDto.getQuantity()));
            }

            String response = postForm(body);

            if (response == null || response.trim().equals("-1")) {
                return "-1";
            }

            return response.trim();

        } catch (Exception e) {
            return "-1";
        }
    }

    @Override
    public boolean cancelTicket(String ticketId) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            body.add("action_type", "cancel_ticket");
            body.add("ticket_id", ticketId);

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