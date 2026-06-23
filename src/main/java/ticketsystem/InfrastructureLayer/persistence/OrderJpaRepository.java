package ticketsystem.InfrastructureLayer.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.order.ActiveOrder;

public interface OrderJpaRepository extends JpaRepository<ActiveOrder, Long> {

    @Query("SELECT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.orderId = :orderId")
    Optional<ActiveOrder> findByIdWithTickets(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets")
    List<ActiveOrder> findAllWithTickets();

    @Query("SELECT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.userId = :userId AND o.eventId = :eventId")
    Optional<ActiveOrder> findByUserIdAndEventIdWithTickets(@Param("userId") Long userId,
            @Param("eventId") Long eventId);

    @Query("SELECT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.sessionToken = :sessionToken AND o.eventId = :eventId")
    Optional<ActiveOrder> findBySessionTokenAndEventIdWithTickets(@Param("sessionToken") String sessionToken,
            @Param("eventId") Long eventId);

    @Query("SELECT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.sessionToken = :sessionToken")
    Optional<ActiveOrder> findBySessionTokenWithTickets(@Param("sessionToken") String sessionToken);

    @Query("SELECT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.userId = :userId")
    Optional<ActiveOrder> findByUserIdWithTickets(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM ActiveOrder o LEFT JOIN FETCH o.tickets WHERE o.eventId = :eventId")
    List<ActiveOrder> findByEventIdWithTickets(@Param("eventId") Long eventId);

        @Query("""
        SELECT o
        FROM ActiveOrder o
        WHERE o.userId IS NULL
        OR o.status = ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus.CANCELLED
        OR (
                o.status <> ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus.PENDING_CHECKOUT
                AND o.expiresAt <= :now
                )
        """)
        List<ActiveOrder> findExpiredAndGuestOrders(@Param("now") LocalDateTime now);
}
