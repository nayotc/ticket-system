package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISecureBarcode;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;

public class SystemAdminServiceTest {

    private ISystemAdminRepository adminRepoMock;
    private IPaymentService paymentMock;
    private ISecureBarcode barcodeMock;
    private SystemAdminService systemAdminService;

    @BeforeEach
    public void setUp() {
        adminRepoMock = mock(ISystemAdminRepository.class);
        paymentMock = mock(IPaymentService.class);
        barcodeMock = mock(ISecureBarcode.class);

        systemAdminService = new SystemAdminService(adminRepoMock, paymentMock, barcodeMock);
    }

    @Test
    public void givenValidPreconditions_whenInitSystem_thenSystemShouldInitializeSuccessfully() {
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(true);
        when(barcodeMock.connect()).thenReturn(true);

        boolean result = systemAdminService.initSystem();

        assertTrue(result, "System should initialize successfully.");
    }

    @Test
    public void givenNoSystemAdmins_whenInitSystem_thenInitializationShouldFail() {
        when(adminRepoMock.countAdmins()).thenReturn(0);

        boolean result = systemAdminService.initSystem();

        assertFalse(result, "System should fail to initialize.");
        verify(paymentMock, never()).connect();
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationShouldFail() {
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(false);

        boolean result = systemAdminService.initSystem();

        assertFalse(result, "System should fail if payment service is down.");
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationShouldFail() {
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(true);
        when(barcodeMock.connect()).thenReturn(false);

        boolean result = systemAdminService.initSystem();

        assertFalse(result, "System should fail if barcode service is down.");
    }
}
