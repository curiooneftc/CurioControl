package org.curioone.control.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Feedforward} models. */
@DisplayName("Feedforward models")
class FeedforwardTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("ConstantFeedforward")
    class Constant {

        @Test
        @DisplayName("pushes with the direction of motion, nothing at rest")
        void directionOfMotion() {
            final Feedforward model = new ConstantFeedforward(0.2);

            assertEquals(0.2, model.calculate(0.0, 100.0, 0.0), DELTA);
            assertEquals(-0.2, model.calculate(0.0, -100.0, 0.0), DELTA);
            assertEquals(0.0, model.calculate(0.0, 0.0, 0.0), DELTA);
        }
    }

    @Nested
    @DisplayName("VelocityFeedforward")
    class Velocity {

        @Test
        @DisplayName("scales with velocity")
        void scalesWithVelocity() {
            final Feedforward model = new VelocityFeedforward(0.002);

            assertEquals(2.0, model.calculate(0.0, 1000.0, 0.0), DELTA);
            assertEquals(-1.0, model.calculate(0.0, -500.0, 0.0), DELTA);
        }
    }

    @Nested
    @DisplayName("AccelerationFeedforward")
    class Acceleration {

        @Test
        @DisplayName("scales with acceleration")
        void scalesWithAcceleration() {
            final Feedforward model = new AccelerationFeedforward(0.0005);

            assertEquals(1.0, model.calculate(0.0, 0.0, 2000.0), DELTA);
            assertEquals(-0.5, model.calculate(0.0, 0.0, -1000.0), DELTA);
        }
    }

    @Nested
    @DisplayName("GravityFeedforward")
    class Gravity {

        @Test
        @DisplayName("holds full effort horizontal, none vertical")
        void horizontalVsVertical() {
            final Feedforward model = new GravityFeedforward(0.3);

            assertEquals(0.3, model.calculate(0.0, 0.0, 0.0), DELTA);
            assertEquals(0.0, model.calculate(Math.PI / 2.0, 0.0, 0.0), DELTA);
            assertEquals(-0.3, model.calculate(Math.PI, 0.0, 0.0), DELTA);
        }
    }

    @Nested
    @DisplayName("CombinedFeedforward")
    class Combined {

        @Test
        @DisplayName("adds all three terms")
        void addsTerms() {
            final Feedforward model = new CombinedFeedforward(0.2, 0.002, 0.0005);

            // 0.2 * 1 + 0.002 * 1000 + 0.0005 * 2000 = 0.2 + 2.0 + 1.0.
            assertEquals(3.2, model.calculate(0.0, 1000.0, 2000.0), DELTA);
        }

        @Test
        @DisplayName("static term follows the sign of velocity")
        void staticTermSign() {
            final Feedforward model = new CombinedFeedforward(0.2, 0.0, 0.0);

            assertEquals(-0.2, model.calculate(0.0, -50.0, 0.0), DELTA);
        }

        @Test
        @DisplayName("rejects non-finite gains and states")
        void rejectsBadArguments() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new CombinedFeedforward(Double.NaN, 0.0, 0.0));

            final Feedforward model = new CombinedFeedforward(0.2, 0.002, 0.0005);
            assertThrows(
                    IllegalArgumentException.class, () -> model.calculate(0.0, Double.NaN, 0.0));
        }
    }
}
