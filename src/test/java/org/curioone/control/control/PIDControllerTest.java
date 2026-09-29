package org.curioone.control.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PIDController}.
 *
 * <p>This is the reference implementation of "how to test a controller properly": step response,
 * steady-state error, anti-windup under saturation, and behaviour when the loop interval varies.
 */
@DisplayName("PIDController")
class PIDControllerTest {

    private static final double DELTA = 1e-9;

    private FakeClock clock;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
    }

    /**
     * Makes the controller's first, inert call so later calls have a time baseline.
     *
     * <p>The first {@code calculate} after construction or {@code reset()} contributes no integral
     * and no derivative, because there is no previous reading. A test that asserts on accumulated
     * state must therefore prime the controller first, or it asserts on zero.
     */
    private void prime(PIDController pid, double target, double current) {
        pid.calculate(target, current);
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects a null clock")
        void rejectsNullClock() {
            assertThrows(IllegalArgumentException.class, () -> new PIDController(1, 0, 0, null));
        }

        @Test
        @DisplayName("rejects NaN gains")
        void rejectsNaNGains() {
            assertThrows(IllegalArgumentException.class, () -> new PIDController(Double.NaN, 0, 0));
        }

        @Test
        @DisplayName("exposes the gains it was given")
        void exposesGains() {
            final PIDController pid = new PIDController(0.1, 0.2, 0.3);

            assertEquals(0.1, pid.getKp(), DELTA);
            assertEquals(0.2, pid.getKi(), DELTA);
            assertEquals(0.3, pid.getKd(), DELTA);
        }
    }

    @Nested
    @DisplayName("proportional response")
    class ProportionalResponse {

        @Test
        @DisplayName("scales output by kP and the error")
        void scalesWithError() {
            final PIDController pid = new PIDController(0.5, 0.0, 0.0, clock);

            clock.advanceMillis(20);
            assertEquals(5.0, pid.calculate(10, 0), DELTA);

            clock.advanceMillis(20);
            assertEquals(2.5, pid.calculate(10, 5), DELTA);
        }

        @Test
        @DisplayName("is negative when the measurement overshoots")
        void reversesOnOvershoot() {
            final PIDController pid = new PIDController(0.5, 0.0, 0.0, clock);

            clock.advanceMillis(20);
            assertEquals(-5.0, pid.calculate(0, 10), DELTA);
        }

        @Test
        @DisplayName("contributes nothing on the first call, when there is no previous reading")
        void firstCallSkipsDerivative() {
            final PIDController pid = new PIDController(0.0, 0.0, 1.0, clock);

            // kD is 1 and there is no previous error, so a naive implementation would divide by a
            // zero dt and produce infinity. The first call must be inert.
            assertEquals(0.0, pid.calculate(100, 0), DELTA);
        }
    }

    @Nested
    @DisplayName("integral term")
    class IntegralTerm {

        @Test
        @DisplayName("accumulates error over time, not over calls")
        void accumulatesOverTime() {
            final PIDController pid = new PIDController(0.0, 1.0, 0.0, clock);
            prime(pid, 10, 0);

            clock.advanceMillis(100);
            pid.calculate(10, 0);
            assertEquals(1.0, pid.getIntegral(), DELTA, "10 units of error for 0.1s");

            clock.advanceMillis(100);
            pid.calculate(10, 0);
            assertEquals(2.0, pid.getIntegral(), DELTA);
        }

        @Test
        @DisplayName("is unaffected by how often it is called")
        void isIndependentOfCallRate() {
            final PIDController slow = new PIDController(0.0, 1.0, 0.0, clock);
            final FakeClock otherClock = new FakeClock();
            final PIDController fast = new PIDController(0.0, 1.0, 0.0, otherClock);
            prime(slow, 10, 0);
            prime(fast, 10, 0);

            // 200 ms in one 200 ms step.
            clock.advanceMillis(200);
            slow.calculate(10, 0);

            // The same 200 ms in ten 20 ms steps.
            for (int i = 0; i < 10; i++) {
                otherClock.advanceMillis(20);
                fast.calculate(10, 0);
            }

            assertEquals(
                    slow.getIntegral(),
                    fast.getIntegral(),
                    DELTA,
                    "a variable FTC loop rate must not change the integral term");
        }

        @Test
        @DisplayName("is bounded by the integral limit")
        void respectsIntegralLimit() {
            final PIDController pid = new PIDController(0.0, 1.0, 0.0, clock);
            pid.setIntegralLimit(0.5);
            prime(pid, 10, 0);

            for (int i = 0; i < 100; i++) {
                clock.advanceMillis(100);
                pid.calculate(10, 0);
            }

            assertEquals(0.5, pid.getIntegral(), DELTA);
        }

        @Test
        @DisplayName("rejects a negative integral limit")
        void rejectsNegativeIntegralLimit() {
            final PIDController pid = new PIDController(1, 1, 1);
            assertThrows(IllegalArgumentException.class, () -> pid.setIntegralLimit(-1.0));
        }

        @Test
        @DisplayName("clearing the limit restores unbounded accumulation")
        void clearingIntegralLimit() {
            final PIDController pid = new PIDController(0.0, 1.0, 0.0, clock);
            pid.setIntegralLimit(0.5);
            pid.clearIntegralLimit();
            prime(pid, 10, 0);

            for (int i = 0; i < 100; i++) {
                clock.advanceMillis(100);
                pid.calculate(10, 0);
            }

            // 100 steps x 10 units of error x 0.1s.
            assertEquals(100.0, pid.getIntegral(), DELTA);
        }
    }

    @Nested
    @DisplayName("derivative term")
    class DerivativeTerm {

        @Test
        @DisplayName("is the rate of change of the error")
        void measuresErrorRate() {
            final PIDController pid = new PIDController(0.0, 0.0, 1.0, clock);

            pid.calculate(0, 0);
            clock.advanceMillis(100);
            // Error goes 0 -> 10 over 0.1s, so d(error)/dt is 100 per second.
            assertEquals(100.0, pid.calculate(10, 0), DELTA);
        }

        @Test
        @DisplayName("is zero when the error is not changing")
        void zeroForSteadyError() {
            final PIDController pid = new PIDController(0.0, 0.0, 1.0, clock);

            pid.calculate(10, 0);
            clock.advanceMillis(100);
            assertEquals(0.0, pid.calculate(10, 0), DELTA, "same error, so no rate of change");
        }
    }

    @Nested
    @DisplayName("output limits")
    class OutputLimits {

        @Test
        @DisplayName("saturate at the upper bound")
        void saturatesHigh() {
            final PIDController pid = new PIDController(10.0, 0.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);

            clock.advanceMillis(20);
            assertEquals(1.0, pid.calculate(100, 0), DELTA);
        }

        @Test
        @DisplayName("saturate at the lower bound")
        void saturatesLow() {
            final PIDController pid = new PIDController(10.0, 0.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);

            clock.advanceMillis(20);
            assertEquals(-1.0, pid.calculate(-100, 0), DELTA);
        }

        @Test
        @DisplayName("can be removed")
        void canBeCleared() {
            final PIDController pid = new PIDController(10.0, 0.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);
            pid.clearOutputLimits();

            clock.advanceMillis(20);
            assertEquals(1000.0, pid.calculate(100, 0), DELTA);
        }

        @Test
        @DisplayName("reject an inverted range")
        void rejectsInvertedRange() {
            final PIDController pid = new PIDController(1, 1, 1);
            assertThrows(IllegalArgumentException.class, () -> pid.setOutputLimits(1.0, -1.0));
        }
    }

    @Nested
    @DisplayName("anti-windup")
    class AntiWindup {

        @Test
        @DisplayName("stops integrating while saturated in the error's direction")
        void doesNotIntegrateWhileSaturated() {
            final PIDController pid = new PIDController(1.0, 5.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);

            // Drive hard against the upper limit for a long time.
            for (int i = 0; i < 200; i++) {
                clock.advanceMillis(50);
                pid.calculate(1000, 0);
            }

            assertEquals(
                    0.0,
                    pid.getIntegral(),
                    DELTA,
                    "the integrator must not charge while the output cannot act on the error");
        }

        @Test
        @DisplayName("releases the output as soon as the error reverses")
        void releasesWhenErrorReverses() {
            final PIDController pid = new PIDController(1.0, 5.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);

            for (int i = 0; i < 50; i++) {
                clock.advanceMillis(50);
                pid.calculate(1000, 0);
            }
            for (int i = 0; i < 50; i++) {
                clock.advanceMillis(50);
                pid.calculate(-1000, 0);
            }

            assertEquals(
                    -1.0,
                    pid.calculate(-1000, 0),
                    DELTA,
                    "after a sustained reversal the controller must be able to drive the "
                            + "other way");
        }

        @Test
        @DisplayName("still integrates when there is room to act on the error")
        void integratesWhenNotSaturated() {
            final PIDController pid = new PIDController(0.01, 1.0, 0.0, clock);
            pid.setOutputLimits(-1.0, 1.0);
            prime(pid, 1.0, 0.0);

            clock.advanceMillis(100);
            pid.calculate(1.0, 0.0);

            assertTrue(pid.getIntegral() > 0.0, "a small error with headroom must accumulate");
        }
    }

    @Nested
    @DisplayName("setpoint tolerance")
    class Tolerance {

        @Test
        @DisplayName("is false at a large error by default")
        void defaultToleranceIsExact() {
            final PIDController pid = new PIDController(1, 0, 0, clock);
            clock.advanceMillis(20);
            pid.calculate(100, 0);

            assertFalse(pid.atSetpoint());
        }

        @Test
        @DisplayName("is true within the tolerance")
        void trueWithinTolerance() {
            final PIDController pid = new PIDController(1, 0, 0, clock);
            pid.setTolerance(5.0);

            clock.advanceMillis(20);
            pid.calculate(3.0, 0.0);

            assertTrue(pid.atSetpoint());
        }

        @Test
        @DisplayName("rejects a negative tolerance")
        void rejectsNegativeTolerance() {
            final PIDController pid = new PIDController(1, 1, 1);
            assertThrows(IllegalArgumentException.class, () -> pid.setTolerance(-1.0));
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("clears the integral, derivative, and the previous reading")
        void clearsAccumulatedState() {
            final PIDController pid = new PIDController(1.0, 1.0, 1.0, clock);

            for (int i = 0; i < 10; i++) {
                clock.advanceMillis(50);
                pid.calculate(100, 0);
            }
            assertTrue(pid.getIntegral() != 0.0);

            pid.reset();

            assertEquals(0.0, pid.getIntegral(), DELTA);
        }

        @Test
        @DisplayName("makes the next call behave like a first call")
        void nextCallIsInert() {
            final PIDController pid = new PIDController(0.0, 0.0, 1.0, clock);

            pid.calculate(0, 0);
            clock.advanceMillis(100);
            pid.calculate(100, 0);

            pid.reset();
            assertEquals(0.0, pid.calculate(100, 0), DELTA, "no dt may be inferred after a reset");
        }
    }

    @Nested
    @DisplayName("argument validation")
    class Validation {

        @Test
        @DisplayName("rejects a non-finite target or measurement")
        void rejectsNonFinite() {
            final PIDController pid = new PIDController(1, 0, 0, clock);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> pid.calculate(Double.NaN, 0),
                    "NaN target");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> pid.calculate(0, Double.POSITIVE_INFINITY),
                    "infinite measurement");
        }

        @Test
        @DisplayName("survives a clock that does not advance")
        void survivesStalledClock() {
            final PIDController pid = new PIDController(0.0, 1.0, 1.0, clock);
            pid.setOutputLimits(-1.0, 1.0);

            // A zero dt must not produce a divide-by-zero in the derivative term.
            for (int i = 0; i < 10; i++) {
                assertTrue(Double.isFinite(pid.calculate(10, 0)));
            }
        }
    }

    @Nested
    @DisplayName("step response")
    class StepResponse {

        /**
         * DC gain of the simulated plant. Must exceed 1 so that full output can reach the target;
         * with a plant gain of 1 the target is only reachable asymptotically, and the test would be
         * measuring the plant rather than the controller.
         */
        private static final double PLANT_GAIN = 2.0;

        /** Plant time constant in seconds. A lag, so the loop is not instantaneous. */
        private static final double PLANT_TAU = 0.2;

        /**
         * Runs a first-order plant under the controller.
         *
         * @param pid the controller
         * @param target the setpoint
         * @param steps number of loop iterations
         * @param millisAt the loop interval for each step
         * @return the final position
         */
        private double simulate(PIDController pid, double target, int steps, int[] millisAt) {
            double position = 0.0;
            for (int step = 0; step < steps; step++) {
                final int millis = millisAt[step % millisAt.length];
                clock.advanceMillis(millis);
                final double output = pid.calculate(target, position);
                final double alpha = Math.min(1.0, (millis / 1000.0) / PLANT_TAU);
                position += (output * PLANT_GAIN - position) * alpha;
            }
            return position;
        }

        @Test
        @DisplayName("drives a simulated plant to its target")
        void reachesTarget() {
            final PIDController pid = new PIDController(0.3, 0.2, 0.01, clock);
            pid.setOutputLimits(-1.0, 1.0);
            pid.setTolerance(0.05);

            final double finalPosition = simulate(pid, 1.0, 3000, new int[] {20});

            assertTrue(
                    Math.abs(1.0 - finalPosition) < 0.1,
                    "expected to reach the target, but settled at " + finalPosition);
            assertTrue(pid.atSetpoint(), "expected to be at setpoint");
        }

        @Test
        @DisplayName("converges despite a wildly variable loop interval")
        void toleratesVariableLoopRate() {
            final PIDController pid = new PIDController(0.3, 0.2, 0.01, clock);
            pid.setOutputLimits(-1.0, 1.0);

            // FTC loop times are not fixed, and swing much more than this in practice.
            final double finalPosition = simulate(pid, 1.0, 3000, new int[] {5, 10, 20, 60});

            assertTrue(
                    Math.abs(1.0 - finalPosition) < 0.15,
                    "a variable loop rate must not destabilise the loop, but ended at "
                            + finalPosition);
        }

        @Test
        @DisplayName("never exceeds the output limits while converging")
        void respectsLimitsThroughout() {
            final PIDController pid = new PIDController(50.0, 20.0, 5.0, clock);
            pid.setOutputLimits(-0.5, 0.5);

            for (int step = 0; step < 500; step++) {
                clock.advanceMillis(20);
                final double output = pid.calculate(1.0, 0.0);
                assertTrue(
                        output >= -0.5 && output <= 0.5,
                        "output escaped its limits: " + output + " at step " + step);
            }
        }
    }

    @Nested
    @DisplayName("live gain changes")
    class LiveGainChanges {

        @Test
        @DisplayName("takes effect immediately")
        void appliesNewGains() {
            final PIDController pid = new PIDController(1.0, 0.0, 0.0, clock);
            pid.setOutputLimits(-100.0, 100.0);

            clock.advanceMillis(20);
            assertEquals(10.0, pid.calculate(10, 0), DELTA);

            pid.setGains(2.0, 0.0, 0.0);
            clock.advanceMillis(20);
            assertEquals(20.0, pid.calculate(10, 0), DELTA);
        }

        @Test
        @DisplayName("preserves the accumulated integral")
        void keepsIntegralOnRetune() {
            final PIDController pid = new PIDController(0.0, 1.0, 0.0, clock);

            // The first call establishes the time baseline and contributes no integral, so a
            // second call is needed before anything has accumulated.
            clock.advanceMillis(100);
            pid.calculate(10, 0);
            clock.advanceMillis(100);
            pid.calculate(10, 0);
            final double before = pid.getIntegral();
            assertTrue(before != 0.0, "expected some integral to have accumulated");

            pid.setGains(0.0, 5.0, 0.0);
            assertEquals(
                    before,
                    pid.getIntegral(),
                    DELTA,
                    "retuning mid-run must not discard integral that is correcting real load");
        }
    }

    @Test
    @DisplayName("a controller with no clock and no gains does nothing harmful")
    void degenerateController() {
        final PIDController pid = new PIDController(0.0, 0.0, 0.0);

        try {
            assertEquals(0.0, pid.calculate(1000, -1000), DELTA);
        } catch (Exception e) {
            fail("an all-zero controller must be inert, not throw: " + e);
        }
    }
}
