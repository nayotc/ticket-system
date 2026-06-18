package ticketsystem.DomainLayer.policy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_rules")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "rule_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 32
)
public abstract class PurchaseRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    protected PurchaseRule() {
    }

    public Long getId() {
        return id;
    }

    public abstract PolicyResult isValid(int quantity, int age);
}