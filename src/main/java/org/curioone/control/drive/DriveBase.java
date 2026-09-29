package org.curioone.control.drive;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * The contract every CurioControl drivetrain implements.
 *
 * <p>Chosen as an abstract class rather than an interface because every implementation shares the
 * same job: take driver input, compute four motor powers, write them, and hold a direction and
 * zero-power configuration. An interface would duplicate that in every drivetrain for no gain.
 *
 * <h2>Not a Subsystem</h2>
 *
 * A drivetrain is deliberately <em>not</em> a {@link org.curioone.control.core.Subsystem}. The
 * drivetrain is driven by explicit calls from the OpMode each loop, so it has no per-loop work of
 * its own to do, and the specification treats it as a peer of subsystems rather than one of them.
 * Keeping the relationship out of the type hierarchy is also what keeps the package graph acyclic:
 * {@code drive} does not depend on {@code core}.
 *
 * <h2>Call order</h2>
 *
 * Call {@link #drive} — or an implementation's more specific method — every loop, and {@link
 * #stop()} when the OpMode ends. A drivetrain left at partial power keeps driving after the OpMode
 * stops, so {@code stop()} is not optional.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public abstract class DriveBase {

    /**
     * Applies a three-axis robot-centric command and writes motor powers.
     *
     * <p>Three axes is the common denominator: strafe, forward, rotation. Implementations normalize
     * their output so no motor is commanded beyond its limits while preserving the requested
     * direction.
     *
     * <p>A drivetrain that cannot strafe — a tank chassis, for instance — ignores the strafe axis.
     * That is a genuine loss of expressiveness, not a silent no-op, and it is why each
     * implementation also offers its own explicit method.
     *
     * @param strafe left/right input in {@code [-1, 1]}, where {@code 1} is right
     * @param forward forward/backward input in {@code [-1, 1]}, where {@code 1} is forward
     * @param rotation rotation input in {@code [-1, 1]}, where {@code 1} is counter-clockwise
     */
    public abstract void drive(double strafe, double forward, double rotation);

    /**
     * Stops all motors and clears any input latched from the last call.
     *
     * <p>Call this when the OpMode ends and whenever a subsystem takes over the drivetrain. A
     * drivetrain left at partial power is the difference between a robot that sits still and one
     * that keeps driving into the wall.
     */
    public abstract void stop();

    /**
     * Sets what every motor does when commanded to zero power.
     *
     * @param behavior {@link DcMotor.ZeroPowerBehavior#BRAKE} to hold position, or {@link
     *     DcMotor.ZeroPowerBehavior#FLOAT} to coast
     * @throws IllegalArgumentException if {@code behavior} is {@code null}
     */
    public abstract void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior);

    /**
     * Returns a human-readable name for this drivetrain, for diagnostics.
     *
     * @return the name, never {@code null}
     */
    public abstract String name();
}
