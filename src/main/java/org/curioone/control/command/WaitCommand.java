package org.curioone.control.command;

import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * A command that does nothing for a fixed duration, then finishes.
 *
 * <p>The pause between "arm arrived" and "release the sample" is real robot behaviour — mechanisms
 * settle — and expressing it as a command keeps the autonomous routine a single composition instead
 * of a composition plus stray timers. Time comes from an injected {@link Clock}, so a test advances
 * a fake past the deadline deterministically.
 *
 * @since 0.3.0
 */
public final class WaitCommand implements Command {

    /** Nanoseconds per second, for the elapsed-time conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final String name;

    private final double seconds;

    private final Clock clock;

    private long startNanos;

    private boolean started;

    /**
     * Creates a wait with a default name, using the system clock.
     *
     * @param seconds how long to wait, in seconds; must not be negative
     * @throws IllegalArgumentException if {@code seconds} is negative or NaN
     */
    public WaitCommand(double seconds) {
        this("Wait", seconds, new SystemClock());
    }

    /**
     * Creates a wait with a default name and an injected time source.
     *
     * @param seconds how long to wait, in seconds; must not be negative
     * @param clock the time source
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or {@code seconds} is
     *     negative or NaN
     */
    public WaitCommand(double seconds, Clock clock) {
        this("Wait", seconds, clock);
    }

    /**
     * Creates a named wait with an injected time source.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param seconds how long to wait, in seconds; must not be negative
     * @param clock the time source
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code clock} is
     *     {@code null}, or {@code seconds} is negative or NaN
     */
    public WaitCommand(String name, double seconds, Clock clock) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (Double.isNaN(seconds) || seconds < 0.0) {
            throw new IllegalArgumentException(
                    "seconds must be non-negative and not NaN but was " + seconds);
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.name = name;
        this.seconds = seconds;
        this.clock = clock;
    }

    @Override
    public void initialize() {
        startNanos = clock.nowNanos();
        started = true;
    }

    @Override
    public boolean isFinished() {
        if (!started) {
            return false;
        }
        return (clock.nowNanos() - startNanos) / NANOS_PER_SECOND >= seconds;
    }

    @Override
    public void end(boolean interrupted) {
        started = false;
    }

    @Override
    public String name() {
        return name;
    }
}
