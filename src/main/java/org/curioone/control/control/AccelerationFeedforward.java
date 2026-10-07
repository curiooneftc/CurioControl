package org.curioone.control.control;

/**
 * Acceleration feedforward: the extra effort proportional to how hard the mechanism is pushed.
 *
 * <p>Computes {@code kA * acceleration}: inertia resists changes in velocity, so the start of a
 * move and the braking at the end need more than the cruise in between. This is the term that makes
 * a motion profile track instead of lagging behind it — pair it with {@link
 * TrapezoidalMotionProfile}, which supplies the acceleration reference.
 *
 * <p>Stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public final class AccelerationFeedforward implements Feedforward {

    private double ka;

    /**
     * Creates an acceleration model.
     *
     * @param ka the acceleration gain, in output units per acceleration unit
     * @throws IllegalArgumentException if {@code ka} is not finite
     */
    public AccelerationFeedforward(double ka) {
        setKa(ka);
    }

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position ignored
     * @param velocity ignored
     * @param acceleration the mechanism acceleration
     * @return {@code kA * acceleration}
     * @throws IllegalArgumentException if any argument is not finite
     */
    @Override
    public double calculate(double position, double velocity, double acceleration) {
        requireFinite("position", position);
        requireFinite("velocity", velocity);
        requireFinite("acceleration", acceleration);
        return ka * acceleration;
    }

    /**
     * Sets the acceleration gain.
     *
     * @param ka the acceleration gain
     * @throws IllegalArgumentException if {@code ka} is not finite
     */
    public void setKa(double ka) {
        requireFinite("ka", ka);
        this.ka = ka;
    }

    /**
     * Returns the acceleration gain.
     *
     * @return {@code kA}
     */
    public double getKa() {
        return ka;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
