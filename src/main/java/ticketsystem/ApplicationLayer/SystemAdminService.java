package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService) {
        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
    }

//Use Case: Ticket System Initialization
    public boolean initSystem() {
        try {
            if (adminRepository.countAdmins() == 0) {
                logError("Initialization failed: No System Admin defined.");
                return false;
            }

            boolean paymentConnected = paymentService.connect();
            if (!paymentConnected) {
                // Alternative Flow: Connection failure
                logError("Failed to connect to payment services.");
                return false;
            }

            boolean barcodeConnected = barcodeService.connect();
            if (!barcodeConnected) {
                // Alternative Flow: Connection failure
                logError("Failed to connect to secure barcode services.");
                return false;
            }

            logEvent("System initialized successfully by System Admin.");
            return true;

        } catch (Exception e) {
            logError("Unexpected error during initialization: " + e.getMessage());
            return false;
        }
    }

    // לוג
    private void logEvent(String msg) {
        /* ... */ }

    private void logError(String msg) {
        /* ... */ }
}
