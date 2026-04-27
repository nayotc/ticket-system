package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.ApplicationLayer.CompanyService;
import java.util.Optional;

class CompanyServiceTest {

    private CompanyService companyService;
    private ICompanyRepository companyRepoMock;
    private AuthService authMock;

    private final String SESSION_ID = "session-123";
    private final String FOUNDER = "noaam";
    private final String OTHER_USER = "someone_else";
    private final String COMPANY_NAME = "BGU_Racing";

    @BeforeEach
    void setUp() {
        companyRepoMock = mock(ICompanyRepository.class);
        authMock = mock(AuthService.class);
        companyService = new CompanyService(companyRepoMock, authMock);
    }

    // --- Tests for UC 4.13 (Close Company) ---

    @Test
    void testCloseCompany_AsFounder_Success() throws Exception {
        // Arrange
        Company company = new Company(COMPANY_NAME, FOUNDER, new PurchasePolicy(), new DiscountPolicy());
        when(authMock.getUsernameBySession(SESSION_ID)).thenReturn(FOUNDER);
        when(companyRepoMock.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));

        // Act
        companyService.closeProductionCompany(SESSION_ID, COMPANY_NAME);

        // Assert
        assertFalse(company.getIsActive());
        verify(companyRepoMock, times(1)).save(company);
    }

    @Test
    void testCloseCompany_UnauthorizedUser_ThrowsException() {
        // Arrange
        Company company = new Company(COMPANY_NAME, FOUNDER, new PurchasePolicy(), new DiscountPolicy());
        when(authMock.getUsernameBySession(SESSION_ID)).thenReturn(OTHER_USER);
        when(companyRepoMock.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));

        // Act & Assert
        Exception ex = assertThrows(Exception.class, () -> 
            companyService.closeProductionCompany(SESSION_ID, COMPANY_NAME));
        assertTrue(ex.getMessage().contains("lack of permissions"));
    }

    // --- Tests for UC 4.14 (Reopen Company) ---

    @Test
    void testReopenCompany_AlreadyActive_ThrowsException() throws Exception {
        // Arrange
        Company company = new Company(COMPANY_NAME, FOUNDER, new PurchasePolicy(), new DiscountPolicy());
        // החברה כבר פעילה כברירת מחדל
        when(authMock.getUsernameBySession(SESSION_ID)).thenReturn(FOUNDER);
        when(companyRepoMock.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));

        // Act & Assert
        Exception ex = assertThrows(Exception.class, () -> 
            companyService.reopenProductionCompany(SESSION_ID, COMPANY_NAME));
        assertTrue(ex.getMessage().contains("No action needed"));
    }

    // --- Tests for UC 4.15 (View Tree) ---

    @Test
    void testViewRolesTree_Success() throws Exception {
        // Arrange
        Company company = new Company(COMPANY_NAME, FOUNDER, new PurchasePolicy(), new DiscountPolicy());
        when(authMock.getUsernameBySession(SESSION_ID)).thenReturn(FOUNDER);
        when(companyRepoMock.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));

        // Act
        String tree = companyService.viewRolesAndPermissionsTree(SESSION_ID, COMPANY_NAME);

        // Assert
        assertNotNull(tree);
        assertTrue(tree.contains(FOUNDER)); // לפי ה-Alternative flow, לפחות המייסד מופיע
    }
}