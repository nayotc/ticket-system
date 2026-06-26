package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.DTO.RoleTreeDTO;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.DTO.CompanyDTO;

@Component
public class RolesTreePresenter {

    private final MembershipService membershipService;
    private final CompanyService companyService;

    public RolesTreePresenter(MembershipService membershipService, CompanyService companyService) {
        this.membershipService = membershipService;
        this.companyService = companyService;
    }

    public RoleTreeDTO loadRoleTree(String memberToken, Long companyId) {
        try {
            if (memberToken == null || memberToken.isBlank()) {
                throw new PresentationException("יש להתחבר למערכת כדי לצפות בעץ התפקידים.");
            }

            if (companyId == null || companyId <= 0) {
                throw new PresentationException("מזהה החברה לא תקין.");
            }

            RoleTreeDTO root = membershipService.viewRolesAndPermissionsTreeDto(memberToken, companyId);

            if (root == null) {
                throw new PresentationException("לא נמצאו בעלי תפקידים להצגה.");
            }

            return root;
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateTreeError(msg,
                    "טעינת עץ התפקידים נכשלה. אנא נסו שוב."
                ));
        }
    }
    
    public CompanyDTO loadCompany(String memberToken, Long companyId) {
        try {
            if (memberToken == null || memberToken.isBlank()) {
                throw new PresentationException("יש להתחבר למערכת כדי לצפות בפרטי החברה.");
            }

            if (companyId == null || companyId <= 0) {
                throw new PresentationException("מזהה החברה לא תקין.");
            }

            return companyService.getCompanyDetails(memberToken, companyId);
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateTreeError(msg,
                    "טעינת פרטי החברה נכשלה. אנא נסו שוב."
                ));
        }
    }
    
    private String extractErrorMessage(Exception e) {
        Throwable current = e;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage();
    }   

    private String translateTreeError(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        if (message.contains("Session authentication failed")) {
            return "החיבור למערכת לא תקין. התחברי מחדש ונסי שוב.";
        }

        if (message.contains("Member ID not found in token")) {
            return "לא נמצא משתמש מחובר. התחברי מחדש ונסי שוב.";
        }

        if (message.contains("Company not found")) {
            return "חברת ההפקה לא נמצאה.";
        }

        if (message.contains("Member not found")) {
            return "המשתמש המחובר לא נמצא במערכת.";
        }

        if (message.contains("Member does not have an active role in this company")) {
            return "אין לך הרשאה לצפות בעץ התפקידים של החברה הזו.";
        }

        if (message.contains("Only Owners or Founder can perform this action")) {
            return "רק בעלים או מייסד החברה יכולים לצפות בעץ התפקידים.";
        }

        return fallback;
    }
}