package org.curioone.control.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Timer} and {@link RateLimiter}. */
@DisplayName("Timer and RateLimiter")
class TimingTest {

    private static final double TOLERANCE = 1e-9;

    @Nested
    @DisplayName("Timer")
    class Timers {

        private final FakeClock clock = new FakeClock();

        @Test
        @DisplayName("starts at zero and measures elapsed time exactly")
        void measuresElapsed() {
            final Timer timer = new Timer(clock);

            assertEquals(0.0, timer.elapsedSeconds(), TOLERANCE);
            clock.advanceMillis(1500);
            assertEquals(1.5, timer.elapsedSeconds(), TOLERANCE);
        }

        @Test
        @DisplayName("restart makes elapsed time zero without touching the clock")
        void restartResets() {
            final Timer timer = new Timer(clock);
            clock.advanceSeconds(3.0);

            timer.restart();

            assertEquals(0.0, timer.elapsedSeconds(), TOLERANCE);
            clock.advanceMillis(250);
            assertEquals(0.25, timer.elapsedSeconds(), TOLERANCE);
        }

        @Test
        @DisplayName("hasElapsed is inclusive at the boundary and does not restart")
        void hasElapsedBoundary() {
            final Timer timer = new Timer(clock);
            clock.advanceMillis(999);

            assertFalse(timer.hasElapsed(1.0));
            clock.advanceMillis(1);
            assertTrue(timer.hasElapsed(1.0));
            assertTrue(timer.hasElapsed(0.0));
            assertTrue(timer.hasElapsed(1.0), "polling must not consume the interval");
        }

        @Test
        @DisplayName("rejects a negative or NaN duration, and a null clock")
        void rejectsBadArguments() {
            final Timer timer = new Timer(clock);

            assertThrows(IllegalArgumentException.class, () -> timer.hasElapsed(-0.1));
            assertThrows(IllegalArgumentException.class, () -> timer.hasElapsed(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> new Timer(null));
        }
    }

    @Nested
    @DisplayName("RateLimiter")
    class Limits {

        private final FakeClock clock = new FakeClock();

        @Test
        @DisplayName("the first poll opens immediately")
        void firstPollOpens() {
            final RateLimiter limiter = new RateLimiter(0.1, clock);

            assertTrue(limiter.poll());
        }

        @Test
        @DisplayName("holds until exactly one period has passed")
        void opensAtPeriod() {
            final RateLimiter limiter = new RateLimiter(0.1, clock);

            assertTrue(limiter.poll());
            clock.advanceMillis(99);
            assertFalse(limiter.poll());
            clock.advanceMillis(1);
            assertTrue(limiter.poll());
        }

        @Test
        @DisplayName("a zero period opens on every poll")
        void zeroPeriodAlwaysOpens() {
            final RateLimiter limiter = new RateLimiter(0.0, clock);

            assertTrue(limiter.poll());
            assertTrue(limiter.poll());
            assertTrue(limiter.poll());
        }

        @Test
        @DisplayName("reset makes the next poll open")
        void resetReopens() {
            final RateLimiter limiter = new RateLimiter(10.0, clock);
            assertTrue(limiter.poll());
            assertFalse(limiter.poll());

            limiter.reset();

            assertTrue(limiter.poll());
        }

        @Test
        @DisplayName("reports the remaining wait and zero once ready")
        void reportsTimeUntilReady() {
            final RateLimiter limiter = new RateLimiter(0.5, clock);

            assertEquals(0.0, limiter.timeUntilReadySeconds(), TOLERANCE);
            assertTrue(limiter.poll());
            assertEquals(0.5, limiter.timeUntilReadySeconds(), TOLERANCE);
            clock.advanceMillis(200);
            assertEquals(0.3, limiter.timeUntilReadySeconds(), TOLERANCE);
            clock.advanceMillis(400);
            assertEquals(0.0, limiter.timeUntilReadySeconds(), TOLERANCE);
        }

        @Test
        @DisplayName("rejects a negative, NaN, or infinite period, and a null clock")
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> new RateLimiter(-0.1));
            assertThrows(IllegalArgumentException.class, () -> new RateLimiter(Double.NaN));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new RateLimiter(Double.POSITIVE_INFINITY));
            assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0.1, null));
        }
    }
}
