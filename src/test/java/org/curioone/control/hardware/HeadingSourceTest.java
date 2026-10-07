package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.core.CurioException;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link HeadingSource} and the {@link IMU} calibration latch. */
@DisplayName("HeadingSource and IMU calibration")
class HeadingSourceTest {

    private static final double DELTA = 1e-9;

    private com.qualcomm.robotcore.hardware.IMU sdk;

    private IMU wrapper;

    @BeforeEach
    void setUp() {
        sdk = Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class);
        wrapper = new IMU(sdk, "imu");
    }

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

    private void givenYaw(double yaw) {
        Mockito.when(sdk.getRobotYawPitchRollAngles())
                .thenReturn(new YawPitchRollAngles(AngleUnit.RADIANS, yaw, 0.0, 0.0, 0L));
    }

    @Nested
    @DisplayName("heading source")
    class Source {

        @Test
        @DisplayName("an IMU serves as a heading source")
        void imuIsASource() {
            givenYaw(Math.PI / 2.0);

            final HeadingSource source = wrapper;

            assertEquals(Math.PI / 2.0, source.heading(), DELTA);
        }

        @Test
        @DisplayName("a fake source drives heading changes without hardware")
        void fakeSource() {
            final double[] heading = {0.0};
            final HeadingSource source = () -> heading[0];

            heading[0] = Math.PI;
            assertEquals(Math.PI, source.heading(), DELTA);
        }
    }

    @Nested
    @DisplayName("calibration latch")
    class Latch {

        @Test
        @DisplayName("starts uncalibrated")
        void startsUncalibrated() {
            assertFalse(wrapper.isCalibrated());
        }

        @Test
        @DisplayName("a successful calibration latches")
        void successLatches() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(true);

            assertTrue(wrapper.calibrate(newParameters()));
            assertTrue(wrapper.isCalibrated());
        }

        @Test
        @DisplayName("a failed calibration leaves the latch alone")
        void failureLeavesLatch() {
            Mockito.when(sdk.initialize(Mockito.any())).thenReturn(false);

            assertFalse(wrapper.calibrate(newParameters()));
            assertFalse(wrapper.isCalibrated());
        }

        @Test
        @DisplayName("requireCalibration throws on failure and latches on success")
        void requireCalibration() {
            final com.qualcomm.robotcore.hardware.IMU.Parameters parameters = newParameters();
            Mockito.when(sdk.initialize(parameters)).thenReturn(false);

            assertThrows(CurioException.class, () -> wrapper.requireCalibration(parameters));
            assertFalse(wrapper.isCalibrated());

            Mockito.when(sdk.initialize(parameters)).thenReturn(true);
            wrapper.requireCalibration(parameters);
            assertTrue(wrapper.isCalibrated());
        }
    }
}
