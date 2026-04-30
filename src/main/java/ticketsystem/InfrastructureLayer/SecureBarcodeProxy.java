package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.ISecureBarcode;

public class SecureBarcodeProxy implements ISecureBarcode {

    public static boolean isConnectionSuccessful = true; // for testing purposes

    @Override
    public boolean connect() {
        return isConnectionSuccessful;
    }
}
