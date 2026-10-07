package org.curioone.control.control;

/**
 * The combined motor model: static friction plus velocity and acceleration terms.
 *
 * <p>Computes {@code kS * sign(velocity) + kV * velocity + kA * acceleration}. This is the model
 * behind the standard drive-motor characterization: {@code kS} is the minimum power that moves at
 * all, {@code kV} the power per unit of cruise velocity, {@code kA} the power per unit of
 * acceleration. Tune them in that order — each later term assumes the earlier ones already account
 * for their share.
 *
 * <p>Stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public final class CombinedFeedforward implements Feedforward {

    private double ks;

    private double kv;

    private double ka;

    /**
     * Creates a combined model.
     *
     * @param ks the static gain, in output units
     * @param kv the velocity gain, in output units per velocity unit
     * @param ka the acceleration gain, in output units per acceleration unit
     * @throws IllegalArgumentException if any gain is not finite
     */
    public CombinedFeedforward(double ks, double kv, double ka) {
        setGains(ks, kv, ka);
    }

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position ignored
     * @param velocity the mechanism velocity; only its sign feeds the static term
     * @param acceleration the mechanism acceleration
     * @return {@code kS * sign(velocity) + kV * velocity + kA * acceleration}
     * @throws IllegalArgumentException if any argument is not finite
     */
    @Override
    public double calculate(double position, double velocity, double acceleration) {
        requireFinite("position", position);
        requireFinite("velocity", velocity);
        requireFinite("acceleration", acceleration);
        return ks * Math.signum(velocity) + kv * velocity + ka * acceleration;
    }

    /**
     * Sets all three gains at once.
     *
     * @param ks the static gain
     * @param kv the velocity gain
     * @param ka the acceleration gain
     * @throws IllegalArgumentException if any gain is not finite
     */
    public void setGains(double ks, double kv, double ka) {
        requireFinite("ks", ks);
        requireFinite("kv", kv);
        requireFinite("ka", ka);
        this.ks = ks;
        this.kv = kv;
        this.ka = ka;
    }

    /**
     * Returns the static gain.
     *
     * @return {@code kS}
     */
    public double getKs() {
        return ks;
    }

    /**
     * Returns the velocity gain.
     *
     * @return {@code kV}
     */
    public double getKv() {
        return kv;
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
