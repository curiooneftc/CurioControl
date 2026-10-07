package org.curioone.control.bench;

import java.util.concurrent.TimeUnit;
import org.curioone.control.control.PIDController;
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
 * Throughput of {@link PIDController#calculate} on a varying measurement.
 *
 * <p>The measurement cycles a precomputed trajectory so the integral and derivative terms move
 * every call: a constant input would let the JIT settle into a path no real loop ever takes.
 * Budget: bounded, allocation-free steady state — the controller keeps no per-call temporaries.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class PidBenchmark {

    /** Power-of-two-minus-one mask for cycling the trajectory without a division. */
    private static final int TRAJECTORY_MASK = 255;

    /** Setpoint the controller chases. */
    private static final double TARGET = 1000.0;

    /** Swing around the setpoint, in the same units. */
    private static final double SWING = 200.0;

    /** State fields are public because JMH generates its runner in this package. */
    public PIDController pid;

    public double[] trajectory;

    public int index;

    /** Builds the controller and the measurement trajectory. */
    @Setup
    public void setUp() {
        pid = new PIDController(0.01, 0.0, 0.001);
        pid.setOutputLimits(-1.0, 1.0);
        pid.setIntegralLimit(0.25);
        trajectory = new double[TRAJECTORY_MASK + 1];
        for (int point = 0; point < trajectory.length; point++) {
            trajectory[point] = TARGET + SWING * Math.sin(point);
        }
    }

    /**
     * Runs one controller calculation.
     *
     * @param hole consumes the output so the JIT cannot eliminate the call
     */
    @Benchmark
    public void calculate(Blackhole hole) {
        final double current = trajectory[index++ & TRAJECTORY_MASK];
        hole.consume(pid.calculate(TARGET, current));
    }
}
