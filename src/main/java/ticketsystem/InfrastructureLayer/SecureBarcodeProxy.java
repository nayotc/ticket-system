package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Component;

import kotlin.random.Random;
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
		return "FAKE_" +
       request.getCustomerId() +
       "_" +
       Random.Default.nextInt(1000000) +
       "_" +
       request.getEventId();}

	@Override
	public boolean cancelTicket(String ticketId) {
		if(ticketId!= "-1")
			return true;
		else return false;
	}

    
}
