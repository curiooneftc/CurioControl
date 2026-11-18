package org.curioone.robot.opmode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.curioone.control.command.Commands;
import org.curioone.control.core.CurioAuto;
import org.curioone.control.core.CurioRobot;
import org.curioone.robot.config.RobotConfig;
import org.curioone.robot.subsystem.Arm;
import org.curioone.robot.subsystem.Drive;
import org.curioone.robot.subsystem.Intake;

/**
 * Blue-side autonomous: drive to the scoring position, lift, release, and return.
 *
 * <p>The routine is a command tree. {@link #buildRoutine()} schedules it once; {@link CurioAuto}
 * runs one scheduler pass per loop and cancels everything when the OpMode stops. Each step starts
 * when the previous one reports finished, and subsystem ownership keeps two commands off the same
 * mechanism.
 */
@Autonomous(name = "Blue Auto", group = "CurioControl")
public final class BlueAuto extends CurioAuto {

    private Drive drive;

    private Arm arm;

    private Intake intake;

    @Override
    protected void configureRobot() {
        robot = new CurioRobot(hardwareMap);

        drive = new Drive(hardwareMap);
        arm = new Arm(hardwareMap);
        intake = new Intake(hardwareMap);

        robot.registerSubsystem(drive);
        robot.registerSubsystem(arm);
        robot.registerSubsystem(intake);
    }

    @Override
    protected void buildRoutine() {
        schedule(
                Commands.sequence(
                        "Blue Auto",
                        drive.driveDistance(RobotConfig.Auto.DRIVE_TO_SCORE_MM),
                        arm.moveTo(RobotConfig.Arm.HIGH),
                        intake.release(),
                        Commands.waitSeconds(RobotConfig.Auto.SCORE_SETTLE_SECONDS),
                        arm.moveTo(RobotConfig.Arm.HOME),
                        drive.driveDistance(-RobotConfig.Auto.DRIVE_TO_SCORE_MM)));
    }
}
