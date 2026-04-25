package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.SecureBarcodeProxy;
import ticketsystem.InfrastructureLayer.SystemAdminRepository;

public class SystemInitializationTest {

    private SystemAdminService systemAdminService;
    private SystemAdminRepository realAdminRepo;

    @BeforeEach
    public void setUp() {
        realAdminRepo = new SystemAdminRepository();
        PaymentServiceProxy paymentProxy = new PaymentServiceProxy();
        SecureBarcodeProxy barcodeProxy = new SecureBarcodeProxy();

        systemAdminService = new SystemAdminService(realAdminRepo, paymentProxy, barcodeProxy);
    }

    @Test
    public void givenSystemAdminExists_whenInitSystem_thenSystemInitializesWithProxies() {
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);

        boolean result = systemAdminService.initSystem();
        assertTrue(result, "Acceptance Test Failed: System should initialize successfully using infrastructure proxies.");
        assertEquals(1, realAdminRepo.countAdmins());
    }

    @Test
    public void givenEmptyRepository_whenInitSystem_thenInitializationFails() {

        boolean result = systemAdminService.initSystem();
        assertFalse(result, "Acceptance Test Failed: System allowed initialization without a System Admin.");
    }
}
