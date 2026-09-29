package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeClock;
import org.curioone.control.support.RecordingTelemetrySink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TelemetryManager}. */
@DisplayName("TelemetryManager")
class TelemetryManagerTest {

    private RecordingTelemetrySink sink;

    private FakeClock clock;

    private TelemetryManager telemetry;

    @BeforeEach
    void setUp() {
        sink = new RecordingTelemetrySink();
        clock = new FakeClock();
        telemetry = new TelemetryManager(sink, clock);
        CurioConfig.TELEMETRY_PERIOD_MILLIS = 0;
    }

    @AfterEach
    void tearDown() {
        // These are mutable global flags; leaving them changed would leak into other tests.
        CurioConfig.TELEMETRY_PERIOD_MILLIS = 200;
        CurioConfig.DEBUG = false;
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(IllegalArgumentException.class, () -> new TelemetryManager(null, clock));
            assertThrows(IllegalArgumentException.class, () -> new TelemetryManager(sink, null));
        }

        @Test
        @DisplayName("a discarding sink accepts and drops everything")
        void discardingSink() {
            final TelemetryManager manager =
                    new TelemetryManager(TelemetrySink.discarding(), clock);

            manager.add("a", 1.0).add("b", 2.0).updateNow();

            assertEquals(0, manager.size());
        }
    }

    @Nested
    @DisplayName("buffering")
    class Buffering {

        @Test
        @DisplayName("does not write until update")
        void buffersUntilUpdate() {
            telemetry.add("Heading", 1.5);

            assertEquals(0, sink.flushes(), "add must not write; only update does");
            assertEquals(1, telemetry.size());
        }

        @Test
        @DisplayName("one update writes every buffered value, then one flush")
        void batchesIntoOneFlush() {
            telemetry.add("a", 1.0).add("b", 2.0).add("c", "three").updateNow();

            assertEquals(1, sink.flushes(), "three values must cost one SDK call, not three");
            assertEquals(3, sink.captions().size());
        }

        @Test
        @DisplayName("keeps insertion order")
        void keepsOrder() {
            telemetry.add("first", 1.0).add("second", 2.0).add("third", 3.0).updateNow();

            assertEquals("first", sink.captionAt(0));
            assertEquals("second", sink.captionAt(1));
            assertEquals("third", sink.captionAt(2));
        }

        @Test
        @DisplayName("empties the buffer after a flush")
        void emptiesAfterFlush() {
            telemetry.add("a", 1.0).updateNow();
            assertEquals(0, telemetry.size());

            telemetry.add("b", 2.0).updateNow();
            assertEquals(0, telemetry.size());
            // The sink accumulates across flushes, so the property under test is that the first
            // value was not re-sent: "a" appears exactly once.
            assertEquals(1, sink.captions().stream().filter("a"::equals).count());
            assertEquals(2, sink.captions().size());
            assertEquals(2, sink.flushes());
        }

        @Test
        @DisplayName("is chainable")
        void chainable() {
            final TelemetryManager returned = telemetry.add("a", 1.0).add("b", 2.0);

            assertTrue(returned == telemetry, "add must return itself for chaining");
        }

        @Test
        @DisplayName("rejects a null key")
        void rejectsNullKey() {
            assertThrows(IllegalArgumentException.class, () -> telemetry.add(null, 1.0));
        }

        @Test
        @DisplayName("grows past its initial capacity without losing values")
        void growsBeyondInitialCapacity() {
            for (int i = 0; i < 100; i++) {
                telemetry.add("key" + i, i);
            }
            telemetry.updateNow();

            assertEquals(100, sink.captions().size());
        }
    }

    @Nested
    @DisplayName("categories")
    class Categories {

        @Test
        @DisplayName("prefixes the caption")
        void prefixesCaption() {
            telemetry.add(TelemetryCategory.DRIVE, "Heading", 1.0).updateNow();

            assertEquals("[DRIVE] Heading", sink.captionAt(0));
        }

        @Test
        @DisplayName("omits the prefix when no category is given")
        void noCategoryHasNoPrefix() {
            telemetry.add("Heading", 1.0).updateNow();

            assertEquals("Heading", sink.captionAt(0));
        }

        @Test
        @DisplayName("discards DEBUG values unless debug mode is on")
        void discardsDebugByDefault() {
            telemetry.add(TelemetryCategory.DEBUG, "noisy", 1.0).add("useful", 2.0).updateNow();

            assertEquals(1, sink.captions().size(), "only the non-debug value should appear");
            assertEquals("useful", sink.captionAt(0));
        }

        @Test
        @DisplayName("keeps DEBUG values when debug mode is on")
        void keepsDebugWhenEnabled() {
            CurioConfig.DEBUG = true;
            telemetry.add(TelemetryCategory.DEBUG, "noisy", 1.0).updateNow();

            assertEquals(1, sink.captions().size());
            assertEquals("[DEBUG] noisy", sink.captionAt(0));
        }
    }

    @Nested
    @DisplayName("cadence")
    class Cadence {

        @BeforeEach
        void slowDown() {
            CurioConfig.TELEMETRY_PERIOD_MILLIS = 200;
        }

        @Test
        @DisplayName("suppresses an update inside the interval")
        void suppressesWithinInterval() {
            telemetry.add("a", 1.0);
            assertTrue(telemetry.update(), "the first update should flush");

            telemetry.add("b", 2.0);
            clock.advanceMillis(50);
            assertFalse(telemetry.update(), "50 ms into a 200 ms interval, must not flush");
            assertEquals(1, sink.flushes());
        }

        @Test
        @DisplayName("does not lose the suppressed values")
        void doesNotLoseSuppressedValues() {
            telemetry.add("a", 1.0);
            telemetry.update();

            telemetry.add("b", 2.0);
            clock.advanceMillis(50);
            telemetry.update();

            clock.advanceMillis(200);
            telemetry.update();

            assertTrue(
                    sink.captions().contains("b"),
                    "a value buffered during a suppressed update must still be sent later");
        }

        @Test
        @DisplayName("flushes once the interval has passed")
        void flushesAfterInterval() {
            telemetry.add("a", 1.0);
            telemetry.update();

            telemetry.add("b", 2.0);
            clock.advanceMillis(250);
            assertTrue(telemetry.update());
            assertEquals(2, sink.flushes());
        }

        @Test
        @DisplayName("flushes every call when the period is zero")
        void zeroPeriodFlushesEveryTime() {
            CurioConfig.TELEMETRY_PERIOD_MILLIS = 0;

            telemetry.add("a", 1.0);
            assertTrue(telemetry.update());
            telemetry.add("b", 2.0);
            assertTrue(telemetry.update());
            assertEquals(2, sink.flushes());
        }

        @Test
        @DisplayName("does nothing when there is nothing buffered")
        void emptyBufferIsNoOp() {
            assertFalse(telemetry.update());
            assertEquals(0, sink.flushes());
        }

        @Test
        @DisplayName("updateNow ignores the interval")
        void updateNowIgnoresInterval() {
            telemetry.add("a", 1.0);
            telemetry.update();
            telemetry.add("b", 2.0);

            telemetry.updateNow();

            assertEquals(2, sink.flushes());
        }
    }
}
