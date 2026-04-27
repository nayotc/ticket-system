package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISecureBarcode;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;

public class SystemAdminServiceTest {

    private ISystemAdminRepository adminRepoMock;
    private IPaymentService paymentMock;
    private ISecureBarcode barcodeMock;
    private SystemAdminService systemAdminService;
    private ISystemLogger loggerMock;

    @BeforeEach
    public void setUp() {
        adminRepoMock = mock(ISystemAdminRepository.class);
        paymentMock = mock(IPaymentService.class);
        barcodeMock = mock(ISecureBarcode.class);
        loggerMock = mock(ISystemLogger.class);

        systemAdminService = new SystemAdminService(adminRepoMock, paymentMock, barcodeMock, loggerMock);
    }

    @Test
    public void givenValidPreconditions_whenInitSystem_thenSystemShouldInitializeSuccessfully() {
        //arrange
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(true);
        when(barcodeMock.connect()).thenReturn(true);

        //act
        boolean result = systemAdminService.initSystem();

        //assert
        assertTrue(result, "System should initialize successfully.");
        verify(loggerMock, times(1)).logEvent(eq("initSystem"), anyString());
    }

    @Test
    public void givenNoSystemAdmins_whenInitSystem_thenInitializationShouldFail() {
        //arrange
        when(adminRepoMock.countAdmins()).thenReturn(0);

        //act
        boolean result = systemAdminService.initSystem();

        //assert
        assertFalse(result, "System should fail to initialize.");
        verify(paymentMock, never()).connect();
        verify(barcodeMock, never()).connect();
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationShouldFail() {
        //arrange
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(false);

        //act
        boolean result = systemAdminService.initSystem();

        //assert
        assertFalse(result, "System should fail if payment service is down.");
        verify(loggerMock, times(1)).logEvent(eq("initSystem"), anyString());
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationShouldFail() {
        //arrange
        when(adminRepoMock.countAdmins()).thenReturn(1);
        when(paymentMock.connect()).thenReturn(true);
        when(barcodeMock.connect()).thenReturn(false);

        //act
        boolean result = systemAdminService.initSystem();

        //assert
        assertFalse(result, "System should fail if barcode service is down.");
        verify(loggerMock, times(1)).logEvent(eq("initSystem"), anyString());

    }
}
