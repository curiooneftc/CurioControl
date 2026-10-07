package org.curioone.control.drive;

import org.curioone.control.math.MathUtil;
import org.curioone.control.math.Pose2d;
import org.curioone.control.util.Range;

/**
 * Proportional go-to-pose: field error in, robot-frame drive powers out.
 *
 * <p>The bread-and-butter autonomous calculation — drive toward a target pose and stop — without
 * hand-rolling the frame rotation and the turn sign every time. Translation error is rotated into
 * the robot frame and scaled by one gain; heading error is scaled by another. Every output
 * saturates at full power rather than exceeding it.
 *
 * <pre>{@code
 * PoseController goTo = new PoseController(0.05, 0.02, 1.0, Math.toRadians(3.0));
 * // ... per loop:
 * PoseController.DriveSignal signal = goTo.calculate(odometry.getPose(), target);
 * drive.mecanum(signal.strafe(), signal.forward(), signal.rotation());
 * }</pre>
 *
 * <p>Proportional only, stateless, allocation-free per call beyond the returned signal: no integral
 * to wind up, no clock to inject. When integral action or profile tracking is needed, graduate to
 * {@code PIDController} pairs on the same errors — this class remains the quick check that the
 * geometry is right before the gains get fancy.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.4.0
 */
public final class PoseController {

    /**
     * Robot-frame drive powers in {@code [-1, 1]}: strafe right, forward, and rotation where
     * positive is counter-clockwise, matching {@link MecanumDrive#mecanum}.
     *
     * @param strafe rightward power
     * @param forward forward power
     * @param rotation rotational power
     */
    public record DriveSignal(double strafe, double forward, double rotation) {
        /**
         * Creates a drive signal, saturating each component to its power limit.
         *
         * <p>Saturation preserves direction intent the way arrival from far away should: full speed
         * toward the target, easing off as the error closes, with no impossible command in between.
         */
        public DriveSignal {
            strafe = Range.clampPower(strafe);
            forward = Range.clampPower(forward);
            rotation = Range.clampPower(rotation);
        }
    }

    private final double driveKp;

    private final double turnKp;

    private final double positionTolerance;

    private final double headingTolerance;

    /**
     * Creates a pose controller.
     *
     * @param driveKp translational gain, in power per distance unit; must be finite
     * @param turnKp rotational gain, in power per radian; must be finite
     * @param positionTolerance arrival radius, in distance units; must not be negative
     * @param headingTolerance arrival heading window, in radians; must not be negative
     * @throws IllegalArgumentException if a gain is not finite, or a tolerance is negative or NaN
     */
    public PoseController(
            double driveKp, double turnKp, double positionTolerance, double headingTolerance) {
        if (!Double.isFinite(driveKp)) {
            throw new IllegalArgumentException("driveKp must be finite but was " + driveKp);
        }
        if (!Double.isFinite(turnKp)) {
            throw new IllegalArgumentException("turnKp must be finite but was " + turnKp);
        }
        if (Double.isNaN(positionTolerance) || positionTolerance < 0.0) {
            throw new IllegalArgumentException(
                    "positionTolerance must be non-negative and not NaN but was "
                            + positionTolerance);
        }
        if (Double.isNaN(headingTolerance) || headingTolerance < 0.0) {
            throw new IllegalArgumentException(
                    "headingTolerance must be non-negative and not NaN but was "
                            + headingTolerance);
        }
        this.driveKp = driveKp;
        this.turnKp = turnKp;
        this.positionTolerance = positionTolerance;
        this.headingTolerance = headingTolerance;
    }

    /**
     * Computes drive powers toward a target pose.
     *
     * <p>Note the rotation sign: framework headings increase toward field-right while positive
     * rotation input turns counter-clockwise, so a positive heading error commands a
     * <em>negative</em> rotation. The negation lives here, once, rather than at every call site
     * debating it.
     *
     * @param current the robot's field pose
     * @param target the target field pose
     * @return the saturated drive signal
     * @throws IllegalArgumentException if either pose is {@code null}
     */
    public DriveSignal calculate(Pose2d current, Pose2d target) {
        requirePoses(current, target);
        final double dx = target.getX() - current.getX();
        final double dy = target.getY() - current.getY();
        final double heading = current.getRotation().getRadians();
        final double cos = Math.cos(heading);
        final double sin = Math.sin(heading);
        // Field error into the robot frame: the forward direction of fieldCentric.
        final double strafe = (dx * cos - dy * sin) * driveKp;
        final double forward = (dx * sin + dy * cos) * driveKp;
        final double error = MathUtil.wrapToPi(target.getRotation().getRadians() - heading);
        return new DriveSignal(strafe, forward, -error * turnKp);
    }

    /**
     * Reports whether the robot is inside the arrival window.
     *
     * @param current the robot's field pose
     * @param target the target field pose
     * @return {@code true} when position and heading are both within tolerance
     * @throws IllegalArgumentException if either pose is {@code null}
     */
    public boolean atTarget(Pose2d current, Pose2d target) {
        requirePoses(current, target);
        final double distance = current.getTranslation().distance(target.getTranslation());
        final double error =
                Math.abs(
                        MathUtil.wrapToPi(
                                target.getRotation().getRadians()
                                        - current.getRotation().getRadians()));
        return distance <= positionTolerance && error <= headingTolerance;
    }

    private static void requirePoses(Pose2d current, Pose2d target) {
        if (current == null) {
            throw new IllegalArgumentException("current must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
    }
}
