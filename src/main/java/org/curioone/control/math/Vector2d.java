package org.curioone.control.math;

/**
 * A displacement in the plane.
 *
 * <p>Units are whatever the caller uses — millimetres, inches, encoder ticks — but both components
 * of one vector must share them. Immutable: every operation returns a new instance.
 *
 * @since 0.2.0
 */
public final class Vector2d {

    /** The zero vector. */
    public static final Vector2d ZERO = new Vector2d(0.0, 0.0);

    private final double x;

    private final double y;

    /**
     * Creates a vector.
     *
     * @param x the x component
     * @param y the y component
     * @throws IllegalArgumentException if either component is not finite
     */
    public Vector2d(double x, double y) {
        requireFinite("x", x);
        requireFinite("y", y);
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x component.
     *
     * @return x
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y component.
     *
     * @return y
     */
    public double getY() {
        return y;
    }

    /**
     * Adds another vector.
     *
     * @param other the vector to add
     * @return {@code this + other}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Vector2d plus(Vector2d other) {
        requireOther(other);
        return new Vector2d(x + other.x, y + other.y);
    }

    /**
     * Subtracts another vector.
     *
     * @param other the vector to subtract
     * @return {@code this - other}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Vector2d minus(Vector2d other) {
        requireOther(other);
        return new Vector2d(x - other.x, y - other.y);
    }

    /**
     * Returns the negated vector.
     *
     * @return {@code -this}
     */
    public Vector2d unaryMinus() {
        return new Vector2d(-x, -y);
    }

    /**
     * Scales the vector.
     *
     * @param scalar the factor
     * @return {@code this * scalar}
     * @throws IllegalArgumentException if {@code scalar} is not finite
     */
    public Vector2d times(double scalar) {
        requireFinite("scalar", scalar);
        return new Vector2d(x * scalar, y * scalar);
    }

    /**
     * Scales the vector by the reciprocal of a divisor.
     *
     * @param divisor the divisor, must be non-zero
     * @return {@code this / divisor}
     * @throws IllegalArgumentException if {@code divisor} is not finite or is zero
     */
    public Vector2d div(double divisor) {
        requireFinite("divisor", divisor);
        if (divisor == 0.0) {
            throw new IllegalArgumentException("divisor must not be zero");
        }
        return new Vector2d(x / divisor, y / divisor);
    }

    /**
     * Returns the dot product with another vector.
     *
     * @param other the other vector
     * @return {@code this · other}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public double dot(Vector2d other) {
        requireOther(other);
        return x * other.x + y * other.y;
    }

    /**
     * Returns the 2D cross product with another vector.
     *
     * @param other the other vector
     * @return the z component of {@code this × other}; positive means {@code other} is
     *     counter-clockwise from {@code this}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public double cross(Vector2d other) {
        requireOther(other);
        return x * other.y - y * other.x;
    }

    /**
     * Returns the length of the vector.
     *
     * @return {@code √(x² + y²)}
     */
    public double norm() {
        return Math.hypot(x, y);
    }

    /**
     * Returns the distance to another vector.
     *
     * @param other the other vector
     * @return the Euclidean distance
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public double distance(Vector2d other) {
        requireOther(other);
        return Math.hypot(x - other.x, y - other.y);
    }

    /**
     * Returns the direction of the vector.
     *
     * @return the angle of {@code (x, y)}; the zero vector yields the zero rotation
     */
    public Rotation2d angle() {
        if (x == 0.0 && y == 0.0) {
            return Rotation2d.ZERO;
        }
        return Rotation2d.fromRadians(Math.atan2(y, x));
    }

    /**
     * Rotates the vector by a rotation.
     *
     * @param rotation the rotation to apply
     * @return the rotated vector
     * @throws IllegalArgumentException if {@code rotation} is {@code null}
     */
    public Vector2d rotateBy(Rotation2d rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("rotation must not be null");
        }
        final double cos = rotation.getCos();
        final double sin = rotation.getSin();
        return new Vector2d(x * cos - y * sin, x * sin + y * cos);
    }

    /**
     * Interpolates toward another vector.
     *
     * @param end the vector at {@code t = 1}
     * @param t the interpolation fraction, clamped to {@code [0, 1]}
     * @return the interpolated vector
     * @throws IllegalArgumentException if {@code end} is {@code null} or {@code t} is NaN
     */
    public Vector2d interpolate(Vector2d end, double t) {
        requireOther(end);
        if (Double.isNaN(t)) {
            throw new IllegalArgumentException("t must not be NaN");
        }
        return new Vector2d(MathUtil.lerp(x, end.x, t), MathUtil.lerp(y, end.y, t));
    }

    private static void requireOther(Vector2d other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vector2d)) {
            return false;
        }
        final Vector2d that = (Vector2d) other;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(x) + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "Vector2d[" + x + ", " + y + "]";
    }
}
