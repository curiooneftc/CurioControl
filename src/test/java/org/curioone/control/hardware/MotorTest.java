package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.curioone.control.support.FakeDcMotor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Motor}, against a fake motor. */
@DisplayName("Motor")
class MotorTest {

    private static final double DELTA = 1e-9;

    private FakeDcMotor sdk;

    private Motor motor;

    @BeforeEach
    void setUp() {
        sdk = new FakeDcMotor();
        motor = new Motor(sdk, "arm");
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            // The cast is required: a bare null is ambiguous between the DcMotorEx and
            // HardwareMap constructors, and ambiguity in a test is a hint that the API is
            // over-loaded for the common case.
            assertThrows(IllegalArgumentException.class, () -> new Motor((DcMotorEx) null, "arm"));
            assertThrows(IllegalArgumentException.class, () -> new Motor(sdk, null));
        }

        @Test
        @DisplayName("keeps the name for diagnostics")
        void keepsName() {
            assertEquals("arm", motor.name());
            assertTrue(motor.toString().contains("arm"), "toString should name the motor");
        }

        @Test
        @DisplayName("exposes the underlying SDK object")
        void exposesSdkObject() {
            assertSame(sdk, motor.getSdkObject());
        }
    }

    @Nested
    @DisplayName("power")
    class Power {

        @Test
        @DisplayName("passes a valid power straight through")
        void passesValidPower() {
            motor.setPower(0.5);
            assertEquals(0.5, motor.getPower(), DELTA);
        }

        @Test
        @DisplayName("stop zeroes the motor")
        void stopZeroes() {
            motor.setPower(0.5);
            motor.stop();
            assertEquals(0.0, sdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("accepts exactly -1 and 1")
        void acceptsBoundaries() {
            motor.setPower(1.0);
            assertEquals(1.0, motor.getPower(), DELTA);
            motor.setPower(-1.0);
            assertEquals(-1.0, sdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("throws above 1 rather than clamping silently")
        void throwsAboveOne() {
            final IllegalArgumentException thrown =
                    assertThrows(IllegalArgumentException.class, () -> motor.setPower(1.5));

            assertTrue(
                    thrown.getMessage().contains("arm"),
                    "the message should name the motor, so the culprit is identifiable");
            assertTrue(
                    thrown.getMessage().contains("1.5"),
                    "the message should include the offending value");
        }

        @Test
        @DisplayName("throws below -1")
        void throwsBelowMinusOne() {
            assertThrows(IllegalArgumentException.class, () -> motor.setPower(-1.5));
        }

        @Test
        @DisplayName("throws on NaN")
        void throwsOnNaN() {
            assertThrows(IllegalArgumentException.class, () -> motor.setPower(Double.NaN));
        }

        @Test
        @DisplayName("throws on infinity")
        void throwsOnInfinity() {
            assertThrows(
                    IllegalArgumentException.class, () -> motor.setPower(Double.POSITIVE_INFINITY));
        }
    }

    @Nested
    @DisplayName("position and velocity")
    class PositionAndVelocity {

        @Test
        @DisplayName("sets and reads a target position")
        void targetPosition() {
            motor.setTargetPosition(500);
            assertEquals(500, motor.getTargetPosition());
        }

        @Test
        @DisplayName("sets and reads a velocity")
        void velocity() {
            motor.setVelocity(1000);
            assertEquals(1000.0, motor.getVelocity(), DELTA);
        }

        @Test
        @DisplayName("reads the current position")
        void currentPosition() {
            sdk.setPositionForTest(250);
            assertEquals(250, motor.getPosition());
        }

        @Test
        @DisplayName("reports busy state")
        void busy() {
            assertEquals(false, motor.isBusy());
        }
    }

    @Nested
    @DisplayName("configuration")
    class Configuration {

        @Test
        @DisplayName("resets the encoder")
        void resetsEncoder() {
            sdk.setPositionForTest(1234);
            motor.resetEncoder();
            assertEquals(0, motor.getPosition(), "resetEncoder must zero the reading");
        }

        @Test
        @DisplayName("sets the direction")
        void setsDirection() {
            motor.setDirection(DcMotorSimple.Direction.REVERSE);
            assertEquals(DcMotorSimple.Direction.REVERSE, sdk.getDirection());
        }

        @Test
        @DisplayName("rejects a null direction")
        void rejectsNullDirection() {
            assertThrows(IllegalArgumentException.class, () -> motor.setDirection(null));
        }

        @Test
        @DisplayName("sets and reads the zero-power behavior")
        void zeroPowerBehavior() {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            assertEquals(DcMotor.ZeroPowerBehavior.FLOAT, motor.getZeroPowerBehavior());
        }

        @Test
        @DisplayName("rejects a null zero-power behavior")
        void rejectsNullZeroPower() {
            assertThrows(IllegalArgumentException.class, () -> motor.setZeroPowerBehavior(null));
        }

        @Test
        @DisplayName("sets and reads the run mode")
        void runMode() {
            motor.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
            assertEquals(DcMotor.RunMode.RUN_TO_POSITION, motor.getRunMode());
        }

        @Test
        @DisplayName("rejects a null run mode")
        void rejectsNullRunMode() {
            assertThrows(IllegalArgumentException.class, () -> motor.setRunMode(null));
        }
    }

    @Nested
    @DisplayName("encoder view")
    class EncoderView {

        @Test
        @DisplayName("is backed by the same motor")
        void backedBySameMotor() {
            final Encoder encoder = motor.encoder();

            assertNotNull(encoder);
            assertSame(sdk, encoder.getSdkObject());
        }

        @Test
        @DisplayName("reads the motor's position")
        void readsMotorPosition() {
            sdk.setPositionForTest(100);
            assertEquals(100, motor.encoder().getPosition());
        }
    }
}
