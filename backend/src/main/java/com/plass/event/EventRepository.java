package com.plass.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** Same as the inherited method, but fetches the venue in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = "venue")
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    Optional<Event> findWithVenueById(Long id);

    @EntityGraph(attributePaths = "venue")
    Page<Event> findAllBy(Pageable pageable);

    @Query("""
            select distinct v.city from Event e join e.venue v
            where e.status = com.plass.event.EventStatus.PUBLISHED and e.startsAt > :now
            order by v.city
            """)
    List<String> findCitiesWithUpcomingEvents(@Param("now") Instant now);

    long countByStatusAndStartsAtAfter(EventStatus status, Instant instant);
}
