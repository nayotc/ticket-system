package ticketsystem.DomainLayer.discount;

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
@Table(name = "discount_conditions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "condition_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 32
)
public abstract class DiscountCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected DiscountCondition() {
    }

    public Long getId() {
        return id;
    }

    public abstract boolean isSatisfied(
            DiscountConditionContext context
    );
}