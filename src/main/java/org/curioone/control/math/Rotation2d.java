package org.curioone.control.math;

/**
 * A rotation in the plane, stored in radians.
 *
 * <p>The value is normalized to {@code [-π, π]} at construction, so two rotations that point the
 * same way compare equal however they were built. Positive is counter-clockwise, matching the FTC
 * IMU convention that field-centric drive consumes.
 *
 * <p>Immutable: every operation returns a new instance.
 *
 * @since 0.2.0
 */
public final class Rotation2d {

    /** The zero rotation. */
    public static final Rotation2d ZERO = new Rotation2d(0.0);

    private final double radians;

    private Rotation2d(double radians) {
        this.radians = radians;
    }

    /**
     * Creates a rotation from radians.
     *
     * @param radians the angle in radians, normalized to {@code [-π, π]}
     * @return the rotation
     * @throws IllegalArgumentException if {@code radians} is not finite
     */
    public static Rotation2d fromRadians(double radians) {
        return new Rotation2d(MathUtil.wrapToPi(radians));
    }

    /**
     * Creates a rotation from degrees.
     *
     * @param degrees the angle in degrees
     * @return the rotation
     * @throws IllegalArgumentException if {@code degrees} is not finite
     */
    public static Rotation2d fromDegrees(double degrees) {
        if (!Double.isFinite(degrees)) {
            throw new IllegalArgumentException("degrees must be finite but was " + degrees);
        }
        return new Rotation2d(MathUtil.wrapToPi(Units.degreesToRadians(degrees)));
    }

    /**
     * Creates a rotation from full turns.
     *
     * @param rotations the angle in turns
     * @return the rotation
     * @throws IllegalArgumentException if {@code rotations} is not finite
     */
    public static Rotation2d fromRotations(double rotations) {
        if (!Double.isFinite(rotations)) {
            throw new IllegalArgumentException("rotations must be finite but was " + rotations);
        }
        return new Rotation2d(MathUtil.wrapToPi(Units.rotationsToRadians(rotations)));
    }

    /**
     * Returns the angle in radians, in {@code [-π, π]}.
     *
     * @return the angle in radians
     */
    public double getRadians() {
        return radians;
    }

    /**
     * Returns the angle in degrees, in {@code [-180, 180]}.
     *
     * @return the angle in degrees
     */
    public double getDegrees() {
        return Units.radiansToDegrees(radians);
    }

    /**
     * Returns the cosine of the angle.
     *
     * @return {@code cos(angle)}
     */
    public double getCos() {
        return Math.cos(radians);
    }

    /**
     * Returns the sine of the angle.
     *
     * @return {@code sin(angle)}
     */
    public double getSin() {
        return Math.sin(radians);
    }

    /**
     * Composes this rotation with another.
     *
     * @param other the rotation to add
     * @return {@code this + other}, normalized
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Rotation2d plus(Rotation2d other) {
        requireOther(other);
        return new Rotation2d(MathUtil.wrapToPi(radians + other.radians));
    }

    /**
     * Returns the relative rotation from another rotation to this one.
     *
     * @param other the rotation to subtract
     * @return {@code this - other}, normalized
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Rotation2d minus(Rotation2d other) {
        requireOther(other);
        return new Rotation2d(MathUtil.wrapToPi(radians - other.radians));
    }

    /**
     * Returns the negated rotation.
     *
     * @return {@code -this}, normalized
     */
    public Rotation2d unaryMinus() {
        return new Rotation2d(MathUtil.wrapToPi(-radians));
    }

    /**
     * Interpolates toward another rotation along the shortest path.
     *
     * <p>Interpolating raw wrapped angles takes the long way round whenever the interval crosses
     * ±π; interpolating the signed difference does not.
     *
     * @param end the rotation at {@code t = 1}
     * @param t the interpolation fraction, clamped to {@code [0, 1]}
     * @return the interpolated rotation
     * @throws IllegalArgumentException if {@code end} is {@code null} or {@code t} is NaN
     */
    public Rotation2d interpolate(Rotation2d end, double t) {
        requireOther(end);
        if (Double.isNaN(t)) {
            throw new IllegalArgumentException("t must not be NaN");
        }
        return plus(end.minus(this).times(MathUtil.clamp(t, 0.0, 1.0)));
    }

    private Rotation2d times(double scalar) {
        return new Rotation2d(MathUtil.wrapToPi(radians * scalar));
    }

    private static void requireOther(Rotation2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rotation2d)) {
            return false;
        }
        return Double.compare(radians, ((Rotation2d) other).radians) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(radians);
    }

    @Override
    public String toString() {
        return "Rotation2d[" + radians + " rad]";
    }
}
