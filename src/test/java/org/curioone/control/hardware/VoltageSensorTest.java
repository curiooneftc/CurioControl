package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.core.HardwareRegistry;
import org.curioone.control.support.FakeHardwareSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link VoltageSensor}. */
@DisplayName("VoltageSensor")
class VoltageSensorTest {

    private static final double DELTA = 1e-9;

    private com.qualcomm.robotcore.hardware.VoltageSensor sdk;

    private VoltageSensor wrapper;

    @BeforeEach
    void setUp() {
        sdk = Mockito.mock(com.qualcomm.robotcore.hardware.VoltageSensor.class);
        wrapper = new VoltageSensor(sdk, "battery");
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
                            new VoltageSensor(
                                    (com.qualcomm.robotcore.hardware.VoltageSensor) null,
                                    "battery"));
            assertThrows(IllegalArgumentException.class, () -> new VoltageSensor(sdk, null));
        }

        @Test
        @DisplayName("resolves from a registry")
        void resolvesFromRegistry() {
            final FakeHardwareSource hardware = new FakeHardwareSource();
            hardware.add("battery", sdk);

            assertSame(
                    sdk,
                    new VoltageSensor(new HardwareRegistry(hardware), "battery").getSdkObject());
        }

        @Test
        @DisplayName("keeps its name and exposes the escape hatch")
        void exposesNameAndSdk() {
            assertEquals("battery", wrapper.name());
            assertSame(sdk, wrapper.getSdkObject());
        }
    }

    @Test
    @DisplayName("reports the measured voltage, so a brownout is visible as a number")
    void readsVoltage() {
        Mockito.when(sdk.getVoltage()).thenReturn(12.4);

        assertEquals(12.4, wrapper.getVoltage(), DELTA);
    }

    @Test
    @DisplayName("reflects a change between reads")
    void reflectsChange() {
        Mockito.when(sdk.getVoltage()).thenReturn(12.8, 11.2);

        assertEquals(12.8, wrapper.getVoltage(), DELTA);
        assertEquals(11.2, wrapper.getVoltage(), DELTA);
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        assertTrue(wrapper.toString().contains("battery"), wrapper.toString());
    }
}
