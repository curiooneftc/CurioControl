package org.curioone.control.core;

import org.curioone.control.command.Command;
import org.curioone.control.command.CommandScheduler;

/**
 * Base class for an autonomous OpMode whose routine is a command tree.
 *
 * <p>Where {@link CurioOpMode} leaves the loop to the team, this class owns a {@link
 * CommandScheduler} and wires it into the OpMode lifecycle (spec §42). Implement {@link
 * #buildRoutine()} to schedule what the robot should do, and optionally {@link #configureRobot()}
 * to register subsystems first:
 *
 * <pre>{@code
 * &#64;Autonomous(name = "Blue Auto")
 * public class BlueAuto extends CurioAuto {
 *     &#64;Override
 *     protected void configureRobot() {
 *         robot.registerSubsystem(new Arm(hardwareMap));
 *         robot.registerSubsystem(new Intake(hardwareMap));
 *     }
 *
 *     &#64;Override
 *     protected void buildRoutine() {
 *         schedule(Commands.sequence(
 *                 new DriveDistance(robot.drive(), 24.0),
 *                 new MoveArm(robot.subsystem(Arm.class), RobotConfig.Arm.HIGH),
 *                 new Score(robot.subsystem(Intake.class))));
 *     }
 * }
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #configureRobot()} — once, after the {@link CurioRobot} exists and before the
 *       routine is built. Register subsystems here.
 *   <li>{@link #buildRoutine()} — once. Schedule the routine; scheduling is staged, so the first
 *       command starts on the first loop, after every subsystem's {@code init()} has run.
 *   <li>{@link #runRobot()} — every loop, runs one scheduler pass.
 *   <li>{@link #stop()} — cancels and ends every command as interrupted, then stops the robot.
 * </ol>
 *
 * <p>{@link #initRobot()} and {@link #runRobot()} are final: the routine is the extension point,
 * not the loop. A team that needs per-loop logic outside the scheduler should put it in a
 * subsystem's {@code loop()}, where it is testable and has a clear owner.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. The SDK calls these methods on the OpMode
 * thread.
 *
 * @since 0.5.0
 */
public abstract class CurioAuto extends CurioOpMode {

    private final CommandScheduler scheduler = new CommandScheduler();

    /**
     * Registers the robot's subsystems, once, before the routine is built.
     *
     * <p>The default implementation does nothing. Override it when the routine needs subsystems:
     * commands scheduled from {@link #buildRoutine()} can then reach them through {@code robot}.
     */
    protected void configureRobot() {
        // Default: a routine that drives the robot directly needs no subsystems.
    }

    /**
     * Builds the autonomous routine by scheduling commands.
     *
     * <p>Called once, after {@link #configureRobot()}. Use {@link #schedule(Command...)}; the
     * commands are staged and start on the first loop, so hardware configured in a subsystem's
     * {@code init()} is ready by then. A routine that never schedules anything is legal, if
     * pointless — the robot simply sits still.
     */
    protected abstract void buildRoutine();

    /**
     * Schedules commands to run.
     *
     * <p>Delegates to the owned scheduler. Scheduling is staged: commands join on the next loop
     * rather than starting mid-iteration.
     *
     * @param commands the commands to schedule
     * @throws IllegalArgumentException if {@code commands} is {@code null} or holds a {@code null}
     */
    protected final void schedule(Command... commands) {
        scheduler.schedule(commands);
    }

    /**
     * Cancels commands, ending them as interrupted.
     *
     * @param commands the commands to cancel
     * @throws IllegalArgumentException if {@code commands} is {@code null} or holds a {@code null}
     */
    protected final void cancel(Command... commands) {
        scheduler.cancel(commands);
    }

    /** Cancels every scheduled command, ending each as interrupted. */
    protected final void cancelAll() {
        scheduler.cancelAll();
    }

    /**
     * Returns the scheduler that runs this OpMode's routine.
     *
     * <p>Exposed for telemetry and for scheduling from inside a command hook. Prefer {@link
     * #schedule(Command...)} from {@link #buildRoutine()}.
     *
     * @return the scheduler, never {@code null}
     */
    protected final CommandScheduler scheduler() {
        return scheduler;
    }

    /**
     * Registers subsystems and builds the routine, once, before the robot starts.
     *
     * <p>Final: the routine is the extension point, not the lifecycle. The robot already exists
     * when this runs, so {@link #configureRobot()} can reach it.
     */
    @Override
    public final void initRobot() {
        configureRobot();
        buildRoutine();
    }

    /** Runs one scheduler pass. Final: the routine is the extension point, not the loop. */
    @Override
    public final void runRobot() {
        scheduler.run();
    }

    /**
     * Stops the routine and the robot.
     *
     * <p>Ends every command as interrupted before the robot's own stop runs, so a mechanism is
     * released by the command that owns it before the subsystem-level cleanup. Override to add
     * cleanup, but call {@code super.stop()} so commands and hardware are still released.
     */
    @Override
    public void stop() {
        scheduler.close();
        super.stop();
    }
}
