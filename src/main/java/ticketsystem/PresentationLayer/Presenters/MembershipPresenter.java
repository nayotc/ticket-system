package ticketsystem.PresentationLayer.Presenters;

import java.util.Set;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.PresentationLayer.Views.CompanyManagement.CompanyManagementState;

@Component
public class MembershipPresenter {
    
    private final MembershipService membershipService;
    private final CompanyService companyService;
    private final EventService eventService;

    public MembershipPresenter(MembershipService membershipService) {
        this.membershipService = membershipService;
        // this.companyService = companyService;
        // this.eventService = eventService;
    }

    public CompanyManagementState loadCompanyManagement(String sessionToken, Long requestedCompanyId) {
        try {
            // כאן יש לקרוא לשירותי האפליקציה ששולפים את כל הנתונים (חברות, צוות, אירועים)
            // ולהרכיב את אובייקט ה-CompanyManagementState
            // return companyService.getCompanyManagementState(sessionToken, requestedCompanyId);
            
            throw new PresentationException("טעינת נתוני החברה טרם מומשה בשכבת האפליקציה.");
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("אירעה שגיאה בעת טעינת נתוני החברה.");
        }
    }

    public void requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) {
        try {
            boolean success = membershipService.requestManagerAssignment(sessionToken, companyId, targetMemberId, permissions);
            if (!success) {
                throw new PresentationException("בקשת מינוי המנהל נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Manager Assignment to company "+ companyId +" failed. Please try again later.");
        }
    }

    public void requestOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) {
        try {
            boolean success = membershipService.requestOwnerAssignment(sessionToken, companyId, targetMemberId);
            if (!success) {
                throw new PresentationException("בקשת מינוי הבעלים נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Owner Assignment to company "+ companyId +" failed. Please try again later.");
        }
    }

    public void updateManagerPermissions(String sessionToken, Long companyId, Long managerId, Set<Permission> permissions) {
        try {
            boolean success = membershipService.updateManagerPermissions(sessionToken, companyId, managerId, permissions);
            if (!success) {
                throw new PresentationException("עדכון ההרשאות נכשל.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to update manager permissions. Please try again later.");
        }
    }

    public void removeManagerAssignment(String sessionToken, Long companyId, Long targetMemberId) {
        try {
            boolean success = membershipService.removeManagerAssignment(sessionToken, companyId, targetMemberId);
            if (!success) {
                throw new PresentationException("הסרת המנהל נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to remove manager assignment. Please try again later.");
        }
    }

    public void removeOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) {
        try {
            boolean success = membershipService.removeOwnerAssignment(sessionToken, companyId, targetMemberId);
            if (!success) {
                throw new PresentationException("הסרת הבעלים נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to remove owner assignment. Please try again later.");
        }
    }

    public void giveUpOwnership(String sessionToken, Long companyId) {
        try {
            boolean success = membershipService.giveUpOwnership(sessionToken, companyId);
            if (!success) {
                throw new PresentationException("ויתור הבעלות נכשל.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to give up ownership. Please try again later.");
        }
    }

    // public void closeProductionCompany(String sessionToken, Long companyId) {
    //     try {
    //         // boolean success = companyService.closeCompany(sessionToken, companyId);
    //         throw new PresentationException("השהיית חברה טרם מומשה.");
    //     } catch (PresentationException e) {
    //         throw e;
    //     } catch (IllegalArgumentException | IllegalStateException e) {
    //         throw new PresentationException(e.getMessage());
    //     } catch (Exception e) {
    //         throw new PresentationException("אירעה שגיאה בעת השהיית החברה.");
    //     }
    // }

    // public void reopenProductionCompany(String sessionToken, Long companyId) {
    //     try {
    //          // boolean success = companyService.reopenCompany(sessionToken, companyId);
    //          throw new PresentationException("פתיחת חברה מחדש טרם מומשה.");
    //     } catch (PresentationException e) {
    //         throw e;
    //     } catch (IllegalArgumentException | IllegalStateException e) {
    //         throw new PresentationException(e.getMessage());
    //     } catch (Exception e) {
    //         throw new PresentationException("אירעה שגיאה בעת פתיחת החברה.");
    //     }
    // }

    public void cancelEvent(String sessionToken, Long companyId, Long eventId) {
        try {
             // boolean success = eventService.cancelEvent(sessionToken, companyId, eventId);
             throw new PresentationException("ביטול אירוע טרם מומש.");
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("אירעה שגיאה בעת ביטול האירוע.");
        }
    }
}