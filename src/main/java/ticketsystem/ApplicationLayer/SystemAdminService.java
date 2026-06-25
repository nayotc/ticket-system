package ticketsystem.ApplicationLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SuspentionUserDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Suspension;
import ticketsystem.DomainLayer.user.User;

@Service
public class SystemAdminService {

    private final ISystemAdminRepository adminRepository;
    private final IPaymentService paymentService;
    private final ITicketIssuingService barcodeService;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final ISystemLogger logger;
    private final IHistoryRepository historyRepository;
    private final MembershipDomainService membershipDomain;
    private final INotifier notificationsService;
    private final ITokenService tokenService;

    public SystemAdminService(ISystemAdminRepository adminRepository,
            IPaymentService paymentService,
            ITicketIssuingService barcodeService,
            IUserRepository userRepository,
            IOrderRepository orderRepository,
            ICompanyRepository companyRepository,
            ISystemLogger logger,
            IHistoryRepository historyRepository,
            MembershipDomainService membershipDomain,
            INotifier notificationsService,
            ITokenService tokenService) {

        this.adminRepository = adminRepository;
        this.paymentService = paymentService;
        this.barcodeService = barcodeService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.companyRepository = companyRepository;
        this.logger = logger;
        this.historyRepository = historyRepository;
        this.membershipDomain = membershipDomain;
        this.notificationsService = notificationsService;
        this.tokenService = tokenService;
    }

    // Use Case: Ticket System Initialization (Boot time - No token required)
    public boolean initSystem() {
        try {
            if (adminRepository.countAdmins() == 0) {
                logger.logError("No system admins found. Please create an admin account to initialize the system.", new Exception("No system admins found."));
                return false;
            }

            if (!paymentService.handshake()) {
                logger.logError("Failed to connect to payment services. Please check your configuration.", new Exception("Failed to connect to payment services."));
                return false;
            }

            if (!barcodeService.handshake()) {
                logger.logError("Failed to connect to secure barcode services.", new Exception("Failed to connect to secure barcode services."));
                return false;
            }

            System.out.println("System initialized successfully by System Admin.");
            logger.logEvent("System initialized successfully by System Admin.", LogLevel.INFO);
            return true;

        } catch (Exception e) {
            logger.logError("Unexpected error during initialization.", e);
            return false;
        }
    }

