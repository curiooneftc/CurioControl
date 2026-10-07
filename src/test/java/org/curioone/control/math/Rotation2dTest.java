package org.curioone.control.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Rotation2d}. */
@DisplayName("Rotation2d")
class Rotation2dTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("normalizes into [-pi, pi]")
        void normalizes() {
            assertEquals(Math.PI, Rotation2d.fromRadians(3.0 * Math.PI).getRadians(), DELTA);
            assertEquals(0.0, Rotation2d.fromDegrees(360.0).getRadians(), DELTA);
            assertEquals(90.0, Rotation2d.fromRadians(Math.PI / 2.0).getDegrees(), DELTA);
        }

        @Test
        @DisplayName("rejects non-finite angles")
        void rejectsNonFinite() {
            assertThrows(IllegalArgumentException.class, () -> Rotation2d.fromRadians(Double.NaN));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Rotation2d.fromDegrees(Double.POSITIVE_INFINITY));
        }
    }

    @Nested
    @DisplayName("composition")
    class Composition {

        @Test
        @DisplayName("plus composes two rotations")
        void plusComposes() {
            final Rotation2d sum = Rotation2d.fromDegrees(90.0).plus(Rotation2d.fromDegrees(90.0));

            assertEquals(180.0, sum.getDegrees(), DELTA);
        }

        @Test
        @DisplayName("composition wraps past pi")
        void wrapsPastPi() {
            final Rotation2d sum =
                    Rotation2d.fromDegrees(170.0).plus(Rotation2d.fromDegrees(170.0));

            assertEquals(-20.0, sum.getDegrees(), DELTA);
        }

        @Test
        @DisplayName("minus gives the relative rotation")
        void minusGivesRelative() {
            final Rotation2d relative =
                    Rotation2d.fromDegrees(90.0).minus(Rotation2d.fromDegrees(30.0));

            assertEquals(60.0, relative.getDegrees(), DELTA);
        }

        @Test
        @DisplayName("rotations that point the same way compare equal")
        void equalityAfterNormalization() {
            assertEquals(Rotation2d.fromDegrees(180.0), Rotation2d.fromDegrees(-180.0));
            assertNotEquals(Rotation2d.fromDegrees(90.0), Rotation2d.fromDegrees(91.0));
        }
    }

    @Nested
    @DisplayName("interpolation")
    class Interpolation {

        @Test
        @DisplayName("hits both endpoints")
        void endpoints() {
            final Rotation2d start = Rotation2d.fromDegrees(10.0);
            final Rotation2d end = Rotation2d.fromDegrees(50.0);

            assertEquals(start, start.interpolate(end, 0.0));
            assertEquals(end, start.interpolate(end, 1.0));
        }

        @Test
        @DisplayName("takes the short way across +-pi")
        void shortWayAcrossPi() {
            final Rotation2d middle =
                    Rotation2d.fromDegrees(170.0).interpolate(Rotation2d.fromDegrees(-170.0), 0.5);

            assertEquals(180.0, Math.abs(middle.getDegrees()), DELTA);
        }
    }
}
