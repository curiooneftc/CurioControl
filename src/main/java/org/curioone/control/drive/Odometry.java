package org.curioone.control.drive;

import org.curioone.control.hardware.HeadingSource;
import org.curioone.control.math.MathUtil;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Transform2d;
import org.curioone.control.math.Vector2d;

/**
 * Tracks the robot's field pose from wheel travel plus a heading source.
 *
 * <p>Each {@link #update()} reads how far the wheels turned since the last call, advances the pose
 * by that robot-frame displacement rotated into the field frame, and rotates by the heading change.
 * The gyroscope (or any {@link HeadingSource}) is the heading authority — wheel-derived rotation
 * needs a calibrated track width and drifts faster, so the wheels only ever vote on translation.
 *
 * <p>Integration is first-order over the loop interval: the displacement applies at the
 * start-of-interval heading. At FTC loop rates the interval is milliseconds and the error is
 * negligible next to wheel slip, which is the actual accuracy limit — odometry measures where the
 * wheels went, including every slip, push, and bump. Fuse with AprilTag solves ({@code
 * AprilTagManager.getRobotPose}) when absolute accuracy matters.
 *
 * <p>Units are inches throughout: wheel travel converts through the caller's ticks and wheel size,
 * and poses come out in the same inches the field — and the vision layout — are measured in.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.4.0
 */
public abstract class Odometry {

    private final HeadingSource heading;

    private Pose2d pose;

    private double lastHeadingRadians;

    private boolean started;

    /**
     * Creates an odometry tracker.
     *
     * <p>Does not validate: subclasses check arguments through {@link #requireHeading} before
     * delegating here, because throwing from a non-final constructor is a finalizer-attack surface
     * the build forbids — and every subclass in this package is final, closing it.
     *
     * @param heading the heading source, already checked non-null by the caller
     */
    protected Odometry(HeadingSource heading) {
        this.heading = heading;
        this.pose = Pose2d.ORIGIN;
    }

    /**
     * Checks a heading source before constructor delegation.
     *
     * <p>Java runs the {@code super(...)} arguments before the superclass constructor, so a
     * subclass validating here fails before any object exists to attack — unlike a throw inside the
     * constructor body itself.
     *
     * @param heading the heading source
     * @return the heading source
     * @throws IllegalArgumentException if {@code heading} is {@code null}
     */
    protected static HeadingSource requireHeading(HeadingSource heading) {
        if (heading == null) {
            throw new IllegalArgumentException("heading must not be null");
        }
        return heading;
    }

    /**
     * Advances the estimate by one loop iteration.
     *
     * <p>The first call baselines the sensors and returns the current pose unchanged — there is no
     * previous reading to difference against, and inventing one would teleport the robot.
     *
     * @return the field pose estimate
     */
    public Pose2d update() {
        final double current = heading.heading();
        if (!started) {
            started = true;
            lastHeadingRadians = current;
            resetWheelBaseline();
            return pose;
        }
        final double deltaRadians = MathUtil.wrapToPi(current - lastHeadingRadians);
        lastHeadingRadians = current;
        pose =
                pose.transformBy(
                        new Transform2d(translationDelta(), Rotation2d.fromRadians(deltaRadians)));
        return pose;
    }

    /**
     * Returns the current field pose estimate.
     *
     * @return the pose
     */
    public Pose2d getPose() {
        return pose;
    }

    /**
     * Teleports the estimate, e.g. to a known autonomous start or a tag solve.
     *
     * <p>Re-baselines sensors too, so the next {@link #update()} differences from the teleport
     * rather than from before it.
     *
     * @param pose the pose to adopt
     * @throws IllegalArgumentException if {@code pose} is {@code null}
     */
    public void resetPose(Pose2d pose) {
        if (pose == null) {
            throw new IllegalArgumentException("pose must not be null");
        }
        this.pose = pose;
        this.started = true;
        this.lastHeadingRadians = heading.heading();
        resetWheelBaseline();
    }

    /**
     * Reads the robot-frame translation since the previous call, in inches.
     *
     * @return the displacement in the robot's frame: x right, y forward
     */
    protected abstract Vector2d translationDelta();

    /** Records current wheel readings as the baseline for the next delta. */
    protected abstract void resetWheelBaseline();
}
