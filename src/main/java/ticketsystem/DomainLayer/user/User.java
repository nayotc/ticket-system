package ticketsystem.DomainLayer.user;

import java.util.ArrayList;
import java.util.List;

public class User {
    private Suspension suspension;
   // private List<Suspension> suspensions=new ArrayList<>();
    public User(){
    }

    public void suspend(Suspension suspension){

        if(suspension == null){
            throw new IllegalArgumentException("Suspension cannot be null");
        }

        if(isSuspended()){
            throw new IllegalStateException("Member is already suspended");
        }

        this.suspension=suspension;
    }

    public void revokeSuspension(){

        Suspension activeSuspension = getSuspension();
        if(activeSuspension == null|| !suspension.isActive()){
            throw new IllegalStateException("Member is not suspended");
        }

        activeSuspension.revoke();
    }

    public boolean isSuspended(){

        return suspension.isActive();
    }

    public Suspension getSuspension(){
            return suspension;
    }
}
