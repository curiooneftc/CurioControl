package org.curioone.control.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.curioone.control.core.Subsystem;
import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * Factories for the commands too small to deserve a class.
 *
 * <p>Not every robot action needs a file: waiting half a second, running a lambda once, or grouping
 * two commands is composition, not architecture. These factories build those commands inline, with
 * names and requirements, so a routine reads as what it does:
 *
 * <pre>{@code
 * Command score = Commands.sequence(
 *         Commands.instant("Raise arm", () -> arm.moveTo(HIGH), arm),
 *         Commands.waitUntil("Sample settled", intake::hasSample, intake),
 *         Commands.instant("Release", intake::release, intake));
 * }</pre>
 *
 * <p>Mechanism-specific factories — {@code moveTo(ticks)}, {@code score()} — belong on the
 * subsystem they drive, next to the mechanism logic, returning {@link Command}. They cannot live on
 * {@link Subsystem} itself: the framework's layering keeps {@code core} free of {@code command}, so
 * the base class stays composable without the dependency pointing both ways. See the commands guide
 * for the pattern.
 *
 * <p><strong>Thread safety:</strong> stateless.
 *
 * @since 0.3.0
 */
public final class Commands {

    private Commands() {
        throw new AssertionError("Commands is a factory holder and must not be instantiated.");
    }

    /**
     * Creates an instant command.
     *
     * @param action the action to run once
     * @param requirements the subsystems the action needs
     * @return the command
     * @throws IllegalArgumentException if {@code action} is {@code null}, or a requirement is
     *     {@code null}
     */
    public static InstantCommand instant(Runnable action, Subsystem... requirements) {
        return new InstantCommand(action, requirements);
    }

    /**
     * Creates a named instant command.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param action the action to run once
     * @param requirements the subsystems the action needs
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code action} is
     *     {@code null}, or a requirement is {@code null}
     */
    public static InstantCommand instant(String name, Runnable action, Subsystem... requirements) {
        return new InstantCommand(name, action, requirements);
    }

    /**
     * Creates a wait with a default name, using the system clock.
     *
     * @param seconds how long to wait, in seconds; must not be negative
     * @return the command
     * @throws IllegalArgumentException if {@code seconds} is negative or NaN
     */
    public static WaitCommand waitSeconds(double seconds) {
        return new WaitCommand(seconds);
    }

    /**
     * Creates a named wait, using the system clock.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param seconds how long to wait, in seconds; must not be negative
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, or {@code seconds}
     *     is negative or NaN
     */
    public static WaitCommand waitSeconds(String name, double seconds) {
        return new WaitCommand(name, seconds, new SystemClock());
    }

    /**
     * Creates a wait with an injected time source.
     *
     * @param seconds how long to wait, in seconds; must not be negative
     * @param clock the time source
     * @return the command
     * @throws IllegalArgumentException if {@code clock} is {@code null}, or {@code seconds} is
     *     negative or NaN
     */
    public static WaitCommand waitSeconds(double seconds, Clock clock) {
        return new WaitCommand(seconds, clock);
    }

    /**
     * Creates a command that runs an action every loop until cancelled.
     *
     * <p>Unlike {@link #instant}, this never finishes on its own: it is the shape of TeleOp
     * defaults and held behaviors — drive-from-sticks, hold-position loops, anything that runs
     * while nothing else owns the subsystem. Cancellation stops future executions; halting the
     * mechanism itself stays the action's business (or a following command's).
     *
     * @param action the action to run every loop
     * @param requirements the subsystems the action needs
     * @return the command
     * @throws IllegalArgumentException if {@code action} is {@code null}, or a requirement is
     *     {@code null}
     */
    public static Command run(Runnable action, Subsystem... requirements) {
        return run("Run", action, requirements);
    }

    /**
     * Creates a named command that runs an action every loop until cancelled.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param action the action to run every loop
     * @param requirements the subsystems the action needs
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code action} is
     *     {@code null}, or a requirement is {@code null}
     */
    public static Command run(String name, Runnable action, Subsystem... requirements) {
        return new RunCommand(name, action, requirements);
    }

    /**
     * Creates a command that finishes when a condition becomes true.
     *
     * <p>Polls the condition every loop. The requirements are held the whole time: waiting on a
     * subsystem without requiring it lets another command drive that subsystem mid-wait, and the
     * condition may never become true for a mechanism someone else is moving.
     *
     * @param condition the condition to poll
     * @param requirements the subsystems held while waiting
     * @return the command
     * @throws IllegalArgumentException if {@code condition} is {@code null}, or a requirement is
     *     {@code null}
     */
    public static Command waitUntil(BooleanSupplier condition, Subsystem... requirements) {
        return waitUntil("WaitUntil", condition, requirements);
    }

