package ticketsystem.DomainLayer.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("GUEST")
public class Guest extends User {

    public Guest() {
    }
}
