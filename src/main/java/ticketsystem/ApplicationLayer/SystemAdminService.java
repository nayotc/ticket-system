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
                System.out.println("No system admins found. Please create an admin account to initialize the system.");
                return false;
            }

            boolean paymentConnected = paymentService.connect();
            if (!paymentConnected) {
                // Alternative Flow: Connection failure
                System.out.println("Failed to connect to payment services. Please check your configuration.");
                return false;
            }

            boolean barcodeConnected = barcodeService.connect();
            if (!barcodeConnected) {
                // Alternative Flow: Connection failure
                System.out.println("Failed to connect to secure barcode services.");
                return false;
            }

            System.out.println("System initialized successfully by System Admin.");
            return true;

        } catch (Exception e) {
            System.out.println("Unexpected error during initialization: " + e.getMessage());
            return false;
        }
    }

}
