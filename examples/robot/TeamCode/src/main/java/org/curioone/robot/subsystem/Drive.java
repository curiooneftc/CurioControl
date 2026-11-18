package org.curioone.robot.subsystem;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.Collections;
import java.util.Set;
import org.curioone.control.command.Command;
import org.curioone.control.core.Subsystem;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.drive.MecanumDrive;
import org.curioone.control.hardware.IMU;
import org.curioone.control.hardware.Motor;
import org.curioone.robot.config.RobotConfig;

/**
 * The drivetrain: four motors, an IMU, and the two drive commands the OpModes need.
 *
 * <p>Owns its hardware so the OpModes never see a motor. {@link #teleop} maps driver input to the
 * mecanum equations, with an optional field-centric mode; {@link #driveDistance} is a small custom
 * {@link Command} that shows how a mechanism exposes an intent the command scheduler can sequence.
 */
public final class Drive extends Subsystem {

    private static final double MILLIMETRES_PER_TICK =
            Math.PI * RobotConfig.Drive.WHEEL_DIAMETER_MM
                    / (RobotConfig.Drive.TICKS_PER_REV * RobotConfig.Drive.GEAR_RATIO);

    private final MecanumDrive drive;

    private final IMU imu;

    private final Motor frontLeft;

    private final Motor frontRight;

    private boolean fieldCentric = true;

    private boolean headingAvailable;

    /**
     * Looks up the four drive motors and the IMU.
     *
     * @param hardwareMap the configured hardware
     */
    public Drive(HardwareMap hardwareMap) {
        frontLeft = new Motor(hardwareMap, RobotConfig.Drive.FRONT_LEFT);
        frontRight = new Motor(hardwareMap, RobotConfig.Drive.FRONT_RIGHT);
        final Motor backLeft = new Motor(hardwareMap, RobotConfig.Drive.BACK_LEFT);
        final Motor backRight = new Motor(hardwareMap, RobotConfig.Drive.BACK_RIGHT);

        drive = new MecanumDrive("Drive", frontLeft, frontRight, backLeft, backRight);
        drive.setFrontLeftInverted(RobotConfig.Drive.FRONT_LEFT_INVERTED);
        drive.setFrontRightInverted(RobotConfig.Drive.FRONT_RIGHT_INVERTED);
        drive.setBackLeftInverted(RobotConfig.Drive.BACK_LEFT_INVERTED);
        drive.setBackRightInverted(RobotConfig.Drive.BACK_RIGHT_INVERTED);
        drive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        imu = new IMU(hardwareMap, RobotConfig.Drive.IMU);
    }

    /**
     * Calibrates the IMU. Field-centric drive falls back to robot-centric if this fails, so a
     * disconnected hub degrades the robot instead of preventing it from moving at all.
     */
    @Override
    public void init() {
        headingAvailable =
                imu.calibrate(
                        new com.qualcomm.robotcore.hardware.IMU.Parameters(
                                new RevHubOrientationOnRobot(
                                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
    }

    /**
     * Drives from the stick inputs.
     *
     * @param strafe left/right, where {@code 1} is right
     * @param forward forward/back, where {@code 1} is forward
     * @param rotation counter-clockwise, where {@code 1} is left
     */
    public void teleop(double strafe, double forward, double rotation) {
        if (fieldCentric && headingAvailable) {
            drive.fieldCentric(strafe, forward, rotation, imu.heading());
        } else {
            drive.drive(strafe, forward, rotation);
        }
    }

    /** Switches between field-centric and robot-centric drive. */
    public void toggleFieldCentric() {
        fieldCentric = !fieldCentric;
    }

    /**
     * Builds a command that drives straight for a distance.
     *
     * <p>Positive is forward, negative is backward. The command owns this subsystem, so the
     * scheduler will not run it alongside another command that drives.
     *
     * @param millimetres distance to travel, in millimetres
     * @return the command
     */
    public Command driveDistance(double millimetres) {
        return new DriveDistance(millimetres);
    }

    /** Stops all four motors. */
    @Override
    public void stop() {
        drive.stop();
    }

    @Override
    protected void publishTelemetry(TelemetryManager telemetry) {
        telemetry.add("Drive/mode", fieldCentric && headingAvailable ? "field" : "robot");
        telemetry.add("Drive/frontLeft", frontLeft.getPosition());
    }

    /** Straight-line drive, finished by encoder distance. */
    private final class DriveDistance implements Command {

        private final double millimetres;

        private final Set<Subsystem> requirements = Collections.singleton(Drive.this);

        private int startTicks;

        DriveDistance(double millimetres) {
            this.millimetres = millimetres;
        }

        @Override
        public void initialize() {
            startTicks = (frontLeft.getPosition() + frontRight.getPosition()) / 2;
        }

        @Override
        public void execute() {
            final double direction = Math.signum(millimetres);
            drive.mecanum(0.0, direction * RobotConfig.Auto.DRIVE_POWER, 0.0);
        }

        @Override
        public boolean isFinished() {
            final int current = (frontLeft.getPosition() + frontRight.getPosition()) / 2;
            final double travelled = (current - startTicks) * MILLIMETRES_PER_TICK;
            return Math.abs(travelled) >= Math.abs(millimetres);
        }

        @Override
        public void end(boolean interrupted) {
            drive.stop();
        }

        @Override
        public Set<Subsystem> requirements() {
            return requirements;
        }

        @Override
        public String name() {
            return String.format("Drive %.0f mm", millimetres);
        }
    }
}
