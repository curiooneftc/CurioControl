package org.curioone.robot.subsystem;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.command.Command;
import org.curioone.control.command.Commands;
import org.curioone.control.core.Subsystem;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.hardware.Motor;
import org.curioone.robot.config.RobotConfig;

/**
 * The scoring arm: one motor on a position target.
 *
 * <p>The OpMode asks for a position; the SDK's {@code RUN_TO_POSITION} mode does the holding. Motion
 * commands are built here, next to the mechanism, so an autonomous routine composes an arm move
 * without knowing which motor name or tick count is involved.
 */
public final class Arm extends Subsystem {

    private final Motor motor;

    /**
     * Looks up the arm motor and puts it in position mode.
     *
     * @param hardwareMap the configured hardware
     */
    public Arm(HardwareMap hardwareMap) {
        motor = new Motor(hardwareMap, RobotConfig.Arm.MOTOR);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setRunMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    /**
     * Sets the arm's target position.
     *
     * @param ticks encoder ticks, where zero is fully down
     */
    public void setTarget(int ticks) {
        motor.setTargetPosition(ticks);
        motor.setPower(RobotConfig.Arm.POWER);
    }

    /**
     * Builds a command that moves the arm and waits until it arrives.
     *
     * @param ticks the target, in encoder ticks
     * @return the command
     */
    public Command moveTo(int ticks) {
        return Commands.sequence(
                "Move arm to " + ticks,
                Commands.instant("Target arm at " + ticks, () -> setTarget(ticks), this),
                Commands.waitUntil("Arm at " + ticks, this::atTarget, this));
    }

    /**
     * Whether the arm is within tolerance of its target.
     *
     * @return {@code true} when the arm has arrived
     */
    public boolean atTarget() {
        return Math.abs(motor.getPosition() - motor.getTargetPosition())
                <= RobotConfig.Arm.TOLERANCE_TICKS;
    }

    /**
     * Returns the arm's current position.
     *
     * @return encoder ticks
     */
    public int position() {
        return motor.getPosition();
    }

    /** Holds the arm once the OpMode ends. */
    @Override
    public void stop() {
        motor.setPower(0.0);
    }

    @Override
    protected void publishTelemetry(TelemetryManager telemetry) {
        telemetry.add("Arm/position", motor.getPosition());
        telemetry.add("Arm/target", motor.getTargetPosition());
        telemetry.add("Arm/atTarget", atTarget());
    }
}
