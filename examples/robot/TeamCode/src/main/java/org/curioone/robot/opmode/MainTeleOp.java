package org.curioone.robot.opmode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.curioone.control.core.CurioOpMode;
import org.curioone.control.core.CurioRobot;
import org.curioone.control.math.MathUtil;
import org.curioone.control.util.EdgeDetector;
import org.curioone.robot.config.RobotConfig;
import org.curioone.robot.subsystem.Arm;
import org.curioone.robot.subsystem.Drive;
import org.curioone.robot.subsystem.Intake;

/**
 * Competition TeleOp: sticks drive, buttons command mechanisms.
 *
 * <p>This is the whole OpMode. It reads input, expresses intent, and lets the subsystems and the
 * robot container own the hardware and the lifecycle.
 */
@TeleOp(name = "Main TeleOp", group = "CurioControl")
public final class MainTeleOp extends CurioOpMode {

    private Drive drive;

    private Arm arm;

    private Intake intake;

    private final EdgeDetector fieldCentricButton = new EdgeDetector();

    private final EdgeDetector armUpButton = new EdgeDetector();

    private final EdgeDetector armDownButton = new EdgeDetector();

    private final EdgeDetector openClawButton = new EdgeDetector();

    private final EdgeDetector closeClawButton = new EdgeDetector();

    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);

        drive = new Drive(hardwareMap);
        arm = new Arm(hardwareMap);
        intake = new Intake(hardwareMap);

        robot.registerSubsystem(drive);
        robot.registerSubsystem(arm);
        robot.registerSubsystem(intake);
    }

    @Override
    public void runRobot() {
        drive.teleop(
                MathUtil.applyDeadband(
                        gamepad1.left_stick_x, RobotConfig.Drive.STICK_DEADBAND),
                MathUtil.applyDeadband(
                        -gamepad1.left_stick_y, RobotConfig.Drive.STICK_DEADBAND),
                MathUtil.applyDeadband(
                        gamepad1.right_stick_x, RobotConfig.Drive.STICK_DEADBAND));

        if (fieldCentricButton.poll(gamepad1.a) == EdgeDetector.Edge.RISING) {
            drive.toggleFieldCentric();
        }

        if (armUpButton.poll(gamepad1.dpad_up) == EdgeDetector.Edge.RISING) {
            arm.setTarget(RobotConfig.Arm.HIGH);
        } else if (armDownButton.poll(gamepad1.dpad_down) == EdgeDetector.Edge.RISING) {
            arm.setTarget(RobotConfig.Arm.HOME);
        }

        if (gamepad1.right_bumper) {
            intake.intake();
        } else if (gamepad1.left_bumper) {
            intake.outtake();
        } else {
            intake.stopMotor();
        }

        if (openClawButton.poll(gamepad1.x) == EdgeDetector.Edge.RISING) {
            intake.openClaw();
        } else if (closeClawButton.poll(gamepad1.y) == EdgeDetector.Edge.RISING) {
            intake.closeClaw();
        }
    }
}
