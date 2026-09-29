package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.core.CurioException;
import org.curioone.control.core.HardwareRegistry;
import org.curioone.control.support.FakeHardwareSource;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link IMU}. */
@DisplayName("IMU")
class IMUTest {

    private static final double DELTA = 1e-9;

    private static final double TAU = 2.0 * Math.PI;

    private com.qualcomm.robotcore.hardware.IMU sdk;

    private IMU wrapper;

    @BeforeEach
    void setUp() {
        sdk = Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class);
        wrapper = new IMU(sdk, "imu");
    }

    /**
     * Builds mounting parameters with an identity orientation.
     *
     * <p>Only used as a token to pass to {@code initialize}: the wrapper forwards the object
     * without inspecting it, because the mounting is a property of the robot the framework cannot
     * know (ADR-012). The IMU is mocked, so the orientation is never used.
     */
    private static com.qualcomm.robotcore.hardware.IMU.Parameters newParameters() {
        final Quaternion identity = Quaternion.identityQuaternion();
        final com.qualcomm.robotcore.hardware.ImuOrientationOnRobot orientation =
                Mockito.mock(com.qualcomm.robotcore.hardware.ImuOrientationOnRobot.class);
        Mockito.when(orientation.imuCoordinateSystemOrientationFromPerspectiveOfRobot())
                .thenReturn(identity);
        Mockito.when(orientation.imuRotationOffset()).thenReturn(identity);
        Mockito.when(orientation.angularVelocityTransform()).thenReturn(identity);

        return new com.qualcomm.robotcore.hardware.IMU.Parameters(orientation);
    }

    /** Makes the fake IMU report a fixed orientation. */
    private void givenOrientation(double yaw, double pitch, double roll) {
        Mockito.when(sdk.getRobotYawPitchRollAngles())
                .thenReturn(new YawPitchRollAngles(AngleUnit.RADIANS, yaw, pitch, roll, 0L));
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new IMU((com.qualcomm.robotcore.hardware.IMU) null, "imu"));
            assertThrows(IllegalArgumentException.class, () -> new IMU(sdk, null));
        }

        @Test
        @DisplayName("resolves from a registry")
        void resolvesFromRegistry() {
            final FakeHardwareSource hardware = new FakeHardwareSource();
            hardware.add("imu", sdk);

            assertSame(sdk, new IMU(new HardwareRegistry(hardware), "imu").getSdkObject());
        }
    }

    @Nested
    @DisplayName("calibration")
    class Calibration {

        @Test
        @DisplayName("reports success without throwing")
        void calibrateSucceeds() {
            final com.qualcomm.robotcore.hardware.IMU.Parameters parameters = newParameters();
            Mockito.when(sdk.initialize(parameters)).thenReturn(true);

            assertTrue(wrapper.calibrate(parameters));
            Mockito.verify(sdk).initialize(parameters);
        }

        @Test
        @DisplayName("reports failure without throwing, for a caller that wants to handle it")
        void calibrateFails() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(false);

            assertEquals(false, wrapper.calibrate(newParameters()));
        }

        @Test
        @DisplayName("rejects null parameters")
        void rejectsNullParameters() {
            assertThrows(IllegalArgumentException.class, () -> wrapper.calibrate(null));
            assertThrows(IllegalArgumentException.class, () -> wrapper.requireCalibration(null));
        }

        @Test
        @DisplayName("requireCalibration throws on failure, naming the IMU")
        void requireThrowsOnFailure() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(false);

            final CurioException thrown =
                    assertThrows(
                            CurioException.class,
                            () -> wrapper.requireCalibration(newParameters()));
            assertTrue(thrown.getMessage().contains("imu"), thrown.getMessage());
        }

        @Test
        @DisplayName(
                "requireCalibration explains the likely cause, since a bad heading is hard "
                        + "to notice in a match")
        void requireExplainsCause() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(false);

            final String message =
                    assertThrows(
                                    CurioException.class,
                                    () -> wrapper.requireCalibration(newParameters()))
                            .getMessage();

            assertTrue(message.contains("still"), "expected the still-robot hint in: " + message);
        }

        @Test
        @DisplayName("requireCalibration returns quietly on success")
        void requirePassesOnSuccess() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(true);

            wrapper.requireCalibration(newParameters());
        }
    }

    @Nested
    @DisplayName("angles")
    class Angles {

        @Test
        @DisplayName("reports yaw, pitch, and roll in radians")
        void reportsAngles() {
            givenOrientation(0.5, 0.25, -0.125);

            assertEquals(0.5, wrapper.yaw(), DELTA);
            assertEquals(0.25, wrapper.pitch(), DELTA);
            assertEquals(-0.125, wrapper.roll(), DELTA);
        }

        @Test
        @DisplayName("heading is the yaw")
        void headingIsYaw() {
            givenOrientation(1.25, 0.0, 0.0);

            assertEquals(wrapper.yaw(), wrapper.heading(), DELTA);
        }

        @Test
        @DisplayName("converts heading to degrees")
        void headingInDegrees() {
            givenOrientation(Math.PI, 0.0, 0.0);

            assertEquals(180.0, wrapper.headingDegrees(), 1e-6);
        }

        @Test
        @DisplayName("wraps a negative heading into [0, 2π)")
        void wrapsNegativeHeading() {
            givenOrientation(-Math.PI / 2.0, 0.0, 0.0);

            final double positive = wrapper.headingPositive();

            assertTrue(positive >= 0.0, "expected a non-negative heading, got " + positive);
            assertEquals(3.0 * Math.PI / 2.0, positive, DELTA);
        }

        @Test
        @DisplayName("leaves an already-positive heading alone")
        void leavesPositiveHeading() {
            givenOrientation(Math.PI / 4.0, 0.0, 0.0);

            assertEquals(Math.PI / 4.0, wrapper.headingPositive(), DELTA);
        }

        @Test
        @DisplayName("wraps exactly -0 to 0, not 2π")
        void wrapsNegativeZero() {
            givenOrientation(-0.0, 0.0, 0.0);

            assertEquals(0.0, wrapper.headingPositive(), DELTA);
        }

        @Test
        @DisplayName("fails loudly when the IMU has not been calibrated")
        void uncalibratedFailsLoudly() {
            Mockito.when(sdk.getRobotYawPitchRollAngles()).thenReturn(null);

            final CurioException thrown = assertThrows(CurioException.class, wrapper::yaw);

            assertTrue(
                    thrown.getMessage().contains("calibrate"),
                    "the message should point at the likely fix: " + thrown.getMessage());
        }
    }

    @Test
    @DisplayName("resets yaw, not the whole orientation")
    void resetsYaw() {
        wrapper.resetHeading();

        Mockito.verify(sdk).resetYaw();
    }

    @Test
    @DisplayName("names itself in toString for diagnostics")
    void toStringIsDiagnostic() {
        assertTrue(wrapper.toString().contains("imu"), wrapper.toString());
    }
}
