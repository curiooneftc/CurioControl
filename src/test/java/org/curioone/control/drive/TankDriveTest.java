package org.curioone.control.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.curioone.control.hardware.Motor;
import org.curioone.control.support.FakeDcMotor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link TankDrive}. */
@DisplayName("TankDrive")
class TankDriveTest {

    private static final double DELTA = 1e-9;

    private FakeDcMotor leftSdk;

    private FakeDcMotor rightSdk;

    private TankDrive drive;

    @BeforeEach
    void setUp() {
        leftSdk = new FakeDcMotor();
        rightSdk = new FakeDcMotor();
        drive = new TankDrive("tank", new Motor(leftSdk, "left"), new Motor(rightSdk, "right"));
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new TankDrive(
                                    null,
                                    new Motor(new FakeDcMotor(), "l"),
                                    new Motor(new FakeDcMotor(), "r")));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TankDrive("tank", null, new Motor(new FakeDcMotor(), "r")));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TankDrive("tank", new Motor(new FakeDcMotor(), "l"), null));
        }

        @Test
        @DisplayName("exposes its name and motors")
        void exposesNameAndMotors() {
            assertEquals("tank", drive.name());
            assertSame(drive.left(), drive.left());
            assertFalse(drive.isLeftInverted());
            assertFalse(drive.isRightInverted());
        }
    }

    @Nested
    @DisplayName("tank")
    class Tank {

        @ParameterizedTest(name = "tank({0}, {1})")
        @CsvSource({
            "0.0, 0.0",
            "1.0, 1.0",
            "1.0, -1.0",
            "-1.0, 1.0",
            "0.5, 0.25",
        })
        @DisplayName("applies power to each side unchanged")
        void appliesDirectly(double left, double right) {
            drive.tank(left, right);

            assertEquals(left, leftSdk.lastPower(), DELTA);
            assertEquals(right, rightSdk.lastPower(), DELTA);
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("rejects a non-finite input rather than writing it to a motor")
        void rejectsNonFinite(double bad) {
            assertThrows(IllegalArgumentException.class, () -> drive.tank(bad, 0.0));
            assertThrows(IllegalArgumentException.class, () -> drive.tank(0.0, bad));
        }

        @Test
        @DisplayName("leaves motors untouched when an input is rejected")
        void rejectionLeavesMotorsAlone() {
            drive.tank(0.5, 0.5);

            assertThrows(IllegalArgumentException.class, () -> drive.tank(Double.NaN, 0.5));

            assertEquals(0.5, leftSdk.lastPower(), DELTA, "a rejected command must not half-apply");
            assertEquals(0.5, rightSdk.lastPower(), DELTA);
        }
    }

    @Nested
    @DisplayName("drive")
    class Drive {

        @Test
        @DisplayName("forward drives both sides equally")
        void forward() {
            drive.drive(0.0, 1.0, 0.0);

            assertEquals(1.0, leftSdk.lastPower(), DELTA);
            assertEquals(1.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("rotation spins the sides in opposite directions")
        void rotation() {
            drive.drive(0.0, 0.0, 1.0);

            assertEquals(1.0, leftSdk.lastPower(), DELTA);
            assertEquals(-1.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("strafe is discarded rather than turning into a turn")
        void strafeIsIgnored() {
            drive.drive(0.0, 0.0, 0.0);
            drive.drive(1.0, 0.0, 0.0);

            assertEquals(
                    0.0,
                    leftSdk.lastPower(),
                    DELTA,
                    "a tank chassis cannot strafe; pretending to would make the robot turn");
            assertEquals(0.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("clamps the sum so opposing axes cannot exceed full power")
        void clampsCombined() {
            drive.drive(0.0, 1.0, 1.0);

            assertEquals(1.0, leftSdk.lastPower(), DELTA);
            assertEquals(0.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("clamps in the other direction too")
        void clampsCombinedNegative() {
            drive.drive(0.0, -1.0, -1.0);

            assertEquals(-1.0, leftSdk.lastPower(), DELTA);
            assertEquals(0.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("rejects a non-finite forward or rotation")
        void rejectsNonFinite() {
            assertThrows(IllegalArgumentException.class, () -> drive.drive(0.0, Double.NaN, 0.0));
            assertThrows(IllegalArgumentException.class, () -> drive.drive(0.0, 0.0, Double.NaN));
        }
    }

    @Nested
    @DisplayName("stop")
    class Stop {

        @Test
        @DisplayName("zeroes both motors")
        void zeroesBoth() {
            drive.tank(1.0, -1.0);

            drive.stop();

            assertEquals(0.0, leftSdk.lastPower(), DELTA);
            assertEquals(0.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("writes zero even when a side is inverted")
        void writesZeroWhenInverted() {
            drive.setInverted(true, true);

            drive.stop();

            assertEquals(0.0, leftSdk.lastPower(), DELTA, "-0.0 and 0.0 must both read as stopped");
            assertEquals(0.0, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("is safe to call twice")
        void idempotent() {
            drive.stop();
            drive.stop();

            assertEquals(0.0, leftSdk.lastPower(), DELTA);
        }
    }

    @Nested
    @DisplayName("inversion")
    class Inversion {

        @Test
        @DisplayName("flips the left motor's sign")
        void leftInverted() {
            drive.setLeftInverted(true);
            drive.tank(0.75, 0.75);

            assertEquals(-0.75, leftSdk.lastPower(), DELTA);
            assertEquals(0.75, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("flips the right motor's sign")
        void rightInverted() {
            drive.setRightInverted(true);
            drive.tank(0.75, 0.75);

            assertEquals(0.75, leftSdk.lastPower(), DELTA);
            assertEquals(-0.75, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("flips both at once")
        void bothInverted() {
            drive.setInverted(true, true);
            drive.tank(0.75, 0.75);

            assertEquals(-0.75, leftSdk.lastPower(), DELTA);
            assertEquals(-0.75, rightSdk.lastPower(), DELTA);
        }

        @Test
        @DisplayName("reports its state")
        void reportsState() {
            drive.setLeftInverted(true);

            assertTrue(drive.isLeftInverted());
            assertFalse(drive.isRightInverted());
        }

        @Test
        @DisplayName("can be undone")
        void reversible() {
            drive.setInverted(true, true);
            drive.setInverted(false, false);
            drive.tank(0.5, 0.5);

            assertEquals(0.5, leftSdk.lastPower(), DELTA);
            assertEquals(0.5, rightSdk.lastPower(), DELTA);
        }
    }

    @Nested
    @DisplayName("configuration")
    class Configuration {

        @Test
        @DisplayName("passes motor directions through")
        void setsDirections() {
            drive.setMotorDirections(
                    DcMotorSimple.Direction.REVERSE, DcMotorSimple.Direction.FORWARD);

            assertEquals(DcMotorSimple.Direction.REVERSE, leftSdk.getDirection());
            assertEquals(DcMotorSimple.Direction.FORWARD, rightSdk.getDirection());
        }

        @Test
        @DisplayName("rejects a null direction rather than writing it to the SDK")
        void rejectsNullDirection() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> drive.setMotorDirections(null, DcMotorSimple.Direction.FORWARD));
        }

        @Test
        @DisplayName("passes zero-power behaviour to both motors")
        void setsZeroPowerBehavior() {
            drive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

            assertEquals(DcMotor.ZeroPowerBehavior.BRAKE, leftSdk.getZeroPowerBehavior());
            assertEquals(DcMotor.ZeroPowerBehavior.BRAKE, rightSdk.getZeroPowerBehavior());
        }

        @Test
        @DisplayName("rejects a null zero-power behaviour")
        void rejectsNullBehavior() {
            assertThrows(IllegalArgumentException.class, () -> drive.setZeroPowerBehavior(null));
        }
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        assertTrue(drive.toString().contains("tank"), drive.toString());
    }
}
