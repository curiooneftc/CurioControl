package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import org.curioone.control.support.FakeDcMotor;
import org.curioone.control.support.FakeHardwareSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link HardwareRegistry}. */
@DisplayName("HardwareRegistry")
class HardwareRegistryTest {

    private FakeHardwareSource hardware;

    private HardwareRegistry registry;

    @BeforeEach
    void setUp() {
        hardware = new FakeHardwareSource();
        registry = new HardwareRegistry(hardware);
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects a null hardware map")
        void rejectsNullHardwareMap() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new HardwareRegistry((com.qualcomm.robotcore.hardware.HardwareMap) null));
        }

        @Test
        @DisplayName("rejects a null source")
        void rejectsNullSource() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new HardwareRegistry((HardwareSource) null));
        }

        @Test
        @DisplayName("has no SDK hardware map when built from a plain source")
        void noSdkMapFromPlainSource() {
            assertNull(registry.hardwareMap());
        }

        @Test
        @DisplayName("a wrong-type config fails the same way as a missing one")
        void wrongTypeFailsLikeMissing() {
            // A name configured as a servo but requested as a motor is indistinguishable from a
            // missing device at the call site, so it must produce the same actionable message.
            hardware.add("arm", Mockito.mock(Servo.class));

            assertThrows(CurioException.class, () -> registry.motor("arm"));
        }
    }

    @Nested
    @DisplayName("resolution")
    class Resolution {

        @Test
        @DisplayName("returns the configured device")
        void returnsDevice() {
            final FakeDcMotor motor = hardware.addMotor("arm");

            assertSame(motor, registry.motor("arm"));
        }

        @Test
        @DisplayName("caches, so a repeat lookup does not hit the source again")
        void caches() {
            final FakeDcMotor motor = hardware.addMotor("arm");

            assertSame(registry.motor("arm"), registry.motor("arm"));
            assertEquals(1, hardware.lookups(), "a cached lookup must not consult the source");
            assertNotNull(motor);
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class, () -> registry.require(DcMotorEx.class, null));
            assertThrows(IllegalArgumentException.class, () -> registry.require(null, "arm"));
        }
    }

    @Nested
    @DisplayName("missing devices")
    class MissingDevices {

        @Test
        @DisplayName("throws rather than returning null")
        void throwsNotNull() {
            assertThrows(CurioException.class, () -> registry.motor("missing"));
        }

        @Test
        @DisplayName("names the device, so the failure is actionable")
        void namesTheDevice() {
            final String message =
                    assertThrows(CurioException.class, () -> registry.motor("armMotor"))
                            .getMessage();

            assertTrue(
                    message.contains("armMotor"),
                    "the message must name the device the team asked for: " + message);
        }

        @Test
        @DisplayName("uses the documented error format")
        void usesDocumentedFormat() {
            final String message =
                    assertThrows(CurioException.class, () -> registry.motor("armMotor"))
                            .getMessage();
            final String[] lines = message.split("\\R");

            assertEquals("[CurioControl] ERROR", lines[0].trim());
            assertTrue(lines[1].contains("Missing hardware device: armMotor"), lines[1]);
            assertTrue(lines[2].contains("Expected configuration name"), lines[2]);
        }

        @Test
        @DisplayName("includes the expected type, which disambiguates a wrong-type config")
        void includesExpectedType() {
            final String message =
                    assertThrows(CurioException.class, () -> registry.require(Servo.class, "thing"))
                            .getMessage();

            assertTrue(
                    message.contains("Servo"),
                    "a device configured as the wrong type looks identical from the code, so the "
                            + "message must say what was expected: "
                            + message);
        }

        @Test
        @DisplayName("a type mismatch is reported exactly like a missing device")
        void typeMismatchReadsAsMissing() {
            // The SDK's HardwareMap returns null for a type mismatch rather than throwing, so a
            // device named "arm" configured as a servo must produce the same clear failure.
            hardware.addMotor("arm");

            assertThrows(CurioException.class, () -> registry.require(Servo.class, "arm"));
        }
    }

    @Nested
    @DisplayName("probing")
    class Probing {

        @Test
        @DisplayName("reports a configured device as present")
        void presentDevice() {
            hardware.addMotor("arm");

            assertTrue(registry.has(DcMotorEx.class, "arm"));
        }

        @Test
        @DisplayName("reports a missing device as absent, without throwing")
        void absentDevice() {
            assertFalse(registry.has(DcMotorEx.class, "arm"));
        }

        @Test
        @DisplayName("treats null arguments as absent rather than throwing")
        void nullsAreAbsent() {
            assertFalse(registry.has(DcMotorEx.class, null));
            assertFalse(registry.has(null, "arm"));
        }
    }

    @Nested
    @DisplayName("diagnostics")
    class Diagnostics {

        @Test
        @DisplayName("lists resolved names in first-use order")
        void listsResolvedNames() {
            hardware.addMotor("zeta");
            hardware.addMotor("alpha");

            registry.motor("zeta");
            registry.motor("alpha");

            assertEquals("[zeta, alpha]", registry.resolvedNames().toString());
        }

        @Test
        @DisplayName("is empty before anything is resolved")
        void emptyInitially() {
            assertTrue(registry.resolvedNames().isEmpty());
        }

        @Test
        @DisplayName("is unmodifiable")
        void unmodifiable() {
            assertThrows(
                    UnsupportedOperationException.class, () -> registry.resolvedNames().add("x"));
        }

        @Test
        @DisplayName("does not list devices that failed to resolve")
        void omitsFailures() {
            assertThrows(CurioException.class, () -> registry.motor("missing"));

            assertTrue(registry.resolvedNames().isEmpty());
        }
    }
}
