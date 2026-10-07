package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.curioone.control.core.HardwareRegistry;
import org.curioone.control.support.FakeHardwareSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

/** Unit tests for {@link ContinuousServo}. */
@DisplayName("ContinuousServo")
class ContinuousServoTest {

    private static final double DELTA = 1e-9;

    private CRServo sdk;

    private ContinuousServo wrapper;

    @BeforeEach
    void setUp() {
        sdk = Mockito.mock(CRServo.class);
        wrapper = new ContinuousServo(sdk, "intake");
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class, () -> new ContinuousServo((CRServo) null, "x"));
            assertThrows(IllegalArgumentException.class, () -> new ContinuousServo(sdk, null));
        }

        @Test
        @DisplayName("resolves from a registry")
        void resolvesFromRegistry() {
            final FakeHardwareSource hardware = new FakeHardwareSource();
            hardware.add("intake", sdk);

            assertSame(
                    sdk,
                    new ContinuousServo(new HardwareRegistry(hardware), "intake").getSdkObject());
        }

        @Test
        @DisplayName("keeps its name and exposes the escape hatch")
        void exposesNameAndSdk() {
            assertEquals("intake", wrapper.name());
            assertSame(sdk, wrapper.getSdkObject());
        }
    }

    @Nested
    @DisplayName("power")
    class Power {

        @ParameterizedTest
        @ValueSource(doubles = {-1.0, -0.5, 0.0, 0.5, 1.0})
        @DisplayName("accepts a power in range")
        void acceptsInRange(double power) {
            wrapper.setPower(power);

            Mockito.verify(sdk).setPower(power);
        }

        @Test
        @DisplayName("stop zeroes the servo")
        void stopZeroes() {
            wrapper.setPower(0.5);
            wrapper.stop();

            Mockito.verify(sdk).setPower(0.0);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1.001, 1.001, 5.0, Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("rejects a power out of range rather than silently clamping")
        void rejectsOutOfRange(double power) {
            assertThrows(IllegalArgumentException.class, () -> wrapper.setPower(power));
            Mockito.verify(sdk, Mockito.never()).setPower(Mockito.anyDouble());
        }

        @Test
        @DisplayName("names the servo in the rejection")
        void rejectionNamesServo() {
            final String message =
                    assertThrows(IllegalArgumentException.class, () -> wrapper.setPower(2.0))
                            .getMessage();

            assertTrue(message.contains("intake"), message);
        }

        @Test
        @DisplayName("reads back the current power")
        void readsPower() {
            Mockito.when(sdk.getPower()).thenReturn(-0.25);

            assertEquals(-0.25, wrapper.getPower(), DELTA);
        }
    }

    @Test
    @DisplayName("passes direction through and rejects null")
    void setsDirection() {
        wrapper.setDirection(DcMotorSimple.Direction.REVERSE);
        Mockito.verify(sdk).setDirection(DcMotorSimple.Direction.REVERSE);

        assertThrows(IllegalArgumentException.class, () -> wrapper.setDirection(null));
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        assertTrue(wrapper.toString().contains("intake"), wrapper.toString());
    }
}
