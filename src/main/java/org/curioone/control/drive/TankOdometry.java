package org.curioone.control.drive;

import org.curioone.control.hardware.Encoder;
import org.curioone.control.hardware.HeadingSource;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Units;
import org.curioone.control.math.Vector2d;

/**
 * Wheel odometry for a tank base: two encoders plus a heading source.
 *
 * <p>A tank chassis cannot strafe, so the lateral component is always zero and forward travel is
 * the mean of both sides — mirroring {@link TankDrive}, which likewise discards strafe rather than
 * inventing it. Rotation comes from the heading source alone.
 *
 * @since 0.4.0
 */
public final class TankOdometry extends Odometry {

    /** One half, for the two-wheel mean. */
    private static final double HALF = 0.5;

    private final Encoder left;

    private final Encoder right;

    private final double ticksPerRev;

    private final double wheelDiameterMm;

    private final double[] lastInches = new double[2];

    /**
     * Creates tank odometry starting at the origin.
     *
     * @param left the left wheel encoder
     * @param right the right wheel encoder
     * @param heading the heading source
     * @param ticksPerRev encoder ticks per wheel revolution; must be positive
     * @param wheelDiameterMm wheel diameter in millimetres; must be positive
     * @throws IllegalArgumentException if an encoder or the heading is {@code null}, or a physical
     *     constant is not finite and positive
     */
    public TankOdometry(
            Encoder left,
            Encoder right,
            HeadingSource heading,
            double ticksPerRev,
            double wheelDiameterMm) {
        this(left, right, heading, ticksPerRev, wheelDiameterMm, Pose2d.ORIGIN);
    }

    /**
     * Creates tank odometry starting at a known pose.
     *
     * @param left the left wheel encoder
     * @param right the right wheel encoder
     * @param heading the heading source
     * @param ticksPerRev encoder ticks per wheel revolution; must be positive
     * @param wheelDiameterMm wheel diameter in millimetres; must be positive
     * @param initialPose the starting field pose
     * @throws IllegalArgumentException if any object is {@code null}, or a physical constant is not
     *     finite and positive
     */
    public TankOdometry(
            Encoder left,
            Encoder right,
            HeadingSource heading,
            double ticksPerRev,
            double wheelDiameterMm,
            Pose2d initialPose) {
        super(requireHeading(heading));
        if (left == null) {
            throw new IllegalArgumentException("left encoder must not be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right encoder must not be null");
        }
        if (!Double.isFinite(ticksPerRev) || ticksPerRev <= 0.0) {
            throw new IllegalArgumentException(
                    "ticksPerRev must be finite and positive but was " + ticksPerRev);
        }
        if (!Double.isFinite(wheelDiameterMm) || wheelDiameterMm <= 0.0) {
            throw new IllegalArgumentException(
                    "wheelDiameterMm must be finite and positive but was " + wheelDiameterMm);
        }
        if (initialPose == null) {
            throw new IllegalArgumentException("initialPose must not be null");
        }
        this.left = left;
        this.right = right;
        this.ticksPerRev = ticksPerRev;
        this.wheelDiameterMm = wheelDiameterMm;
        resetPose(initialPose);
    }

    @Override
    protected Vector2d translationDelta() {
        final double leftNow = inches(left);
        final double rightNow = inches(right);
        final double forward = (leftNow - lastInches[0] + (rightNow - lastInches[1])) * HALF;
        lastInches[0] = leftNow;
        lastInches[1] = rightNow;
        return new Vector2d(0.0, forward);
    }

    @Override
    protected void resetWheelBaseline() {
        lastInches[0] = inches(left);
        lastInches[1] = inches(right);
    }

    private double inches(Encoder encoder) {
        return Units.millimetersToInches(encoder.getDistance(ticksPerRev, wheelDiameterMm));
    }
}
