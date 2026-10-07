package org.curioone.control.util;

/**
 * Settles a noisy boolean signal: a trip sensor, a vision "target visible" flag, a debounced
 * button.
 *
 * <p>The output follows the input only once the input has held steady for the configured duration.
 * A flickering line reads as its stable value instead of machine-gunning downstream edges — pair
 * with {@link EdgeDetector} when the edge itself is what matters:
 *
 * <pre>{@code
 * if (pressA.update(debounced.update(gamepad1.a))) {
 *     scheduler.schedule(score);
 * }
 * }</pre>
 *
 * <p>Time comes from an injected {@link Clock}, so a test advances a fake past the settle window
 * deterministically.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.4.0
 */
public final class Debouncer {

    /** Nanoseconds per second, for the duration conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Clock clock;

    private final double stableSeconds;

    private boolean output;

    private boolean candidate;

    private long candidateStartNanos = Long.MIN_VALUE;

    /**
     * Creates a debouncer using the system clock.
     *
     * @param stableSeconds how long the input must hold before the output follows; zero passes the
     *     input straight through
     * @throws IllegalArgumentException if {@code stableSeconds} is negative or NaN
     */
    public Debouncer(double stableSeconds) {
        this(stableSeconds, new SystemClock());
    }

    /**
     * Creates a debouncer with an injected time source.
     *
     * @param stableSeconds how long the input must hold before the output follows; zero passes the
     *     input straight through
     * @param clock the time source
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or {@code stableSeconds}
     *     is negative or NaN
     */
    public Debouncer(double stableSeconds, Clock clock) {
        if (Double.isNaN(stableSeconds) || stableSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "stableSeconds must be non-negative and not NaN but was " + stableSeconds);
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.stableSeconds = stableSeconds;
        this.clock = clock;
    }

    /**
     * Polls the signal.
     *
     * @param input the current raw level
     * @return the settled level
     */
    public boolean update(boolean input) {
        final long now = clock.nowNanos();
        if (input != candidate) {
            candidate = input;
            candidateStartNanos = now;
        }
        if ((now - candidateStartNanos) / NANOS_PER_SECOND >= stableSeconds) {
            output = candidate;
        }
        return output;
    }

    /**
     * Returns the settled level without polling.
     *
     * @return the current output
     */
    public boolean output() {
        return output;
    }
}
