package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeHardwareSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

/** Unit tests for {@link Servo}. */
@DisplayName("Servo")
class ServoTest {

    private static final double DELTA = 1e-9;

    private com.qualcomm.robotcore.hardware.Servo sdk;

    private Servo wrapper;

    @BeforeEach
    void setUp() {
        sdk = Mockito.mock(com.qualcomm.robotcore.hardware.Servo.class);
        wrapper = new Servo(sdk, "claw");
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Servo((com.qualcomm.robotcore.hardware.Servo) null, "claw"));
            assertThrows(IllegalArgumentException.class, () -> new Servo(sdk, null));
        }

        @Test
        @DisplayName("resolves from a registry")
        void resolvesFromRegistry() {
            final FakeHardwareSource hardware = new FakeHardwareSource();
            hardware.add("claw", sdk);

            final Servo resolved =
                    new Servo(new org.curioone.control.core.HardwareRegistry(hardware), "claw");

            assertSame(sdk, resolved.getSdkObject());
        }

        @Test
        @DisplayName("keeps its name and exposes the escape hatch")
        void exposesNameAndSdk() {
            assertEquals("claw", wrapper.name());
            assertSame(sdk, wrapper.getSdkObject());
        }
    }

    @Nested
    @DisplayName("position")
    class Position {

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.001, 0.5, 0.999, 1.0})
        @DisplayName("accepts a position in range")
        void acceptsInRange(double position) {
            wrapper.setPosition(position);

            Mockito.verify(sdk).setPosition(position);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.001, 1.001, 2.0, -2.0, Double.NaN})
        @DisplayName("rejects a position out of range rather than silently clamping")
        void rejectsOutOfRange(double position) {
            // The decision (ADR-011) is to throw, not clamp: a clamped value means a limit
            // expression was miscalculated, and hiding that is how a mechanism drifts.
            assertThrows(IllegalArgumentException.class, () -> wrapper.setPosition(position));
            Mockito.verify(sdk, Mockito.never()).setPosition(Mockito.anyDouble());
        }

        @Test
        @DisplayName("names the servo in the rejection, so the cause is findable")
        void rejectionNamesServo() {
            final String message =
                    assertThrows(IllegalArgumentException.class, () -> wrapper.setPosition(1.5))
                            .getMessage();

            assertTrue(message.contains("claw"), message);
        }

        @Test
        @DisplayName("reads back the commanded position")
        void readsPosition() {
            Mockito.when(sdk.getPosition()).thenReturn(0.42);

            assertEquals(0.42, wrapper.getPosition(), DELTA);
        }
    }

    @Nested
    @DisplayName("configuration")
    class Configuration {

        @Test
        @DisplayName("passes direction through")
        void setsDirection() {
            wrapper.setDirection(com.qualcomm.robotcore.hardware.Servo.Direction.REVERSE);

            Mockito.verify(sdk)
                    .setDirection(com.qualcomm.robotcore.hardware.Servo.Direction.REVERSE);
        }

        @Test
        @DisplayName("rejects a null direction")
        void rejectsNullDirection() {
            assertThrows(IllegalArgumentException.class, () -> wrapper.setDirection(null));
        }

        @Test
        @DisplayName("passes range scaling through unchanged")
        void scalesRange() {
            wrapper.scaleRange(0.1, 0.9);

            Mockito.verify(sdk).scaleRange(0.1, 0.9);
        }
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        assertTrue(wrapper.toString().contains("claw"), wrapper.toString());
    }
}
