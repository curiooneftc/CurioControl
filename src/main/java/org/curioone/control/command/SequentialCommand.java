package org.curioone.control.command;

import java.util.List;
import java.util.Set;
import org.curioone.control.core.Subsystem;

/**
 * A command that runs its children one after another, in order.
 *
 * <p>Each child initializes when its turn starts — not up front — executes every loop, and ends
 * when it reports finished. The next child initializes immediately, but first executes on the
 * following loop, so every transition has a one-cycle boundary where nothing runs. That boundary is
 * deliberate: a child that finishes and a child that starts in the same loop iteration would
 * interleave their end and initialize in an order no test could pin down.
 *
 * <pre>{@code
 * Command auto = new SequentialCommand(
 *         new DriveForwardCommand(drive, 24.0),
 *         new MoveArmCommand(arm, RobotConfig.Arm.HIGH),
 *         new ScoreCommand(intake));
 * }</pre>
 *
 * <p>The requirements are the union of the children's: while the sequence runs, no outside command
 * may touch any subsystem any child needs. Nesting works — a sequence is a command, so a child may
 * itself be a sequence or a parallel group.
 *
 * @since 0.3.0
 */
public final class SequentialCommand implements Command {

    private final String name;

    private final List<Command> commands;

    private final Set<Subsystem> requirements;

    private int index;

    /**
     * Creates a sequence with a default name.
     *
     * @param commands the children in run order; must contain at least one, none {@code null}
     * @throws IllegalArgumentException if {@code commands} is {@code null} or empty, or holds a
     *     {@code null}
     */
    public SequentialCommand(Command... commands) {
        this("Sequential", commands);
    }

    /**
     * Creates a named sequence.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param commands the children in run order; must contain at least one, none {@code null}
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code commands}
     *     is {@code null} or empty, or holds a {@code null}
     */
    public SequentialCommand(String name, Command... commands) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        this.name = name;
        this.commands = Commands.copyOf(commands);
        this.requirements = Commands.unionOf(this.commands);
        this.index = 0;
    }

    @Override
    public void initialize() {
        index = 0;
        commands.get(0).initialize();
    }

    @Override
    public void execute() {
        if (isFinished()) {
            return;
        }
        final Command current = commands.get(index);
        current.execute();
        if (current.isFinished()) {
            current.end(false);
            index++;
            if (index < commands.size()) {
                commands.get(index).initialize();
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted && index < commands.size()) {
            commands.get(index).end(true);
        }
    }

    @Override
    public boolean isFinished() {
        return index >= commands.size();
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
