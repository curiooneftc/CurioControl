package org.curioone.control.support;

import org.curioone.control.util.Clock;

/**
 * A {@link Clock} that a test advances by hand.
 *
 * <p>Lets a test state exactly how much time passed, so a controller's behaviour over ten seconds
 * of loop iterations is verified in microseconds, deterministically, with no sleeping and no
 * tolerance tuning.
 *
 * <pre>{@code
 * FakeClock clock = new FakeClock();
 * PIDController pid = new PIDController(kP, kI, kD, clock);
 * pid.calculate(100, 0);
 * clock.advanceMillis(20);
 * pid.calculate(100, 5);   // exactly 20 ms of integral, every time
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.1.0
 */
public final class FakeClock implements Clock {

    private long nanos;

    /** Creates a clock reading zero. */
    public FakeClock() {
        this.nanos = 0L;
    }

    @Override
    public long nowNanos() {
        return nanos;
    }

    /**
     * Advances the clock.
     *
     * @param millis milliseconds to advance; must not be negative
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    public void advanceMillis(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis must not be negative");
        }
        nanos += millis * 1_000_000L;
    }

    /**
     * Advances the clock.
     *
     * @param seconds seconds to advance; must not be negative
     * @throws IllegalArgumentException if {@code seconds} is negative
     */
    public void advanceSeconds(double seconds) {
        if (seconds < 0.0) {
            throw new IllegalArgumentException("seconds must not be negative");
        }
        nanos += (long) (seconds * 1_000_000_000.0);
    }
}
