package com.plass.event;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByRowLabelAscSeatNumberAsc(Long eventId);

    List<Seat> findByEventIdAndIdIn(Long eventId, Collection<Long> ids);

    /** Counts for many events in one query, avoiding an N+1 problem on the event list page. */
    @Query("""
            select new com.plass.event.SeatCounts(
                s.event.id,
                count(s),
                sum(case when s.status = com.plass.event.SeatStatus.AVAILABLE then 1 else 0 end),
                sum(case when s.status = com.plass.event.SeatStatus.HELD then 1 else 0 end),
                sum(case when s.status = com.plass.event.SeatStatus.BOOKED then 1 else 0 end))
            from Seat s
            where s.event.id in :eventIds
            group by s.event.id
            """)
    List<SeatCounts> countByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
