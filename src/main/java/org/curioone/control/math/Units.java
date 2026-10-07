package org.curioone.control.math;

/**
 * Unit conversions between the frames an FTC robot actually uses.
 *
 * <p>Distances on an FTC field are measured in inches, mechanisms in millimetres, and the SDK in
 * whatever the call takes. Every conversion lives here so a factor of 25.4 never appears inline at
 * a call site, where it is indistinguishable from a typo.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.2.0
 */
public final class Units {

    /** Millimetres per inch. */
    public static final double MM_PER_INCH = 25.4;

    /** Inches per foot, for field dimensions. */
    public static final double INCHES_PER_FOOT = 12.0;

    /** Millimetres per metre. */
    public static final double MM_PER_METER = 1000.0;

    private Units() {
        throw new AssertionError("Units is a constant holder and must not be instantiated.");
    }

    /**
     * Converts inches to millimetres.
     *
     * @param inches the distance in inches
     * @return the distance in millimetres
     */
    public static double inchesToMillimeters(double inches) {
        return inches * MM_PER_INCH;
    }

    /**
     * Converts millimetres to inches.
     *
     * @param millimeters the distance in millimetres
     * @return the distance in inches
     */
    public static double millimetersToInches(double millimeters) {
        return millimeters / MM_PER_INCH;
    }

    /**
     * Converts metres to millimetres.
     *
     * @param meters the distance in metres
     * @return the distance in millimetres
     */
    public static double metersToMillimeters(double meters) {
        return meters * MM_PER_METER;
    }

    /**
     * Converts millimetres to metres.
     *
     * @param millimeters the distance in millimetres
     * @return the distance in metres
     */
    public static double millimetersToMeters(double millimeters) {
        return millimeters / MM_PER_METER;
    }

    /**
     * Converts inches to metres.
     *
     * @param inches the distance in inches
     * @return the distance in metres
     */
    public static double inchesToMeters(double inches) {
        return millimetersToMeters(inchesToMillimeters(inches));
    }

    /**
     * Converts metres to inches.
     *
     * @param meters the distance in metres
     * @return the distance in inches
     */
    public static double metersToInches(double meters) {
        return millimetersToInches(metersToMillimeters(meters));
    }

    /**
     * Converts feet to inches, for field-dimension constants.
     *
     * @param feet the distance in feet
     * @return the distance in inches
     */
    public static double feetToInches(double feet) {
        return feet * INCHES_PER_FOOT;
    }

    /**
     * Converts degrees to radians.
     *
     * @param degrees the angle in degrees
     * @return the angle in radians
     */
    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /**
     * Converts radians to degrees.
     *
     * @param radians the angle in radians
     * @return the angle in degrees
     */
    public static double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * Converts full turns to radians.
     *
     * @param rotations the angle in turns
     * @return the angle in radians
     */
    public static double rotationsToRadians(double rotations) {
        return rotations * MathUtil.TAU;
    }

    /**
     * Converts radians to full turns.
     *
     * @param radians the angle in radians
     * @return the angle in turns
     */
    public static double radiansToRotations(double radians) {
        return radians / MathUtil.TAU;
    }
}
