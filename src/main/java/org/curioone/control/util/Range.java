package org.curioone.control.util;

/**
 * Clamping helpers for the ranges CurioControl validates against.
 *
 * <p>Motor power lives in {@code [-1, 1]}, servo position in {@code [0, 1]}. These constants and
 * the clamp helpers are the single definition of those ranges, so a wrapper and its test cannot
 * disagree about what "valid" means.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.1.0
 */
public final class Range {

    /** Lowest legal motor power. */
    public static final double MIN_POWER = -1.0;

    /** Highest legal motor power. */
    public static final double MAX_POWER = 1.0;

    /** Lowest legal servo position. */
    public static final double MIN_SERVO_POSITION = 0.0;

    /** Highest legal servo position. */
    public static final double MAX_SERVO_POSITION = 1.0;

    private Range() {
        throw new AssertionError("Range is a constant holder and must not be instantiated.");
    }

    /**
     * Clamps a value into an inclusive range.
     *
     * @param value the value to clamp
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code value}, limited to {@code [min, max]}
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}, or if {@code
     *     value} is NaN — a NaN would silently defeat every comparison below
     */
    public static double clamp(double value, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException(
                    "min (" + min + ") must not be greater than max (" + max + ")");
        }
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("value must not be NaN");
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value to a valid motor power.
     *
     * @param power the requested power
     * @return the power limited to {@code [-1, 1]}
     * @throws IllegalArgumentException if {@code power} is NaN
     */
    public static double clampPower(double power) {
        return clamp(power, MIN_POWER, MAX_POWER);
    }

    /**
     * Clamps a value to a valid servo position.
     *
     * @param position the requested position
     * @return the position limited to {@code [0, 1]}
     * @throws IllegalArgumentException if {@code position} is NaN
     */
    public static double clampServoPosition(double position) {
        return clamp(position, MIN_SERVO_POSITION, MAX_SERVO_POSITION);
    }

    /**
     * Reports whether a value is a legal motor power.
     *
     * @param power the value to test
     * @return {@code true} if the value is finite and within {@code [-1, 1]}
     */
    public static boolean isValidPower(double power) {
        return Double.isFinite(power) && power >= MIN_POWER && power <= MAX_POWER;
    }

    /**
     * Reports whether a value is a legal servo position.
     *
     * @param position the value to test
     * @return {@code true} if the value is finite and within {@code [0, 1]}
     */
    public static boolean isValidServoPosition(double position) {
        return Double.isFinite(position)
                && position >= MIN_SERVO_POSITION
                && position <= MAX_SERVO_POSITION;
    }
}
