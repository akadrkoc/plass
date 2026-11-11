package com.plass.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move forward, e.g. to make a seat hold expire without waiting 10 minutes. */
public class MutableClock extends Clock {

    private volatile Instant fixedInstant;

    @Override
    public Instant instant() {
        Instant fixed = fixedInstant;
        return fixed != null ? fixed : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant(), zone);
    }

    public void advance(Duration duration) {
        fixedInstant = instant().plus(duration);
    }

    public void reset() {
        fixedInstant = null;
    }
}
