package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.PresentationLayer.Components.ManagementSideNav;
import ticketsystem.DomainLayer.user.Permission;


@Component
public class CompanyPresenter implements ManagementSideNav.ManagementSideNavPresenter {

    private final CompanyService companyService;

    public CompanyPresenter(CompanyService companyService) {
        this.companyService = companyService;
    }

    public CompanyDTO createProductionCompany(String sessionToken, String companyName) {
        try {
            return companyService.createProductionCompany(sessionToken, companyName);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "יצירת חברת ההפקה נכשלה. יש לנסות שוב.")
            );

        } catch (Exception e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "יצירת חברת ההפקה נכשלה. יש לנסות שוב.")
            );
        }
    }

    public CompanyDTO closeProductionCompany(String sessionToken, long companyId) {
        try {
            return companyService.closeProductionCompany(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "סגירת חברת ההפקה נכשלה. יש לנסות שוב.")
            );

        } catch (Exception e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "סגירת חברת ההפקה נכשלה. יש לנסות שוב.")
            );
        }
    }

    public CompanyDTO reopenProductionCompany(String sessionToken, long companyId) {
        try {
            return companyService.reopenProductionCompany(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "פתיחת חברת ההפקה מחדש נכשלה. יש לנסות שוב.")
            );

        } catch (Exception e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "פתיחת חברת ההפקה מחדש נכשלה. יש לנסות שוב.")
            );
        }
    }

    public CompanyDTO getCompanyDetails(String sessionToken, long companyId) {
        try {
            return companyService.getCompanyDetails(sessionToken, companyId);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "טעינת פרטי חברת ההפקה נכשלה. יש לנסות שוב.")
            );

        } catch (Exception e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "טעינת פרטי חברת ההפקה נכשלה. יש לנסות שוב.")
            );
        }
    }

    public Long getFirstManagedCompanyId(String sessionToken) {
        try {
            return companyService.getFirstManagedCompanyId(sessionToken);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "לא ניתן לטעון את חברת ההפקה המנוהלת. יש לנסות שוב.")
            );

        } catch (Exception e) {
            throw new PresentationException(
                    translateCompanyError(e.getMessage(), "לא ניתן לטעון את חברת ההפקה המנוהלת. יש לנסות שוב.")
            );
        }
    }

    /**
     * Translates known company-related error messages into Hebrew.
     *
     * If the message is not recognized, the method returns the provided fallback
     * message instead of exposing the original technical message to the user.
     *
     * @param message original error message from the lower layers
     * @param fallbackMessage Hebrew fallback message for unknown errors
     * @return Hebrew user-facing error message
     */
    private String translateCompanyError(String message, String fallbackMessage) {
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        return switch (message) {
            case "Suspended users can only perform view actions" ->
                    "משתמש מושהה יכול לצפות במידע בלבד ולא לבצע פעולות במערכת.";

            case "Error: Invalid or expired session token." ->
                    "פג תוקף החיבור למערכת. יש להתחבר מחדש.";

            case "Error: Company not found." ->
                    "לא נמצאה חברת ההפקה המבוקשת.";

            case "Error: User does not have permission to view this company." ->
                    "אין לך הרשאה לצפות בפרטי חברת ההפקה הזו.";

            case "Member not found" ->
                    "לא נמצא משתמש מחובר מתאים.";

            default ->
                    fallbackMessage;
        };
    }

    @Override
    public boolean hasPermission(String sessionToken, long companyId, Permission permission) {
        try {
            return companyService.hasPermission(sessionToken, companyId, permission);

        } catch (Exception e) {
            return false;
        }
    }
}
