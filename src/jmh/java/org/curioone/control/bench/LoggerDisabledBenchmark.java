package org.curioone.control.bench;

import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.curioone.control.core.CurioConfig;
import org.curioone.control.support.FakeClock;
import org.curioone.control.telemetry.Logger;
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

/**
 * Floor cost of the {@link Logger} with logging disabled.
 *
 * <p>Each call records three values and ends the row — every one returning after a single flag
 * check. A correct JIT eliminates the no-op path down to that check, which is exactly the claim:
 * the disabled cost is the check itself, and the allocation budget is zero by construction (no
 * {@code new} on the disabled path). Time here is a regression tripwire, not a precision
 * measurement; allocation is asserted structurally and with {@code -prof gc} runs.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class LoggerDisabledBenchmark {

    /** State fields are public because JMH generates its runner in this package. */
    public Logger logger;

    /** Restored after the trial: the logging flag is global framework configuration. */
    public boolean savedLoggingEnabled;

    /** Builds a logger whose sink never fills, and switches logging off. */
    @Setup
    public void setUp() {
        savedLoggingEnabled = CurioConfig.LOGGING_ENABLED;
        CurioConfig.LOGGING_ENABLED = false;
        logger = new Logger(new StringWriter(), new FakeClock());
        logger.registerField("heading");
        logger.registerField("arm");
        logger.registerField("power");
    }

    /** Restores the logging flag. */
    @TearDown
    public void tearDown() {
        CurioConfig.LOGGING_ENABLED = savedLoggingEnabled;
    }

    /** Records a row with logging disabled. */
    @Benchmark
    public void recordDisabled() {
        logger.record("heading", 0.5);
        logger.record("arm", 100.0);
        logger.record("power", 0.5);
        logger.update();
    }
}
