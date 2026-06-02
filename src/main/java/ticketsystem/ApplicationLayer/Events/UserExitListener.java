package ticketsystem.ApplicationLayer.Events;

public interface UserExitListener {

    void onUserExit(String userToken);// this method will be called when the user exits the application, and it will
                                      // receive the sessionToken as a parameter. The implementation of this method
                                      // will define what actions to take when a user exits, such as saving the user's
                                      // current state, clearing session data, or performing any necessary cleanup
                                      // operations.

}