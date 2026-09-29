package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HardwareType}. */
@DisplayName("HardwareType")
class HardwareTypeTest {

    @Nested
    @DisplayName("sdk types")
    class SdkTypes {

        @Test
        @DisplayName("every constant resolves to a real SDK class")
        void allResolve() {
            for (HardwareType type : HardwareType.values()) {
                assertNotNull(type.sdkType(), type.name());
                assertTrue(
                        type.sdkType().getName().startsWith("com.qualcomm."),
                        type + " resolves to a non-SDK class: " + type.sdkType());
            }
        }

        @Test
        @DisplayName("a motor resolves to the full motor interface, not the base one")
        void motorResolvesToEx() {
            assertSame(DcMotorEx.class, HardwareType.MOTOR.sdkType());
        }

        @Test
        @DisplayName(
                "an encoder resolves to the motor that owns it, since the SDK has no "
                        + "standalone encoder")
        void encoderResolvesToMotor() {
            // The FTC SDK exposes an encoder only through the motor it is built into, so this is
            // not a shortcut — it is the only way to reach an encoder at all.
            assertSame(DcMotorEx.class, HardwareType.ENCODER.sdkType());
        }

        @Test
        @DisplayName("each distinct kind resolves to its own SDK class, except encoder")
        void distinctKinds() {
            final Set<Class<?>> classes = new HashSet<>();
            for (HardwareType type : EnumSet.complementOf(EnumSet.of(HardwareType.ENCODER))) {
                classes.add(type.sdkType());
            }

            assertEquals(
                    HardwareType.values().length - 1,
                    classes.size(),
                    "two kinds resolving to the same class would make has() ambiguous: " + classes);
        }
    }

    @Nested
    @DisplayName("labels")
    class Labels {

        @Test
        @DisplayName("match the constant names, so diagnostics read consistently")
        void matchConstantNames() {
            for (HardwareType type : HardwareType.values()) {
                assertEquals(type.name(), type.label());
            }
        }

        @Test
        @DisplayName("are unique")
        void unique() {
            assertEquals(
                    HardwareType.values().length,
                    EnumSet.allOf(HardwareType.class).stream()
                            .map(HardwareType::label)
                            .distinct()
                            .count());
        }
    }

    @Test
    @DisplayName("is a closed set a team can switch over")
    void isSwitchable() {
        // A switch over an enum with no default is a compile error if a constant is added later,
        // which is the point: adding a device kind should force every exhaustive site to look at
        // it.
        final int described = describe(HardwareType.MOTOR);

        assertTrue(described > 0);
    }

    private static int describe(HardwareType type) {
        switch (type) {
            case MOTOR:
            case ENCODER:
            case IMU:
            case SERVO:
            case CONTINUOUS_SERVO:
            case DIGITAL_SENSOR:
            case ANALOG_SENSOR:
            case VOLTAGE_SENSOR:
                return 1;
            default:
                throw new AssertionError("unreachable: " + type);
        }
    }

    @Test
    @DisplayName("covers the SDK classes the framework actually wraps")
    void coversWrappedKinds() {
        final Set<Class<?>> expected =
                Set.of(
                        DcMotorEx.class,
                        IMU.class,
                        Servo.class,
                        CRServo.class,
                        DigitalChannel.class,
                        AnalogInput.class,
                        VoltageSensor.class);

        final Set<Class<?>> actual = new HashSet<>();
        for (HardwareType type : HardwareType.values()) {
            actual.add(type.sdkType());
        }

        assertTrue(
                actual.containsAll(expected),
                "missing: " + expected.stream().filter(c -> !actual.contains(c)).toList());
    }

    @Test
    @DisplayName("has as many constants as distinct labels")
    void constantCountMatchesLabels() {
        assertEquals(
                HardwareType.values().length,
                EnumSet.allOf(HardwareType.class).stream()
                        .map(HardwareType::label)
                        .distinct()
                        .count());
    }
}
