package ticketsystem.DomainLayer.lottery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents the registration of one member to a specific lottery.
 *
 * <p>The registration belongs to the lifecycle of its owning {@link Lottery}.
 * It stores whether the member won and, when relevant, the authentication code
 * that allows the winner to participate in the pre-sale flow.</p>
 *
 * <p>A member may be registered only once to the same lottery. This invariant
 * is enforced both by the Lottery domain object and by a database-level unique
 * constraint.</p>
 */
@Entity
@Table(
        name = "lottery_registrations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lottery_registration_member",
                        columnNames = {"lottery_id", "member_id"}
                )
        }
)
public class LotteryRegistration {

    /**
     * Internal database identifier.
     *
     * <p>This identifier has no business meaning. The meaningful identity of a
     * registration is the combination of lottery and member.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    /**
     * The lottery that owns this registration.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lottery_id", nullable = false)
    private Lottery lottery;

    /**
     * Identifier of the registered member.
     *
     * <p>This remains a scalar identifier rather than a JPA relationship so the
     * Lottery persistence model does not depend on the User module already being
     * migrated to JPA.</p>
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * Indicates whether the registered member was selected as a winner.
     */
    @Column(name = "is_winner", nullable = false)
    private boolean winner;

    /**
     * Authentication code assigned to the member after winning the lottery.
     *
     * <p>The value remains null for participants who were not selected.</p>
     */
    @Column(name = "auth_code", length = 100)
    private String authCode;

    /**
     * Required by JPA.
     */
    protected LotteryRegistration() {
    }

    /**
     * Creates a new registration belonging to the supplied lottery.
     *
     * @param lottery  owning lottery
     * @param memberId registered member identifier
     */
    LotteryRegistration(Lottery lottery, long memberId) {
        this.lottery = lottery;
        this.memberId = memberId;
        this.winner = false;
        this.authCode = null;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public long getMemberId() {
        return memberId;
    }

    public boolean isWinner() {
        return winner;
    }

    public String getAuthCode() {
        return authCode;
    }

    /**
     * Marks the registered member as a lottery winner and stores the generated
     * authentication code.
     *
     * @param generatedCode code generated for the winning member
     */
    public void markAsWinner(String generatedCode) {
        this.winner = true;
        this.authCode = generatedCode;
    }
}