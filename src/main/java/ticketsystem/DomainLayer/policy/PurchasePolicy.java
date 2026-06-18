package ticketsystem.DomainLayer.policy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_policies")
public class PurchasePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER,
            optional = false
    )
    @JoinColumn(
            name = "root_rule_id",
            nullable = false,
            unique = true
    )
    private PurchaseRule rootRule;

    protected PurchasePolicy() {
    }

    public PurchasePolicy(PurchaseRule rootRule) {
        if(rootRule == null) {
            throw new IllegalArgumentException("Root rule cannot be null");
        }
        this.rootRule = rootRule;
    }

    public Long getId() {
        return id;
    }

    public PolicyResult validate(int quantity, int age) {
        return rootRule.isValid(quantity, age);
    }

    public PurchaseRule getRootRule() {
        return rootRule;
    }

    public static PurchasePolicy noRestrictions() {
        return new PurchasePolicy(new AlwaysAllowRule());
    }
}