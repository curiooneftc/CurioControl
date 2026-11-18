package org.curioone.control.util;

/**
 * Gates work to a fixed maximum rate.
 *
 * <p>Not every loop needs to do every job: vision, logging, and expensive telemetry often want 20
 * Hz in a 50 Hz loop. Poll once per loop and do the work only when the gate opens:
 *
 * <pre>{@code
 * if (visionGate.poll()) {
 *     vision.process();
 * }
 * }</pre>
 *
 * <p>The first poll always opens. A period of zero opens on every poll. Time comes from an injected
 * {@link Clock}, so a test advances a fake rather than sleeping.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.5.0
 */
public final class RateLimiter {

    /** Nanoseconds per second, for the period conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Clock clock;

    private final double periodSeconds;

    private final long periodNanos;

    private boolean started;

    private long lastNanos;

    /**
     * Creates a limiter using the system clock.
     *
     * @param periodSeconds minimum seconds between openings; zero opens every poll
     * @throws IllegalArgumentException if {@code periodSeconds} is negative, NaN, or infinite
     */
    public RateLimiter(double periodSeconds) {
        this(periodSeconds, new SystemClock());
    }

    /**
     * Creates a limiter with an injected time source.
     *
     * @param periodSeconds minimum seconds between openings; zero opens every poll
     * @param clock the time source
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or {@code periodSeconds}
     *     is negative, NaN, or infinite
     */
    public RateLimiter(double periodSeconds, Clock clock) {
        if (Double.isNaN(periodSeconds)
                || Double.isInfinite(periodSeconds)
                || periodSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "periodSeconds must be finite and non-negative but was " + periodSeconds);
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.periodSeconds = periodSeconds;
        this.periodNanos = (long) (periodSeconds * NANOS_PER_SECOND);
        this.clock = clock;
    }

    /**
     * Polls the gate, consuming an opening if one is available.
     *
     * <p>Call once per loop. The first call opens; after that a call opens only when at least the
     * configured period has passed since the previous opening.
     *
     * @return {@code true} if the caller may do the gated work now
     */
    public boolean poll() {
        final long now = clock.nowNanos();
        if (!started || now - lastNanos >= periodNanos) {
            started = true;
            lastNanos = now;
            return true;
        }
        return false;
    }

    /**
     * Returns the gate to its initial state, so the next poll opens.
     *
     * <p>Use it when the work is triggered by something other than time — a mode change, a new
     * target — and the first sample should not wait out the previous period.
     */
    public void reset() {
        started = false;
        lastNanos = 0L;
    }

    /**
     * Returns the configured period.
     *
     * @return the minimum seconds between openings
     */
    public double periodSeconds() {
        return periodSeconds;
    }

    /**
     * Returns how long until the gate can open again.
     *
     * @return seconds until ready, or zero if it is ready now
     */
    public double timeUntilReadySeconds() {
        if (!started) {
            return 0.0;
        }
        final long remaining = periodNanos - (clock.nowNanos() - lastNanos);
        return remaining <= 0L ? 0.0 : remaining / NANOS_PER_SECOND;
    }
}
