package org.curioone.control.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PIDFController}. */
@DisplayName("PIDFController")
class PIDFControllerTest {

    private static final double DELTA = 1e-9;

    private FakeClock clock;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
    }

    @Nested
    @DisplayName("feedforward contribution")
    class Contribution {

        @Test
        @DisplayName("adds kF times the reference to the PID output")
        void addsScaledReference() {
            final PIDFController controller = new PIDFController(0.5, 0.0, 0.0, 2.0, clock);

            controller.calculate(10.0, 0.0, 0.0);
            clock.advanceMillis(20);

            // P term 0.5 * 10 = 5, plus 2.0 * 1.5 = 3.
            assertEquals(8.0, controller.calculate(10.0, 0.0, 1.5), DELTA);
        }

        @Test
        @DisplayName("with no reference it behaves like a PID controller")
        void behavesLikePid() {
            final PIDFController controller = new PIDFController(0.5, 0.0, 0.0, 1.0, clock);
            final PIDController pid = new PIDController(0.5, 0.0, 0.0, clock);

            controller.calculate(10.0, 0.0, 0.0);
            pid.calculate(10.0, 0.0);
            clock.advanceMillis(20);

            assertEquals(pid.calculate(10.0, 2.0), controller.calculate(10.0, 2.0, 0.0), DELTA);
        }

        @Test
        @DisplayName("the attached model matches an explicit value")
        void modelMatchesExplicit() {
            final PIDFController explicit = new PIDFController(0.5, 0.0, 0.0, 1.0, clock);
            final FakeClock modelClock = new FakeClock();
            final PIDFController modeled =
                    new PIDFController(
                            0.5, 0.0, 0.0, 1.0, new VelocityFeedforward(0.01), modelClock);

            explicit.calculate(10.0, 0.0, 5.0);
            modeled.calculate(10.0, 0.0, 0.0, 500.0, 0.0);
            clock.advanceMillis(20);
            modelClock.advanceMillis(20);

            assertEquals(
                    explicit.calculate(10.0, 2.0, 5.0),
                    modeled.calculate(10.0, 2.0, 2.0, 500.0, 0.0),
                    DELTA);
        }

        @Test
        @DisplayName("state-based calculate without a model fails loudly")
        void missingModelFails() {
            final PIDFController controller = new PIDFController(0.5, 0.0, 0.0, 1.0, clock);

            // try/catch rather than assertThrows: calculate returns a value, and discarding it
            // inside an assertion lambda trips the unused-return check.
            try {
                controller.calculate(10.0, 0.0, 0.0, 0.0, 0.0);
                fail("calculate without a model must throw");
            } catch (IllegalStateException expected) {
                assertTrue(
                        expected.getMessage().contains("no feedforward model"),
                        "got: " + expected.getMessage());
            }
        }

        @Test
        @DisplayName("output limits bind the combined output")
        void limitsBindCombinedOutput() {
            final PIDFController controller = new PIDFController(1.0, 0.0, 0.0, 1.0, clock);
            controller.setOutputLimits(-1.0, 1.0);

            controller.calculate(10.0, 0.0, 0.0);
            clock.advanceMillis(20);

            assertEquals(1.0, controller.calculate(10.0, 0.0, 10.0), DELTA);
        }
    }

    @Nested
    @DisplayName("shared-gain simulation")
    class Simulation {

        /**
         * Runs a position plant with a constant load under one controller.
         *
         * <p>The plant integrates {@code output - load}: holding the target needs a steady output
         * equal to the load, which a P-only loop can only produce by sitting off-target.
         */
        private double runPlant(PIDFController controller, double feedforward, double target) {
            double position = 0.0;
            final double load = 0.3;
            final double dtSeconds = 0.02;
            // A P-only loop closes 1% of the error per step here, so 1000 steps settle well
            // past the transient for both controllers.
            for (int step = 0; step < 1000; step++) {
                final double output = controller.calculate(target, position, feedforward);
                position += (output - load) * dtSeconds * 10.0;
                clock.advanceMillis(20);
            }
            return position;
        }

        @Test
        @DisplayName("feedforward converges where plain P leaves steady-state error")
        void feedforwardConverges() {
            final double target = 100.0;

            final PIDFController withoutFf = new PIDFController(0.05, 0.0, 0.0, 1.0, clock);
            final double plainError = Math.abs(target - runPlant(withoutFf, 0.0, target));

            clock = new FakeClock();
            final PIDFController withFf = new PIDFController(0.05, 0.0, 0.0, 1.0, clock);
            final double ffError = Math.abs(target - runPlant(withFf, 0.3, target));

            // P-only must hold output 0.3 against the load: error settles at 0.3 / 0.05 = 6.
            assertTrue(plainError > 3.0, "expected steady-state error, got " + plainError);
            assertTrue(ffError < 1.0, "expected convergence, got error " + ffError);
        }
    }

    @Nested
    @DisplayName("configuration")
    class Configuration {

        @Test
        @DisplayName("rejects null clocks, null models, and NaN gains")
        void rejectsBadArguments() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PIDFController(1.0, 0.0, 0.0, 1.0, (FakeClock) null));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PIDFController(1.0, 0.0, 0.0, Double.NaN));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PIDFController(1.0, 0.0, 0.0, 1.0, (Feedforward) null));
        }

        @Test
        @DisplayName("exposes its gains")
        void exposesGains() {
            final PIDFController controller = new PIDFController(0.1, 0.2, 0.3, 0.4);

            assertEquals(0.1, controller.getKp(), DELTA);
            assertEquals(0.2, controller.getKi(), DELTA);
            assertEquals(0.3, controller.getKd(), DELTA);
            assertEquals(0.4, controller.getKf(), DELTA);
        }
    }
}
