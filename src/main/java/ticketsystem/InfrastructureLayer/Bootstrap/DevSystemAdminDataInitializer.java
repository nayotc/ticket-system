package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@Order(30)
public class DevSystemAdminDataInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin@tixnow.com";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_FULLNAME = "System Administrator";
    private static final String ADMIN_PHONE = "0509999999";
    private static final LocalDate ADMIN_BIRTH_DATE = LocalDate.of(2000, 6, 1);

    private static final LocalDate SUSPENDED_BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private static final String SUSPENDED_USERNAME = "banned@test.com";

    private final UserService userService;
    private final IUserRepository userRepository;
    private final ISystemAdminRepository systemAdminRepository;

    public DevSystemAdminDataInitializer(UserService userService, 
                                         IUserRepository userRepository, 
                                         ISystemAdminRepository systemAdminRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.systemAdminRepository = systemAdminRepository;
    }

    @Override
    public void run(String... args) {
        long adminId = createSystemAdmin();
        if (adminId != -1) {
            createSuspendedUserDemo(adminId);
        }
    }

    private long createSystemAdmin() {
        if (userRepository.isUsernameTaken(ADMIN_USERNAME)) {
            System.out.println("Dev System Admin already exists: " + ADMIN_USERNAME);
            Member existingAdmin = userRepository.getMemberByUsername(ADMIN_USERNAME);
            return existingAdmin != null ? existingAdmin.getId() : -1;
        }

        System.out.println("Generating test System Admin account...");

        // 1. יצירת חשבון משתמש רגיל כדי שהמנהל יוכל לבצע Login לקבלת טוקן
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, ADMIN_USERNAME, ADMIN_PASSWORD, ADMIN_FULLNAME, ADMIN_PHONE, ADMIN_BIRTH_DATE);

        Member adminMember = userRepository.getMemberByUsername(ADMIN_USERNAME);
        if (adminMember == null) {
            System.out.println("Failed to fetch the newly created admin member.");
            return -1;
        }

        long adminId = adminMember.getId();

        // 2. רישום המשתמש כמנהל מערכת ב-Repository של ה-Admins
        if (!systemAdminRepository.isSystemAdmin(String.valueOf(adminId))) {
            SystemAdmin systemAdmin = new SystemAdmin(String.valueOf(adminId), ADMIN_USERNAME, true);
            
            // הערה: אם קראת לפונקציית השמירה ב-ISystemAdminRepository בשם אחר (כמו addAdmin או insert),
            // תשני את המילה 'save' בהתאם למה שמוגדר אצלך בממשק.
            systemAdminRepository.addAdmin(systemAdmin);
        }

        System.out.println("=========================================================================");
        System.out.println("SYSTEM ADMIN ACCOUNT GENERATED SUCCESSFULLY");
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Username: " + ADMIN_USERNAME);
        System.out.println("Password: " + ADMIN_PASSWORD);
        System.out.println("Role: System Administrator [ID: " + adminId + "]");
        System.out.println("=========================================================================");
        
        return adminId;
    }

    private void createSuspendedUserDemo(long adminId) {
        if (userRepository.isUsernameTaken(SUSPENDED_USERNAME)) {
            return;
        }

        System.out.println("Generating a suspended user for dashboard UI testing...");

        // יצירת המשתמש
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, SUSPENDED_USERNAME, "123456", "Suspended User", "0508888888", SUSPENDED_BIRTH_DATE);
        Member suspendedMember = userRepository.getMemberByUsername(SUSPENDED_USERNAME);

        if (suspendedMember != null) {
            // הפעלת פעולת השעיה ישירות על אובייקט ה-Domain (השעיה לשבועיים)
            suspendedMember.suspendMember(
                    adminId, 
                    LocalDateTime.now().minusDays(1), 
                    LocalDateTime.now().plusDays(14), 
                    "הפרת תנאי השימוש של המערכת בבדיקות דמה."
            );
            
            // עדכון המשתמש במסד הנתונים
            userRepository.updateMember(suspendedMember);
            System.out.println("Suspended user created: " + SUSPENDED_USERNAME);
        }
    }
}