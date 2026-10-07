package org.curioone.control.control;

/**
 * Gravity feedforward: the holding effort that varies with arm angle.
 *
 * <p>Computes {@code kG * cos(position)}: an arm held horizontal fights full gravity, an arm held
 * straight up fights none. The position is the arm angle in <strong>radians measured from
 * horizontal</strong> — horizontal arm is zero, straight up is π/2. Measuring from vertical instead
 * is the classic misconfiguration, and it inverts the compensation exactly where it matters most;
 * the cosine convention is chosen so a wrong zero shows up immediately as an arm that sags at
 * horizontal rather than subtly everywhere.
 *
 * <p>Stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public final class GravityFeedforward implements Feedforward {

    private double kg;

    /**
     * Creates a gravity model.
     *
     * @param kg the gravity gain: the output that holds the arm horizontal
     * @throws IllegalArgumentException if {@code kg} is not finite
     */
    public GravityFeedforward(double kg) {
        setKg(kg);
    }

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position the arm angle in radians from horizontal
     * @param velocity ignored
     * @param acceleration ignored
     * @return {@code kG * cos(position)}
     * @throws IllegalArgumentException if any argument is not finite
     */
    @Override
    public double calculate(double position, double velocity, double acceleration) {
        requireFinite("position", position);
        requireFinite("velocity", velocity);
        requireFinite("acceleration", acceleration);
        return kg * Math.cos(position);
    }

    /**
     * Sets the gravity gain.
     *
     * @param kg the gravity gain
     * @throws IllegalArgumentException if {@code kg} is not finite
     */
    public void setKg(double kg) {
        requireFinite("kg", kg);
        this.kg = kg;
    }

    /**
     * Returns the gravity gain.
     *
     * @return {@code kG}
     */
    public double getKg() {
        return kg;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
