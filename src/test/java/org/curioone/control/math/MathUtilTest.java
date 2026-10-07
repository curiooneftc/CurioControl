package org.curioone.control.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MathUtil}. */
@DisplayName("MathUtil")
class MathUtilTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("clamp")
    class Clamp {

        @Test
        @DisplayName("passes values inside the range through")
        void passesThrough() {
            assertEquals(0.5, MathUtil.clamp(0.5, 0.0, 1.0), DELTA);
        }

        @Test
        @DisplayName("limits values outside the range")
        void limits() {
            assertEquals(1.0, MathUtil.clamp(2.0, 0.0, 1.0), DELTA);
            assertEquals(0.0, MathUtil.clamp(-2.0, 0.0, 1.0), DELTA);
        }

        @Test
        @DisplayName("rejects an inverted range and NaN")
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> MathUtil.clamp(0.5, 1.0, 0.0));
            assertThrows(
                    IllegalArgumentException.class, () -> MathUtil.clamp(Double.NaN, 0.0, 1.0));
        }
    }

    @Nested
    @DisplayName("lerp")
    class Lerp {

        @Test
        @DisplayName("hits both endpoints exactly")
        void endpoints() {
            assertEquals(2.0, MathUtil.lerp(2.0, 8.0, 0.0), DELTA);
            assertEquals(8.0, MathUtil.lerp(2.0, 8.0, 1.0), DELTA);
        }

        @Test
        @DisplayName("interpolates the midpoint")
        void midpoint() {
            assertEquals(5.0, MathUtil.lerp(2.0, 8.0, 0.5), DELTA);
        }

        @Test
        @DisplayName("clamps the fraction instead of extrapolating")
        void clampsFraction() {
            assertEquals(2.0, MathUtil.lerp(2.0, 8.0, -1.0), DELTA);
            assertEquals(8.0, MathUtil.lerp(2.0, 8.0, 2.0), DELTA);
        }
    }

    @Nested
    @DisplayName("deadband")
    class Deadband {

        @Test
        @DisplayName("zeroes inputs inside the band")
        void zeroesInsideBand() {
            assertEquals(0.0, MathUtil.applyDeadband(0.05, 0.1), DELTA);
            assertEquals(0.0, MathUtil.applyDeadband(-0.1, 0.1), DELTA);
            assertEquals(0.0, MathUtil.applyDeadband(0.0, 0.1), DELTA);
        }

        @Test
        @DisplayName("rescales outside the band so full stick stays full power")
        void rescalesOutsideBand() {
            assertEquals(1.0, MathUtil.applyDeadband(1.0, 0.1), DELTA);
            assertEquals(-1.0, MathUtil.applyDeadband(-1.0, 0.1), DELTA);
            assertEquals(0.5, MathUtil.applyDeadband(0.55, 0.1), DELTA);
        }

        @Test
        @DisplayName("rejects bad bands")
        void rejectsBadBands() {
            assertThrows(IllegalArgumentException.class, () -> MathUtil.applyDeadband(0.5, -0.1));
            assertThrows(IllegalArgumentException.class, () -> MathUtil.applyDeadband(0.5, 1.0));
            assertThrows(
                    IllegalArgumentException.class, () -> MathUtil.applyDeadband(Double.NaN, 0.1));
        }
    }

    @Nested
    @DisplayName("angle wrapping")
    class Wrapping {

        @Test
        @DisplayName("wrapToPi keeps the sign of the shorter way")
        void wrapToPiKeepsSign() {
            // Exactly -pi canonicalizes to +pi: one name for facing backwards.
            assertEquals(Math.PI, MathUtil.wrapToPi(3.0 * Math.PI), DELTA);
            assertEquals(Math.PI, MathUtil.wrapToPi(-3.0 * Math.PI), DELTA);
            assertEquals(0.0, MathUtil.wrapToPi(2.0 * Math.PI), DELTA);
        }

        @Test
        @DisplayName("wrapToTau lands in [0, 2pi)")
        void wrapToTauRange() {
            assertEquals(0.0, MathUtil.wrapToTau(2.0 * Math.PI), DELTA);
            assertEquals(Math.PI, MathUtil.wrapToTau(-Math.PI), DELTA);
            assertEquals(3.0 * Math.PI / 2.0, MathUtil.wrapToTau(-Math.PI / 2.0), DELTA);
        }

        @Test
        @DisplayName("rejects non-finite angles")
        void rejectsNonFinite() {
            assertThrows(IllegalArgumentException.class, () -> MathUtil.wrapToPi(Double.NaN));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MathUtil.wrapToTau(Double.POSITIVE_INFINITY));
        }
    }
}
