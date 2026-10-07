package org.curioone.control.command;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.curioone.control.core.Subsystem;

/**
 * A command that runs its children together and finishes when all of them have finished.
 *
 * <p>Every child initializes up front and executes every loop until it reports finished, at which
 * point it ends and drops out while the rest keep running. The group finishes — and releases the
 * union of every child's requirements — only when the last child does. A child that finishes early
 * does not stop the others; if that is what a routine needs, it is a race, not a parallel group,
 * and this class deliberately does not offer that mode. Two completion semantics that look alike in
 * the moment behave differently under cancellation, and conflating them is how an autonomous
 * routine ends up half-done with no error.
 *
 * <pre>{@code
 * Command score = new ParallelCommand(
 *         new MoveArmCommand(arm, RobotConfig.Arm.HIGH),
 *         new SpinIntakeCommand(intake, 1.0));
 * }</pre>
 *
 * @since 0.3.0
 */
public final class ParallelCommand implements Command {

    private final String name;

    private final List<Command> commands;

    private final Set<Subsystem> requirements;

    private final boolean[] finished;

    /**
     * Creates a parallel group with a default name.
     *
     * @param commands the children; must contain at least one, none {@code null}
     * @throws IllegalArgumentException if {@code commands} is {@code null} or empty, or holds a
     *     {@code null}
     */
    public ParallelCommand(Command... commands) {
        this("Parallel", commands);
    }

    /**
     * Creates a named parallel group.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param commands the children; must contain at least one, none {@code null}
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code commands}
     *     is {@code null} or empty, or holds a {@code null}
     */
    public ParallelCommand(String name, Command... commands) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        this.name = name;
        this.commands = Commands.copyOf(commands);
        this.requirements = Commands.unionOf(this.commands);
        this.finished = new boolean[this.commands.size()];
    }

    @Override
    public void initialize() {
        Arrays.fill(finished, false);
        for (Command command : commands) {
            command.initialize();
        }
    }

    @Override
    public void execute() {
        for (int child = 0; child < commands.size(); child++) {
            if (finished[child]) {
                continue;
            }
            final Command command = commands.get(child);
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                finished[child] = true;
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (!interrupted) {
            return;
        }
        for (int child = 0; child < commands.size(); child++) {
            if (!finished[child]) {
                commands.get(child).end(true);
                finished[child] = true;
            }
        }
    }

    @Override
    public boolean isFinished() {
        for (boolean done : finished) {
            if (!done) {
                return false;
            }
        }
        return true;
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
