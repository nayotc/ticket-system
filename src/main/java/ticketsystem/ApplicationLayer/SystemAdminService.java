package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private final ITokenService tokenService;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final ISystemLogger logger;
    private final CompanyService companyService;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService,
            IUserRepository userRepository,
            IOrderRepository orderRepository,
            ITokenService tokenService,
            ICompanyRepository companyRepository,
            ISystemLogger logger) {

        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
        this.companyRepository = companyRepository;
        this.logger = logger;
        companyService = new CompanyService(companyRepository, tokenService);
    }

//Use Case: Ticket System Initialization
    public boolean initSystem() {
        try {
            if (adminRepository.countAdmins() == 0) {
                logger.logError("No system admins found. Please create an admin account to initialize the system.", new Exception("No system admins found."));
                return false;
            }

            boolean paymentConnected = paymentService.connect();
            if (!paymentConnected) {
                // Alternative Flow: Connection failure
                logger.logError("Failed to connect to payment services. Please check your configuration.", new Exception("Failed to connect to payment services."));
                return false;
            }

            boolean barcodeConnected = barcodeService.connect();
            if (!barcodeConnected) {
                // Alternative Flow: Connection failure
                logger.logError("Failed to connect to secure barcode services.", new Exception("Failed to connect to secure barcode services."));
                return false;
            }

            System.out.println("System initialized successfully by System Admin.");
            logger.logEvent("System initialized successfully by System Admin.", LogbackSystemLogger.LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logError("Unexpected error during initialization.", e);
            return false;
        }
    }

// Use-case: Delete Member by Admin
    public String deleteMemberByAdmin(long adminId, long memberId) {
        SystemAdmin admin = adminRepository.getAdminById("" + adminId);
        if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
            logger.logEvent("ERROR: Unauthorized access. Invalid admin credentials.", LogbackSystemLogger.LogLevel.WARN);
            return "ERROR: Unauthorized access. Invalid admin credentials.";
        }
        User member = userRepository.getMemberById(memberId);
        if (member == null) {
            logger.logEvent("ERROR: Member with ID " + memberId + " was not found.", LogbackSystemLogger.LogLevel.INFO);
            return "ERROR: Member with ID " + memberId + " was not found.";
        }
        try {
            orderRepository.deleteActiveOrdersByUserId(memberId);
            companyService.removeUserFromAllCompanies(memberId);
            boolean userRemoved = userRepository.removeRegisteredMember(memberId);
            if (!userRemoved) {
                logger.logEvent("ERROR: Failed to remove member from the system.", LogbackSystemLogger.LogLevel.INFO);
                return "ERROR: Failed to remove member from the system.";
            }
            return "SUCCESS: Member deactivated and associated records cleaned up.";

        } catch (Exception e) {
            logger.logEvent("ERROR: An unexpected error occurred while deleting the member: " + e.getMessage(), LogbackSystemLogger.LogLevel.INFO);
            return "ERROR: An unexpected error occurred while deleting the member: " + e.getMessage();
        }
    }

    // Use Case: Close Production Company by System Admin
    public CompanyDTO closeProductionCompanyByAdmin(long adminId, long companyId) throws Exception {
        SystemAdmin admin = adminRepository.getAdminById("" + adminId);
        if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
            logger.logError("Unauthorized access. Invalid admin credentials.", new Exception("Unauthorized access. Invalid admin credentials."));
            throw new Exception("Unauthorized access. Invalid admin credentials.");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception("Error: Company not found."));
        company.closeBySystemAdmin(adminId);
        logger.logEvent("Production company closed by System Admin.", LogbackSystemLogger.LogLevel.INFO);
        companyRepository.save(company);
        return new CompanyDTO(company);
    }

}
