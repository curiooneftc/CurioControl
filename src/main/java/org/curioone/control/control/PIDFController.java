package org.curioone.control.control;

import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * A closed-loop PID controller with an open-loop feedforward term.
 *
 * <p>Computes {@code kP·e + kI·∫e + kD·ė + kF·ff}. The PID half is a {@link PIDController} doing
 * exactly what it always does; the feedforward half carries the predictable load — friction,
 * gravity, cruise power — so the integral never has to wind up to discover it. That division is the
 * whole point: feedback corrects the model's errors, it does not substitute for the model.
 *
 * <pre>{@code
 * PIDFController arm = new PIDFController(0.01, 0.0, 0.001, 1.0);
 * arm.setFeedforward(new GravityFeedforward(holdPower));
 * motor.setPower(arm.calculate(target, motor.getPosition(), motor.getPosition(), 0.0, 0.0));
 * }</pre>
 *
 * <p>The feedforward reference can be supplied two ways: as an explicit value the caller computed,
 * or through an attached {@link Feedforward} model evaluated against the caller's motion state.
 * Both scale by {@code kF}; both go through the same output limits.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * <p><strong>Allocation:</strong> none per {@code calculate} call.
 *
 * @since 0.2.0
 */
public final class PIDFController {

    private final PIDController pid;

    private double kf;

    private Feedforward feedforward;

    private double outputMin = Double.NEGATIVE_INFINITY;

    private double outputMax = Double.POSITIVE_INFINITY;

    /**
     * Creates a controller using the system clock.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward scale
     * @throws IllegalArgumentException if a gain is NaN
     */
    public PIDFController(double kP, double kI, double kD, double kF) {
        this(kP, kI, kD, kF, new SystemClock());
    }

    /**
     * Creates a controller with an injected time source.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward scale
     * @param clock the time source, which a test can drive by hand
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or a gain is NaN
     */
    public PIDFController(double kP, double kI, double kD, double kF, Clock clock) {
        pid = new PIDController(kP, kI, kD, clock);
        setKf(kF);
    }

    /**
     * Creates a controller with an attached feedforward model.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward scale
     * @param feedforward the model evaluated by the state-based {@code calculate}
     * @throws IllegalArgumentException if {@code feedforward} is {@code null}, or a gain is NaN
     */
    public PIDFController(double kP, double kI, double kD, double kF, Feedforward feedforward) {
        this(kP, kI, kD, kF, feedforward, new SystemClock());
    }

    /**
     * Creates a controller with an attached feedforward model and an injected time source.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward scale
     * @param feedforward the model evaluated by the state-based {@code calculate}
     * @param clock the time source, which a test can drive by hand
     * @throws IllegalArgumentException if {@code feedforward} or {@code clock} is {@code null}, or
     *     a gain is NaN
     */
    public PIDFController(
            double kP, double kI, double kD, double kF, Feedforward feedforward, Clock clock) {
        pid = new PIDController(kP, kI, kD, clock);
        setKf(kF);
        setFeedforward(feedforward);
    }

    /**
     * Computes the control output with an explicit feedforward value.
     *
     * @param target the desired setpoint
     * @param current the measured value
     * @param feedforward the open-loop reference, scaled by {@code kF} and added to the PID output
     * @return the control output, limited to the configured output range
     * @throws IllegalArgumentException if any argument is not finite
     */
    public double calculate(double target, double current, double feedforward) {
        requireFinite("feedforward", feedforward);
        final double feedback = pid.calculate(target, current);
        return clamp(feedback + kf * feedforward);
    }

    /**
     * Computes the control output with the attached feedforward model.
     *
     * <p>The model sees the caller's motion state, not the error: feedforward predicts what the
     * mechanism needs, feedback corrects the prediction.
     *
     * @param target the desired setpoint
     * @param current the measured value
     * @param position the mechanism position for the model
     * @param velocity the mechanism velocity for the model
     * @param acceleration the mechanism acceleration for the model
     * @return the control output, limited to the configured output range
     * @throws IllegalStateException if no feedforward model is attached
     * @throws IllegalArgumentException if any argument is not finite
     */
    public double calculate(
            double target, double current, double position, double velocity, double acceleration) {
        if (feedforward == null) {
            throw new IllegalStateException(
                    "no feedforward model attached: call setFeedforward(...) first, "
                            + "or use calculate(target, current, feedforward)");
        }
        return calculate(target, current, feedforward.calculate(position, velocity, acceleration));
    }

