package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.RecordingTelemetrySink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Subsystem}. */
@DisplayName("Subsystem")
class SubsystemTest {

    /** A subsystem that overrides nothing, to exercise the defaults. */
    private static final class Bare extends Subsystem {}

    /** A subsystem overriding every hook. */
    private static final class Everything extends Subsystem {
        private int inits;
        private int loops;
        private int stops;
        private int publishes;

        @Override
        public void init() {
            inits++;
        }

        @Override
        public void loop() {
            loops++;
        }

        @Override
        protected void publishTelemetry(TelemetryManager telemetry) {
            publishes++;
        }

        @Override
        public void stop() {
            stops++;
        }
    }

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("every hook is a no-op, so a subsystem overrides only what it needs")
        void hooksAreNoOps() {
            final Bare bare = new Bare();

            bare.init();
            bare.loop();
            bare.publishTelemetry(
                    new TelemetryManager(
                            new RecordingTelemetrySink(),
                            new org.curioone.control.support.FakeClock()));
            bare.stop();
        }

        @Test
        @DisplayName("publishTelemetry receives a usable manager")
        void telemetryIsUsable() {
            final RecordingTelemetrySink sink = new RecordingTelemetrySink();
            final TelemetryManager telemetry =
                    new TelemetryManager(sink, new org.curioone.control.support.FakeClock());
            final Everything subsystem = new Everything();

            subsystem.publishTelemetry(telemetry);
            telemetry.add("x", 1.0).updateNow();

            assertEquals(1, subsystem.publishes);
            assertTrue(sink.captions().contains("x"));
        }
    }

    @Nested
    @DisplayName("naming")
    class Naming {

        @Test
        @DisplayName("defaults to the simple class name")
        void defaultsToClassName() {
            assertEquals("Bare", new Bare().name());
        }

        @Test
        @DisplayName("uses an explicitly set name")
        void usesExplicitName() {
            final Bare bare = new Bare();
            bare.setName("left intake");

            assertEquals("left intake", bare.name());
        }

        @Test
        @DisplayName("falls back to the class name when the name is cleared")
        void nullNameFallsBack() {
            final Bare bare = new Bare();
            bare.setName("temporary");
            bare.setName(null);

            assertEquals("Bare", bare.name());
        }

        @Test
        @DisplayName("never returns null, even before registration")
        void neverNull() {
            assertFalse(new Everything().name().isEmpty());
        }
    }

    @Test
    @DisplayName("an override is what runs, not the base implementation")
    void overridesWin() {
        final Everything subsystem = new Everything();

        subsystem.init();
        subsystem.loop();
        subsystem.stop();

        assertEquals(1, subsystem.inits);
        assertEquals(1, subsystem.loops);
        assertEquals(1, subsystem.stops);
    }
}
