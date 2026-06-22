package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;

public interface EventJpaRepository extends JpaRepository<Event,Long>, JpaSpecificationExecutor<Event>  {
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.map.elements
            WHERE e.id = :eventId
            """)
    Optional<Event> findByIdWithMap(@Param("eventId") Long eventId);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.map.elements
            WHERE e.companyId = :companyId
            """)
    List<Event> findByCompanyIdWithMap(@Param("companyId") Long companyId);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.map.elements
            """)
    List<Event> findAllWithMap();

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                update Seat s
                set s.status = :newStatus
                where s.seatingArea.id = :areaId
                and s.position.row = :row
                and s.position.number = :number
                """)
        int updateSeatStatus(@Param("areaId") Long areaId, @Param("row") int row, @Param("number") int number, @Param("newStatus") SeatStatus newStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                update StandingArea a
                set a.reserved = a.reserved + :reservedDelta
                where a.id = :areaId
                and a.reserved + :reservedDelta >= 0
                and a.reserved + :reservedDelta + a.sold <= a.capacity
                """)
        int updateStandingAreaReservedCount(@Param("areaId") Long areaId, @Param("reservedDelta") int reservedDelta);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                update StandingArea a
                set a.reserved = a.reserved - :quantity,
                a.sold = a.sold + :quantity
                where a.id = :areaId
                and a.reserved >= :quantity
                and a.sold + :quantity <= a.capacity
                """)
        int markStandingTicketsAsSold(
                @Param("areaId") Long areaId,
                @Param("quantity") int quantity
        );


}
