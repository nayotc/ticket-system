package ticketsystem.DomainLayer.history;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Represents a snapshot of a ticket at the time it was purchased.
 *
 * <p>A purchased ticket belongs to a {@link Purchase} aggregate and does not
 * have an independent lifecycle or repository. It is persisted as an
 * embeddable value inside the purchase history collection table.</p>
 *
 * <p>The stored values are historical snapshots. Future changes to the
 * original event, seat or ticket price must not change this object.</p>
 */
@Embeddable
public class PurchasedTicket {

    /**
     * Business identifier of the original ticket.
     *
     * <p>This is not a database-generated identifier. It identifies the ticket
     * that was purchased in the original order.</p>
     */
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    /**
     * Seat row at purchase time.
     *
     * <p>The value may be {@code null} for tickets that do not represent a
     * numbered seat, such as standing-area tickets.</p>
     */
    @Column(name = "seat_row")
    private Integer row;

    /**
     * Seat number at purchase time.
     *
     * <p>The value may be {@code null} for tickets that do not represent a
     * numbered seat.</p>
     */
    @Column(name = "seat_chair")
    private Integer chair;

    @Column(name = "area_name")
private String areaName;

    /**
     * Price paid for this ticket.
     *
     * <p>{@link BigDecimal} is used instead of {@code double} because monetary
     * values must be persisted and calculated without binary floating-point
     * rounding errors.</p>
     */
    @Column(
            name = "price",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal price;

    /**
     * Current historical status of the purchased ticket.
     *
     * <p>The enum is stored by name rather than ordinal so that adding or
     * reordering enum constants will not corrupt existing database values.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    /**
     * Secure barcode issued for the purchased ticket.
     */
    @Column(name = "secure_barcode", length = 512)
    private String secureBarcode;

    /**
     * Constructor required by JPA and temporarily by the existing DTO mapping.
     */
    public PurchasedTicket() {
        this.status = TicketStatus.ACTIVE;
    }

    /**
     * Creates a purchased-ticket snapshot.
     *
     * @param ticketId original business ticket identifier
     * @param row seat row, or {@code null} for a non-seated ticket
     * @param chair seat number, or {@code null} for a non-seated ticket
     * @param price ticket price at the time of purchase
     * @param secureBarcode barcode issued for the purchased ticket
     */
    public PurchasedTicket(
        Long ticketId,
        String areaName,
        Integer row,
        Integer chair,
        BigDecimal price,
        String secureBarcode
) {
       this.ticketId = Objects.requireNonNull(
                ticketId,
                "Ticket ID cannot be null."
        );

        this.areaName = areaName;
        this.row = row;
        this.chair = chair;

        setPrice(price);

        this.secureBarcode = secureBarcode;
        this.status = TicketStatus.ACTIVE;
    }

    /**
     * Returns the original business ticket identifier.
     *
     * @return original ticket identifier
     */
    public Long getTicketId() {
        return ticketId;
    }

    /**
     * Updates the original business ticket identifier.
     *
     * <p>This setter is retained while the existing DTO conversion is being
     * replaced by the explicit history mapper.</p>
     *
     * @param ticketId original ticket identifier
     */
    public void setTicketId(Long ticketId) {
        this.ticketId = Objects.requireNonNull(
                ticketId,
                "Ticket ID cannot be null."
        );
    }

    /**
     * Returns the seat row.
     *
     * @return seat row, or {@code null}
     */
    public Integer getRow() {
        return row;
    }

    /**
     * Updates the seat row.
     *
     * @param row seat row, or {@code null}
     */
    public void setRow(Integer row) {
        this.row = row;
    }

    /**
     * Returns the seat number.
     *
     * @return seat number, or {@code null}
     */
    public Integer getChair() {
        return chair;
    }

    /**
     * Updates the seat number.
     *
     * @param chair seat number, or {@code null}
     */
    public void setChair(Integer chair) {
        this.chair = chair;
    }

    /**
     * Returns the ticket price.
     *
     * @return ticket price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Updates the ticket price.
     *
     * @param price non-negative ticket price
     */
    public void setPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException(
                    "Purchased ticket price cannot be null."
            );
        }

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Purchased ticket price cannot be negative."
            );
        }

        this.price = price;
    }

    /**
     * Returns the current ticket status.
     *
     * @return ticket status
     */
    public TicketStatus getStatus() {
        return status;
    }

    /**
     * Updates the current ticket status.
     *
     * @param status new ticket status
     */
    public void setStatus(TicketStatus status) {
        this.status = Objects.requireNonNull(
                status,
                "Ticket status cannot be null."
        );
    }

    /**
     * Returns the secure barcode.
     *
     * @return secure barcode
     */
    public String getSecureBarcode() {
        return secureBarcode;
    }

    /**
     * Updates the secure barcode.
     *
     * <p>This setter is retained while the existing DTO conversion is being
     * replaced by the explicit history mapper.</p>
     *
     * @param secureBarcode secure barcode
     */
    public void setSecureBarcode(String secureBarcode) {
        this.secureBarcode = secureBarcode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

}