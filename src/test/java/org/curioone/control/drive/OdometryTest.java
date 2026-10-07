package org.curioone.control.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.curioone.control.hardware.Encoder;
import org.curioone.control.hardware.HeadingSource;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Units;
import org.curioone.control.support.FakeDcMotor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MecanumOdometry} and {@link TankOdometry}. */
@DisplayName("Odometry")
class OdometryTest {

    private static final double DELTA = 1e-6;

    private static final double TICKS_PER_REV = 1000.0;

    private static final double WHEEL_DIAMETER_MM = 100.0;

    /** Inches per thousand ticks at the test geometry. */
    private static final double INCHES_PER_KTICKS =
            Units.millimetersToInches(Math.PI * WHEEL_DIAMETER_MM);

    private final double[] heading = new double[1];

    private final HeadingSource source = () -> heading[0];

    private FakeDcMotor frontLeft;

    private FakeDcMotor frontRight;

    private FakeDcMotor backLeft;

    private FakeDcMotor backRight;

    private Encoder fl;

    private Encoder fr;

    private Encoder bl;

    private Encoder br;

    @BeforeEach
    void setUp() {
        heading[0] = 0.0;
        frontLeft = new FakeDcMotor();
        frontRight = new FakeDcMotor();
        backLeft = new FakeDcMotor();
        backRight = new FakeDcMotor();
        fl = new Encoder(frontLeft, "fl");
        fr = new Encoder(frontRight, "fr");
        bl = new Encoder(backLeft, "bl");
        br = new Encoder(backRight, "br");
    }

    private MecanumOdometry mecanum() {
        return new MecanumOdometry(fl, fr, bl, br, source, TICKS_PER_REV, WHEEL_DIAMETER_MM);
    }

    private void setTicks(int flTicks, int frTicks, int blTicks, int brTicks) {
        frontLeft.setPositionForTest(flTicks);
        frontRight.setPositionForTest(frTicks);
        backLeft.setPositionForTest(blTicks);
        backRight.setPositionForTest(brTicks);
    }

    @Nested
    @DisplayName("MecanumOdometry")
    class Mecanum {

        @Test
        @DisplayName("driving forward advances along the heading")
        void drivesForward() {
            final MecanumOdometry odometry = mecanum();

            odometry.update();
            setTicks(1000, 1000, 1000, 1000);
            final Pose2d pose = odometry.update();

            assertEquals(0.0, pose.getX(), DELTA);
            assertEquals(INCHES_PER_KTICKS, pose.getY(), DELTA);
            assertEquals(0.0, pose.getRotation().getRadians(), DELTA);
        }

        @Test
        @DisplayName("strafing moves sideways without rotating")
        void strafesSideways() {
            final MecanumOdometry odometry = mecanum();

            odometry.update();
            setTicks(1000, -1000, -1000, 1000);
            final Pose2d pose = odometry.update();

            assertEquals(INCHES_PER_KTICKS, pose.getX(), DELTA);
            assertEquals(0.0, pose.getY(), DELTA);
            assertEquals(0.0, pose.getRotation().getRadians(), DELTA);
        }

        @Test
        @DisplayName("translation rotates into the field frame by the heading")
        void fieldRelative() {
            final MecanumOdometry odometry = mecanum();
            heading[0] = Math.PI / 2.0;

            odometry.update();
            setTicks(1000, 1000, 1000, 1000);
            final Pose2d pose = odometry.update();

            // Facing field-right, robot-forward is field +x.
            assertEquals(INCHES_PER_KTICKS, pose.getX(), DELTA);
            assertEquals(0.0, pose.getY(), DELTA);
        }

        @Test
        @DisplayName("spinning in place changes heading without translating")
        void spinsInPlace() {
            final MecanumOdometry odometry = mecanum();

            odometry.update();
            // Wheel pattern of a clockwise spin; the translation cancels by construction.
            setTicks(-500, -500, 500, 500);
            heading[0] = Math.PI / 2.0;
            final Pose2d pose = odometry.update();

            assertEquals(0.0, pose.getX(), DELTA);
            assertEquals(0.0, pose.getY(), DELTA);
            assertEquals(Math.PI / 2.0, pose.getRotation().getRadians(), DELTA);
        }

        @Test
        @DisplayName("the first update baselines instead of teleporting")
        void firstUpdateBaselines() {
            setTicks(5000, 5000, 5000, 5000);
            final MecanumOdometry odometry = mecanum();

            final Pose2d pose = odometry.update();

            assertEquals(Pose2d.ORIGIN, pose);
        }

        @Test
        @DisplayName("resetPose teleports and re-baselines")
        void resetPose() {
            final MecanumOdometry odometry = mecanum();

            odometry.update();
            setTicks(1000, 1000, 1000, 1000);
            odometry.update();
            odometry.resetPose(Pose2d.fromHeading(10.0, 20.0, 1.0));
            final Pose2d pose = odometry.update();

            assertEquals(10.0, pose.getX(), DELTA);
            assertEquals(20.0, pose.getY(), DELTA);
        }

        @Test
        @DisplayName("rejects nulls and bad constants")
        void rejectsBadArguments() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new MecanumOdometry(
                                    null, fr, bl, br, source, TICKS_PER_REV, WHEEL_DIAMETER_MM));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new MecanumOdometry(
                                    fl, fr, bl, br, null, TICKS_PER_REV, WHEEL_DIAMETER_MM));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MecanumOdometry(fl, fr, bl, br, source, 0.0, WHEEL_DIAMETER_MM));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MecanumOdometry(fl, fr, bl, br, source, TICKS_PER_REV, -1.0));
            assertThrows(IllegalArgumentException.class, () -> mecanum().resetPose(null));
        }
    }

    @Nested
    @DisplayName("TankOdometry")
    class Tank {

        private TankOdometry tank() {
            return new TankOdometry(fl, fr, source, TICKS_PER_REV, WHEEL_DIAMETER_MM);
        }

        @Test
        @DisplayName("forward is the mean of both sides, strafe is always zero")
        void forwardMean() {
            final TankOdometry odometry = tank();

            odometry.update();
            setTicks(1000, 500, 0, 0);
            final Pose2d pose = odometry.update();

            assertEquals(0.0, pose.getX(), DELTA);
            assertEquals(0.75 * INCHES_PER_KTICKS, pose.getY(), DELTA);
        }

        @Test
        @DisplayName("differential rotation comes from the heading source")
        void headingFromSource() {
            final TankOdometry odometry = tank();

            odometry.update();
            heading[0] = -Math.PI / 2.0;
            final Pose2d pose = odometry.update();

            assertEquals(-Math.PI / 2.0, pose.getRotation().getRadians(), DELTA);
        }

        @Test
        @DisplayName("starts at a known pose when constructed with one")
        void initialPose() {
            final TankOdometry odometry =
                    new TankOdometry(
                            fl,
                            fr,
                            source,
                            TICKS_PER_REV,
                            WHEEL_DIAMETER_MM,
                            new Pose2d(5.0, 6.0, Rotation2d.ZERO));

            assertEquals(new Pose2d(5.0, 6.0, Rotation2d.ZERO), odometry.getPose());
        }

        @Test
        @DisplayName("rejects nulls and bad constants")
        void rejectsBadArguments() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TankOdometry(null, fr, source, TICKS_PER_REV, WHEEL_DIAMETER_MM));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TankOdometry(fl, fr, source, TICKS_PER_REV, WHEEL_DIAMETER_MM, null));
        }
    }
}
