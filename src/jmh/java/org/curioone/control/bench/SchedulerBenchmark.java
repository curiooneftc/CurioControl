package org.curioone.control.bench;

import java.util.concurrent.TimeUnit;
import org.curioone.control.command.Command;
import org.curioone.control.command.CommandScheduler;
import org.curioone.control.command.InstantCommand;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Cost of {@link CommandScheduler#run()} on two passes: idle steady state, and churn.
 *
 * <p>The steady pass runs three never-finishing no-op commands — the hottest scheduler path, and
 * the one budgeted allocation-free when idle queues stay empty. The churn pass schedules and
 * completes three instant commands every call, exercising staging, initialization, and ending.
 * Budgets: steady is flat and alloc-free; churn is bounded by the commands moving that pass.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class SchedulerBenchmark {

    /** A command that runs forever and does nothing, for the steady pass. */
    private static final class Idle implements Command {
        @Override
        public boolean isFinished() {
            return false;
        }
    }

    /** State fields are public because JMH generates its runner in this package. */
    public CommandScheduler steady;

    /** Idle commands live as long as the trial, so the steady pass never stages. */
    public Command probe;

    /** State fields are public because JMH generates its runner in this package. */
    public CommandScheduler churn;

    /** Builds both schedulers: one settled, one fed fresh commands every call. */
    @Setup
    public void setUp() {
        steady = new CommandScheduler();
        probe = new Idle();
        steady.schedule(new Idle(), new Idle(), probe);
        steady.run();
        churn = new CommandScheduler();
    }

    /**
     * Runs one settled scheduler pass.
     *
     * @param hole consumes scheduler state so the JIT cannot eliminate the call
     */
    @Benchmark
    public void steady(Blackhole hole) {
        steady.run();
        hole.consume(steady.isScheduled(probe));
    }

    /**
     * Schedules, runs, and completes three instant commands.
     *
     * @param hole consumes scheduler state so the JIT cannot eliminate the call
     */
    @Benchmark
    public void churn(Blackhole hole) {
        churn.schedule(
                new InstantCommand(() -> {}),
                new InstantCommand(() -> {}),
                new InstantCommand(() -> {}));
        churn.run();
        hole.consume(churn.scheduledCommands().isEmpty());
    }
}
