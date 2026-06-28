package ticketsystem.PresentationLayer.Presenters;

import java.util.List;
import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.MyAccountDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.PresentationLayer.Views.MyAccount.AccountProfileEditData;

@Component
public class MyAccountPresenter {

    private final UserService userService;
    private final HistoryService historyService;

    public MyAccountPresenter(UserService userService,HistoryService historyService) {
        this.userService = userService;
        this.historyService=historyService;
    }

    public MyAccountDTO loadProfile(String token) {
        try {
            return userService.getMyAccountDTO(token);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "טעינת פרטי המשתמש נכשלה."
                ));
        }
    }

    private boolean updateUsername(String token, String password, String username, String newUsername) {
        try {
            return userService.updateMemberUsername(token, password, username, newUsername);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "עדכון שם המשתמש נכשל."
                ));
        }
    }

    private boolean updatePassword(String token, String password, String username, String newPassword) {
        try {
            return userService.updateMemberPassword(token, password, username, newPassword);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));

        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "עדכון הסיסמה נכשל."
                ));
        }
    }

    public boolean updateFullName(String token,String password, String username, String newFullName) {
        try {
            return userService.updateMemberFullName(token,password,username, newFullName);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "עדכון השם המלא נכשל."
                ));
        }
    }

    public boolean updatePhone(String token,String password, String username,  String newPhone) {
        try {
            return userService.updateMemberPhone(token, password, username, newPhone);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "עדכון מספר הטלפון נכשל."
                ));
        }
    }

    public List<OrderDTO> loadPurchaseHistory(String token) {
        try {
            return historyService.getHistoryForUser(token);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    e.getMessage()
                ));
        
        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "טעינת היסטוריית הרכישות נכשלה."
                ));
        }
    }


   public void updatePersonalDetails(String token, AccountProfileEditData data) {
        if (data == null) {
            throw new PresentationException("פרטי המשתמש חסרים.");
        }

        if (data.currentPassword() == null || data.currentPassword().isBlank()) {
            throw new PresentationException("יש להזין סיסמה נוכחית כדי לשמור שינויים.");
        }

        try {
            MyAccountDTO currentMember = loadProfile(token);

            String currentUsername = currentMember.getEmail();
            String currentFullName = currentMember.getFullName();
            String currentPhone = currentMember.getPhone();

            if (data.fullName() != null
                    && !data.fullName().isBlank()
                    && !data.fullName().equals(currentFullName)) {

                updateFullName(
                        token,
                        data.currentPassword(),
                        currentUsername,
                        data.fullName()
                );
            }

            if (data.phone() != null
                    && !data.phone().isBlank()
                    && !data.phone().equals(currentPhone)) {

                updatePhone(
                        token,
                        data.currentPassword(),
                        currentUsername,
                        data.phone()
                );
            }

            if (data.email() != null
                    && !data.email().isBlank()
                    && !data.email().equals(currentUsername)) {

                updateUsername(
                        token,
                        data.currentPassword(),
                        currentUsername,
                        data.email()
                );

                currentUsername = data.email();
            }

            if (data.newPassword() != null
                    && !data.newPassword().isBlank()) {

                updatePassword(
                        token,
                        data.currentPassword(),
                        currentUsername,
                        data.newPassword()
                );
            }

        } catch (PresentationException e) {
            throw e;

        } catch (Exception e) {
            throw PresentationException.dispatch(e, 
                msg -> translateMyAccountError(msg,
                    "שמירת פרטי החשבון נכשלה."
                ));
        }
    }

    public void openPurchaseDetails(String purchaseId){

    }

    // TODO: Implement the translation to Hebrew message
    private String translateMyAccountError(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        return switch (message.trim()) {

            default -> fallback;
        };
    }

}