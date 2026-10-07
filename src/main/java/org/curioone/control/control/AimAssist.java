package org.curioone.control.control;

import org.curioone.control.math.MathUtil;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Vector2d;

/**
 * Pose-based aim assist: turn so the robot faces a field target.
 *
 * <p>Given the robot's field pose — from odometry, an AprilTag solve, or any fusion of the two —
 * and a target point, this answers the two numbers a turn loop needs: which heading points at the
 * target, and how far off the robot is from it. Feed the error into a {@link PIDController} whose
 * output drives rotation, and the robot tracks the target.
 *
 * <pre>{@code
 * double error = AimAssist.headingError(robotPose, targetPosition);
 * turnMotor.setPower(turnPid.calculate(0.0, -error));
 * }</pre>
 *
 * <p>Deliberately decoupled from vision: it takes a pose, not a detection, so a {@code control}
 * user never needs a vision dependency and the same helper serves tag-based, odometry-based, and
 * fixed-target aiming. The wiring — vision pose into this helper into a loop — lives in team code;
 * see the vision guide.
 *
 * <p>Pure Java: numbers in, numbers out. No FTC imports.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.4.0
 */
public final class AimAssist {

    private AimAssist() {
        throw new AssertionError("AimAssist is a static helper and must not be instantiated.");
    }

    /**
     * Returns the heading that points the robot at a target.
     *
     * @param robot the robot's field pose
     * @param target the target position, in the same units as the pose
     * @return the heading from the robot to the target
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static Rotation2d desiredHeading(Pose2d robot, Vector2d target) {
        requirePoses(robot, target);
        final double dx = target.getX() - robot.getX();
        final double dy = target.getY() - robot.getY();
        return Rotation2d.fromRadians(Math.atan2(dx, dy));
    }

    /**
     * Returns the signed turn error toward a target.
     *
     * <p>Positive means turn counter-clockwise in framework headings (toward field-right from
     * forward); negative the other way. Wrapped to {@code [-π, π]}, so the robot always takes the
     * short way round.
     *
     * @param robot the robot's field pose
     * @param target the target position, in the same units as the pose
     * @return the heading error in radians
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static double headingError(Pose2d robot, Vector2d target) {
        requirePoses(robot, target);
        return MathUtil.wrapToPi(
                desiredHeading(robot, target).getRadians() - robot.getRotation().getRadians());
    }

    /**
     * Returns the distance to a target.
     *
     * @param robot the robot's field pose
     * @param target the target position, in the same units as the pose
     * @return the Euclidean distance
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static double distanceTo(Pose2d robot, Vector2d target) {
        requirePoses(robot, target);
        return robot.getTranslation().distance(target);
    }

    private static void requirePoses(Pose2d robot, Vector2d target) {
        if (robot == null) {
            throw new IllegalArgumentException("robot must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
    }
}
