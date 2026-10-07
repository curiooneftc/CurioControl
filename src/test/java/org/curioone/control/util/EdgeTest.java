package org.curioone.control.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link EdgeDetector} and {@link Debouncer}. */
@DisplayName("EdgeDetector and Debouncer")
class EdgeTest {

    @Nested
    @DisplayName("EdgeDetector")
    class Edges {

        @Test
        @DisplayName("reports each transition exactly once")
        void reportsTransitionsOnce() {
            final EdgeDetector detector = new EdgeDetector();

            assertEquals(EdgeDetector.Edge.NONE, detector.poll(false));
            assertEquals(EdgeDetector.Edge.RISING, detector.poll(true));
            assertEquals(EdgeDetector.Edge.NONE, detector.poll(true));
            assertEquals(EdgeDetector.Edge.FALLING, detector.poll(false));
            assertEquals(EdgeDetector.Edge.NONE, detector.poll(false));
        }

        @Test
        @DisplayName("an initial high starts high, with no phantom edge")
        void initialHigh() {
            final EdgeDetector detector = new EdgeDetector(true);

            assertEquals(EdgeDetector.Edge.NONE, detector.poll(true));
            assertEquals(EdgeDetector.Edge.FALLING, detector.poll(false));
        }

        @Test
        @DisplayName("reset re-anchors the history")
        void reset() {
            final EdgeDetector detector = new EdgeDetector();

            detector.reset(true);
            assertEquals(EdgeDetector.Edge.NONE, detector.poll(true));
        }
    }

    @Nested
    @DisplayName("Debouncer")
    class Debounce {

        @Test
        @DisplayName("holds the old level until the input settles")
        void holdsUntilSettled() {
            final FakeClock clock = new FakeClock();
            final Debouncer debouncer = new Debouncer(0.1, clock);

            assertFalse(debouncer.update(true));
            clock.advanceMillis(99);
            assertFalse(debouncer.update(true));
            clock.advanceMillis(1);
            assertTrue(debouncer.update(true));
            assertTrue(debouncer.output());
        }

        @Test
        @DisplayName("a flicker shorter than the window never flips the output")
        void flickerIgnored() {
            final FakeClock clock = new FakeClock();
            final Debouncer debouncer = new Debouncer(0.1, clock);

            assertFalse(debouncer.update(true));
            clock.advanceMillis(50);
            assertFalse(debouncer.update(false));
            clock.advanceMillis(200);
            assertFalse(debouncer.update(false));
        }

        @Test
        @DisplayName("zero delay passes the input straight through")
        void zeroDelayPassesThrough() {
            final Debouncer debouncer = new Debouncer(0.0, new FakeClock());

            assertTrue(debouncer.update(true));
            assertFalse(debouncer.update(false));
        }

        @Test
        @DisplayName("rejects negative durations and null clocks")
        void rejectsBadArguments() {
            final FakeClock clock = new FakeClock();

            assertThrows(IllegalArgumentException.class, () -> new Debouncer(-1.0, clock));
            assertThrows(IllegalArgumentException.class, () -> new Debouncer(Double.NaN, clock));
            assertThrows(IllegalArgumentException.class, () -> new Debouncer(0.1, null));
        }
    }
}
