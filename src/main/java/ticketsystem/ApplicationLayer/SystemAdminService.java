package ticketsystem.ApplicationLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import java.util.Map;

import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final ISystemLogger logger;
    private final IHistoryRepository historyRepository;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService,
            IUserRepository userRepository,
            IOrderRepository orderRepository,
            ITokenService tokenService,
            ICompanyRepository companyRepository,
            ISystemLogger logger, IHistoryRepository historyRepository) {

        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.companyRepository = companyRepository;
        this.logger = logger;
        this.historyRepository = historyRepository;
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
            removeUserFromAllCompanies(memberId);
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

    public void removeUserFromAllCompanies(long memberIdToDelete) throws Exception {
        logger.logEvent("Company role cleanup started for memberId=" + memberIdToDelete,
                LogbackSystemLogger.LogLevel.INFO);

        try {
            boolean isFounderAnywhere = companyRepository.existsByFounderId(memberIdToDelete);

            if (isFounderAnywhere) {
                throw new Exception("Cannot delete user: The user is a Founder of one or more companies.");
            }

            List<Company> relevantCompanies
                    = companyRepository.findByOwnersContainingOrManagersContaining(memberIdToDelete, memberIdToDelete);

            logger.logEvent("Company role cleanup found " + relevantCompanies.size()
                    + " relevant companies for memberId=" + memberIdToDelete,
                    LogbackSystemLogger.LogLevel.DEBUG);

            for (Company company : relevantCompanies) {
                company.removeUserFromAllRoles(memberIdToDelete);
                companyRepository.save(company);

                logger.logEvent("Member removed from company roles, memberId=" + memberIdToDelete
                        + ", companyId=" + company.getId(),
                        LogbackSystemLogger.LogLevel.INFO);
            }

            logger.logEvent("Company role cleanup completed for memberId=" + memberIdToDelete,
                    LogbackSystemLogger.LogLevel.INFO);

        } catch (RuntimeException e) {
            logger.logError("Company role cleanup failed due to an unexpected system error, memberId="
                    + memberIdToDelete, e);
            throw e;

        } catch (Exception e) {
            logger.logEvent("Company role cleanup rejected, memberId=" + memberIdToDelete
                    + ", reason=" + e.getMessage(),
                    LogbackSystemLogger.LogLevel.WARN);
            throw new Exception("Failed to remove user from companies: " + e.getMessage(), e);
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
    // Use Case: View Purchase History by Buyer 6.4
    public Map<Long, List<Purchase>> getPurchaseHistoryByBuyer(long adminId) {
        try{
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                throw new SecurityException("ERROR: Unauthorized access. Invalid admin credentials.");
            }
            List<Purchase> allOrders = historyRepository.getAllPurchases();
            checkIfHistoryIsEmpty(allOrders); 
            return allOrders.stream()
                .collect(Collectors.groupingBy(Purchase::getMemberId));
        } catch (Exception e) {
            logger.logEvent("ERROR: An unexpected error occurred while retrieving purchase history: " + e.getMessage(), LogbackSystemLogger.LogLevel.WARN);
            throw e;
        }     

    }

    // Use Case: View Purchase History by Company and Event 6.4
    public Map<Long, Map<String, List<Purchase>>> getPurchaseHistoryByCompanyAndEvent(long adminId) {
        try{
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                throw new SecurityException("ERROR: Unauthorized access. Invalid admin credentials.");
            }
            List<Purchase> allPurchases = historyRepository.getAllPurchases();
            checkIfHistoryIsEmpty(allPurchases);
            
            return allPurchases.stream()
                                .collect(Collectors.groupingBy(
                                        Purchase::getCompanyId,
                                        Collectors.groupingBy(Purchase::getEventName) 
                                ));
        }
        catch (Exception e) {
            logger.logEvent("ERROR: An unexpected error occurred while retrieving purchase history: " + e.getMessage(), LogbackSystemLogger.LogLevel.WARN);
            throw e;
        }
    }



    private void checkIfHistoryIsEmpty(List<Purchase> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalStateException("No purchase history is available.");
        }
    }

    // logs documentation of the system admin service
    public List<String> viewEventLogs(long adminId) throws Exception {
        SystemAdmin admin = adminRepository.getAdminById("" + adminId);
        if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
            logger.logError("Unauthorized access. Invalid admin credentials.", new Exception("Unauthorized access. Invalid admin credentials."));
            throw new Exception("Unauthorized access. Invalid admin credentials.");
        }
        return readLastNLines(Paths.get("logs/events.log"), 100);
    }

    public List<String> viewErrorLogs(long adminId) throws Exception {
        SystemAdmin admin = adminRepository.getAdminById("" + adminId);
        if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
            logger.logError("Unauthorized access. Invalid admin credentials.", new Exception("Unauthorized access. Invalid admin credentials."));
            throw new Exception("Unauthorized access. Invalid admin credentials.");
        }
        return readLastNLines(Paths.get("logs/errors.log"), 100);
    }

    private List<String> readLastNLines(Path path, int maxLines) throws Exception {
        try (Stream<String> lines = Files.lines(path)) {
            List<String> allLines = lines.collect(Collectors.toList());
            int start = Math.max(0, allLines.size() - maxLines);
            return allLines.subList(start, allLines.size());
        }
    }
}
