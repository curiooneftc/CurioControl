package org.curioone.control.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link Range}. */
@DisplayName("Range")
class RangeTest {

    private static final double DELTA = 1e-12;

    @Nested
    @DisplayName("constants")
    class Constants {

        @Test
        @DisplayName("motor power spans [-1, 1]")
        void powerRange() {
            assertEquals(-1.0, Range.MIN_POWER, DELTA);
            assertEquals(1.0, Range.MAX_POWER, DELTA);
        }

        @Test
        @DisplayName("servo position spans [0, 1]")
        void servoRange() {
            assertEquals(0.0, Range.MIN_SERVO_POSITION, DELTA);
            assertEquals(1.0, Range.MAX_SERVO_POSITION, DELTA);
        }
    }

    @Nested
    @DisplayName("clamp")
    class Clamp {

        @ParameterizedTest(name = "clamp({0}) = {1}")
        @CsvSource({
            "-5.0, -1.0",
            "-1.0, -1.0",
            "-0.5, -0.5",
            "0.0, 0.0",
            "0.5, 0.5",
            "1.0, 1.0",
            "5.0, 1.0",
        })
        @DisplayName("clamps a general value into range")
        void clamps(double value, double expected) {
            assertEquals(expected, Range.clamp(value, -1.0, 1.0), DELTA);
        }

        @Test
        @DisplayName("leaves a value already in range untouched")
        void leavesInRangeAlone() {
            assertEquals(0.25, Range.clamp(0.25, 0.0, 1.0), DELTA);
        }

        @Test
        @DisplayName("handles an inverted range by throwing, not by returning nonsense")
        void rejectsInvertedRange() {
            assertThrows(IllegalArgumentException.class, () -> Range.clamp(0.5, 1.0, -1.0));
        }

        @Test
        @DisplayName("accepts a zero-width range")
        void acceptsZeroWidth() {
            assertEquals(2.0, Range.clamp(2.0, 2.0, 2.0), DELTA);
            assertEquals(2.0, Range.clamp(99.0, 2.0, 2.0), DELTA);
        }

        @Test
        @DisplayName("rejects NaN, which would defeat every comparison")
        void rejectsNaN() {
            final IllegalArgumentException thrown =
                    assertThrows(
                            IllegalArgumentException.class, () -> Range.clamp(Double.NaN, 0, 1));

            assertTrue(
                    thrown.getMessage().contains("NaN"),
                    "the message must say why, since NaN slipping through is silent otherwise");
        }

        @Test
        @DisplayName("passes infinities through to the bound, rather than rejecting them")
        void handlesInfinities() {
            assertEquals(1.0, Range.clamp(Double.POSITIVE_INFINITY, -1.0, 1.0), DELTA);
            assertEquals(-1.0, Range.clamp(Double.NEGATIVE_INFINITY, -1.0, 1.0), DELTA);
        }
    }

    @Nested
    @DisplayName("clampPower")
    class ClampPower {

        @ParameterizedTest
        @ValueSource(doubles = {-2.0, -1.0, -0.75, 0.0, 0.75, 1.0, 2.0})
        @DisplayName("matches the general clamp over the power range")
        void matchesGeneralClamp(double power) {
            assertEquals(
                    Range.clamp(power, Range.MIN_POWER, Range.MAX_POWER),
                    Range.clampPower(power),
                    DELTA);
        }
    }

    @Nested
    @DisplayName("clampServoPosition")
    class ClampServoPosition {

        @ParameterizedTest
        @ValueSource(doubles = {-0.1, 0.0, 0.5, 1.0, 1.1})
        @DisplayName("matches the general clamp over the servo range")
        void matchesGeneralClamp(double position) {
            assertEquals(
                    Range.clamp(position, Range.MIN_SERVO_POSITION, Range.MAX_SERVO_POSITION),
                    Range.clampServoPosition(position),
                    DELTA);
        }
    }

    @Nested
    @DisplayName("isValidPower")
    class IsValidPower {

        @ParameterizedTest
        @ValueSource(doubles = {-1.0, -0.001, 0.0, 0.001, 1.0})
        @DisplayName("accepts values in range")
        void acceptsInRange(double power) {
            assertTrue(Range.isValidPower(power));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1.001, 1.001, 42.0, -42.0})
        @DisplayName("rejects values out of range")
        void rejectsOutOfRange(double power) {
            assertFalse(Range.isValidPower(power));
        }

        @Test
        @DisplayName("rejects NaN and infinity, which a bounds check alone would let through")
        void rejectsNonFinite() {
            // NaN fails every comparison, so `power >= MIN && power <= MAX` is false anyway.
            // Infinity would pass a one-sided check, which is the real hazard here.
            assertFalse(Range.isValidPower(Double.NaN));
            assertFalse(Range.isValidPower(Double.POSITIVE_INFINITY));
            assertFalse(Range.isValidPower(Double.NEGATIVE_INFINITY));
        }
    }

    @Nested
    @DisplayName("isValidServoPosition")
    class IsValidServoPosition {

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.5, 1.0})
        @DisplayName("accepts values in range")
        void acceptsInRange(double position) {
            assertTrue(Range.isValidServoPosition(position));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.001, 1.001, Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("rejects values out of range")
        void rejectsOutOfRange(double position) {
            assertFalse(Range.isValidServoPosition(position));
        }
    }

    @Nested
    @DisplayName("as a constant holder")
    class Construction {

        @Test
        @DisplayName("cannot be instantiated")
        void notInstantiable() throws ReflectiveOperationException {
            final Constructor<Range> constructor = Range.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // Reflection wraps whatever the constructor throws.
            final InvocationTargetException thrown =
                    assertThrows(InvocationTargetException.class, constructor::newInstance);
            assertSame(AssertionError.class, thrown.getCause().getClass());
        }

        @Test
        @DisplayName("is final")
        void isFinal() {
            assertTrue(Modifier.isFinal(Range.class.getModifiers()));
        }
    }
}
