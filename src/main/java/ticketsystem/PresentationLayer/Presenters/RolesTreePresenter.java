package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.DTO.RoleTreeDTO;
import ticketsystem.PresentationLayer.Session.UiSession;

@Component
public class RolesTreePresenter {

    private final MembershipService membershipService;

    public RolesTreePresenter(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public RoleTreeDTO loadRoleTree(Long companyId) {
        try {
            if (companyId == null || companyId <= 0) {
                throw new PresentationException("מזהה החברה לא תקין.");
            }

            String memberToken = UiSession.getMemberToken();

            if (memberToken == null) {
                throw new PresentationException("יש להתחבר למערכת כדי לצפות בעץ התפקידים.");
            }

            RoleTreeDTO root = membershipService.viewRolesAndPermissionsTreeDto(memberToken, companyId);

            if (root == null) {
                throw new PresentationException("לא נמצאו בעלי תפקידים להצגה.");
            }

            return root;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage()));

        }  catch (Exception e) {
    throw new PresentationException(translateError(extractErrorMessage(e)));
}
        
}        
    private String extractErrorMessage(Exception e) {
        Throwable current = e;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage();
    }

    

    private String translateError(String message) {
        if (message == null || message.isBlank()) {
            return "טעינת עץ התפקידים נכשלה. נסו שוב.";
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

        return "טעינת עץ התפקידים נכשלה. נסו שוב.";
    }
}