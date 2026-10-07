package org.curioone.control.math;

/**
 * Shared scalar math: clamping, interpolation, and angle wrapping.
 *
 * <p>Angles are radians throughout this package. The two wrappers exist because forgetting which
 * range a heading lives in is a recurring source of field-centric bugs: {@link #wrapToPi} is for
 * errors and differences, where the sign says which way to turn, and {@link #wrapToTau} is for
 * absolute headings used as indices or compared against bounded targets.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.2.0
 */
public final class MathUtil {

    /** Radians in half a turn. */
    public static final double PI = Math.PI;

    /** Radians in a full turn. */
    public static final double TAU = 2.0 * Math.PI;

    private MathUtil() {
        throw new AssertionError("MathUtil is a static helper and must not be instantiated.");
    }

    /**
     * Clamps a value into an inclusive range.
     *
     * @param value the value to clamp
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code value}, limited to {@code [min, max]}
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}, or any argument
     *     is NaN
     */
    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isNaN(min) || Double.isNaN(max)) {
            throw new IllegalArgumentException("arguments must not be NaN");
        }
        if (min > max) {
            throw new IllegalArgumentException(
                    "min (" + min + ") must not be greater than max (" + max + ")");
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linearly interpolates between two values.
     *
     * <p>The fraction is clamped to {@code [0, 1]}, so extrapolating past an endpoint is a
     * deliberate two-step operation rather than an accident from a fraction that overshot.
     *
     * @param start the value at {@code t = 0}
     * @param end the value at {@code t = 1}
     * @param t the interpolation fraction
     * @return {@code start + (end - start) * t}, with {@code t} clamped to {@code [0, 1]}
     * @throws IllegalArgumentException if any argument is NaN
     */
    public static double lerp(double start, double end, double t) {
        if (Double.isNaN(start) || Double.isNaN(end) || Double.isNaN(t)) {
            throw new IllegalArgumentException("arguments must not be NaN");
        }
        final double fraction = clamp(t, 0.0, 1.0);
        return start + (end - start) * fraction;
    }

    /**
     * Wraps an angle to {@code [-π, π]}.
     *
     * <p>Use for signed differences: the sign of the result says which way is shorter.
     *
     * @param radians the angle in radians
     * @return the equivalent angle in {@code [-π, π]}
     * @throws IllegalArgumentException if {@code radians} is not finite
     */
    public static double wrapToPi(double radians) {
        requireFinite("radians", radians);
        final double wrapped = radians % TAU;
        if (wrapped > PI) {
            return wrapped - TAU;
        }
        if (wrapped < -PI) {
            return wrapped + TAU;
        }
        return wrapped;
    }

    /**
     * Wraps an angle to {@code [0, 2π)}.
     *
     * <p>Use for absolute headings that are compared against bounded targets or used as indices.
     *
     * @param radians the angle in radians
     * @return the equivalent angle in {@code [0, 2π)}
     * @throws IllegalArgumentException if {@code radians} is not finite
     */
    public static double wrapToTau(double radians) {
        requireFinite("radians", radians);
        final double wrapped = radians % TAU;
        return wrapped < 0.0 ? wrapped + TAU : wrapped;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
