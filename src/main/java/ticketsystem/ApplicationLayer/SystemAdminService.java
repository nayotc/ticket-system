package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.User;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private final ITokenService tokenService;
    private final CompanyService companyService;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService,
            IUserRepository userRepository,
            CompanyService companyService,
            IOrderRepository orderRepository, ITokenService tokenService) {
        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;

    }

//Use Case: Ticket System Initialization
    public boolean initSystem() {
        try {
            if (adminRepository.countAdmins() == 0) {
                //System.out.println("No system admins found. Please create an admin account to initialize the system.");
                return false;
            }

            boolean paymentConnected = paymentService.connect();
            if (!paymentConnected) {
                // Alternative Flow: Connection failure
                // System.out.println("Failed to connect to payment services. Please check your configuration.");
                return false;
            }

            boolean barcodeConnected = barcodeService.connect();
            if (!barcodeConnected) {
                // Alternative Flow: Connection failure
                //System.out.println("Failed to connect to secure barcode services.");
                return false;
            }

            System.out.println("System initialized successfully by System Admin.");
            return true;

        } catch (Exception e) {
            //System.out.println("Unexpected error during initialization: " + e.getMessage());
            return false;
        }
    }

    // Use-case: Delete Member by Admin
    public String deleteMemberByAdmin(String sessionId, long memberId) {

        if (!tokenService.validateToken(sessionId)) {
            return "ERROR: Unauthorized access. Admin is not logged in or lacks permissions.";
        }
        try {
            Long adminId = getSystemAdminId(sessionId);
            if (adminId == null || adminId <= 0) {
                return "ERROR: Unauthorized access. Admin is not logged in or lacks permissions.";
            }
        } catch (Exception e) {
            return "ERROR: Unauthorized access. Admin is not logged in or lacks permissions.";
        }
        User member = userRepository.getMemberById(memberId);
        if (member == null) {
            return "ERROR: Member with ID " + memberId + " was not found.";
        }
        try {
            orderRepository.deleteActiveOrdersByUserId(memberId);
            companyService.removeUserFromAllCompanies(memberId);
            boolean userRemoved = userRepository.removeRegisteredMember(memberId);
            if (!userRemoved) {
                return "ERROR: Failed to remove member from the system.";
            }
            return "SUCCESS: Member deactivated and associated records cleaned up.";

        } catch (Exception e) {
            return "ERROR: An unexpected error occurred while deleting the member: " + e.getMessage();
        }
    }

    // Use Case: Close Production Company by System Admin
    public CompanyDTO closeProductionCompanyByAdmin(String sessionId, long companyId) throws Exception {
        long adminId = getSystemAdminId(sessionId);
        //return companyService.closeProductionCompanyBySystemAdmin(companyId, adminId);
        return null;
    }

    private long getSystemAdminId(String sessionId) throws Exception {
        if (!tokenService.validateToken(sessionId)) {
            throw new Exception("Invalid or expired session token.");
        }

        if (tokenService.isGuestToken(sessionId)) {
            throw new Exception("Only logged-in members can perform system admin actions.");
        }

        Long adminId = tokenService.extractUserId(sessionId);

        if (adminId == null) {
            throw new Exception("Member ID was not found in the session token.");
        }

        if (!adminRepository.isSystemAdmin(String.valueOf(adminId))) {
            throw new Exception("Member is not an active system admin.");
        }

        return adminId;
    }
}
