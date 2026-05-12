package ticketsystem.ApplicationLayer.Events;

public interface UserSignInListener {

    void onUserSignIn(String guestToken, String memberToken);//this method will be called when the user signs in, and it will receive the userId and sessionToken as parameters. The implementation of this method will define what actions to take when a user signs in, such as merging guest orders into the user's account or updating the user's session information.
    
}
