package com.plass.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"event", "event.venue"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"event", "event.venue"})
    Optional<Booking> findWithEventById(Long id);

    List<Booking> findByUserIdAndEventIdAndStatus(Long userId, Long eventId, BookingStatus status);

    List<Booking> findByEventIdAndStatusIn(Long eventId, Collection<BookingStatus> statuses);

    /** Oldest overdue holds first; processed in small batches by the expiry job. */
    List<Booking> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(BookingStatus status,
            Instant now);

    boolean existsByReference(String reference);

    // ---- reporting queries for the admin dashboard ----

    long countByStatus(BookingStatus status);

    @Query("select coalesce(sum(b.totalAmount), 0) from Booking b "
            + "where b.status = com.plass.booking.BookingStatus.CONFIRMED")
    BigDecimal totalConfirmedRevenue();

    @Query("select count(i) from BookingItem i "
            + "where i.booking.status = com.plass.booking.BookingStatus.CONFIRMED")
    long countSoldTickets();

    @Query("""
            select new com.plass.booking.EventRevenue(b.event.id, sum(b.totalAmount))
            from Booking b
            where b.status = com.plass.booking.BookingStatus.CONFIRMED and b.event.id in :eventIds
            group by b.event.id
            """)
    List<EventRevenue> revenueByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @Query("""
            select new com.plass.booking.SaleLine(b.confirmedAt, i.price)
            from BookingItem i join i.booking b
            where b.status = com.plass.booking.BookingStatus.CONFIRMED and b.confirmedAt >= :from
            """)
    List<SaleLine> findSaleLinesSince(@Param("from") Instant from);

    @EntityGraph(attributePaths = {"user", "event"})
    List<Booking> findTop8ByStatusOrderByConfirmedAtDesc(BookingStatus status);
}
