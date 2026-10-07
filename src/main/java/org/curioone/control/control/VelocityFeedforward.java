package org.curioone.control.control;

/**
 * Velocity feedforward: the sustained effort proportional to speed.
 *
 * <p>Computes {@code kV * velocity}: back-EMF and viscous friction grow with speed, so holding a
 * cruise velocity needs a steady output the feedback loop would otherwise have to wind up to
 * produce. Tune it by driving at a known velocity and dividing the measured holding output by that
 * velocity.
 *
 * <p>Stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public final class VelocityFeedforward implements Feedforward {

    private double kv;

    /**
     * Creates a velocity model.
     *
     * @param kv the velocity gain, in output units per velocity unit
     * @throws IllegalArgumentException if {@code kv} is not finite
     */
    public VelocityFeedforward(double kv) {
        setKv(kv);
    }

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position ignored
     * @param velocity the mechanism velocity
     * @param acceleration ignored
     * @return {@code kV * velocity}
     * @throws IllegalArgumentException if any argument is not finite
     */
    @Override
    public double calculate(double position, double velocity, double acceleration) {
        requireFinite("position", position);
        requireFinite("velocity", velocity);
        requireFinite("acceleration", acceleration);
        return kv * velocity;
    }

    /**
     * Sets the velocity gain.
     *
     * @param kv the velocity gain
     * @throws IllegalArgumentException if {@code kv} is not finite
     */
    public void setKv(double kv) {
        requireFinite("kv", kv);
        this.kv = kv;
    }

    /**
     * Returns the velocity gain.
     *
     * @return {@code kV}
     */
    public double getKv() {
        return kv;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
