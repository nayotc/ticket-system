package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.TicketIssueRequest;

public interface ITicketIssuingService {

    boolean handshake();

    String issueTicket(TicketIssueRequest request);

    boolean cancelTicket(String ticketId);
}