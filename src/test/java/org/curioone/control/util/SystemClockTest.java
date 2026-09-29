package org.curioone.control.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SystemClock}. */
@DisplayName("SystemClock")
class SystemClockTest {

    @Test
    @DisplayName("advances, since it is backed by a real time source")
    void advances() {
        final Clock clock = new SystemClock();
        final long first = clock.nowNanos();

        // Busy-wait rather than sleep: this is a sanity check on a monotonic source, not a timing
        // assertion, and a sleep would make the suite slow and still flaky on a loaded machine.
        long second = first;
        while (second == first) {
            second = clock.nowNanos();
        }

        assertTrue(second > first, "expected " + second + " > " + first);
    }

    @Test
    @DisplayName("never goes backwards, which is the property controllers depend on")
    void neverGoesBackwards() {
        final Clock clock = new SystemClock();
        long previous = clock.nowNanos();

        for (int i = 0; i < 1_000; i++) {
            final long now = clock.nowNanos();
            assertTrue(now >= previous, "time went backwards: " + now + " < " + previous);
            previous = now;
        }
    }

    @Test
    @DisplayName("resolves to nanoseconds, not milliseconds")
    void isNanoseconds() {
        // A clock accidentally returning millis would still advance, and would silently change
        // every controller's gains by a factor of a million. The magnitude is the only tell.
        assertTrue(Math.abs(new SystemClock().nowNanos()) > 1_000_000_000L);
    }

    @Test
    @DisplayName("can be shared, being stateless")
    void isStateless() {
        assertTrue(
                Modifier.isFinal(SystemClock.class.getModifiers())
                        && SystemClock.class.getDeclaredFields().length == 0,
                "SystemClock must hold no state for sharing between controllers to be safe");
    }

    @Test
    @DisplayName("implements the Clock port")
    void implementsClock() {
        assertTrue(Clock.class.isAssignableFrom(SystemClock.class));
    }
}
