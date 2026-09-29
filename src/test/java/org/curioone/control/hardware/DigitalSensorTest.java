package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeSdkDevices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DigitalSensor}. */
@DisplayName("DigitalSensor")
class DigitalSensorTest {

    private FakeSdkDevices.FakeDigitalChannel channel;

    private DigitalSensor sensor;

    private void setUpChannel(boolean state) {
        channel = new FakeSdkDevices.FakeDigitalChannel(state);
        sensor = new DigitalSensor(channel, "limitSwitch");
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
                            new DigitalSensor(
                                    (com.qualcomm.robotcore.hardware.DigitalChannel) null, "x"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new DigitalSensor(new FakeSdkDevices.FakeDigitalChannel(), null));
        }

        @Test
        @DisplayName("keeps its name and exposes the escape hatch")
        void exposesNameAndChannel() {
            setUpChannel(false);

            assertEquals("limitSwitch", sensor.name());
            assertSame(channel, sensor.getSdkObject());
        }
    }

    @Nested
    @DisplayName("reading")
    class Reading {

        @Test
        @DisplayName("reports a high line as high")
        void readsHigh() {
            setUpChannel(true);

            assertTrue(sensor.isHigh());
        }

        @Test
        @DisplayName("reports a low line as low")
        void readsLow() {
            setUpChannel(false);

            assertFalse(sensor.isHigh());
        }

        @Test
        @DisplayName("reflects a change without re-reading configuration")
        void reflectsChange() {
            setUpChannel(false);
            channel.setState(true);

            assertTrue(sensor.isHigh());
        }
    }

    @Nested
    @DisplayName("trigger polarity")
    class TriggerPolarity {

        @Test
        @DisplayName("an active-high sensor triggers on a high line")
        void activeHighOnHigh() {
            setUpChannel(true);

            assertTrue(sensor.isTriggered(true));
            assertFalse(sensor.isTriggered(false));
        }

        @Test
        @DisplayName("an active-low sensor triggers on a low line")
        void activeLowOnLow() {
            setUpChannel(false);

            assertTrue(sensor.isTriggered(false));
            assertFalse(sensor.isTriggered(true));
        }

        @Test
        @DisplayName("the two polarities are exact opposites, so no state is ambiguous")
        void polaritiesAreComplementary() {
            setUpChannel(true);
            assertTrue(
                    sensor.isTriggered(true) != sensor.isTriggered(false),
                    "a switch must either be triggered or not, never both or neither");

            setUpChannel(false);
            assertTrue(sensor.isTriggered(true) != sensor.isTriggered(false));
        }
    }

    @Nested
    @DisplayName("output")
    class Output {

        @Test
        @DisplayName("drives the line high")
        void drivesHigh() {
            setUpChannel(false);

            sensor.setHigh(true);

            assertTrue(sensor.isHigh());
        }

        @Test
        @DisplayName("drives the line low")
        void drivesLow() {
            setUpChannel(true);

            sensor.setHigh(false);

            assertFalse(sensor.isHigh());
        }
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        setUpChannel(false);

        assertTrue(sensor.toString().contains("limitSwitch"), sensor.toString());
    }
}
