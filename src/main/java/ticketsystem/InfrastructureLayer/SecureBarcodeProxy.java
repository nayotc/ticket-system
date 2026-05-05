package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.ISecureBarcode;

public class SecureBarcodeProxy implements ISecureBarcode {

    public static boolean isConnectionSuccessful = true; // for testing purposes

    @Override
    public boolean connect() {
        return isConnectionSuccessful;
    }

    @Override
    public String generate(int ticketId, int eventId, int orderId) {
        System.out.println("Secure Barcode Proxy: Generating secure barcode for Ticket ID: " + ticketId + ", Event ID: " + eventId + ", Order ID: " + orderId);
        // Simulate barcode generation logic here
        return "SECURE_BARCODE_" + ticketId + "_" + eventId + "_" + orderId;
    }

    @Override
    public String generate(int ticketId, int eventId, int orderId) {
        System.out.println("Secure Barcode Proxy: Generating secure barcode for Ticket ID: " + ticketId + ", Event ID: " + eventId + ", Order ID: " + orderId);
        // Simulate barcode generation logic here
        return "SECURE_BARCODE_" + ticketId + "_" + eventId + "_" + orderId;
    }
}
