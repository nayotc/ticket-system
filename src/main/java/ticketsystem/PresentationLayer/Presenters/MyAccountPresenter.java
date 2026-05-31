package ticketsystem.PresentationLayer.Presenters;

import java.util.List;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.MemberDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.PresentationLayer.Views.MyAccount.AccountProfileEditData;
import ticketsystem.PresentationLayer.Views.MyAccount.AccountProfileViewData;

@Component
public class MyAccountPresenter {

    private final UserService userService;
    private final HistoryService historyService;

    public MyAccountPresenter(UserService userService,HistoryService historyService) {
        this.userService = userService;
        this.historyService=historyService;
    }

    
    public MemberDTO loadProfile(String token) {
        try {
            return userService.getMemberDTO(token);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("טעינת פרטי המשתמש נכשלה.");
        }
    }


    private boolean updateUsername(String token, String password, String username, String newUsername) {
        try {
            return userService.updateMemberUsername(token, password, username, newUsername);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("עדכון שם המשתמש נכשל.");
        }
    }

    private boolean updatePassword(String token, String password, String username, String newPassword) {
        try {
            return userService.updateMemberPassword(token, password, username, newPassword);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("עדכון הסיסמה נכשל.");
        }
    }

    public boolean updateFullName(String token,String password, String username, String newFullName) {
    try {
        return userService.updateMemberFullName(token,password,username, newFullName);

    } catch (IllegalArgumentException | IllegalStateException e) {
        throw new PresentationException(e.getMessage());

    } catch (Exception e) {
        throw new PresentationException("עדכון השם המלא נכשל.");
    }
}

public boolean updatePhone(String token,String password, String username,  String newPhone) {
    try {
        return userService.updateMemberPhone(token, password, username, newPhone);

    } catch (IllegalArgumentException | IllegalStateException e) {
        throw new PresentationException(e.getMessage());

    } catch (Exception e) {
        throw new PresentationException("עדכון מספר הטלפון נכשל.");
    }
}


    public List<OrderDTO> loadPurchaseHistory(String token) {
        try {
            return historyService.getHistoryForUser(token);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("טעינת היסטוריית הרכישות נכשלה.");
        }
    }


    //  public void updatePersonalDetails(String token, AccountProfileEditData data) {

    //     if (data == null) {
    //         throw new PresentationException("פרטי המשתמש חסרים.");
    //     }

    //     try {

    //         String currentUsername = loadProfile(token).getEmail();

    //         if (data.email() != null
    //                 && !data.email().isBlank()
    //                 && !data.email().equals(currentUsername)) {

    //             updateUsername(
    //                     token,
    //                     data.currentPassword(),
    //                     currentUsername,
    //                     data.email()
    //             );

    //             currentUsername = data.email();
    //         }

    //         if (data.newPassword() != null
    //                 && !data.newPassword().isBlank()) {

    //             updatePassword(
    //                     token,
    //                     data.currentPassword(),
    //                     currentUsername,
    //                     data.newPassword()
    //             );
    //         }

    //     } catch (PresentationException e) {
    //         throw e;

    //     } catch (Exception e) {
    //         throw new PresentationException("שמירת פרטי החשבון נכשלה.");
    //     }
    // }
    public void updatePersonalDetails(String token, AccountProfileEditData data) {

    if (data == null) {
        throw new PresentationException("פרטי המשתמש חסרים.");
    }

    try {
        MemberDTO currentMember = loadProfile(token);

        String currentUsername = currentMember.getEmail(); 
        String currentFullName = currentMember.getFullName();
        String currentPhone = currentMember.getPhone();
        

        if (data.fullName() != null
                && !data.fullName().isBlank()
                && !data.fullName().equals(currentFullName)) {

            updateFullName(
                    token,data.currentPassword(),data.email(),
                    data.fullName()
            );
        }

        if (data.phone() != null
                && !data.phone().isBlank()
                && !data.phone().equals(currentPhone)) {

            updatePhone(
                    token,data.currentPassword(),data.email(),
                    data.phone()
            );
        }

        if (data.email() != null
                && !data.email().isBlank()
                && !data.email().equals(currentUsername)) {

            updateUsername(
                    token,
                    data.currentPassword(),currentUsername,
                    data.email()
            );

            currentUsername = data.email();
        }

        if (data.newPassword() != null
                && !data.newPassword().isBlank()) {

            updatePassword(
                    token,
                    
                    currentUsername,
                    data.newPassword()
            );
        }

    } catch (PresentationException e) {
        throw e;

    } catch (Exception e) {
        throw new PresentationException("שמירת פרטי החשבון נכשלה.");
    }
}

        public void openPurchaseDetails(String purchaseId){

        }

}