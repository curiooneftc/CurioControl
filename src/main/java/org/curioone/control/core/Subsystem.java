package org.curioone.control.core;

/**
 * Base class for a mechanism that owns its hardware.
 *
 * <p>A subsystem is the unit of robot behaviour: it looks up its own devices, holds its
 * mechanism-specific logic, and exposes intent rather than hardware. An OpMode then reads as a
 * description of what the robot should do, not a sequence of motor writes.
 *
 * <p>Lifecycle, driven by {@link CurioRobot}:
 *
 * <ol>
 *   <li>{@link #init()} — once, before the first loop
 *   <li>{@link #loop()} — every iteration, and only while the OpMode is running
 *   <li>{@link #publishTelemetry(TelemetryManager)} — every iteration, after {@link #loop()}
 *   <li>{@link #stop()} — once, when the OpMode ends, including on an early exit
 * </ol>
 *
 * <p>Every hook has a no-op default, so a subsystem only overrides what it needs. A subsystem that
 * holds a motor should override {@link #stop()} to zero it — a mechanism left at full power after
 * an OpMode ends is the difference between a robot that sits still and one that keeps driving into
 * the wall.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class Arm extends Subsystem {
 *     private final Motor motor;
 *
 *     public Arm(HardwareMap hardwareMap) {
 *         this.motor = new Motor(hardwareMap, "arm");
 *         motor.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
 *     }
 *
 *     public void moveTo(int ticks) {
 *         motor.setTargetPosition(ticks);
 *     }
 *
 *     &#64;Override
 *     protected void publishTelemetry(TelemetryManager telemetry) {
 *         telemetry.add("Arm/current", motor.getPosition());
 *         telemetry.add("Arm/error", motor.getTargetPosition() - motor.getPosition());
 *     }
 *
 *     &#64;Override
 *     public void stop() {
 *         motor.setPower(0.0);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> a subsystem is called only from the OpMode thread, in the
 * order above. It need not be thread-safe.
 *
 * @since 0.1.0
 */
public abstract class Subsystem {

    private String name;

    /**
     * Returns this subsystem's name, used in diagnostics.
     *
     * <p>Defaults to the simple class name until {@link #init()} runs or {@link
     * CurioRobot#registerSubsystem(Subsystem, String)} overrides it.
     *
     * @return a human-readable name, never {@code null}
     */
    public final String name() {
        return name != null ? name : getClass().getSimpleName();
    }

    /**
     * Sets the name used in diagnostics.
     *
     * @param name a human-readable name; {@code null} restores the class-name default
     */
    final void setName(String name) {
        this.name = name;
    }

    /**
     * Runs once before the first {@link #loop()} call.
     *
     * <p>Look up hardware and configure it here rather than in the constructor: a constructor runs
     * before the OpMode's hardware map is fully populated, and an exception thrown from a
     * constructor surfaces far from the call that caused it.
     *
     * <p>The default implementation does nothing.
     */
    public void init() {
        // Default: most subsystems need no initialization.
    }

    /**
     * Runs once per OpMode iteration while the OpMode is active.
     *
     * <p>The default implementation does nothing. Keep this allocation-free: it is the hottest code
     * a team writes.
     */
    public void loop() {
        // Default: most subsystems are driven by explicit calls from the OpMode.
    }

    /**
     * Reports this subsystem's values to telemetry.
     *
     * <p>Called once per iteration, after {@link #loop()}. Values are buffered and flushed by
     * {@link TelemetryManager#update()}, so adding several is cheap and does not produce several
     * lines of driver output.
     *
     * <p>The default implementation does nothing.
     *
     * @param telemetry the manager to report to, never {@code null}
     */
    protected void publishTelemetry(TelemetryManager telemetry) {
        // Default: not every subsystem has anything worth watching.
    }

    /**
     * Runs once when the OpMode ends, including on an early exit or a watchdog stop.
     *
     * <p>This is the hook that prevents a mechanism from being left moving. Override it to zero
     * motors and release anything that could keep driving.
     *
     * <p>The default implementation does nothing.
     */
    public void stop() {
        // Default: nothing to release.
    }
}
