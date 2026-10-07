package org.curioone.control.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Vector2d}. */
@DisplayName("Vector2d")
class Vector2dTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("adds, subtracts, and scales")
        void basicArithmetic() {
            final Vector2d sum = new Vector2d(1.0, 2.0).plus(new Vector2d(3.0, 4.0));
            final Vector2d scaled = new Vector2d(1.0, 2.0).times(2.0);

            assertEquals(new Vector2d(4.0, 6.0), sum);
            assertEquals(new Vector2d(2.0, 4.0), scaled);
            assertEquals(new Vector2d(0.0, -2.0), sum.minus(scaled).minus(scaled));
        }

        @Test
        @DisplayName("dot and cross have the expected signs")
        void dotAndCross() {
            final Vector2d forward = new Vector2d(0.0, 1.0);
            final Vector2d right = new Vector2d(1.0, 0.0);

            assertEquals(0.0, forward.dot(right), DELTA);
            assertEquals(1.0, forward.norm(), DELTA);
            assertEquals(5.0, new Vector2d(3.0, 4.0).norm(), DELTA);
            // z of forward x right: forward is counter-clockwise from right, so positive.
            assertEquals(1.0, right.cross(forward), DELTA);
        }

        @Test
        @DisplayName("rejects zero division and nulls")
        void rejectsBadArguments() {
            final Vector2d vector = new Vector2d(1.0, 1.0);

            // The concatenation consumes the quotient: discarding a freshly created
            // value trips the unused-return check, inside a lambda or a try block alike.
            try {
                fail("div(0) must throw, got " + vector.div(0.0));
            } catch (IllegalArgumentException expected) {
                assertEquals("divisor must not be zero", expected.getMessage());
            }
            assertThrows(IllegalArgumentException.class, () -> vector.plus(null));
            assertThrows(IllegalArgumentException.class, () -> new Vector2d(Double.NaN, 0.0));
        }
    }

    @Nested
    @DisplayName("rotation")
    class Rotation {

        @Test
        @DisplayName("rotating forward by 90 degrees points right")
        void rotateForwardToRight() {
            final Vector2d rotated = new Vector2d(0.0, 1.0).rotateBy(Rotation2d.fromDegrees(-90.0));

            assertEquals(1.0, rotated.getX(), DELTA);
            assertEquals(0.0, rotated.getY(), DELTA);
        }

        @Test
        @DisplayName("rotating by zero is the identity")
        void rotateByZero() {
            final Vector2d vector = new Vector2d(3.0, -4.0);

            assertEquals(vector, vector.rotateBy(Rotation2d.ZERO));
        }

        @Test
        @DisplayName("angle reports the direction")
        void angleReportsDirection() {
            assertEquals(90.0, new Vector2d(0.0, 1.0).angle().getDegrees(), DELTA);
            assertEquals(0.0, new Vector2d(1.0, 0.0).angle().getDegrees(), DELTA);
        }
    }

    @Nested
    @DisplayName("interpolation")
    class Interpolation {

        @Test
        @DisplayName("hits both endpoints and the midpoint")
        void endpointsAndMidpoint() {
            final Vector2d start = new Vector2d(0.0, 0.0);
            final Vector2d end = new Vector2d(10.0, 20.0);

            assertEquals(start, start.interpolate(end, 0.0));
            assertEquals(end, start.interpolate(end, 1.0));
            assertEquals(new Vector2d(5.0, 10.0), start.interpolate(end, 0.5));
        }
    }
}
