package org.curioone.control.command;

import java.util.Set;
import org.curioone.control.core.Subsystem;

/**
 * One robot action, run by the {@link CommandScheduler}.
 *
 * <p>A command is a small state machine with a fixed shape: {@link #initialize()} once, {@link
 * #execute()} every loop, {@link #end(boolean)} once, and {@link #isFinished()} polled in between.
 * The scheduler owns the lifecycle — a command never calls its own hooks, and user code never calls
 * them either. That single ownership is what makes commands composable: a {@link SequentialCommand}
 * drives its children through the same hooks the scheduler uses.
 *
 * <pre>{@code
 * Command score = Commands.sequence(
 *         new MoveArmCommand(arm, RobotConfig.Arm.HIGH),
 *         Commands.waitUntil(intake::hasSample),
 *         new ScoreCommand(intake));
 * scheduler.schedule(score);
 * }</pre>
 *
 * <h2>Requirements and ownership</h2>
 *
 * <p>A command declares the subsystems it needs via {@link #requirements()}. The scheduler never
 * runs two commands that need the same subsystem: scheduling a second one preempts (cancels) the
 * first. A command that touches a subsystem without declaring it is the classic way to get two
 * writers on one motor, and the scheduler cannot protect what it cannot see.
 *
 * <p>Only {@code initialize}, {@code execute}, and {@code end} have no-op defaults. {@link
 * #isFinished()} is abstract on purpose: a command that never finishes is a command that never
 * releases its subsystems, and that decision should be visible in the code rather than inherited
 * from a default.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. The scheduler calls every hook from the
 * OpMode thread.
 *
 * @since 0.3.0
 */
public interface Command {

    /**
     * Prepares the command to run.
     *
     * <p>Called once, when the command is scheduled — not when it is constructed. Read sensors and
     * capture start state here; a constructor runs before the OpMode's hardware is ready.
     */
    default void initialize() {
        // Default: most commands need no setup.
    }

    /**
     * Performs the command's work for one loop iteration.
     *
     * <p>Called every loop while the command is scheduled and unfinished. Keep it allocation-free:
     * this is per-loop hot-path code.
     */
    default void execute() {
        // Default: instantaneous commands do their work in initialize.
    }

    /**
     * Cleans up after the command.
     *
     * <p>Called exactly once per run: with {@code false} when {@link #isFinished()} reported
     * completion, with {@code true} when the command was cancelled or preempted. Stop the mechanism
     * here — a cancelled command that leaves its motor running is how a robot drives into a wall
     * after the autonomous routine moved on.
     *
     * @param interrupted {@code true} if the command did not finish on its own
     */
    default void end(boolean interrupted) {
        // Default: nothing to release.
    }

    /**
     * Reports whether the command has completed its work.
     *
     * <p>Polled every loop after {@link #execute()}. A command that is finished is ended with
     * {@code end(false)} and releases its subsystems.
     *
     * @return {@code true} when the work is done
     */
    boolean isFinished();

    /**
     * Returns the subsystems this command needs exclusive access to.
     *
     * <p>The returned set must be immutable and stable for the command's lifetime: the scheduler
     * reads it on every schedule, and a set that changes under it would corrupt the ownership
     * bookkeeping.
     *
     * @return the required subsystems, never {@code null}
     */
    default Set<Subsystem> requirements() {
        return Set.of();
    }

    /**
     * Returns a human-readable name for telemetry and diagnostics.
     *
     * <p>Defaults to the simple class name. Composed commands and factories accept an explicit
     * name, which is what makes a scheduler dump readable during autonomous debugging.
     *
     * @return the name, never {@code null}
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
