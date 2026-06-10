package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.DTO.TicketIssueRequest;

@Component
public class SecureBarcodeProxy implements ITicketIssuingService {

    public static boolean isConnectionSuccessful = true; // for testing purposes

    @Override
    public boolean handshake() {
        return isConnectionSuccessful;
    }


	@Override
	public String issueTicket(TicketIssueRequest request) {
		return "FAKE "+ request.getCustomerId() + "_" + request.getEventId();
	}

	@Override
	public boolean cancelTicket(String ticketId) {
		return true;
	}

    
}
