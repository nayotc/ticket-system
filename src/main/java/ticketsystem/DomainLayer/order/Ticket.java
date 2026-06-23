package ticketsystem.DomainLayer.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "seat_row", nullable = false)
    private int row;

    @Column(name = "seat_chair", nullable = false)
    private int chair;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ActiveOrder activeOrder;

    protected Ticket() {
    }

    public Ticket(Long ticketId, Long eventId, Long areaId, int row, int chair, BigDecimal price) {
        if (ticketId != null) {
            this.ticketId = ticketId;
        }
        this.eventId = eventId;
        this.areaId = areaId;
        this.row = row;
        this.chair = chair;
        this.price = price;
    }

    public Ticket(Ticket other) {
        this.ticketId = other.ticketId;
        this.eventId = other.eventId;
        this.areaId = other.areaId;
        this.row = other.row;
        this.chair = other.chair;
        this.price = other.price;
    }

    public Ticket copy() {
        return new Ticket(this);
    }

    public Long getTicketId() {
        return this.ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public Long getAreaId() {
        return this.areaId;
    }

    public int getRow() {
        return this.row;
    }

    public int getChair() {
        return this.chair;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    void setActiveOrder(ActiveOrder activeOrder) {
        this.activeOrder = activeOrder;
    }

    public boolean isSeat(){
        return row!=0 && chair !=0;
    }
}
