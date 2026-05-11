package ticketsystem.ApplicationLayer;

public interface ISecureBarcode {

    boolean connect();
    String generate(int ticketId, int eventId, int orderId);
}
