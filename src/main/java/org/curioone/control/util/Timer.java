package org.curioone.control.util;

/**
 * Measures elapsed time from a restarted origin.
 *
 * <p>The common shape in robot code is "do this for three seconds" or "wait until the arm has had
 * 500 ms to settle". A timer makes that explicit and testable:
 *
 * <pre>{@code
 * if (timer.hasElapsed(3.0)) {
 *     scheduler.schedule(nextStep);
 *     timer.restart();
 * }
 * }</pre>
 *
 * <p>Time comes from an injected {@link Clock}, so a test advances a fake clock and asserts exact
 * behaviour without sleeping.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.5.0
 */
public final class Timer {

    /** Nanoseconds per second, for the duration conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Clock clock;

    private long startNanos;

    /** Creates a timer using the system clock and starting now. */
    public Timer() {
        this(new SystemClock());
    }

    /**
     * Creates a timer with an injected time source, starting now.
     *
     * @param clock the time source
     * @throws IllegalArgumentException if {@code clock} is {@code null}
     */
    public Timer(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
        this.startNanos = clock.nowNanos();
    }

    /** Restarts the timer, making elapsed time zero from this moment. */
    public void restart() {
        startNanos = clock.nowNanos();
    }

    /**
     * Returns elapsed time since construction or the last {@link #restart()}.
     *
     * @return elapsed nanoseconds, never negative
     */
    public long elapsedNanos() {
        return clock.nowNanos() - startNanos;
    }

    /**
     * Returns elapsed time since construction or the last {@link #restart()}.
     *
     * @return elapsed seconds
     */
    public double elapsedSeconds() {
        return elapsedNanos() / NANOS_PER_SECOND;
    }

    /**
     * Whether at least the given duration has elapsed.
     *
     * <p>Does not restart: poll once per loop and restart explicitly when the next interval should
     * begin.
     *
     * @param seconds the duration to test, in seconds; zero is always true
     * @return {@code true} when elapsed time is at least {@code seconds}
     * @throws IllegalArgumentException if {@code seconds} is negative or NaN
     */
    public boolean hasElapsed(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0.0) {
            throw new IllegalArgumentException(
                    "seconds must be non-negative and not NaN but was " + seconds);
        }
        return elapsedSeconds() >= seconds;
    }
}