    /**
     * Clears accumulated state.
     *
     * @see PIDController#reset()
     */
    public void reset() {
        pid.reset();
    }

    /**
     * Reports whether the current error is within the configured tolerance.
     *
     * @return {@code true} if the last computed error was within tolerance
     * @see PIDController#atSetpoint()
     */
    public boolean atSetpoint() {
        return pid.atSetpoint();
    }

    /**
     * Sets the error considered close enough to the setpoint.
     *
     * @param tolerance absolute error tolerance, in the same units as the measured value
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN
     */
    public void setTolerance(double tolerance) {
        pid.setTolerance(tolerance);
    }

    /**
     * Returns the current error tolerance.
     *
     * @return the tolerance
     */
    public double getTolerance() {
        return pid.getTolerance();
    }

    /**
     * Limits the output range.
     *
     * <p>Applies to the combined PID-plus-feedforward output: a gravity term that alone saturates
     * the motor is tuned wrong, and clamping a half silently would hide that. The same limits are
     * forwarded to the inner PID loop so its anti-windup guard saturates in agreement.
     *
     * @param min the lowest permitted output
     * @param max the highest permitted output
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}, or either is NaN
     */
    public void setOutputLimits(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max)) {
            throw new IllegalArgumentException("output limits must not be NaN");
        }
        if (min > max) {
            throw new IllegalArgumentException(
                    "min (" + min + ") must not be greater than max (" + max + ")");
        }
        this.outputMin = min;
        this.outputMax = max;
        pid.setOutputLimits(min, max);
    }

    /** Removes the output limits. */
    public void clearOutputLimits() {
        this.outputMin = Double.NEGATIVE_INFINITY;
        this.outputMax = Double.POSITIVE_INFINITY;
        pid.clearOutputLimits();
    }

    /**
     * Bounds the accumulated integral term.
     *
     * @param limit the maximum magnitude of the accumulated error; must not be negative
     * @throws IllegalArgumentException if {@code limit} is negative or NaN
     */
    public void setIntegralLimit(double limit) {
        pid.setIntegralLimit(limit);
    }

    /** Removes the integral limit. */
    public void clearIntegralLimit() {
        pid.clearIntegralLimit();
    }

    /**
     * Sets all four gains at once.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward scale
     * @throws IllegalArgumentException if a gain is NaN
     */
    public void setGains(double kP, double kI, double kD, double kF) {
        pid.setGains(kP, kI, kD);
        setKf(kF);
    }

    /**
     * Returns the proportional gain.
     *
     * @return {@code kP}
     */
    public double getKp() {
        return pid.getKp();
    }

    /**
     * Returns the integral gain.
     *
     * @return {@code kI}
     */
    public double getKi() {
        return pid.getKi();
    }

    /**
     * Returns the derivative gain.
     *
     * @return {@code kD}
     */
    public double getKd() {
        return pid.getKd();
    }

    /**
     * Sets the feedforward scale.
     *
     * @param kF the scale applied to the feedforward reference
     * @throws IllegalArgumentException if {@code kF} is NaN
     */
    public void setKf(double kF) {
        if (Double.isNaN(kF)) {
            throw new IllegalArgumentException("kF must not be NaN");
        }
        this.kf = kF;
    }

    /**
     * Returns the feedforward scale.
     *
     * @return {@code kF}
     */
    public double getKf() {
        return kf;
    }

    /**
     * Attaches the feedforward model used by the state-based {@code calculate}.
     *
     * @param feedforward the model
     * @throws IllegalArgumentException if {@code feedforward} is {@code null}
     */
    public void setFeedforward(Feedforward feedforward) {
        if (feedforward == null) {
            throw new IllegalArgumentException("feedforward must not be null");
        }
        this.feedforward = feedforward;
    }

    /** Detaches the feedforward model. */
    public void clearFeedforward() {
        this.feedforward = null;
    }

    /**
     * Returns the accumulated integral of the error.
     *
     * @return the current integral term, in error-seconds
     */
    public double getIntegral() {
        return pid.getIntegral();
    }

    private double clamp(double value) {
        return Math.max(outputMin, Math.min(outputMax, value));
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
