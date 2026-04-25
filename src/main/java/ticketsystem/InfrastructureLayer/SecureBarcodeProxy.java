package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.ISecureBarcode;

public class SecureBarcodeProxy implements ISecureBarcode {

    @Override
    public boolean connect() {
        System.out.println("Secure Barcode Proxy Successfully connected to external Secure Barcode Service.");
        return true;
    }
}
