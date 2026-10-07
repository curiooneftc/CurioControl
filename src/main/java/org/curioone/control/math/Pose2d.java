package org.curioone.control.math;

/**
 * An absolute position and heading on the field.
 *
 * <p>Which units the coordinates use — inches, millimetres — is the caller's choice, but one pose
 * must never mix them: combining an inch pose with a millimetre transform silently scales the
 * result by 25.4. The field convention is +x right, +y forward, heading counter-clockwise from
 * forward, matching the FTC IMU.
 *
 * <p>Immutable: every operation returns a new instance.
 *
 * @since 0.2.0
 */
public final class Pose2d {

    /** The origin, facing forward. */
    public static final Pose2d ORIGIN = new Pose2d(0.0, 0.0, Rotation2d.ZERO);

    private final Vector2d translation;

    private final Rotation2d rotation;

    /**
     * Creates a pose.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param rotation the heading
     * @throws IllegalArgumentException if {@code rotation} is {@code null}, or a coordinate is not
     *     finite
     */
    public Pose2d(double x, double y, Rotation2d rotation) {
        this(new Vector2d(x, y), rotation);
    }

    /**
     * Creates a pose.
     *
     * @param translation the position
     * @param rotation the heading
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Pose2d(Vector2d translation, Rotation2d rotation) {
        if (translation == null) {
            throw new IllegalArgumentException("translation must not be null");
        }
        if (rotation == null) {
            throw new IllegalArgumentException("rotation must not be null");
        }
        this.translation = translation;
        this.rotation = rotation;
    }

    /**
     * Creates a pose from a heading in radians.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param headingRadians the heading in radians
     * @return the pose
     * @throws IllegalArgumentException if any argument is not finite
     */
    public static Pose2d fromHeading(double x, double y, double headingRadians) {
        return new Pose2d(x, y, Rotation2d.fromRadians(headingRadians));
    }

    /**
     * Returns the x coordinate.
     *
     * @return x
     */
    public double getX() {
        return translation.getX();
    }

    /**
     * Returns the y coordinate.
     *
     * @return y
     */
    public double getY() {
        return translation.getY();
    }

    /**
     * Returns the position.
     *
     * @return the translation, never {@code null}
     */
    public Vector2d getTranslation() {
        return translation;
    }

    /**
     * Returns the heading.
     *
     * @return the rotation, never {@code null}
     */
    public Rotation2d getRotation() {
        return rotation;
    }

    /**
     * Applies a transform to this pose.
     *
     * @param transform the relative motion, in this pose's frame
     * @return the resulting pose
     * @throws IllegalArgumentException if {@code transform} is {@code null}
     */
    public Pose2d plus(Transform2d transform) {
        return transformBy(transform);
    }

    /**
     * Applies a transform to this pose.
     *
     * <p>The transform's translation is rotated into the field frame by this pose's heading before
     * being added — that rotation is the entire difference between "drive forward 24 inches" and
     * "drive toward field-forward 24 inches".
     *
     * @param transform the relative motion, in this pose's frame
     * @return the resulting pose
     * @throws IllegalArgumentException if {@code transform} is {@code null}
     */
    public Pose2d transformBy(Transform2d transform) {
        if (transform == null) {
            throw new IllegalArgumentException("transform must not be null");
        }
        return new Pose2d(
                translation.plus(transform.getTranslation().rotateBy(rotation)),
                rotation.plus(transform.getRotation()));
    }

    /**
     * Returns the transform that maps another pose onto this one.
     *
     * <p>The translation is expressed in {@code other}'s frame: it answers "where am I, as seen
     * from {@code other}".
     *
     * @param other the reference pose
     * @return the relative transform from {@code other} to this pose
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Transform2d minus(Pose2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        final Rotation2d inverse = other.rotation.unaryMinus();
        return new Transform2d(
                translation.minus(other.translation).rotateBy(inverse),
                rotation.minus(other.rotation));
    }

    /**
     * Returns this pose expressed in another pose's frame.
     *
     * @param other the reference pose
     * @return this pose, as seen from {@code other}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Pose2d relativeTo(Pose2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        final Transform2d relative = minus(other);
        return new Pose2d(relative.getTranslation(), relative.getRotation());
    }

    /**
     * Returns the distance to another pose, ignoring heading.
     *
     * @param other the other pose
     * @return the Euclidean distance between the positions
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public double distance(Pose2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        return translation.distance(other.translation);
    }

    /**
     * Interpolates toward another pose.
     *
     * <p>Translation interpolates linearly; heading takes the shortest path.
     *
     * @param end the pose at {@code t = 1}
     * @param t the interpolation fraction, clamped to {@code [0, 1]}
     * @return the interpolated pose
     * @throws IllegalArgumentException if {@code end} is {@code null} or {@code t} is NaN
     */
    public Pose2d interpolate(Pose2d end, double t) {
        if (end == null) {
            throw new IllegalArgumentException("end must not be null");
        }
        if (Double.isNaN(t)) {
            throw new IllegalArgumentException("t must not be NaN");
        }
        return new Pose2d(
                translation.interpolate(end.translation, t), rotation.interpolate(end.rotation, t));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pose2d)) {
            return false;
        }
        final Pose2d that = (Pose2d) other;
        return translation.equals(that.translation) && rotation.equals(that.rotation);
    }

    @Override
    public int hashCode() {
        return 31 * translation.hashCode() + rotation.hashCode();
    }

    @Override
    public String toString() {
        return "Pose2d[" + translation + ", " + rotation + "]";
    }
}
