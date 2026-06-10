package ticketsystem.InfrastructureLayer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.DTO.TicketIssueRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalTicketIssuingService implements ITicketIssuingService {

    private final RestTemplate restTemplate = new RestTemplate();
    //"${external.system.url:https://damp-lynna-wsep-1984852e.koyeb.app/}"
   
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

            return response != null && response.trim().equalsIgnoreCase("OK");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String issueTicket(TicketIssueRequest requestDto) {
        try {
            Map<String, String> request = new HashMap<>();

            request.put("action_type", "issue_ticket");
            request.put("customer_id", requestDto.getCustomerId());
            request.put("event_id", requestDto.getEventId());
            request.put("zone", requestDto.getZoneType().name());

            if (requestDto.isSeating()) {
                request.put("is_seating", "true");
                request.put("seats", requestDto.getSeatsJson());
            } else {
                request.put("quantity", String.valueOf(requestDto.getQuantity()));
            }

            String response = restTemplate.postForObject(
                    externalSystemUrl,
                    request,
                    String.class
            );

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
            Map<String, String> request = Map.of(
                    "action_type", "cancel_ticket",
                    "ticket_id", ticketId
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