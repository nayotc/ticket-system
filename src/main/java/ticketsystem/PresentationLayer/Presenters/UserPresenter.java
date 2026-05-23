package ticketsystem.PresentationLayer.Presenters;

import ticketsystem.ApplicationLayer.UserService;

public class UserPresenter {
    private final UserService userService;

    public UserPresenter(UserService userService) {
        this.userService = userService;
    }

    // TODO: Confirm whether the View needs the returned guest token directly.
    public String visitSystem(){
        return userService.visitSystem();
    }

    // TODO: Confirm whether sign-up should return only success/failure or a user/member DTO.
    public boolean signUp(String sessionToken, String username, String password){
       return userService.signUp(sessionToken, username, password);
    }

    // TODO: Confirm whether login should return only the new member token or a richer result for the View.
    public String login(String sessionToken, String username, String password){
        return userService.login(sessionToken, username, password);
    }

    public boolean exit(String sessionToken){
        return userService.exit(sessionToken);
    }

    public String logOut(String sessionToken){
        return userService.logOut(sessionToken);
    }

    public boolean updateMemberUsername(String sessionToken, String password, String username, String newUsername) {
        return userService.updateMemberUsername(sessionToken, password, username, newUsername);
    }

    public boolean updateMemberPassword(String sessionToken, String password, String username, String newPassword) {
        return userService.updateMemberPassword(sessionToken,password,username,newPassword);
    }



}
