package ticketsystem.InfrastructureLayer;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.InfrastructureLayer.persistence.EventJpaRepository;
import java.util.ArrayList;
import java.util.Locale;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventSearchResultView;

@Repository
public class EventRepository implements IEventRepository {

    private final EventJpaRepository eventJpaRepository;

    public EventRepository(EventJpaRepository eventJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    @Transactional
    public void addEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getId() != null && eventJpaRepository.existsById(event.getId())) {
            throw new IllegalArgumentException("Event already exists with id: " + event.getId());
        }

        eventJpaRepository.saveAndFlush(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventJpaRepository.findByIdWithMap(eventId)
                .map(Event::copy)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getId() == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (!eventJpaRepository.existsById(event.getId())) {
            throw new IllegalArgumentException("Event not found with id: " + event.getId());
        }

        try {
            eventJpaRepository.saveAndFlush(event);
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + event.getId()
            );
        }
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, long expectedVersion) {
        Event event = eventJpaRepository.findByIdWithMap(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

        if (event.getVersion() != expectedVersion) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + eventId
            );
        }

        try {
            eventJpaRepository.delete(event);
            eventJpaRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + eventId
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getEventsByCompanyId(Long companyId) {
        return eventJpaRepository.findByCompanyIdWithMap(companyId).stream()
                .map(Event::copy)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventJpaRepository.findAllWithMap().stream()
                .map(Event::copy)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSearchResultView> searchEvents(
            SearchCriteria criteria,
            List<Long> companyIds
    ) {
        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }

        if (companyIds == null || companyIds.isEmpty()) {
            return List.of();
        }

        if (criteria.getMinPrice() != null
                && criteria.getMaxPrice() != null
                && criteria.getMinPrice().compareTo(criteria.getMaxPrice()) > 0) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        Specification<Event> specification =
                createCatalogSpecification(criteria, companyIds);

        return eventJpaRepository.findBy(
                specification,
                query -> query.as(EventSearchResultView.class).all()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSearchResultView> getFeaturedEvents(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        Specification<Event> activeEvents = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        Event.eventStatus.ACTIVE
                );

        return eventJpaRepository.findBy(
                activeEvents,
                query -> query
                        .as(EventSearchResultView.class)
                        .sortBy(Sort.by(Sort.Direction.DESC, "rate"))
                        .page(PageRequest.of(0, limit))
                        .getContent()
        );
    }

    private Specification<Event> createCatalogSpecification(
            SearchCriteria criteria,
            List<Long> companyIds
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("status"),
                            Event.eventStatus.ACTIVE
                    )
            );

            predicates.add(root.get("companyId").in(companyIds));

            if (criteria.getCategory() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("category"),
                                criteria.getCategory()
                        )
                );
            }

            if (criteria.getLocation() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("location"),
                                criteria.getLocation()
                        )
                );
            }

            if (criteria.getFromDate() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("date"),
                                criteria.getFromDate()
                        )
                );
            }

            if (criteria.getToDate() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("date"),
                                criteria.getToDate()
                        )
                );
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("ticketPrice"),
                                criteria.getMinPrice()
                        )
                );
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("ticketPrice"),
                                criteria.getMaxPrice()
                        )
                );
            }

            if (criteria.getEventRate() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("rate"),
                                criteria.getEventRate()
                        )
                );
            }

            if (criteria.getArtist() != null && !criteria.getArtist().isBlank()) {
                String artistPattern =
                        "%" + criteria.getArtist().toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        criteriaBuilder.like(
                                normalizeSearchExpression(
                                        criteriaBuilder,
                                        root.get("artistName")
                                ),
                                artistPattern
                        )
                );
            }

            if (criteria.getSearchTerm() != null
                    && !criteria.getSearchTerm().isBlank()) {
                String searchPattern =
                        "%" + criteria.getSearchTerm().toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        normalizeSearchExpression(
                                                criteriaBuilder,
                                                root.get("name")
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        normalizeSearchExpression(
                                                criteriaBuilder,
                                                root.get("artistName")
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        normalizeSearchExpression(
                                                criteriaBuilder,
                                                root.get("location").as(String.class)
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        normalizeSearchExpression(
                                                criteriaBuilder,
                                                root.get("category").as(String.class)
                                        ),
                                        searchPattern
                                )
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private Expression<String> normalizeSearchExpression(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Expression<String> expression
    ) {
        Expression<String> normalized =
                criteriaBuilder.lower(expression);

        for (String character : List.of(" ", "_", "-", "/", ".", ",")) {
            normalized = criteriaBuilder.function(
                    "replace",
                    String.class,
                    normalized,
                    criteriaBuilder.literal(character),
                    criteriaBuilder.literal("")
            );
        }

        return normalized;
    }
}