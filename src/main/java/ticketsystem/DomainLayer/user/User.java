package ticketsystem.DomainLayer.user;

import java.util.ArrayList;
import java.util.List;

public class User {

    private List<Suspension> suspensions=new ArrayList<>();
    public User(){
    }

    public void suspend(Suspension suspension){

        if(suspension == null){
            throw new IllegalArgumentException("Suspension cannot be null");
        }

        if(isSuspended()){
            throw new IllegalStateException("Member is already suspended");
        }

        suspensions.add(suspension);
    }

    public void revokeSuspension(){

        Suspension activeSuspension = getActiveSuspension();

        if(activeSuspension == null){
            throw new IllegalStateException("Member is not suspended");
        }

        activeSuspension.revoke();
    }

    public boolean isSuspended(){

        return suspensions.stream()
                .anyMatch(Suspension::isActive);
    }

    public Suspension getActiveSuspension(){

        return suspensions.stream()
                .filter(Suspension::isActive)
                .findFirst()
                .orElse(null);
    }
}
