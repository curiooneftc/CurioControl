package org.curioone.control.control;

/**
 * Static-friction feedforward: the constant kick needed to break stiction.
 *
 * <p>Computes {@code kS * sign(velocity)}: full effort in the direction of motion, nothing at rest.
 * A mechanism that needs power to <em>hold</em> still against gravity wants {@link
 * GravityFeedforward} instead — this term is zero at zero velocity on purpose, so it never fights
 * the holding controller.
 *
 * <p>Stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public final class ConstantFeedforward implements Feedforward {

    private double ks;

    /**
     * Creates a static-friction model.
     *
     * @param ks the static gain, in output units
     * @throws IllegalArgumentException if {@code ks} is not finite
     */
    public ConstantFeedforward(double ks) {
        setKs(ks);
    }

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position ignored
     * @param velocity the mechanism velocity; only its sign is used
     * @param acceleration ignored
     * @return {@code kS * sign(velocity)}, or {@code 0} at rest
     * @throws IllegalArgumentException if any argument is not finite
     */
    @Override
    public double calculate(double position, double velocity, double acceleration) {
        requireFinite("position", position);
        requireFinite("velocity", velocity);
        requireFinite("acceleration", acceleration);
        return ks * Math.signum(velocity);
    }

    /**
     * Sets the static gain.
     *
     * @param ks the static gain, in output units
     * @throws IllegalArgumentException if {@code ks} is not finite
     */
    public void setKs(double ks) {
        requireFinite("ks", ks);
        this.ks = ks;
    }

    /**
     * Returns the static gain.
     *
     * @return {@code kS}
     */
    public double getKs() {
        return ks;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
