package ticketsystem.DomainLayer;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "waiting_queue_entries")
public class WaitingQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long eventId;
    private String token;
    private LocalDateTime joinTime;

    // Default constructor for JPA
    public WaitingQueueEntry() {
    }

    public WaitingQueueEntry(long eventId, String token) {
        this.eventId = eventId;
        this.token = token;
        this.joinTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}
