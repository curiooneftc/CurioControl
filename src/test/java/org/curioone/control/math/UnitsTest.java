package org.curioone.control.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Units}. */
@DisplayName("Units")
class UnitsTest {

    private static final double DELTA = 1e-9;

    @Test
    @DisplayName("inches and millimetres round-trip")
    void inchMillimeterRoundTrip() {
        assertEquals(25.4, Units.inchesToMillimeters(1.0), DELTA);
        assertEquals(1.0, Units.millimetersToInches(25.4), DELTA);
        assertEquals(96.0, Units.millimetersToInches(Units.inchesToMillimeters(96.0)), DELTA);
    }

    @Test
    @DisplayName("metres convert through millimetres")
    void metricConversions() {
        assertEquals(1000.0, Units.metersToMillimeters(1.0), DELTA);
        assertEquals(1.0, Units.millimetersToMeters(1000.0), DELTA);
        assertEquals(12.0, Units.feetToInches(1.0), DELTA);
        assertEquals(1.0, Units.metersToInches(Units.inchesToMeters(1.0)), 1e-6);
    }

    @Test
    @DisplayName("angle conversions agree with Math")
    void angleConversions() {
        assertEquals(Math.PI, Units.degreesToRadians(180.0), DELTA);
        assertEquals(180.0, Units.radiansToDegrees(Math.PI), DELTA);
        assertEquals(MathUtil.TAU, Units.rotationsToRadians(1.0), DELTA);
        assertEquals(1.0, Units.radiansToRotations(MathUtil.TAU), DELTA);
    }
}
