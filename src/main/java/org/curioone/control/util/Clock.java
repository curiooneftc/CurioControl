package org.curioone.control.util;

/**
 * A source of monotonic time.
 *
 * <p>Every time-dependent component in CurioControl reads time through this interface rather than
 * calling {@link System#nanoTime()} directly. That is what makes {@code PIDController}, telemetry
 * cadence, and the utility classes below fully deterministic in tests: a test supplies a fake clock
 * and advances it by hand, so a behaviour that takes ten seconds of real time is verified in
 * microseconds and never flakes (ADR-009).
 *
 * <p>Production code uses {@link SystemClock}. Tests use a fake.
 *
 * <p><strong>Units:</strong> nanoseconds, on the same monotonic basis as {@link System#nanoTime()}.
 * A monotonic source rather than {@link System#currentTimeMillis()} means a wall-clock adjustment
 * mid-match cannot make an integral term jump.
 *
 * <p><strong>Thread safety:</strong> implementations must be safe to call from multiple threads.
 * {@link SystemClock} is stateless; a fake driven by one test thread need not be.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Clock {

    /**
     * Returns the current time in nanoseconds.
     *
     * <p>Only the difference between two readings is meaningful. The absolute value is an arbitrary
     * origin; code must never compare it against a wall-clock time.
     *
     * @return the current time in nanoseconds, monotonically non-decreasing
     */
    long nowNanos();
}
