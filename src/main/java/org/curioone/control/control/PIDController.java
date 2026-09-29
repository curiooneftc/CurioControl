package org.curioone.control.control;

import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * A closed-loop PID controller.
 *
 * <p>Computes an output from the error between a target and a measured value, combining
 * proportional, integral, and derivative terms with delta-time-aware accumulation. Pure Java: it
 * takes numbers, not a motor, and imports no FTC or Android type (ADR-003).
 *
 * <pre>{@code
 * PIDController pid = new PIDController(0.01, 0.0, 0.001);
 * motor.setPower(pid.calculate(targetTicks, motor.getPosition()));
 * }</pre>
 *
 * <h2>Delta time</h2>
 *
 * The integral and derivative terms are scaled by the time since the previous call, read from an
 * injected {@link Clock}. FTC loop times vary substantially in practice, and a controller that
 * assumes a fixed interval produces an integral term that scales with how busy the loop is — so a
 * PID tuned on an idle robot behaves differently during a match. In tests, a fake clock makes that
 * reproducible rather than incidental.
 *
 * <h2>Integral windup</h2>
 *
 * While the output is saturated, error keeps accumulating. On reaching the target the integrator
 * holds a large charge and drives the mechanism straight past. Two guards prevent this, and both
 * are active by default:
 *
 * <ul>
 *   <li><strong>Conditional integration.</strong> The integrator stops accumulating while the
 *       output is saturated and the error would push it further out. The standard fix, and the one
 *       that recovers correctly once the mechanism unloads.
 *   <li><strong>An integral limit.</strong> A hard bound via {@link #setIntegralLimit(double)}, as
 *       a second line of defence.
 * </ul>
 *
 * <h2>Tuning</h2>
 *
 * Tune in order: P until the motion overshoots, then back off; I until it settles briskly without
 * overshooting on arrival; then a little D to damp the approach. Full procedure in the
 * control-tuning guide.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * <p><strong>Allocation:</strong> none per {@code calculate} call.
 *
 * @since 0.1.0
 */
public final class PIDController {

    private final Clock clock;

    private double kP;

    private double kI;

    private double kD;

    private double integral;

    private double lastError;

    private double lastOutput;

    private double derivative;

    private long lastNanos = Long.MIN_VALUE;

    private double outputMin = Double.NEGATIVE_INFINITY;

    private double outputMax = Double.POSITIVE_INFINITY;

    private double integralLimit = Double.POSITIVE_INFINITY;

    private double tolerance;

    /**
     * Creates a controller using the system clock.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     */
    public PIDController(double kP, double kI, double kD) {
        this(kP, kI, kD, new SystemClock());
    }

    /**
     * Creates a controller with an injected time source.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param clock the time source, which a test can drive by hand
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or a gain is NaN
     */
    public PIDController(double kP, double kI, double kD, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
        setGains(kP, kI, kD);
    }

    /**
     * Computes the control output for this loop iteration.
     *
     * <p>The first call after construction or {@link #reset()} contributes no integral and no
     * derivative: there is no previous reading to measure against, and inventing one produces a
     * spike on the first loop of every run.
     *
     * @param target the desired setpoint
     * @param current the measured value
     * @return the control output, limited to the configured output range
     * @throws IllegalArgumentException if either argument is not finite
     */
    public double calculate(double target, double current) {
        requireFinite("target", target);
        requireFinite("current", current);

        final double error = target - current;
        final double dt = deltaSeconds();
        final double proportional = kP * error;

        // Conditional integration. While the previous output was saturated, stop accumulating
        // an error that would push it further out. Using the previous output is deliberate: the
        // current one is not known until the integral term is included, and basing the decision
        // on the proportional term alone misjudges a controller that is saturated because of
        // accumulated integral.
        final boolean saturatedHigh = lastOutput >= outputMax;
        final boolean saturatedLow = lastOutput <= outputMin;
        final boolean windingFurther =
                (saturatedHigh && error > 0.0) || (saturatedLow && error < 0.0);

        if (!windingFurther) {
            integral += error * dt;
            integral = clampSymmetric(integral, integralLimit);
        }

        final double integralTerm = kI * integral;

        if (dt > 0.0) {
            derivative = (error - lastError) / dt;
        }
        final double derivativeTerm = kD * derivative;

        lastError = error;
        lastOutput = clamp(proportional + integralTerm + derivativeTerm);
        return lastOutput;
    }

    /**
     * Clears accumulated state.
     *
     * <p>Zeroes the integral and derivative terms and forgets the previous reading, so the next
     * {@link #calculate} behaves like a first call. Call this when a mechanism is re-targeted
     * discontinuously, or before restarting a closed loop.
     */
    public void reset() {
        integral = 0.0;
        lastError = 0.0;
        lastOutput = 0.0;
        derivative = 0.0;
        lastNanos = Long.MIN_VALUE;
    }

    /**
     * Reports whether the current error is within the configured tolerance.
     *
     * <p>With a tolerance of {@code 0} — the default — this is true only at an exact setpoint,
     * which is rarely what a caller means. Set a real tolerance for any mechanism with finite
     * resolution.
     *
     * @return {@code true} if the last computed error was within tolerance
     */
    public boolean atSetpoint() {
        return Math.abs(lastError) <= tolerance;
    }

    /**
     * Sets the error considered close enough to the setpoint.
     *
     * @param tolerance absolute error tolerance, in the same units as the measured value
     * @throws IllegalArgumentException if {@code tolerance} is negative or NaN
     */
    public void setTolerance(double tolerance) {
        if (Double.isNaN(tolerance) || tolerance < 0.0) {
            throw new IllegalArgumentException("tolerance must be non-negative and not NaN");
        }
        this.tolerance = tolerance;
    }

    /**
     * Returns the current error tolerance.
     *
     * @return the tolerance
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * Limits the output range.
     *
     * <p>Saturating the output is what makes a controller safe to hand straight to {@code
     * motor.setPower}: without it, a gain that is momentarily too high commands an impossible motor
     * power.
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
    }

    /** Removes the output limits. */
    public void clearOutputLimits() {
        this.outputMin = Double.NEGATIVE_INFINITY;
        this.outputMax = Double.POSITIVE_INFINITY;
    }

    /**
     * Bounds the accumulated integral term.
     *
     * <p>A second line of defence against windup, behind conditional integration. Set it when a
     * mechanism overshoots badly on arrival from a long move.
     *
     * @param limit the maximum magnitude of the accumulated error; must not be negative
     * @throws IllegalArgumentException if {@code limit} is negative or NaN
     */
    public void setIntegralLimit(double limit) {
        if (Double.isNaN(limit) || limit < 0.0) {
            throw new IllegalArgumentException("integralLimit must be non-negative and not NaN");
        }
        this.integralLimit = limit;
    }

    /** Removes the integral limit. */
    public void clearIntegralLimit() {
        this.integralLimit = Double.POSITIVE_INFINITY;
    }

    /**
     * Sets all three gains at once.
     *
     * <p>Does not reset accumulated state: retuning mid-run should not discard the integral that is
     * correcting for a real load. Call {@link #reset()} explicitly if that is wanted.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @throws IllegalArgumentException if a gain is NaN
     */
    public void setGains(double kP, double kI, double kD) {
        requireFinite("kP", kP);
        requireFinite("kI", kI);
        requireFinite("kD", kD);
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    /**
     * Returns the proportional gain.
     *
     * @return {@code kP}
     */
    public double getKp() {
        return kP;
    }

    /**
     * Returns the integral gain.
     *
     * @return {@code kI}
     */
    public double getKi() {
        return kI;
    }

    /**
     * Returns the derivative gain.
     *
     * @return {@code kD}
     */
    public double getKd() {
        return kD;
    }

    /**
     * Returns the accumulated integral of the error.
     *
     * @return the current integral term, in error-seconds
     */
    public double getIntegral() {
        return integral;
    }

    private double deltaSeconds() {
        final long now = clock.nowNanos();
        if (lastNanos == Long.MIN_VALUE) {
            lastNanos = now;
            return 0.0;
        }
        final double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        // A negative delta means the clock went backwards or the caller re-used a controller
        // after a reset. Treating it as zero is the safe reading: a large negative dt would
        // fling the integral in the wrong direction.
        return dt > 0.0 ? dt : 0.0;
    }

    private double clamp(double value) {
        return Math.max(outputMin, Math.min(outputMax, value));
    }

    private static double clampSymmetric(double value, double limit) {
        if (Double.isInfinite(limit)) {
            return value;
        }
        return Math.max(-limit, Math.min(limit, value));
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }
}
