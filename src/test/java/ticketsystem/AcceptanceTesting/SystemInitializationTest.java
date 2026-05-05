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
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.wasConnectCalled = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;
        systemAdminService = new SystemAdminService(realAdminRepo, paymentProxy, barcodeProxy);
    }

    @Test
    public void givenSystemAdminExists_whenInitSystem_thenSystemInitializesWithProxies() {
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);
        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        boolean result = systemAdminService.initSystem();
        assertTrue(result, "Acceptance Test Failed: System should initialize successfully using infrastructure proxies.");
        assertEquals(1, realAdminRepo.countAdmins());
    }

    @Test
    public void givenEmptyRepository_whenInitSystem_thenInitializationFails() {
        PaymentServiceProxy.isConnectionSuccessful = true;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System allowed initialization without a System Admin.");
        assertFalse(PaymentServiceProxy.wasConnectCalled, "Payment service should not be contacted if no admin exists.");
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if payment service is down.");
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = false;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if barcode service is down.");
    }
}
