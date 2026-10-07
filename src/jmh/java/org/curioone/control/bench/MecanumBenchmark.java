package org.curioone.control.bench;

import java.util.concurrent.TimeUnit;
import org.curioone.control.drive.MecanumDrive;
import org.curioone.control.hardware.Motor;
import org.curioone.control.support.FakeDcMotor;
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
 * Cost of one {@link MecanumDrive#mecanum} call: wheel equations, normalization, motor writes.
 *
 * <p>Inputs cycle precomputed stick positions, including saturated diagonals that exercise the
 * normalization path. The motors are fakes — the benchmark measures the framework's math, not an
 * SDK the harness does not have. Budget: allocation-free; the drive keeps no per-call temporaries.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class MecanumBenchmark {

    /** Power-of-two-minus-one mask for cycling the inputs without a division. */
    private static final int INPUT_MASK = 255;

    /** State fields are public because JMH generates its runner in this package. */
    public MecanumDrive drive;

    public FakeDcMotor frontLeft;

    public double[] strafe;

    public double[] forward;

    public double[] rotation;

    public int index;

    /** Builds the drivetrain on fakes and a cycled input trajectory. */
    @Setup
    public void setUp() {
        frontLeft = new FakeDcMotor();
        final FakeDcMotor frontRight = new FakeDcMotor();
        final FakeDcMotor backLeft = new FakeDcMotor();
        final FakeDcMotor backRight = new FakeDcMotor();
        drive =
                new MecanumDrive(
                        "bench",
                        new Motor(frontLeft, "fl"),
                        new Motor(frontRight, "fr"),
                        new Motor(backLeft, "bl"),
                        new Motor(backRight, "br"));
        strafe = new double[INPUT_MASK + 1];
        forward = new double[INPUT_MASK + 1];
        rotation = new double[INPUT_MASK + 1];
        for (int point = 0; point <= INPUT_MASK; point++) {
            // Full diagonals: the saturated path is the one worth measuring.
            strafe[point] = Math.sin(point);
            forward[point] = Math.cos(point);
            rotation[point] = Math.sin(point) * Math.cos(point);
        }
    }

    /**
     * Runs one mecanum calculation and consumes a wheel power so the call stays live.
     *
     * @param hole consumes a wheel power so the JIT cannot eliminate the call
     */
    @Benchmark
    public void mecanum(Blackhole hole) {
        final int at = index++ & INPUT_MASK;
        drive.mecanum(strafe[at], forward[at], rotation[at]);
        hole.consume(frontLeft.lastPower());
    }
}
