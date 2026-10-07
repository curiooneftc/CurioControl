package org.curioone.control.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.curioone.control.command.Command;
import org.curioone.control.core.Subsystem;

/**
 * A {@link Command} that records its lifecycle for assertions.
 *
 * <p>Command tests need to prove ordering — initialize before execute, end after finish, no double
 * initialization — and a mock verifies calls but reads poorly for sequences. This records a short
 * event string per hook ({@code "init-A"}, {@code "exec-A"}, {@code "end-A:false"}) so a test
 * asserts the whole lifecycle as one list.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.3.0
 */
public final class RecordingCommand implements Command {

    private final String name;

    private final Set<Subsystem> requirements;

    private final BooleanSupplier finished;

    private final List<String> events = new ArrayList<>();

    private int executions;

    /**
     * Creates a command that never finishes on its own.
     *
     * @param name the name
     * @param requirements the requirements
     */
    public RecordingCommand(String name, Subsystem... requirements) {
        this(name, () -> false, requirements);
    }

    /**
     * Creates a command with a custom finish condition.
     *
     * @param name the name
     * @param finished polled by {@link #isFinished()}
     * @param requirements the requirements
     */
    public RecordingCommand(String name, BooleanSupplier finished, Subsystem... requirements) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (finished == null) {
            throw new IllegalArgumentException("finished must not be null");
        }
        if (requirements == null) {
            throw new IllegalArgumentException("requirements must not be null");
        }
        for (Subsystem requirement : requirements) {
            if (requirement == null) {
                throw new IllegalArgumentException("requirements must not contain null");
            }
        }
        this.name = name;
        this.finished = finished;
        this.requirements = Set.copyOf(List.of(requirements));
    }

    @Override
    public void initialize() {
        events.add("init-" + name);
    }

    @Override
    public void execute() {
        executions++;
        events.add("exec-" + name);
    }

    @Override
    public void end(boolean interrupted) {
        events.add("end-" + name + ":" + interrupted);
    }

    @Override
    public boolean isFinished() {
        return finished.getAsBoolean();
    }

    @Override
    public Set<Subsystem> requirements() {
        return requirements;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the recorded events in order.
     *
     * @return the event log
     */
    public List<String> events() {
        return events;
    }

    /**
     * Returns how many times {@link #execute()} ran.
     *
     * @return the execution count
     */
    public int executions() {
        return executions;
    }
}
