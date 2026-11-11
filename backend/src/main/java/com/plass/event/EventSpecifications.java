package com.plass.event;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/** Small, composable query building blocks for the event search. */
final class EventSpecifications {

    private EventSpecifications() {
    }

    static Specification<Event> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED);
    }

    static Specification<Event> startsAfter(Instant instant) {
        return (root, query, cb) -> cb.greaterThan(root.<Instant>get("startsAt"), instant);
    }

    static Specification<Event> inCity(String city) {
        return (root, query, cb) -> cb.equal(root.get("venue").get("city"), city);
    }

    static Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** Case-insensitive match on the event title or venue name. */
    static Specification<Event> matchesText(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("venue").<String>get("name")), pattern, '\\'));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
