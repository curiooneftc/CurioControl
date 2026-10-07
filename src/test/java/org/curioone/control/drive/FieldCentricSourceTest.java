package org.curioone.control.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.curioone.control.hardware.HeadingSource;
import org.curioone.control.hardware.IMU;
import org.curioone.control.hardware.Motor;
import org.curioone.control.support.FakeDcMotor;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for field-centric drive through a {@link HeadingSource}. */
@DisplayName("Field-centric drive from a heading source")
class FieldCentricSourceTest {

    private static final double DELTA = 1e-9;

    private FakeDcMotor frontLeft;

    private FakeDcMotor frontRight;

    private FakeDcMotor backLeft;

    private FakeDcMotor backRight;

    private MecanumDrive drive;

    @BeforeEach
    void setUp() {
        frontLeft = new FakeDcMotor();
        frontRight = new FakeDcMotor();
        backLeft = new FakeDcMotor();
        backRight = new FakeDcMotor();
        drive =
                new MecanumDrive(
                        "test",
                        new Motor(frontLeft, "fl"),
                        new Motor(frontRight, "fr"),
                        new Motor(backLeft, "bl"),
                        new Motor(backRight, "br"));
    }

    private double[] powersAfter(double strafe, double forward, double rotation, double heading) {
        drive.fieldCentric(strafe, forward, rotation, heading);
        return new double[] {
            frontLeft.lastPower(),
            frontRight.lastPower(),
            backLeft.lastPower(),
            backRight.lastPower()
        };
    }

    @Test
    @DisplayName("a lambda source matches the raw-heading call")
    void lambdaMatchesRawHeading() {
        final HeadingSource source = () -> Math.PI / 2.0;

        final double[] raw = powersAfter(0.0, 1.0, 0.0, Math.PI / 2.0);
        drive.fieldCentric(0.0, 1.0, 0.0, source);
        final double[] sourced =
                new double[] {
                    frontLeft.lastPower(),
                    frontRight.lastPower(),
                    backLeft.lastPower(),
                    backRight.lastPower()
                };

        for (int wheel = 0; wheel < raw.length; wheel++) {
            assertEquals(raw[wheel], sourced[wheel], DELTA);
        }
    }

    @Test
    @DisplayName("an IMU drives the wheels through the source seam")
    void imuDrivesWheels() {
        final com.qualcomm.robotcore.hardware.IMU sdk =
                Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class);
        Mockito.when(sdk.getRobotYawPitchRollAngles())
                .thenReturn(new YawPitchRollAngles(AngleUnit.RADIANS, Math.PI, 0.0, 0.0, 0L));

        drive.fieldCentric(0.0, 1.0, 0.0, new IMU(sdk, "imu"));
        final double[] sourced =
                new double[] {
                    frontLeft.lastPower(),
                    frontRight.lastPower(),
                    backLeft.lastPower(),
                    backRight.lastPower()
                };
        final double[] raw = powersAfter(0.0, 1.0, 0.0, Math.PI);

        for (int wheel = 0; wheel < raw.length; wheel++) {
            assertEquals(raw[wheel], sourced[wheel], DELTA);
        }
    }

    @Test
    @DisplayName("a null source fails loudly")
    void nullSourceFails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> drive.fieldCentric(0.0, 1.0, 0.0, (HeadingSource) null));
    }
}
