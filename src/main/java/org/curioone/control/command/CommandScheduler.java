package org.curioone.control.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.curioone.control.core.Subsystem;
import org.curioone.control.core.TelemetryCategory;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * Runs commands from the OpMode loop, one thread, with exclusive subsystem ownership.
 *
 * <p>Call {@link #run()} once per loop. Everything else — scheduling, cancelling, preemption —
 * funnels through two staged queues that {@code run()} drains first, so a command scheduled from
 * inside another command's hook takes effect on the next pass rather than mutating the running set
 * mid-iteration. There is exactly one ordering, and it never depends on who called what from where:
 *
 * <ol>
 *   <li>Staged cancellations end with {@code end(true)} and leave.
 *   <li>Staged schedules preempt conflicting commands, join, and initialize.
 *   <li>Every scheduled command executes; finished ones end with {@code end(false)} and leave.
 *   <li>Subsystems with nothing running get their default commands, initialized immediately.
 * </ol>
 *
 * <h2>Ownership</h2>
 *
 * <p>Two commands that need the same subsystem never run together: scheduling the second ends the
 * first with {@code end(true)}. Preemption is silent but not invisible — {@link
 * #publishTelemetry(TelemetryManager)} reports what is running, so a routine that keeps losing its
 * arm shows up on the driver display instead of as a mystery.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call everything from the OpMode thread.
 *
 * @since 0.3.0
 */
public final class CommandScheduler {

    /** Nanoseconds per second, for the elapsed-time conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Clock clock;

    private final Set<Command> scheduled = new LinkedHashSet<>();

    private final List<Command> toSchedule = new ArrayList<>();

    private final List<Command> toCancel = new ArrayList<>();

    private final Map<Subsystem, Command> defaults = new LinkedHashMap<>();

    private final Map<Command, Long> startNanos = new LinkedHashMap<>();

    private final List<Command> finished = new ArrayList<>();

    private long preemptions;

    private String lastPreempted;

    /**
     * Creates a scheduler using the system clock.
     *
     * <p>The clock feeds the per-command elapsed times in {@link #publishTelemetry}. It is
     * injectable so a test can assert on exact tenures.
     */
    public CommandScheduler() {
        this(new SystemClock());
    }

    /**
     * Creates a scheduler with an injected time source.
     *
     * @param clock the time source
     * @throws IllegalArgumentException if {@code clock} is {@code null}
     */
    public CommandScheduler(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
    }

    /**
     * Schedules commands to run.
     *
     * <p>Scheduling is staged: a command scheduled from inside a hook — including from {@link
     * #run()} itself — joins on the next pass, never mid-iteration. A command that is already
     * scheduled is ignored rather than initialized twice.
     *
     * @param commands the commands to schedule
     * @throws IllegalArgumentException if {@code commands} is {@code null} or holds a {@code null}
     */
    public void schedule(Command... commands) {
        if (commands == null) {
            throw new IllegalArgumentException("commands must not be null");
        }
        for (Command command : commands) {
            if (command == null) {
                throw new IllegalArgumentException("commands must not contain null");
            }
            if (!scheduled.contains(command) && !toSchedule.contains(command)) {
                toSchedule.add(command);
            }
        }
    }

    /**
     * Cancels commands, ending them as interrupted.
     *
     * <p>Like scheduling, cancellation is staged through {@link #run()}: a command cancelled from
     * inside a hook stops on the next pass. Cancelling a command that is not scheduled does
     * nothing.
     *
     * @param commands the commands to cancel
     * @throws IllegalArgumentException if {@code commands} is {@code null} or holds a {@code null}
     */
    public void cancel(Command... commands) {
        if (commands == null) {
            throw new IllegalArgumentException("commands must not be null");
        }
        for (Command command : commands) {
            if (command == null) {
                throw new IllegalArgumentException("commands must not contain null");
            }
            toSchedule.remove(command);
            if (!toCancel.contains(command)) {
                toCancel.add(command);
            }
        }
    }

    /** Cancels every scheduled command, ending each as interrupted. */
    public void cancelAll() {
        toSchedule.clear();
        for (Command command : scheduled) {
            if (!toCancel.contains(command)) {
                toCancel.add(command);
            }
        }
    }

    /**
     * Runs one scheduler pass: cancellations, schedules, executions, then defaults.
     *
     * <p>Call once per OpMode loop, in the same place every loop — before or after the subsystem
     * updates, but consistently. A command's {@code execute} sees the freshest sensor values when
     * the scheduler runs after the subsystems that produce them.
     */
    /**
     * Runs one scheduler pass: cancellations, schedules, executions, then defaults.
     *
     * <p>Call once per OpMode loop, in the same place every loop — before or after the subsystem
     * updates, but consistently. A command's {@code execute} sees the freshest sensor values when
     * the scheduler runs after the subsystems that produce them.
     *
     * <p>An idle pass allocates nothing: empty queues are skipped rather than drained, and the
     * finished list is reused. An active pass allocates bounded temporaries proportional to the
     * commands moving that pass — the steady-state cost of composition, measured by the JMH harness
     * rather than assumed.
     */
    public void run() {
        // Drain into copies: end() hooks may schedule or cancel, which appends to the staged
        // lists, and iterating a list that a hook extends is a ConcurrentModificationException.
        // Anything staged from inside this pass takes effect on the next one.
        if (!toCancel.isEmpty()) {
            final List<Command> cancellations = new ArrayList<>(toCancel);
            toCancel.clear();
            for (Command command : cancellations) {
                if (scheduled.remove(command)) {
                    startNanos.remove(command);
                    command.end(true);
                }
            }
        }

        if (!toSchedule.isEmpty()) {
            final List<Command> additions = new ArrayList<>(toSchedule);
            toSchedule.clear();
            for (Command command : additions) {
                if (toCancel.contains(command)) {
                    continue;
                }
                preemptConflicts(command);
                if (scheduled.add(command)) {
                    startNanos.put(command, clock.nowNanos());
                    command.initialize();
                }
            }
        }

        finished.clear();
        for (Command command : scheduled) {
            // A command cancelled earlier in this pass — from inside another command's hook —
            // sits out the rest of the pass and ends on the next one.
            if (toCancel.contains(command)) {
                continue;
            }
            command.execute();
            if (command.isFinished()) {
                finished.add(command);
            }
        }
        for (Command command : finished) {
            scheduled.remove(command);
            startNanos.remove(command);
            command.end(false);
        }

        // Iterated live: defaults configuration does not belong in command hooks, and a hook
        // that reconfigures defaults mid-pass fails loudly rather than corrupting the map.
        for (Map.Entry<Subsystem, Command> entry : defaults.entrySet()) {
            if (isFree(entry.getKey()) && !scheduled.contains(entry.getValue())) {
                scheduled.add(entry.getValue());
                startNanos.put(entry.getValue(), clock.nowNanos());
                entry.getValue().initialize();
            }
        }
    }

    /**
     * Sets the command that runs on a subsystem when nothing else needs it.
     *
     * <p>A default is how a drivetrain keeps driving the sticks while no autonomous command owns
     * it, or how an arm holds position between moves. It starts on the first {@link #run()} after
     * its subsystem goes free, and any scheduled command needing that subsystem preempts it like
     * anything else. Replaces any previous default for the subsystem.
     *
     * @param subsystem the subsystem to cover
     * @param command the default command
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public void setDefaultCommand(Subsystem subsystem, Command command) {
        if (subsystem == null) {
            throw new IllegalArgumentException("subsystem must not be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        defaults.put(subsystem, command);
    }

    /**
     * Removes a subsystem's default command, cancelling it if it is running.
     *
     * @param subsystem the subsystem
     * @throws IllegalArgumentException if {@code subsystem} is {@code null}
     */
    public void removeDefaultCommand(Subsystem subsystem) {
        if (subsystem == null) {
            throw new IllegalArgumentException("subsystem must not be null");
        }
        final Command removed = defaults.remove(subsystem);
        if (removed != null) {
            cancel(removed);
        }
    }

    /**
     * Reports whether a command is currently scheduled.
     *
     * @param command the command
     * @return {@code true} if it will execute on the next {@link #run()}
     * @throws IllegalArgumentException if {@code command} is {@code null}
     */
    public boolean isScheduled(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        return scheduled.contains(command);
    }

    /**
     * Returns a snapshot of the scheduled commands, in run order.
     *
     * @return an unmodifiable copy of the running set
     */
    public List<Command> scheduledCommands() {
        return List.copyOf(scheduled);
    }

    /**
     * Reports scheduler state to telemetry.
     *
     * <p>Writes the running count, the running names with their tenures, the lifetime preemption
     * count, and the last preempted command, all under the {@code DEBUG} category — so a quiet
     * dashboard stays quiet and a debugging session shows exactly which commands own which
     * subsystems, for how long, and what got bumped. Call once per loop; the manager's flush
     * cadence applies as usual. Does not flush — the caller owns the flush, alongside everything
     * else it reports.
     *
     * @param telemetry the manager to report to
     * @throws IllegalArgumentException if {@code telemetry} is {@code null}
     */
    public void publishTelemetry(TelemetryManager telemetry) {
        if (telemetry == null) {
            throw new IllegalArgumentException("telemetry must not be null");
        }
        telemetry.add(TelemetryCategory.DEBUG, "Scheduler/running", scheduled.size());
        if (scheduled.isEmpty()) {
            telemetry.add(TelemetryCategory.DEBUG, "Scheduler/commands", "(none)");
        } else {
            final StringBuilder names = new StringBuilder();
            final long now = clock.nowNanos();
            for (Command command : scheduled) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(command.name())
                        .append(" (")
                        .append(tenureSeconds(command, now))
                        .append('s')
                        .append(')');
            }
            telemetry.add(TelemetryCategory.DEBUG, "Scheduler/commands", names.toString());
        }
        telemetry.add(TelemetryCategory.DEBUG, "Scheduler/preemptions", Long.toString(preemptions));
        telemetry.add(
                TelemetryCategory.DEBUG,
                "Scheduler/last-preempted",
                lastPreempted == null ? "(none)" : lastPreempted);
    }

    /**
     * Returns how many preemptions have happened on this scheduler.
     *
     * @return the lifetime preemption count
     */
    public long preemptions() {
        return preemptions;
    }

    private void preemptConflicts(Command incoming) {
        final Set<Subsystem> wanted = incoming.requirements();
        if (wanted.isEmpty()) {
            return;
        }
        final List<Command> conflicts = new ArrayList<>();
        for (Command running : scheduled) {
            for (Subsystem subsystem : running.requirements()) {
                if (wanted.contains(subsystem)) {
                    conflicts.add(running);
                    break;
                }
            }
        }
        for (Command conflict : conflicts) {
            scheduled.remove(conflict);
            startNanos.remove(conflict);
            conflict.end(true);
            preemptions++;
            lastPreempted = conflict.name();
        }
    }

    private String tenureSeconds(Command command, long nowNanos) {
        final Long started = startNanos.get(command);
        final double seconds = started == null ? 0.0 : (nowNanos - started) / NANOS_PER_SECOND;
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private boolean isFree(Subsystem subsystem) {
        for (Command running : scheduled) {
            if (running.requirements().contains(subsystem)) {
                return false;
            }
        }
        return true;
    }
}
