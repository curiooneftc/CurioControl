package org.curioone.control.math;

/**
 * A relative motion in the plane: a translation plus a rotation.
 *
 * <p>A transform maps one {@link Pose2d} to another. The translation is expressed in the frame of
 * the pose it is applied to, which is what makes chaining work: {@code a.plus(b)} applies {@code
 * a}, then applies {@code b} in the resulting frame.
 *
 * <p>Immutable: every operation returns a new instance.
 *
 * @since 0.2.0
 */
public final class Transform2d {

    /** The identity transform. */
    public static final Transform2d IDENTITY = new Transform2d(Vector2d.ZERO, Rotation2d.ZERO);

    private final Vector2d translation;

    private final Rotation2d rotation;

    /**
     * Creates a transform.
     *
     * @param translation the translation, in the frame of the pose it is applied to
     * @param rotation the rotation
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Transform2d(Vector2d translation, Rotation2d rotation) {
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
     * Creates a transform from components.
     *
     * @param x the x translation
     * @param y the y translation
     * @param rotation the rotation
     * @throws IllegalArgumentException if {@code rotation} is {@code null}, or a component is not
     *     finite
     */
    public Transform2d(double x, double y, Rotation2d rotation) {
        this(new Vector2d(x, y), rotation);
    }

    /**
     * Returns the translation.
     *
     * @return the translation, never {@code null}
     */
    public Vector2d getTranslation() {
        return translation;
    }

    /**
     * Returns the x translation.
     *
     * @return x
     */
    public double getX() {
        return translation.getX();
    }

    /**
     * Returns the y translation.
     *
     * @return y
     */
    public double getY() {
        return translation.getY();
    }

    /**
     * Returns the rotation.
     *
     * @return the rotation, never {@code null}
     */
    public Rotation2d getRotation() {
        return rotation;
    }

    /**
     * Composes this transform with another.
     *
     * <p>The result applies {@code this} first, then {@code other} in the resulting frame.
     *
     * @param other the transform to apply after this one
     * @return the composed transform
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Transform2d plus(Transform2d other) {
        requireOther(other);
        return new Transform2d(
                translation.plus(other.translation.rotateBy(rotation)),
                rotation.plus(other.rotation));
    }

    /**
     * Returns the inverse transform.
     *
     * <p>Applying a transform and then its inverse returns to the original pose.
     *
     * @return the inverse
     */
    public Transform2d inverse() {
        final Rotation2d invertedRotation = rotation.unaryMinus();
        return new Transform2d(
                translation.unaryMinus().rotateBy(invertedRotation), invertedRotation);
    }

    /**
     * Interpolates toward another transform.
     *
     * @param end the transform at {@code t = 1}
     * @param t the interpolation fraction, clamped to {@code [0, 1]}
     * @return the interpolated transform
     * @throws IllegalArgumentException if {@code end} is {@code null} or {@code t} is NaN
     */
    public Transform2d interpolate(Transform2d end, double t) {
        requireOther(end);
        if (Double.isNaN(t)) {
            throw new IllegalArgumentException("t must not be NaN");
        }
        return new Transform2d(
                translation.interpolate(end.translation, t), rotation.interpolate(end.rotation, t));
    }

    private static void requireOther(Transform2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transform2d)) {
            return false;
        }
        final Transform2d that = (Transform2d) other;
        return translation.equals(that.translation) && rotation.equals(that.rotation);
    }

    @Override
    public int hashCode() {
        return 31 * translation.hashCode() + rotation.hashCode();
    }

    @Override
    public String toString() {
        return "Transform2d[" + translation + ", " + rotation + "]";
    }
}
