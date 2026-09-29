package org.curioone.control.util;

/**
 * The production {@link Clock}, backed by {@link System#nanoTime()}.
 *
 * <p>Stateless and therefore safe to share. {@link System#nanoTime()} is monotonic, so readings
 * never go backwards even if the system clock is adjusted.
 *
 * <p><strong>Allocation:</strong> none per call. Safe in the OpMode loop.
 *
 * @since 0.1.0
 */
public final class SystemClock implements Clock {

    @Override
    public long nowNanos() {
        return System.nanoTime();
    }
}