    private long extractAdminIdSafely(String sessionToken) {        
        try {
            if (sessionToken == null || sessionToken.isBlank()) {
                throw new IllegalArgumentException("Unauthorized access. Session token is empty.");
            }
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Unauthorized access. Invalid session ID.");
            }
            return tokenService.extractUserId(sessionToken);
        } catch (Exception e) {
            logger.logEvent("Token extraction failed: " + e.getMessage(), LogLevel.WARN);
            throw new IllegalArgumentException("Unauthorized access. Invalid or expired session token.", e);
        }
    }

    private SystemAdmin validateSystemAdminAccess(String sessionToken) {
        long adminId = extractAdminIdSafely(sessionToken);
        String adminIdStr = String.valueOf(adminId);
        SystemAdmin admin = adminRepository.getAdminById(adminIdStr);

        if (!adminRepository.isSystemAdmin(adminIdStr) || admin == null || !admin.isActive()) {
            logger.logEvent("ERROR: Unauthorized access attempt. Invalid admin credentials.", LogLevel.WARN);
            throw new IllegalStateException("Unauthorized access. Invalid admin credentials.");
        }
        
        return admin;
    }

    // Use-case: Delete Member by Admin
    public String deleteMemberByAdmin(String sessionToken, long memberId) throws Exception {
        String context = "memberId=" + memberId;
        logger.logEvent("Started - deleteMemberByAdmin. " + context, LogLevel.INFO);
        
        try {
            try {
                validateSystemAdminAccess(sessionToken);
            } catch (Exception e) {
                return "ERROR: Unauthorized access. Invalid admin credentials.";
            }
            ActiveOrder activeOrder = orderRepository.getActiveOrderByUserId(memberId);
            if (activeOrder != null) {
                activeOrder.cancelOrder();
                orderRepository.updateOrder(activeOrder);
            }
            removeUserFromAllCompanies(memberId);
            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                logger.logEvent("ERROR: Member with ID " + memberId + " was not found.", LogLevel.INFO);
                return "ERROR: Member with ID " + memberId + " was not found.";
            }
            member.deactivate();
            boolean userRemoved = userRepository.updateMember(member);
            
            if (!userRemoved) {
                logger.logEvent("ERROR: Failed to delete member from the system.", LogLevel.INFO);
                return "ERROR: Failed to delete member from the system.";
            }
            notificationsService.notifyMember(memberId, "Your account has been deactivated by a system administrator. You will no longer have access to the system.");
            logger.logEvent("Completed - deleteMemberByAdmin. " + context, LogLevel.INFO);
            return "SUCCESS: Member deactivated and associated records cleaned up.";
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - deleteMemberByAdmin. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - deleteMemberByAdmin. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public void removeUserFromAllCompanies(long memberIdToDelete) throws Exception {
        String context = "memberId=" + memberIdToDelete;
        logger.logEvent("Started - removeUserFromAllCompanies. " + context, LogLevel.INFO);
        
        try {
            membershipDomain.cancelAllRolesForMember(memberIdToDelete);
            logger.logEvent("Completed - removeUserFromAllCompanies for memberId=" + memberIdToDelete, LogLevel.INFO);

        } catch (RuntimeException e) {
            logger.logError("Company role cleanup failed due to an unexpected system error, memberId=" + memberIdToDelete, e);
            throw e;

        } catch (Exception e) {
            logger.logEvent("Company role cleanup rejected, memberId=" + memberIdToDelete + ", reason=" + e.getMessage(), LogLevel.WARN);
            throw new Exception("Failed to remove user from companies: " + e.getMessage(), e);
        }
    }

    // Use Case 6.1: Close Production Company by System Admin
    public CompanyDTO closeProductionCompanyByAdmin(String sessionToken, long companyId) throws Exception {
        String context = "companyId=" + companyId;
        logger.logEvent("Started - closeProductionCompanyByAdmin. " + context, LogLevel.INFO);
        
        try {
            validateSystemAdminAccess(sessionToken);

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

            logger.logEvent("Completed - closeProductionCompanyByAdmin. companyId=" + companyId, LogLevel.INFO);
            return new CompanyDTO(company);
        
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - closeProductionCompanyByAdmin. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - closeProductionCompanyByAdmin. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    // Use Case 6.4: View Purchase History by Company and Event
    @Transactional(readOnly = true)
    public Map<Long, Map<String, List<OrderDTO>>> getPurchaseHistoryByCompanyAndEvent(String sessionToken) {
        logger.logEvent("Started - getPurchaseHistoryByCompanyAndEvent.", LogLevel.INFO);
        
        try {
            try {
                validateSystemAdminAccess(sessionToken);
            } catch (Exception e) {
                throw new SecurityException("Unauthorized access. Invalid admin credentials.", e);
            }

            List<Purchase> allPurchases = historyRepository.getAllPurchases();
            checkIfHistoryIsEmpty(allPurchases);
            if (allPurchases == null || allPurchases.isEmpty()) {
                throw new IllegalStateException("No purchases have been made yet.");
            }
            return allPurchases.stream()
                    .collect(Collectors.groupingBy(
                            Purchase::getCompanyId,
                            Collectors.groupingBy(
                                    Purchase::getEventName,
                                    Collectors.mapping(
                                            HistoryMapper::toOrderDTO,
                                            Collectors.toList()
                                    )
                            )
                    ));

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            if (!(e instanceof IllegalStateException && "No purchase history is available.".equals(e.getMessage()))) {
                logger.logEvent("Failed - getPurchaseHistoryByCompanyAndEvent. Error: " + e.getMessage(), LogLevel.WARN);
            }
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - getPurchaseHistoryByCompanyAndEvent. Unexpected error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve purchase history.", e);
        }
    }

    // Use Case 6.4: View Purchase History by Buyer
    // Use Case 6.4: View Purchase History by Buyer
    @Transactional(readOnly = true)
    public Map<Long, List<OrderDTO>> getPurchaseHistoryByBuyer(String sessionToken) {
        logger.logEvent("Started - getPurchaseHistoryByBuyer.", LogLevel.INFO);
        
        try {
            try {
                validateSystemAdminAccess(sessionToken);
            } catch (Exception e) {
                throw new SecurityException("Unauthorized access. Invalid admin credentials.", e);
            }

            List<Purchase> allOrders = historyRepository.getAllPurchases();
            if (allOrders == null || allOrders.isEmpty()) {
                throw new IllegalStateException("No purchases have been made yet.");
            }

            Map<Long, List<OrderDTO>> result = new HashMap<>();

            for (Purchase purchase : allOrders) {
                Long buyerMemberId = purchase.getMemberId();
                OrderDTO orderDTO = HistoryMapper.toOrderDTO(purchase);
                result.computeIfAbsent(buyerMemberId, k -> new ArrayList<>()).add(orderDTO);
            }

            return result;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            if (!(e instanceof IllegalStateException && "No purchase history is available.".equals(e.getMessage()))) {
                logger.logEvent("Failed - getPurchaseHistoryByBuyer. Error: " + e.getMessage(), LogLevel.WARN);
            }
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - getPurchaseHistoryByBuyer. Unexpected error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve purchase history.", e);
        }
    }

    private void checkIfHistoryIsEmpty(List<Purchase> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalStateException("No purchase history is available.");
        }
    }

    public List<String> viewEventLogs(String sessionToken) throws Exception {
        logger.logEvent("Started - viewEventLogs.", LogLevel.INFO);
        
        try {
            validateSystemAdminAccess(sessionToken);
            logger.logEvent("Completed - viewEventLogs.", LogLevel.INFO);
            return readLastNLines(Paths.get("logs/events.log"), 100);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - viewEventLogs. Error: " + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - viewEventLogs. Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public List<String> viewErrorLogs(String sessionToken) throws Exception {
        logger.logEvent("Started - viewErrorLogs.", LogLevel.INFO);
        
        try {
            validateSystemAdminAccess(sessionToken);
            logger.logEvent("Completed - viewErrorLogs.", LogLevel.INFO);
            return readLastNLines(Paths.get("logs/errors.log"), 100);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - viewErrorLogs. Error: " + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - viewErrorLogs. Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    private List<String> readLastNLines(Path path, int maxLines) throws Exception {
        if (!Files.exists(path)) {
            return List.of("No logs recorded yet.");
        }
        try (Stream<String> lines = Files.lines(path)) {
            List<String> allLines = lines.collect(Collectors.toList());
            int start = Math.max(0, allLines.size() - maxLines);
            return allLines.subList(start, allLines.size());
        }
    }

    // Use Case 6.7: Suspend Member by Admin
    public boolean suspendMemberByAdmin(String sessionToken, long memberId, LocalDateTime startDate, LocalDateTime endDate, String reason) {
        String context = "targetMemberId=" + memberId + ", startDate=" + startDate + ", endDate=" + endDate + ", reason=" + reason;
        logger.logEvent("Started - suspendMemberByAdmin. " + context, LogLevel.INFO);

        try {
            SystemAdmin admin = validateSystemAdminAccess(sessionToken);

            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                logger.logEvent("Member with ID " + memberId + " was not found.", LogLevel.INFO);
                throw new IllegalArgumentException("Member with ID " + memberId + " was not found.");
            }

            member.suspendMember(Long.parseLong(admin.getAdminId()), startDate, endDate, reason);
            if (notificationsService != null) {
                notificationsService.notifyMember(memberId, "Your account has been suspended from " + startDate + " to " + endDate + " for the following reason: " + reason);
            }

            boolean updated = userRepository.updateMember(member);
            logger.logEvent("Completed - suspendMemberByAdmin. " + context + ", success=" + updated, LogLevel.INFO);
            return updated;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - suspendMemberByAdmin. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - suspendMemberByAdmin. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw new RuntimeException("Unexpected error during member suspension.", e);
        }
    }

    // Use Case 6.8: Revoke Suspension of Member by Admin
    public boolean revokeMemberByAdmin(String sessionToken, long memberId) {
        String context = "targetMemberId=" + memberId;
        logger.logEvent("Started - revokeMemberByAdmin. " + context, LogLevel.INFO);

        try {
            validateSystemAdminAccess(sessionToken);

            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                logger.logEvent("Member with ID " + memberId + " was not found.", LogLevel.INFO);
                throw new IllegalArgumentException("Member with ID " + memberId + " was not found.");
            }

            member.revokeSuspension();
            if (notificationsService != null) {
                notificationsService.notifyMember(memberId, "Your account suspension has been revoked. You now have access to your account.");
            }

            boolean updated = userRepository.updateMember(member);
            logger.logEvent("Completed - revokeMemberByAdmin. " + context + ", success=" + updated, LogLevel.INFO);
            return updated;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - revokeMemberByAdmin. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - revokeMemberByAdmin. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw new RuntimeException("Unexpected error revoking suspension.", e);
        }
    }

    // Use Case 6.9: View Suspended Members by Admin
    public List<SuspentionUserDTO> viewSuspendedMembersByAdmin(String sessionToken) {
        logger.logEvent("Started - viewSuspendedMembersByAdmin.", LogLevel.INFO);

        try {
            validateSystemAdminAccess(sessionToken);

            List<SuspentionUserDTO> suspendedUsersDTOs = userRepository.findSuspendedMembers().stream()
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

            logger.logEvent("Completed - viewSuspendedMembersByAdmin. Count=" + suspendedUsersDTOs.size(), LogLevel.INFO);
            return suspendedUsersDTOs;

        } catch (IllegalArgumentException | IllegalStateException e) {
            if (!(e instanceof IllegalStateException && "No suspended members found.".equals(e.getMessage()))) {
                logger.logEvent("Failed - viewSuspendedMembersByAdmin. Error: " + e.getMessage(), LogLevel.WARN);
            }
            throw e;

        } catch (Exception e) {
            logger.logError("Failed - viewSuspendedMembersByAdmin. Unexpected error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve suspended members.", e);
        }
    }

    @Transactional
    public void promoteMemberToSystemAdmin(long memberId) throws Exception {
        try {
            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                throw new Exception("Member with ID " + memberId + " not found for promotion to System Admin.");
            }

            String adminId = String.valueOf(memberId);
            SystemAdmin systemAdmin = adminRepository.findById(adminId).orElse(null);

            if (systemAdmin != null) {
                if (systemAdmin.isActive()) {
                    throw new IllegalStateException("Member with ID " + memberId + " is already an active System Admin.");
                }
                systemAdmin.activate();
            } else {
                systemAdmin = new SystemAdmin(adminId, member.getUserName(), true);
            }

            adminRepository.addAdmin(systemAdmin);

            if (notificationsService != null) {
                notificationsService.notifyMember(memberId, "Congratulations! You have been promoted to System Admin.");
            }

            logger.logEvent("Member with ID " + memberId + " promoted to System Admin successfully.", LogLevel.INFO);

        } catch (Exception e) {
            logger.logError("Failed to promote member with ID " + memberId + " to System Admin: " + e.getMessage(), e);
            throw new Exception("Failed to promote member to System Admin: " + e.getMessage(), e);
        }
    }

    public boolean isSystemAdmin(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }

        try {
            validateSystemAdminAccess(sessionToken);
            return true;
        } catch (Exception e) {
            logger.logEvent("Failed to verify admin status: Invalid token or unauthorized", LogLevel.WARN);
            return false;
        }
    }

    public long getCurrentAdminId(String sessionToken) {
        try {
            return extractAdminIdSafely(sessionToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Session token is invalid or expired.", e);
        }
    }
}