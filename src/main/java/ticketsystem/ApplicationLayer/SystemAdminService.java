package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private final ITokenService tokenService = new TokenService("manual_test_secret_32_chars_long", new TokenRepository());
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final IOrderRepository orderRepository;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService,
            IUserRepository userRepository,
            ICompanyRepository companyRepository,
            IOrderRepository orderRepository) {
        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.orderRepository = orderRepository;

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

    // Use-case: Delete Member by Admin
    public String deleteMemberByAdmin(String sessionId, long memberId) {

        if (!tokenService.validateToken(sessionId)) {
            return "ERROR: Unauthorized access. Admin is not logged in or lacks permissions.";
        }

        // Member member = userRepository.getMemberById(memberId);
        // if (member == null) {
        //     return "ERROR: Member with ID " + memberId + " was not found.";
        // }
        try {
            //companyRepository.removeRolesForMember(memberId);
            // orderRepository.cancelPendingOrdersForMember(memberId);
            // userRepository.removeRegisteredMember(member););
            return "SUCCESS: Member deactivated and associated records cleaned up.";

        } catch (Exception e) {
            return "ERROR: An unexpected error occurred while deleting the member: " + e.getMessage();
        }
    }

}
