package org.curioone.control.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PoseController}. */
@DisplayName("PoseController")
class PoseControllerTest {

    private static final double DELTA = 1e-9;

    private static final PoseController CONTROL =
            new PoseController(0.05, 0.02, 1.0, Math.toRadians(3.0));

    private static final Pose2d ORIGIN = new Pose2d(0.0, 0.0, Rotation2d.ZERO);

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("drives forward toward a target straight ahead")
        void drivesForward() {
            final PoseController.DriveSignal signal =
                    CONTROL.calculate(ORIGIN, new Pose2d(0.0, 10.0, Rotation2d.ZERO));

            assertEquals(0.0, signal.strafe(), DELTA);
            assertEquals(10.0 * 0.05, signal.forward(), DELTA);
            assertEquals(0.0, signal.rotation(), DELTA);
        }

        @Test
        @DisplayName("strafes toward a target to the side")
        void strafesSideways() {
            final PoseController.DriveSignal signal =
                    CONTROL.calculate(ORIGIN, new Pose2d(10.0, 0.0, Rotation2d.ZERO));

            assertEquals(10.0 * 0.05, signal.strafe(), DELTA);
            assertEquals(0.0, signal.forward(), DELTA);
        }

        @Test
        @DisplayName("rotates with the inverted sign headings require")
        void rotationSign() {
            // Target heading +90 degrees from facing forward: the robot must turn toward
            // field-right, which is negative rotation input (counter-clockwise is positive).
            final PoseController.DriveSignal signal =
                    CONTROL.calculate(ORIGIN, new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(90.0)));

            assertEquals(0.0, signal.strafe(), DELTA);
            assertEquals(0.0, signal.forward(), DELTA);
            assertTrue(signal.rotation() < 0.0, "rotation was " + signal.rotation());
            assertEquals(-Math.PI / 2.0 * 0.02, signal.rotation(), DELTA);
        }

        @Test
        @DisplayName("a rotated robot still drives at the field target")
        void rotatedRobot() {
            // Facing field-right: a field-forward target is robot-left, i.e. negative strafe.
            final Pose2d facingRight = new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(90.0));
            final PoseController.DriveSignal signal =
                    CONTROL.calculate(facingRight, new Pose2d(0.0, 10.0, Rotation2d.ZERO));

            assertEquals(-10.0 * 0.05, signal.strafe(), DELTA);
            assertEquals(0.0, signal.forward(), DELTA);
        }

        @Test
        @DisplayName("far targets saturate at full power, never beyond")
        void saturates() {
            final PoseController.DriveSignal signal =
                    CONTROL.calculate(ORIGIN, new Pose2d(0.0, 1000.0, Rotation2d.ZERO));

            assertEquals(1.0, signal.forward(), DELTA);
        }

        @Test
        @DisplayName("rejects nulls and bad gains")
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> CONTROL.calculate(null, ORIGIN));
            assertThrows(IllegalArgumentException.class, () -> CONTROL.calculate(ORIGIN, null));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PoseController(Double.NaN, 0.02, 1.0, 0.05));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PoseController(0.05, 0.02, -1.0, 0.05));
        }
    }

    @Nested
    @DisplayName("atTarget")
    class AtTarget {

        @Test
        @DisplayName("true inside the window, false outside either axis")
        void window() {
            final Pose2d target = new Pose2d(24.0, 0.0, Rotation2d.ZERO);

            assertTrue(CONTROL.atTarget(target, target));
            assertTrue(CONTROL.atTarget(new Pose2d(23.5, 0.0, Rotation2d.ZERO), target));
            assertFalse(CONTROL.atTarget(new Pose2d(20.0, 0.0, Rotation2d.ZERO), target));
            assertFalse(
                    CONTROL.atTarget(new Pose2d(24.0, 0.0, Rotation2d.fromDegrees(10.0)), target));
        }
    }
}
