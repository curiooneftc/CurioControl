package org.curioone.control.drive;

import org.curioone.control.hardware.Encoder;
import org.curioone.control.hardware.HeadingSource;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Units;
import org.curioone.control.math.Vector2d;

/**
 * Wheel odometry for a mecanum base: four encoders plus a heading source.
 *
 * <p>Wheel travel decomposes through the inverse of the mecanum wheel equations — the same
 * combinations {@link MecanumDrive} uses forward, so the two can never disagree about which wheel
 * does what. Rotation comes from the heading source alone; the wheels vote on translation only.
 *
 * <pre>{@code
 * MecanumOdometry odometry = new MecanumOdometry(
 *         robot.encoder("fl"), robot.encoder("fr"),
 *         robot.encoder("bl"), robot.encoder("br"),
 *         robot.imu(), RobotConfig.Drive.TICKS_PER_REV, RobotConfig.Drive.WHEEL_DIAMETER_MM);
 * // ... per loop:
 * Pose2d pose = odometry.update();
 * }</pre>
 *
 * @since 0.4.0
 */
public final class MecanumOdometry extends Odometry {

    /** Index of the front-left wheel in the wheel arrays. */
    private static final int FRONT_LEFT = 0;

    /** Index of the front-right wheel in the wheel arrays. */
    private static final int FRONT_RIGHT = 1;

    /** Index of the back-left wheel in the wheel arrays. */
    private static final int BACK_LEFT = 2;

    /** Index of the back-right wheel in the wheel arrays. */
    private static final int BACK_RIGHT = 3;

    /** Wheel count, for the mecanum mean. */
    private static final int WHEEL_COUNT = 4;

    private final Encoder frontLeft;

    private final Encoder frontRight;

    private final Encoder backLeft;

    private final Encoder backRight;

    private final double ticksPerRev;

    private final double wheelDiameterMm;

    private final double[] lastInches = new double[4];

    private final double[] scratchInches = new double[4];

    /**
     * Creates mecanum odometry starting at the origin.
     *
     * @param frontLeft the front-left wheel encoder
     * @param frontRight the front-right wheel encoder
     * @param backLeft the back-left wheel encoder
     * @param backRight the back-right wheel encoder
     * @param heading the heading source
     * @param ticksPerRev encoder ticks per wheel revolution; must be positive
     * @param wheelDiameterMm wheel diameter in millimetres; must be positive
     * @throws IllegalArgumentException if an encoder or the heading is {@code null}, or a physical
     *     constant is not finite and positive
     */
    public MecanumOdometry(
            Encoder frontLeft,
            Encoder frontRight,
            Encoder backLeft,
            Encoder backRight,
            HeadingSource heading,
            double ticksPerRev,
            double wheelDiameterMm) {
        this(
                frontLeft,
                frontRight,
                backLeft,
                backRight,
                heading,
                ticksPerRev,
                wheelDiameterMm,
                Pose2d.ORIGIN);
    }

    /**
     * Creates mecanum odometry starting at a known pose.
     *
     * <p>Start autonomous here rather than constructing at the origin and teleporting: a pose the
     * robot never held, even for one loop, is how a dashboard plot gains a phantom jump at the
     * start of every run.
     *
     * @param frontLeft the front-left wheel encoder
     * @param frontRight the front-right wheel encoder
     * @param backLeft the back-left wheel encoder
     * @param backRight the back-right wheel encoder
     * @param heading the heading source
     * @param ticksPerRev encoder ticks per wheel revolution; must be positive
     * @param wheelDiameterMm wheel diameter in millimetres; must be positive
     * @param initialPose the starting field pose
     * @throws IllegalArgumentException if any object is {@code null}, or a physical constant is not
     *     finite and positive
     */
    public MecanumOdometry(
            Encoder frontLeft,
            Encoder frontRight,
            Encoder backLeft,
            Encoder backRight,
            HeadingSource heading,
            double ticksPerRev,
            double wheelDiameterMm,
            Pose2d initialPose) {
        super(requireHeading(heading));
        this.frontLeft = requireEncoder("frontLeft", frontLeft);
        this.frontRight = requireEncoder("frontRight", frontRight);
        this.backLeft = requireEncoder("backLeft", backLeft);
        this.backRight = requireEncoder("backRight", backRight);
        if (!Double.isFinite(ticksPerRev) || ticksPerRev <= 0.0) {
            throw new IllegalArgumentException(
                    "ticksPerRev must be finite and positive but was " + ticksPerRev);
        }
        if (!Double.isFinite(wheelDiameterMm) || wheelDiameterMm <= 0.0) {
            throw new IllegalArgumentException(
                    "wheelDiameterMm must be finite and positive but was " + wheelDiameterMm);
        }
        this.ticksPerRev = ticksPerRev;
        this.wheelDiameterMm = wheelDiameterMm;
        if (initialPose == null) {
            throw new IllegalArgumentException("initialPose must not be null");
        }
        resetPose(initialPose);
    }

    @Override
    protected Vector2d translationDelta() {
        readInches(scratchInches);
        double forward = 0.0;
        double strafe = 0.0;
        for (int wheel = 0; wheel < WHEEL_COUNT; wheel++) {
            final double delta = scratchInches[wheel] - lastInches[wheel];
            lastInches[wheel] = scratchInches[wheel];
            forward += delta;
            strafe += (wheel == FRONT_LEFT || wheel == BACK_RIGHT) ? delta : -delta;
        }
        return new Vector2d(strafe / WHEEL_COUNT, forward / WHEEL_COUNT);
    }

    @Override
    protected void resetWheelBaseline() {
        readInches(lastInches);
    }

    private void readInches(double[] into) {
        into[FRONT_LEFT] =
                Units.millimetersToInches(frontLeft.getDistance(ticksPerRev, wheelDiameterMm));
        into[FRONT_RIGHT] =
                Units.millimetersToInches(frontRight.getDistance(ticksPerRev, wheelDiameterMm));
        into[BACK_LEFT] =
                Units.millimetersToInches(backLeft.getDistance(ticksPerRev, wheelDiameterMm));
        into[BACK_RIGHT] =
                Units.millimetersToInches(backRight.getDistance(ticksPerRev, wheelDiameterMm));
    }

    private static Encoder requireEncoder(String label, Encoder encoder) {
        if (encoder == null) {
            throw new IllegalArgumentException(label + " encoder must not be null");
        }
        return encoder;
    }
}
