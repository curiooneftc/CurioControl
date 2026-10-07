package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.util.Range;

/**
 * A validated view onto one FTC motor.
 *
 * <p>Wraps, and does not hide. The underlying {@link DcMotorEx} is always available through {@link
 * #getSdkObject()}, so anything this class does not cover is one call away (ADR-005).
 *
 * <p>What the wrapper adds over the raw SDK motor:
 *
 * <ul>
 *   <li><strong>Validation.</strong> {@link #setPower(double)} rejects a power outside {@code [-1,
 *       1]} instead of letting the hardware clamp it silently.
 *   <li><strong>One range definition.</strong> {@link Range} is the only place that knows what a
 *       legal power is.
 *   <li><strong>A name.</strong> For diagnostics, so a failure says which motor.
 * </ul>
 *
 * <h2>Why an out-of-range power throws</h2>
 *
 * A power of {@code 1.5} is always a bug — a gain set too high, a value left over from tuning.
 * Silently clamping it means the motor runs 50% too fast forever and the real defect stays hidden.
 * Throwing surfaces it on the first loop, on the bench, where it is cheap to find. A
 * competition-day concern is real, but a robot that mysteriously drives too fast is a worse outcome
 * than a clear error.
 *
 * <h2>Run modes</h2>
 *
 * The SDK's three power modes are mutually exclusive and easy to get wrong:
 *
 * <ul>
 *   <li>{@link DcMotor.RunMode#RUN_WITHOUT_ENCODER} — {@link #setPower(double)} sets output
 *   <li>{@link DcMotor.RunMode#RUN_USING_ENCODER} — {@link #setVelocity(double)} sets output
 *   <li>{@link DcMotor.RunMode#RUN_TO_POSITION} — {@link #setTargetPosition(int)} sets output
 * </ul>
 *
 * <p>Setting a value that the current mode ignores is the usual cause of "my motor does nothing",
 * so {@link #setRunMode(DcMotor.RunMode)} is the single place that changes it, and the Javadoc on
 * each setter says which mode it requires.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class Motor {

    private final DcMotorEx motor;

    private final String name;

    /**
     * Wraps an SDK motor.
     *
     * @param motor the motor to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if {@code motor} or {@code name} is {@code null}
     */
    public Motor(DcMotorEx motor, String name) {
        if (motor == null) {
            throw new IllegalArgumentException("motor must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.motor = motor;
        this.name = name;
    }

    /**
     * Looks a motor up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no motor is configured under that name
     */
    public Motor(HardwareMap hardwareMap, String name) {
        this(new org.curioone.control.core.HardwareRegistry(hardwareMap).motor(name), name);
    }

    /**
     * Looks a motor up in a registry and wraps it.
     *
     * <p>Resolving through a shared {@link org.curioone.control.core.HardwareRegistry} rather than
     * a fresh {@code HardwareMap} lookup means the device is found once and reused, which is what
     * {@code CurioRobot} does.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no motor is configured under that name
     */
    public Motor(org.curioone.control.core.HardwareRegistry registry, String name) {
        this(registry.motor(name), name);
    }

    /**
     * Returns the underlying SDK motor.
     *
     * <p>The escape hatch. Use it for anything this wrapper does not expose, rather than extending
     * the wrapper for a one-off need.
     *
     * @return the SDK motor, never {@code null}
     */
    public DcMotorEx getSdkObject() {
        return motor;
    }

    /**
     * Returns the configuration name this motor was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    // --- Output -----------------------------------------------------------------

    /**
     * Sets output power directly.
     *
     * <p>Requires {@link DcMotor.RunMode#RUN_WITHOUT_ENCODER}.
     *
     * @param power output power in {@code [-1, 1]}, where {@code 1} is full speed
     * @throws IllegalArgumentException if {@code power} is NaN or outside {@code [-1, 1]}
     */
    public void setPower(double power) {
        if (!Range.isValidPower(power)) {
            throw new IllegalArgumentException(
                    "power for motor '"
                            + name
                            + "' must be in ["
                            + Range.MIN_POWER
                            + ", "
                            + Range.MAX_POWER
                            + "] but was "
                            + power);
        }
        motor.setPower(power);
    }

    /**
     * Stops the motor.
     *
     * <p>Sets power to zero through the validated path. The one-liner for subsystem {@code stop()}
     * hooks, where a bare {@code setPower(0.0)} reads as an arbitrary value rather than an intent.
     */
    public void stop() {
        setPower(0.0);
    }

    /**
     * Sets a target velocity.
     *
     * <p>Requires {@link DcMotor.RunMode#RUN_USING_ENCODER}.
     *
     * @param ticksPerSecond target velocity in encoder ticks per second
     */
    public void setVelocity(double ticksPerSecond) {
        motor.setVelocity(ticksPerSecond);
    }

    /**
     * Sets a target position.
     *
     * <p>Requires {@link DcMotor.RunMode#RUN_TO_POSITION}.
     *
     * @param ticks target position in encoder ticks
     */
    public void setTargetPosition(int ticks) {
        motor.setTargetPosition(ticks);
    }

    /**
     * Returns the current target position.
     *
     * @return the target in encoder ticks
     */
    public int getTargetPosition() {
        return motor.getTargetPosition();
    }

    // --- Measurement ------------------------------------------------------------

    /**
     * Returns the current output power.
     *
     * @return power in {@code [-1, 1]}
     */
    public double getPower() {
        return motor.getPower();
    }

    /**
     * Returns the current velocity.
     *
     * @return velocity in encoder ticks per second
     */
    public double getVelocity() {
        return motor.getVelocity();
    }

    /**
     * Returns the current position.
     *
     * @return position in encoder ticks
     */
    public int getPosition() {
        return motor.getCurrentPosition();
    }

    /**
     * Reports whether the motor has reached its target.
     *
     * @return {@code true} while the motor is still moving toward a target
     */
    public boolean isBusy() {
        return motor.isBusy();
    }

    /**
     * Reports whether the motor is drawing more current than its alert threshold.
     *
     * @return {@code true} if the motor is overcurrent
     */
    public boolean isOverCurrent() {
        return motor.isOverCurrent();
    }

    // --- Configuration ----------------------------------------------------------

    /**
     * Zeroes the encoder, so the current position reads as zero.
     *
     * <p>Call this at the start of a run, before any position comparison, or every measurement is
     * offset by wherever the robot happened to be when the OpMode started.
     */
    public void resetEncoder() {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    /**
     * Sets the motor's direction.
     *
     * <p>A mirrored motor is the usual cause of a mecanum robot that spins instead of strafing, and
     * the symptom looks like a controller problem rather than a configuration one. Getting this
     * right belongs in the robot's configuration, not in the framework.
     *
     * @param direction {@link DcMotorSimple.Direction#FORWARD} or {@link
     *     DcMotorSimple.Direction#REVERSE}
     */
    public void setDirection(DcMotorSimple.Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        motor.setDirection(direction);
    }

    /**
     * Sets what the motor does when commanded to zero power.
     *
     * @param behavior {@link DcMotor.ZeroPowerBehavior#BRAKE} to hold position, or {@link
     *     DcMotor.ZeroPowerBehavior#FLOAT} to let the motor coast
     */
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("behavior must not be null");
        }
        motor.setZeroPowerBehavior(behavior);
    }

    /**
     * Returns what the motor does when commanded to zero power.
     *
     * @return the zero-power behavior
     */
    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior() {
        return motor.getZeroPowerBehavior();
    }

    /**
     * Sets the run mode.
     *
     * @param mode {@link DcMotor.RunMode#RUN_WITHOUT_ENCODER}, {@link
     *     DcMotor.RunMode#RUN_USING_ENCODER} or {@link DcMotor.RunMode#RUN_TO_POSITION}
     */
    public void setRunMode(DcMotor.RunMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        motor.setMode(mode);
    }

    /**
     * Returns the current run mode.
     *
     * @return the run mode
     */
    public DcMotor.RunMode getRunMode() {
        return motor.getMode();
    }

    /**
     * Returns an {@link Encoder} view onto this motor's built-in encoder.
     *
     * <p>The FTC SDK has no standalone encoder device; an encoder is always the one inside a motor.
     * Going through the motor that owns it is therefore the accurate model, not a workaround.
     *
     * @return an encoder backed by this motor
     */
    public Encoder encoder() {
        return new Encoder(motor, name + "/encoder");
    }

    @Override
    public String toString() {
        return "Motor[" + name + "]";
    }
}
