package ticketsystem.PresentationLayer.Presenters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.CompanyRoleDTO;
import ticketsystem.DTO.MemberDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DomainLayer.user.Permission;

import ticketsystem.PresentationLayer.Presenters.CompanyManagementState;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.CompanyStats;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.ManagedCompanyItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.EventManagementItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.TeamMemberItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.PolicySummary;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.RoleType;

@Component
public class MembershipPresenter {
    
    private final MembershipService membershipService;
    private final ITokenService tokenService;
    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;

    public MembershipPresenter(MembershipService membershipService, ITokenService tokenService, UserService userService, CompanyService companyService, EventService eventService) {
        this.membershipService = membershipService;
        this.tokenService = tokenService;
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
    }

    public CompanyManagementState loadCompanyManagement(String sessionToken, Long requestedCompanyId) {
        try {
            // 1. אימות תקינות הטוקן ושליפת מזהה המשתמש הנוכחי
            Long currentUserId = tokenService.extractUserId(sessionToken);
            if (currentUserId == null) {
                throw new PresentationException("פג תוקף החיבור של המשתמש. אנא התחבר מחדש.");
            }

            // 2. שליפת רשימת החברות שהמשתמש הוא חבר בהן משכבת האפליקציה
            List<CompanyDTO> userCompanies = membershipService.getCompaniesByMember(sessionToken);
            
            if (userCompanies.isEmpty()) {
                return new CompanyManagementState(
                        List.of(), null, false, false, List.of(), List.of(), 
                        new CompanyStats(0, 0), null
                );
            }

            // 3. קביעת החברה שנבחרה (אם לא נשלח מזהה ספציפי, נבחר בחברה הראשונה ברשימה)
            CompanyDTO selectedCompanyDto = userCompanies.stream()
                    .filter(c -> requestedCompanyId != null && c.getId() == requestedCompanyId)
                    .findFirst()
                    .orElse(userCompanies.get(0));

            Long companyId = selectedCompanyDto.getId();

            // המרת רשימת ה-DTOs של החברות ל-ManagedCompanyItem עבור התפריט במסך
            List<ManagedCompanyItem> managedCompanies = userCompanies.stream()
                    .map(dto -> new ManagedCompanyItem(
                            dto.getId(),
                            dto.getName(),
                            dto.getFounderId(),
                            "Member #" + dto.getFounderId(),
                            dto.isActive()
                    ))
                    .collect(Collectors.toList());

            ManagedCompanyItem selectedCompanyItem = managedCompanies.stream()
                    .filter(c -> c.id() == companyId)
                    .findFirst()
                    .orElse(managedCompanies.get(0));

            // 4. שליפת חברי הצוות של החברה הנבחרת משכבת האפליקציה (כעת מקבלים DTOs)
            List<MemberDTO> teamMembersDto = membershipService.getCompanyTeamMembers(sessionToken, companyId);
            List<TeamMemberItem> uiTeamMembers = new ArrayList<>();

            boolean isCurrentUserOwner = false;
            boolean canCurrentUserManageTeam = false;

            // 5. מיפוי פולימורפי של חברי הצוות מתוך ה-DTO
            for (MemberDTO member : teamMembersDto) {
                CompanyRoleDTO role = member.getRoles().stream()
                        .filter(r -> r.getCompanyId().equals(companyId))
                        .findFirst()
                        .orElse(null);

                if (role == null || "CANCELLED".equals(role.getStatus())) {
                    continue; // מדלגים על תפקידים שבוטלו
                }

                RoleType uiRoleType;
                String roleLabel;
                Set<Permission> uiPermissions = Set.of();

                if ("FOUNDER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.FOUNDER;
                    roleLabel = "Founder";
                } else if ("OWNER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.OWNER;
                    roleLabel = "Owner";
                } else if ("MANAGER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.MANAGER;
                    roleLabel = "Manager";
                    // המרת ההרשאות בצורה בטוחה שמתמודדת עם פורמטים מותאמים אישית של Enums
                    if (role.getPermissions() != null) {
                        uiPermissions = role.getPermissions().stream()
                                .map(permStr -> {
                                    for (Permission p : Permission.values()) {
                                        // השוואה גם לשם הקבוע (name) וגם לערך המודפס (toString)
                                        if (p.name().equals(permStr) || p.toString().equals(permStr)) {
                                            return p;
                                        }
                                    }
                                    return null; // במקרה שההרשאה לא זוהתה
                                })
                                .filter(p -> p != null) // סינון ערכים ריקים כדי למנוע קריסות
                                .collect(Collectors.toSet());
                    }
                } else {
                    continue;
                }

                // בדיקת סטטוס המשתמש המחובר כעת לצורך קביעת דגלי הגישה (owner, canManageTeam)
                if (member.getMemberId().equals(currentUserId)) {
                    if (uiRoleType == RoleType.FOUNDER || uiRoleType == RoleType.OWNER) {
                        isCurrentUserOwner = true;
                        canCurrentUserManageTeam = true;
                    }
                }

                // קביעה האם המשתמש הנוכחי הוא הממנה של חבר הצוות הזה (חוקי היררכיה להסרה)
                boolean removable = false;
                if (role.getAppointedByMemberId() != null && role.getAppointedByMemberId().equals(currentUserId)) {
                    removable = true;
                }

                uiTeamMembers.add(new TeamMemberItem(
                        member.getMemberId(),
                        member.getUserName(), 
                        roleLabel,
                        uiRoleType,
                        uiPermissions,
                        removable
                ));
            }

            // 6. שליפת רשימת אירועי החברה ומיפויים דרך EventService
            List<EventDTO> domainEvents = eventService.getEventsByCompany(sessionToken, companyId);
            
            List<EventManagementItem> uiEvents = domainEvents.stream()
                    // ב-record ניגשים לשדות דרך שם השדה: id() ו-name()
                    .map(e -> new EventManagementItem(e.id(), e.name())) 
                    .collect(Collectors.toList());

            // 7. בניית הסטטיסטיקות ותמציות המדיניות
            int activeEventsCount = (int) domainEvents.stream()
                    // בודקים אם שדה ה-status קיים ושווה למחרוזת "ACTIVE"
                    .filter(e -> e.status() != null && e.status().equals("ACTIVE")) 
                    .count();
                    
            int pendingAssignmentsCount = membershipService.getPendingAssignmentsCount(companyId);
            CompanyStats uiStats = new CompanyStats(activeEventsCount, pendingAssignmentsCount);

            String purchasePolicy = companyService.getPurchasePolicySummary(companyId);
            String discountPolicy = companyService.getDiscountPolicySummary(companyId);
            PolicySummary uiPolicySummary = new PolicySummary(purchasePolicy, discountPolicy);

            // 8. החזרת הסטייט המלא והמובנה ישירות ל-View
            return new CompanyManagementState(
                    managedCompanies,
                    selectedCompanyItem,
                    isCurrentUserOwner,
                    canCurrentUserManageTeam,
                    uiTeamMembers,
                    uiEvents,
                    uiStats,
                    uiPolicySummary
            );

        } catch (PresentationException e) {
            // שגיאות שכבר טופלו ועטפו בהצלחה ב-Presenter
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // תפיסת שגיאות אימות וולידציה שמגיעות משכבת הלוגיקה והעברתן כמסר נקי למסך
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            // חסימת שגיאות מערכת פנימיות (כמו בעיות תקשורת עם ה-DB) והצגת הודעה כללית ומאובטחת
            throw new PresentationException("אירעה שגיאה בלתי צפויה בעת טעינת נתוני ניהול החברה.");
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
             boolean success = eventService.cancelEvent(sessionToken, eventId);
            if (!success) {
                throw new PresentationException("ביטול אירוע טרם מומש.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to cancel event. Please try again later.");
        }
    }
}