package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.event.Event;

public interface EventJpaRepository extends JpaRepository<Event,Long>, JpaSpecificationExecutor<Event>  {
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.purchasePolicy pp
            LEFT JOIN FETCH pp.rootRule
            LEFT JOIN FETCH e.discountPolicy dp
            LEFT JOIN FETCH e.map.elements
            WHERE e.id = :eventId
            """)
    Optional<Event> findByIdWithMap(@Param("eventId") Long eventId);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.purchasePolicy pp
            LEFT JOIN FETCH pp.rootRule
            LEFT JOIN FETCH e.discountPolicy dp
            LEFT JOIN FETCH e.map.elements
            WHERE e.companyId = :companyId
            """)
    List<Event> findByCompanyIdWithMap(@Param("companyId") Long companyId);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.purchasePolicy pp
            LEFT JOIN FETCH pp.rootRule
            LEFT JOIN FETCH e.discountPolicy dp
            LEFT JOIN FETCH e.map.elements
            """)
    List<Event> findAllWithMap();

}
