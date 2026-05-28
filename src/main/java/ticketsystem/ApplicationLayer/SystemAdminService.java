package ticketsystem.ApplicationLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import com.fasterxml.jackson.databind.ObjectMapper;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SuspentionUserDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import java.util.Map;
import java.util.Set;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Suspension;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.MembershipDomainService;

public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode barcodeService;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final ISystemLogger logger;
    private final IHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;
    private final MembershipDomainService membershipDomain;
    private final INotifier notificationsService;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ISecureBarcode barcodeService,
            IUserRepository userRepository,
            IOrderRepository orderRepository,
            ITokenService tokenService,
            ICompanyRepository companyRepository,
            ISystemLogger logger, IHistoryRepository historyRepository,
            MembershipDomainService membershipDomain, INotifier notificationsService) {

        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.companyRepository = companyRepository;
        this.logger = logger;
        this.historyRepository = historyRepository;
        this.membershipDomain = membershipDomain;
        this.objectMapper = new ObjectMapper();
        this.notificationsService=notificationsService;
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
            membershipDomain.cancelAllRolesForMember(memberIdToDelete);

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

   // Use Case 6.1: Close Production Company by System Admin
public CompanyDTO closeProductionCompanyByAdmin(long adminId, long companyId) throws Exception {
    SystemAdmin admin = adminRepository.getAdminById("" + adminId);

    if (!adminRepository.isSystemAdmin("" + adminId) || admin == null || !admin.isActive()) {
        logger.logEvent("Unauthorized access. Invalid admin credentials.",
                LogbackSystemLogger.LogLevel.WARN);
        throw new Exception("Unauthorized access. Invalid admin credentials.");
    }

    Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new Exception("Error: Company not found."));

    Set<Long> staffMemberIds = membershipDomain.getManagementSubTreeMemberIds(
            company.getFounderId(),
            company.getId()
    );

    membershipDomain.cancelAllRolesForCompany(companyId);

    company.closeBySystemAdmin();

    companyRepository.save(company);

    if (notificationsService != null) {
        notificationsService.notifyMembers(
                staffMemberIds,
                "The production company \"" + company.getName()
                        + "\" was closed by a system administrator, and your role in this company was removed."
        );
    }

    logger.logEvent("Production company closed by System Admin. companyId=" + companyId
                    + ", adminId=" + adminId,
            LogbackSystemLogger.LogLevel.INFO);

    return new CompanyDTO(company);
}

    // Use Case: View Purchase History by Company and Event 6.4
    public Map<Long, Map<String, List<OrderDTO>>> getPurchaseHistoryByCompanyAndEvent(long adminId) {
            try {
                SystemAdmin admin = adminRepository.getAdminById("" + adminId);
                if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                    throw new SecurityException("ERROR: Unauthorized access. Invalid admin credentials.");
                }
                List<Purchase> allPurchases = historyRepository.getAllPurchases();
                checkIfHistoryIsEmpty(allPurchases);
                                if(allPurchases == null || allPurchases.isEmpty()){
                    throw new IllegalStateException("No purchases have been made yet.");
                }
                return allPurchases.stream()
                    .collect(Collectors.groupingBy(
                        Purchase::getCompanyId, 
                        Collectors.groupingBy(
                            Purchase::getEventName, 
                            Collectors.mapping(    
                                purchase -> objectMapper.convertValue(purchase, OrderDTO.class),
                                Collectors.toList()
                            )
                        ) 
                    ));
            } catch (Exception e) {
                if (!(e instanceof IllegalStateException && "No purchase history is available.".equals(e.getMessage()))) {
                    logger.logEvent("ERROR: An unexpected error occurred while retrieving purchase history: " + e.getMessage(), LogbackSystemLogger.LogLevel.WARN);
                }
                throw e; 
            } 
        }

    // Use Case: View Purchase History by Buyer 6.4
    public Map<Long, List<OrderDTO>> getPurchaseHistoryByBuyer(long adminId) {
        try {
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (!adminRepository.isSystemAdmin("" + adminId) || admin == null || !admin.isActive()) {
                throw new SecurityException("ERROR: Unauthorized access. Invalid admin credentials.");
            }

            List<Purchase> allOrders = historyRepository.getAllPurchases();
            if (allOrders == null || allOrders.isEmpty()) {
                throw new IllegalStateException("No purchases have been made yet.");
            }

            Map<Long, List<OrderDTO>> result = new HashMap<>();

            for (Purchase purchase : allOrders) {
                Long buyerMemberId = purchase.getMemberId();
                OrderDTO orderDTO = objectMapper.convertValue(purchase, OrderDTO.class);

                result.computeIfAbsent(buyerMemberId, k -> new ArrayList<>())
                        .add(orderDTO);
            }

            return result;

        } catch (Exception e) {
            if (!(e instanceof IllegalStateException && "No purchase history is available.".equals(e.getMessage()))) {
                logger.logEvent("ERROR: An unexpected error occurred while retrieving purchase history: " + e.getMessage(),
                        LogbackSystemLogger.LogLevel.WARN);
            }
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

    //uc 6.7 - Suspend Member by Admin
    public boolean suspendMemberByAdmin(long adminId, long memberId, LocalDateTime startDate,
                      LocalDateTime endDate, String reason ) {
        try{
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                logger.logEvent("Unauthorized access. Invalid admin credentials.",
                        LogbackSystemLogger.LogLevel.WARN);
                        throw new IllegalArgumentException("Unauthorized access. Invalid admin credentials.");
            }
            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                logger.logEvent("Member with ID " + memberId + " was not found.",
                        LogbackSystemLogger.LogLevel.INFO);
                        throw new IllegalArgumentException("Member with ID " + memberId + " was not found.");
            }
            member.suspendMember(adminId, startDate, endDate, reason);
            return userRepository.updateMember(member);
        }
        catch(Exception e){
            logger.logEvent("An unexpected error occurred while suspending the member: " + e.getMessage(),
                    LogbackSystemLogger.LogLevel.INFO);
            throw e;
        }
    }

    //uc 6.8 - Revoke Suspension of Member by Admin
    public boolean revokeMemberByAdmin(long adminId, long memberId) {
        try {
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                logger.logEvent("Unauthorized access. Invalid admin credentials.",
                        LogbackSystemLogger.LogLevel.WARN);
                throw new IllegalArgumentException("Unauthorized access. Invalid admin credentials.");
            }
            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                logger.logEvent("Member with ID " + memberId + " was not found.",
                        LogbackSystemLogger.LogLevel.INFO);
                throw new IllegalArgumentException("Member with ID " + memberId + " was not found.");
            }
            member.revokeSuspension();
            return userRepository.updateMember(member);
        } catch (Exception e) {
            logger.logEvent("An unexpected error occurred while revoking the member: " + e.getMessage(),
                    LogbackSystemLogger.LogLevel.INFO);
            throw e;
        }
    }

    //uc 6.9 - View Suspended Members by Admin
    public List<SuspentionUserDTO> viewSuspendedMembersByAdmin(long adminId) {
        try {
            SystemAdmin admin = adminRepository.getAdminById("" + adminId);
            if (adminRepository.isSystemAdmin("" + adminId) == false || admin == null || !admin.isActive()) {
                logger.logEvent("Unauthorized access. Invalid admin credentials.",
                        LogbackSystemLogger.LogLevel.WARN);
                throw new IllegalArgumentException("Unauthorized access. Invalid admin credentials.");
            }
            List<SuspentionUserDTO> suspendedUsersDTOs = userRepository.getAllMembers().stream()
                    .filter(Member::isSuspended)
                    .map(member -> {
                        Suspension suspension = member.getSuspension();
                        LocalDateTime start = suspension.getStartDate();
                        LocalDateTime end = suspension.getEndDate();
                        Long durationInDays = null;
                        if (end != null && start != null) {
                            durationInDays = ChronoUnit.DAYS.between(start, end);
                        }
                        
                        return new SuspentionUserDTO(
                                member.getId(),
                                suspension.getReason(),
                                start,
                                end,
                                durationInDays
                        );
                    })
                    .collect(Collectors.toList());
            if (suspendedUsersDTOs.isEmpty()) {
                throw new IllegalStateException("No suspended members found.");
            }
            return suspendedUsersDTOs;
        } catch (Exception e) {
            logger.logEvent("An unexpected error occurred while viewing suspended members: " + e.getMessage(),
                    LogbackSystemLogger.LogLevel.INFO);
            throw e;
        }
    }
}
