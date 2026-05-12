package ticketsystem.ApplicationLayer;

public interface ISecureBarcode {

    boolean connect();
    String generateSecureBarcode(Long ticketId, Long eventId, Long orderId);
    
}
