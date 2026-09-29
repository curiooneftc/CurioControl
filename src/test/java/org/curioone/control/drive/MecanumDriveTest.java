package org.curioone.control.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.DcMotor;
import org.curioone.control.hardware.Motor;
import org.curioone.control.support.FakeDcMotor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MecanumDrive}. */
@DisplayName("MecanumDrive")
class MecanumDriveTest {

    private static final double DELTA = 1e-9;

    private FakeDcMotor frontLeft;

    private FakeDcMotor frontRight;

    private FakeDcMotor backLeft;

    private FakeDcMotor backRight;

    private MecanumDrive drive;

    @BeforeEach
    void setUp() {
        frontLeft = new FakeDcMotor();
        frontRight = new FakeDcMotor();
        backLeft = new FakeDcMotor();
        backRight = new FakeDcMotor();
        drive =
                new MecanumDrive(
                        "test",
                        new Motor(frontLeft, "fl"),
                        new Motor(frontRight, "fr"),
                        new Motor(backLeft, "bl"),
                        new Motor(backRight, "br"));
    }

    @Nested
    @DisplayName("wheel equations")
    class WheelEquations {

        @Test
        @DisplayName("forward alone drives every wheel the same way")
        void straightForward() {
            drive.mecanum(0.0, 1.0, 0.0);

            assertEquals(1.0, frontLeft.lastPower(), DELTA);
            assertEquals(1.0, frontRight.lastPower(), DELTA);
            assertEquals(1.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("strafe right spins the wheels the way a mecanum base does")
        void strafeRight() {
            drive.mecanum(1.0, 0.0, 0.0);

            // Front-left and back-right lead; the other pair trails.
            assertEquals(1.0, frontLeft.lastPower(), DELTA);
            assertEquals(-1.0, frontRight.lastPower(), DELTA);
            assertEquals(-1.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("rotation turns the robot in place")
        void rotateInPlace() {
            drive.mecanum(0.0, 0.0, 1.0);

            assertEquals(-1.0, frontLeft.lastPower(), DELTA);
            assertEquals(-1.0, frontRight.lastPower(), DELTA);
            assertEquals(1.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("zero input commands nothing")
        void zeroInput() {
            drive.mecanum(0.0, 0.0, 0.0);

            assertEquals(0.0, frontLeft.lastPower(), DELTA);
            assertEquals(0.0, frontRight.lastPower(), DELTA);
            assertEquals(0.0, backLeft.lastPower(), DELTA);
            assertEquals(0.0, backRight.lastPower(), DELTA);
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("leaves output alone when nothing exceeds the limit")
        void leavesSmallInputsAlone() {
            assertEquals(1.0, MecanumDrive.normalizationScale(0.5, -0.5, 0.3, -0.3), DELTA);
        }

        @Test
        @DisplayName("scales a saturated diagonal so no motor exceeds 1")
        void scalesSaturatedDiagonal() {
            // Forward + strafe + rotation all at full: the worst corner needs 1.5.
            final double scale = MecanumDrive.normalizationScale(1.5, -1.5, -0.5, 0.5);

            assertEquals(1.0 / 1.5, scale, DELTA);
            assertEquals(1.0, 1.5 * scale, DELTA, "the saturated motor lands exactly on the limit");
            assertEquals(-1.0, -1.5 * scale, DELTA);
        }

        @Test
        @DisplayName("preserves direction, which per-motor clamping would destroy")
        void preservesDirection() {
            drive.mecanum(1.0, 1.0, 0.0);

            // Before normalization the powers are 2, 0, 0, 2. Clamping each to 1 would turn
            // this into a forward drive; scaling keeps it a diagonal.
            assertEquals(1.0, frontLeft.lastPower(), DELTA);
            assertEquals(0.0, frontRight.lastPower(), DELTA);
            assertEquals(0.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("never commands a motor beyond the power limit")
        void neverExceedsLimits() {
            drive.mecanum(1.0, 1.0, 1.0);

            for (FakeDcMotor motor :
                    new FakeDcMotor[] {frontLeft, frontRight, backLeft, backRight}) {
                assertTrue(
                        Math.abs(motor.lastPower()) <= 1.0 + DELTA,
                        "a motor was commanded past its limit: " + motor.lastPower());
            }
        }

        @Test
        @DisplayName("treats all-zero as unscaled, avoiding a division by zero")
        void allZeroIsSafe() {
            assertEquals(1.0, MecanumDrive.normalizationScale(0, 0, 0, 0), DELTA);
        }
    }

    @Nested
    @DisplayName("field-centric")
    class FieldCentric {

        @Test
        @DisplayName("at zero heading matches robot-centric")
        void zeroHeadingMatches() {
            drive.mecanum(0.2, 0.3, 0.0);
            final double expected = frontLeft.lastPower();

            final MecanumDrive other =
                    new MecanumDrive(
                            "other",
                            new Motor(new FakeDcMotor(), "fl"),
                            new Motor(new FakeDcMotor(), "fr"),
                            new Motor(new FakeDcMotor(), "bl"),
                            new Motor(new FakeDcMotor(), "br"));
            other.mecanum(0.2, 0.3, 0.0);
            other.fieldCentric(0.2, 0.3, 0.0, 0.0);

            assertEquals(expected, other.frontLeft().getPower(), DELTA);
        }

        @Test
        @DisplayName("at a quarter turn, pushing forward moves field-forward, not sideways")
        void forwardIsIndependentOfHeading() {
            final FakeDcMotor[] motors =
                    new FakeDcMotor[] {
                        new FakeDcMotor(), new FakeDcMotor(), new FakeDcMotor(), new FakeDcMotor()
                    };
            final MecanumDrive rotated =
                    new MecanumDrive(
                            "rot",
                            new Motor(motors[0], "fl"),
                            new Motor(motors[1], "fr"),
                            new Motor(motors[2], "bl"),
                            new Motor(motors[3], "br"));

            // The robot is facing field-right (a quarter turn counter-clockwise). Pushing the
            // stick forward must still drive the robot toward field-forward, which from that
            // orientation means strafing to the robot's own left.
            rotated.fieldCentric(0.0, 1.0, 0.0, Math.PI / 2.0);

            assertEquals(-1.0, motors[0].lastPower(), DELTA, "front-left strafes left");
            assertEquals(1.0, motors[1].lastPower(), DELTA);
            assertEquals(1.0, motors[2].lastPower(), DELTA);
            assertEquals(-1.0, motors[3].lastPower(), DELTA, "back-right strafes left");

            // Crucially, it is a strafe, not a rotation: the front pair differs from the back
            // pair's sign. A rotation here would mean the robot turned instead of translating.
            // Compared with a tolerance rather than for exact inequality — the two powers differ
            // by a real margin, and an exact float comparison would make this a flaky assertion.
            assertTrue(
                    Math.abs(motors[0].lastPower() - motors[1].lastPower()) > DELTA,
                    "field-forward at 90 degrees must translate, not spin the robot");
        }

        @Test
        @DisplayName("at zero heading, forward drives the robot straight forward")
        void zeroHeadingDrivesForward() {
            drive.fieldCentric(0.0, 1.0, 0.0, 0.0);

            assertEquals(1.0, frontLeft.lastPower(), DELTA);
            assertEquals(1.0, frontRight.lastPower(), DELTA);
            assertEquals(1.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("rotation is unaffected by heading")
        void rotationIsIndependentOfHeading() {
            drive.fieldCentric(0.0, 0.0, 0.5, 0.0);
            assertEquals(-0.5, frontLeft.lastPower(), DELTA);

            drive.fieldCentric(0.0, 0.0, 0.5, 1.234);
            assertEquals(-0.5, frontLeft.lastPower(), DELTA);
        }
    }

    @Nested
    @DisplayName("direction configuration")
    class Directions {

        @Test
        @DisplayName("inverts one motor without touching the others")
        void invertsOneMotor() {
            drive.setFrontLeftInverted(true);
            drive.mecanum(0.0, 1.0, 0.0);

            assertEquals(-1.0, frontLeft.lastPower(), DELTA);
            assertEquals(1.0, frontRight.lastPower(), DELTA);
            assertEquals(1.0, backLeft.lastPower(), DELTA);
            assertEquals(1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("inverts all four at once")
        void invertsAllFour() {
            drive.setInverted(true, true, true, true);
            drive.mecanum(0.0, 1.0, 0.0);

            assertEquals(-1.0, frontLeft.lastPower(), DELTA);
            assertEquals(-1.0, frontRight.lastPower(), DELTA);
            assertEquals(-1.0, backLeft.lastPower(), DELTA);
            assertEquals(-1.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("reports the configured inversions")
        void reportsInversions() {
            drive.setInverted(true, false, true, false);

            assertTrue(drive.isFrontLeftInverted());
            assertEquals(false, drive.isFrontRightInverted());
            assertTrue(drive.isBackLeftInverted());
            assertEquals(false, drive.isBackRightInverted());
        }

        @Test
        @DisplayName("sets both sides' motor directions")
        void setsMotorDirections() {
            drive.setMotorDirections(
                    com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
                    com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD);

            assertEquals(
                    com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
                    frontLeft.getDirection());
            assertEquals(
                    com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
                    frontRight.getDirection());
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("stop zeroes every motor")
        void stopZeroes() {
            drive.mecanum(1.0, 1.0, 1.0);
            drive.stop();

            assertEquals(0.0, frontLeft.lastPower(), DELTA);
            assertEquals(0.0, frontRight.lastPower(), DELTA);
            assertEquals(0.0, backLeft.lastPower(), DELTA);
            assertEquals(0.0, backRight.lastPower(), DELTA);
        }

        @Test
        @DisplayName("applies a zero-power behavior to all four")
        void appliesZeroPowerBehavior() {
            drive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

            assertEquals(DcMotor.ZeroPowerBehavior.FLOAT, frontLeft.getZeroPowerBehavior());
            assertEquals(DcMotor.ZeroPowerBehavior.FLOAT, backRight.getZeroPowerBehavior());
        }

        @Test
        @DisplayName("rejects a null zero-power behavior")
        void rejectsNullZeroPower() {
            assertThrows(IllegalArgumentException.class, () -> drive.setZeroPowerBehavior(null));
        }

        @Test
        @DisplayName("has a name")
        void hasName() {
            assertEquals("test", drive.name());
            assertNotNull(drive.toString());
        }
    }

    @Test
    @DisplayName("rejects non-finite input rather than writing NaN to a motor")
    void rejectsNonFiniteInput() {
        assertThrows(IllegalArgumentException.class, () -> drive.mecanum(Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> drive.mecanum(0, Double.NaN, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> drive.fieldCentric(0, 0, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("rejects a null motor")
    void rejectsNullMotor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MecanumDrive("x", new Motor(frontLeft, "fl"), null, null, null));
    }
}
