package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.core.HardwareRegistry;
import org.curioone.control.support.FakeHardwareSource;
import org.curioone.control.support.FakeSdkDevices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AnalogSensor}. */
@DisplayName("AnalogSensor")
class AnalogSensorTest {

    private static final double DELTA = 1e-9;

    private FakeSdkDevices.FakeAnalogInput input;

    private AnalogSensor sensor;

    private void setUpInput(double maxVoltage) {
        input = new FakeSdkDevices.FakeAnalogInput(maxVoltage);
        sensor = new AnalogSensor(input, "pot");
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new AnalogSensor(
                                    (com.qualcomm.robotcore.hardware.AnalogInput) null, "x"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new AnalogSensor(new FakeSdkDevices.FakeAnalogInput(), null));
        }

        @Test
        @DisplayName("resolves from a registry")
        void resolvesFromRegistry() {
            final FakeSdkDevices.FakeAnalogInput device = new FakeSdkDevices.FakeAnalogInput();
            final FakeHardwareSource hardware = new FakeHardwareSource();
            hardware.add("pot", device);

            assertSame(
                    device, new AnalogSensor(new HardwareRegistry(hardware), "pot").getSdkObject());
        }

        @Test
        @DisplayName("keeps its name and exposes the escape hatch")
        void exposesNameAndInput() {
            setUpInput(3.3);

            assertEquals("pot", sensor.name());
            assertSame(input, sensor.getSdkObject());
        }
    }

    @Nested
    @DisplayName("voltage")
    class Voltage {

        @Test
        @DisplayName("reports the raw reading")
        void readsVoltage() {
            setUpInput(3.3);
            input.setVoltage(1.65);

            assertEquals(1.65, sensor.getVoltage(), DELTA);
        }
    }

    @Nested
    @DisplayName("fraction")
    class Fraction {

        @Test
        @DisplayName("is zero at zero volts")
        void zeroAtZero() {
            setUpInput(3.3);
            input.setVoltage(0.0);

            assertEquals(0.0, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("is one at the configured maximum")
        void oneAtMax() {
            setUpInput(3.3);
            input.setVoltage(3.3);

            assertEquals(1.0, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("scales linearly in between")
        void linearInBetween() {
            setUpInput(5.0);
            input.setVoltage(2.5);

            assertEquals(0.5, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("honours a non-default configured maximum")
        void honoursConfiguredMaximum() {
            setUpInput(10.0);
            input.setVoltage(1.0);

            assertEquals(0.1, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("returns zero rather than dividing by zero on a degenerate maximum")
        void degenerateMaximum() {
            // A channel configured with a zero maximum cannot produce a meaningful fraction.
            // Returning 0 is a defined answer; NaN or Infinity would propagate into control code.
            setUpInput(0.0);
            input.setVoltage(1.0);

            assertEquals(0.0, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("returns zero for a negative configured maximum")
        void negativeMaximum() {
            setUpInput(-1.0);
            input.setVoltage(1.0);

            assertEquals(0.0, sensor.fraction(), DELTA);
        }

        @Test
        @DisplayName("exceeds one when the reading overdrives the channel")
        void overdrivenChannel() {
            // Documented honestly: the voltage is reported as measured, not clamped, so a
            // misconfigured channel is visible rather than hidden.
            setUpInput(3.3);
            input.setVoltage(5.0);

            assertEquals(5.0 / 3.3, sensor.fraction(), DELTA);
        }
    }

    @Nested
    @DisplayName("mapTo")
    class MapTo {

        @Test
        @DisplayName("maps the low endpoint to the low value")
        void mapsLow() {
            setUpInput(3.3);
            input.setVoltage(0.0);

            assertEquals(-10.0, sensor.mapTo(-10.0, 10.0), DELTA);
        }

        @Test
        @DisplayName("maps the high endpoint to the high value")
        void mapsHigh() {
            setUpInput(3.3);
            input.setVoltage(3.3);

            assertEquals(10.0, sensor.mapTo(-10.0, 10.0), DELTA);
        }

        @Test
        @DisplayName("interpolates linearly in between")
        void interpolates() {
            // 1 V of 4 V is a quarter of the range: -10 + 0.25 * 20 = -5.
            setUpInput(4.0);
            input.setVoltage(1.0);

            assertEquals(-5.0, sensor.mapTo(-10.0, 10.0), DELTA);
        }

        @Test
        @DisplayName("reaches the midpoint at half the range")
        void midpointAtHalfRange() {
            setUpInput(4.0);
            input.setVoltage(2.0);

            assertEquals(0.0, sensor.mapTo(-10.0, 10.0), DELTA);
        }

        @Test
        @DisplayName("collapses to the low value when the range is inverted")
        void invertedRange() {
            setUpInput(3.3);
            input.setVoltage(0.0);

            assertEquals(10.0, sensor.mapTo(10.0, -10.0), DELTA);
        }
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        setUpInput(3.3);

        assertTrue(sensor.toString().contains("pot"), sensor.toString());
    }
}
