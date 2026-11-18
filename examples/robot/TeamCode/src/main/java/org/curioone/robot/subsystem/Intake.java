package org.curioone.robot.subsystem;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.command.Command;
import org.curioone.control.command.Commands;
import org.curioone.control.core.Subsystem;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.hardware.Motor;
import org.curioone.control.hardware.Servo;
import org.curioone.robot.config.RobotConfig;

/** The intake: a roller motor and a claw servo. */
public final class Intake extends Subsystem {

    private final Motor motor;

    private final Servo claw;

    /**
     * Looks up the intake motor and claw, and starts with the claw closed.
     *
     * @param hardwareMap the configured hardware
     */
    public Intake(HardwareMap hardwareMap) {
        motor = new Motor(hardwareMap, RobotConfig.Intake.MOTOR);
        claw = new Servo(hardwareMap, RobotConfig.Intake.CLAW);
        claw.setPosition(RobotConfig.Intake.CLAW_CLOSED);
    }

    /** Pulls a sample in and closes the claw. */
    public void intake() {
        motor.setPower(RobotConfig.Intake.INTAKE_POWER);
        closeClaw();
    }

    /** Pushes a sample out and opens the claw. */
    public void outtake() {
        motor.setPower(RobotConfig.Intake.OUTTAKE_POWER);
        openClaw();
    }

    /** Stops the roller without moving the claw. */
    public void stopMotor() {
        motor.stop();
    }

    /** Opens the claw. */
    public void openClaw() {
        claw.setPosition(RobotConfig.Intake.CLAW_OPEN);
    }

    /** Closes the claw. */
    public void closeClaw() {
        claw.setPosition(RobotConfig.Intake.CLAW_CLOSED);
    }

    /**
     * Builds a command that opens the claw, gives the sample time to fall, then stops the roller.
     *
     * @return the command
     */
    public Command release() {
        return Commands.sequence(
                "Release sample",
                Commands.instant("Open claw", this::openClaw, this),
                Commands.waitSeconds(0.3),
                Commands.instant("Stop intake", this::stopMotor, this));
    }

    /** Stops the roller when the OpMode ends. */
    @Override
    public void stop() {
        motor.stop();
    }

    @Override
    protected void publishTelemetry(TelemetryManager telemetry) {
        telemetry.add("Intake/power", motor.getPower());
        telemetry.add("Intake/claw", claw.getPosition());
    }
}
