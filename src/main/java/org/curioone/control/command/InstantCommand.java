package org.curioone.control.command;

import java.util.Arrays;
import java.util.Set;
import org.curioone.control.core.Subsystem;

/**
 * A command that runs an action once and finishes immediately.
 *
 * <p>The action runs in {@link #initialize()}, and {@link #isFinished()} is true from the start, so
 * the command occupies exactly one scheduler run: scheduled, initialized, executed, ended. It still
 * participates in ownership — its requirements preempt and are preempted like any other command's —
 * which is what makes "stop the intake, then start the next thing" safe to express as a sequence.
 *
 * <pre>{@code
 * scheduler.schedule(Commands.instant("Stop intake", () -> intake.setPower(0.0), intake));
 * }</pre>
 *
 * @since 0.3.0
 */
public final class InstantCommand implements Command {

    private final String name;

    private final Runnable action;

    private final Set<Subsystem> requirements;

    /**
     * Creates an instant command with a default name.
     *
     * @param action the action to run once
     * @param requirements the subsystems this command needs
     * @throws IllegalArgumentException if {@code action} is {@code null}, or a requirement is
     *     {@code null}
     */
    public InstantCommand(Runnable action, Subsystem... requirements) {
        this("Instant", action, requirements);
    }

    /**
     * Creates a named instant command.
     *
     * @param name the name for telemetry and diagnostics; must not be blank
     * @param action the action to run once
     * @param requirements the subsystems this command needs
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank, {@code action} is
     *     {@code null}, or a requirement is {@code null}
     */
    public InstantCommand(String name, Runnable action, Subsystem... requirements) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.name = name;
        this.action = action;
        this.requirements = copyRequirements(requirements);
    }

    @Override
    public void initialize() {
        action.run();
    }

    @Override
    public boolean isFinished() {
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

    static Set<Subsystem> copyRequirements(Subsystem... requirements) {
        if (requirements == null) {
            throw new IllegalArgumentException("requirements must not be null");
        }
        for (Subsystem requirement : requirements) {
            if (requirement == null) {
                throw new IllegalArgumentException("requirements must not contain null");
            }
        }
        return Set.copyOf(Arrays.asList(requirements));
    }
}
