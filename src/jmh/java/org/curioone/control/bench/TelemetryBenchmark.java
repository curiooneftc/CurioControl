package org.curioone.control.bench;

import java.util.concurrent.TimeUnit;
import org.curioone.control.core.CurioConfig;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.core.TelemetrySink;
import org.curioone.control.support.FakeClock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Cost of buffering four telemetry values and flushing them to a sink.
 *
 * <p>Runs with the flush period at zero so every call exercises the full path: buffer, format
 * through the sink, clear. The sink discards — the benchmark measures the framework's batching, not
 * I/O. Budget: no collection growth per call; the documented per-value costs (constant keys, boxed
 * doubles) are the only allocations.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TelemetryBenchmark {

    /** State fields are public because JMH generates its runner in this package. */
    public TelemetryManager telemetry;

    /** Restored after the trial: the flush period is global framework configuration. */
    public int savedPeriodMillis;

    /** Buffers values and a discarding sink; forces a flush every call. */
    @Setup
    public void setUp() {
        savedPeriodMillis = CurioConfig.TELEMETRY_PERIOD_MILLIS;
        CurioConfig.TELEMETRY_PERIOD_MILLIS = 0;
        telemetry = new TelemetryManager(TelemetrySink.discarding(), new FakeClock());
    }

    /** Restores the flush period. */
    @TearDown
    public void tearDown() {
        CurioConfig.TELEMETRY_PERIOD_MILLIS = savedPeriodMillis;
    }

    /**
     * Buffers four values of mixed types and flushes.
     *
     * @param hole consumes the buffer size so the JIT cannot eliminate the call
     */
    @Benchmark
    public void update(Blackhole hole) {
        telemetry.add("bench-angle", 1.5);
        telemetry.add("bench-ticks", 42);
        telemetry.add("bench-state", "drive");
        telemetry.add("bench-voltage", 12.6);
        telemetry.update();
        hole.consume(telemetry.size());
    }
}