    /**
     * Creates a named command that finishes when a condition becomes true.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param condition the condition to poll
     * @param requirements the subsystems held while waiting
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code condition}
     *     is {@code null}, or a requirement is {@code null}
     */
    public static Command waitUntil(
            String name, BooleanSupplier condition, Subsystem... requirements) {
        return new ConditionCommand(name, condition, requirements);
    }

    /**
     * Creates a sequence with a default name.
     *
     * @param commands the children in run order
     * @return the command
     * @throws IllegalArgumentException if {@code commands} is {@code null} or empty, or holds a
     *     {@code null}
     */
    public static SequentialCommand sequence(Command... commands) {
        return new SequentialCommand(commands);
    }

    /**
     * Creates a named sequence.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param commands the children in run order
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code commands}
     *     is {@code null} or empty, or holds a {@code null}
     */
    public static SequentialCommand sequence(String name, Command... commands) {
        return new SequentialCommand(name, commands);
    }

    /**
     * Creates a parallel group with a default name.
     *
     * @param commands the children
     * @return the command
     * @throws IllegalArgumentException if {@code commands} is {@code null} or empty, or holds a
     *     {@code null}
     */
    public static ParallelCommand parallel(Command... commands) {
        return new ParallelCommand(commands);
    }

    /**
     * Creates a named parallel group.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param commands the children
     * @return the command
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code commands}
     *     is {@code null} or empty, or holds a {@code null}
     */
    public static ParallelCommand parallel(String name, Command... commands) {
        return new ParallelCommand(name, commands);
    }

    /**
     * Copies child commands into an immutable list, validating as it goes.
     *
     * @param commands the children
     * @return an unmodifiable copy
     * @throws IllegalArgumentException if {@code commands} is {@code null} or empty, or holds a
     *     {@code null}
     */
    static List<Command> copyOf(Command... commands) {
        if (commands == null) {
            throw new IllegalArgumentException("commands must not be null");
        }
        if (commands.length == 0) {
            throw new IllegalArgumentException("commands must contain at least one command");
        }
        final List<Command> copy = new ArrayList<>(commands.length);
        for (Command command : commands) {
            if (command == null) {
                throw new IllegalArgumentException("commands must not contain null");
            }
            copy.add(command);
        }
        return Collections.unmodifiableList(copy);
    }

    /**
     * Unions children's requirements into an immutable set.
     *
     * @param commands the children
     * @return an unmodifiable union
     * @throws IllegalArgumentException if a child returns {@code null} requirements
     */
    static Set<Subsystem> unionOf(List<Command> commands) {
        final Set<Subsystem> union = new HashSet<>();
        for (Command command : commands) {
            final Set<Subsystem> child = command.requirements();
            if (child == null) {
                throw new IllegalArgumentException(
                        "command " + command.name() + " returned null requirements");
            }
            union.addAll(child);
        }
        return Collections.unmodifiableSet(union);
    }

    /**
     * A per-loop action behind {@link #run}.
     *
     * <p>Private because it carries no behaviour worth naming: the action and the requirements are
     * the whole command.
     */
    private static final class RunCommand implements Command {

        private final String name;

        private final Runnable action;

        private final Set<Subsystem> requirements;

        RunCommand(String name, Runnable action, Subsystem... requirements) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name must not be null or empty");
            }
            if (action == null) {
                throw new IllegalArgumentException("action must not be null");
            }
            this.name = name;
            this.action = action;
            this.requirements = InstantCommand.copyRequirements(requirements);
        }

        @Override
        public void execute() {
            action.run();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public Set<Subsystem> requirements() {
            return requirements;
        }

        @Override
        public String name() {
            return name;
        }
    }

    /**
     * A condition-polled command behind {@link #waitUntil}.
     *
     * <p>Private because it carries no behaviour worth naming: the condition and the requirements
     * are the whole command.
     */
    private static final class ConditionCommand implements Command {

        private final String name;

        private final BooleanSupplier condition;

        private final Set<Subsystem> requirements;

        ConditionCommand(String name, BooleanSupplier condition, Subsystem... requirements) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name must not be null or empty");
            }
            if (condition == null) {
                throw new IllegalArgumentException("condition must not be null");
            }
            this.name = name;
            this.condition = condition;
            this.requirements = InstantCommand.copyRequirements(requirements);
        }

        @Override
        public boolean isFinished() {
            return condition.getAsBoolean();
        }

        @Override
        public Set<Subsystem> requirements() {
            return requirements;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
