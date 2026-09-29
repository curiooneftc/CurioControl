package org.curioone.control.drive;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.hardware.Motor;

/**
 * Tank / differential drivetrain: independent power per side.
 *
 * <pre>{@code
 * TankDrive drive = robot.drive().tank();
 * drive.tank(gamepad1.left_stick_y, gamepad1.right_stick_y);
 * }</pre>
 *
 * <p>There is no strafing. Turning is differential: the wheels on one side turn faster than the
 * other, and the difference is what rotates the robot.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class TankDrive extends DriveBase {

    private final Motor left;

    private final Motor right;

    private final String name;

    private boolean leftInverted;

    private boolean rightInverted;

    /**
     * Builds a tank drivetrain from two configured motors.
     *
     * @param hardwareMap the configured hardware
     * @param name a name for diagnostics
     * @param leftName configuration name of the left motor
     * @param rightName configuration name of the right motor
     * @throws IllegalArgumentException if the hardware map or any name is {@code null}
     * @throws org.curioone.control.core.CurioException if either motor is not configured
     */
    public TankDrive(HardwareMap hardwareMap, String name, String leftName, String rightName) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("hardwareMap must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.left = new Motor(hardwareMap, leftName);
        this.right = new Motor(hardwareMap, rightName);
    }

    /**
     * Builds a tank drivetrain from two already-wrapped motors.
     *
     * @param name a name for diagnostics
     * @param left the left motor
     * @param right the right motor
     * @throws IllegalArgumentException if the name or either motor is {@code null}
     */
    public TankDrive(String name, Motor left, Motor right) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (left == null) {
            throw new IllegalArgumentException("left motor must not be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right motor must not be null");
        }
        this.name = name;
        this.left = left;
        this.right = right;
    }

    /**
     * Applies per-side power directly.
     *
     * @param leftPower left motor power in {@code [-1, 1]}, where positive is forward
     * @param rightPower right motor power in {@code [-1, 1]}, where positive is forward
     * @throws IllegalArgumentException if either power is not finite
     */
    public void tank(double leftPower, double rightPower) {
        requireFinite("leftPower", leftPower);
        requireFinite("rightPower", rightPower);
        write(leftPower, rightPower);
    }

    /**
     * Applies a three-axis command, ignoring strafe.
     *
     * <p>A tank chassis cannot strafe, so the strafe axis is discarded and the remaining axes
     * become the differential turn. Silently pretending to strafe would be worse: the robot would
     * turn instead of sliding, and the cause would be hard to see.
     *
     * @param strafe ignored
     * @param forward forward/backward input in {@code [-1, 1]}
     * @param rotation rotation input in {@code [-1, 1]}, where {@code 1} is counter-clockwise
     * @throws IllegalArgumentException if an input is not finite
     */
    @Override
    public void drive(double strafe, double forward, double rotation) {
        requireFinite("forward", forward);
        requireFinite("rotation", rotation);
        write(clamp(forward + rotation), clamp(forward - rotation));
    }

    @Override
    public void stop() {
        write(0.0, 0.0);
    }

    @Override
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("behavior must not be null");
        }
        left.setZeroPowerBehavior(behavior);
        right.setZeroPowerBehavior(behavior);
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Sets the direction of both motors.
     *
     * @param leftDirection direction applied to the left motor
     * @param rightDirection direction applied to the right motor
     * @throws IllegalArgumentException if either direction is {@code null}
     */
    public void setMotorDirections(
            DcMotorSimple.Direction leftDirection, DcMotorSimple.Direction rightDirection) {
        left.setDirection(leftDirection);
        right.setDirection(rightDirection);
    }

    /**
     * Inverts one side's contribution.
     *
     * @param leftInverted whether to invert the left motor
     * @param rightInverted whether to invert the right motor
     */
    public void setInverted(boolean leftInverted, boolean rightInverted) {
        this.leftInverted = leftInverted;
        this.rightInverted = rightInverted;
    }

    /**
     * Inverts the left motor's contribution.
     *
     * @param inverted whether to invert the left motor
     */
    public void setLeftInverted(boolean inverted) {
        this.leftInverted = inverted;
    }

    /**
     * Inverts the right motor's contribution.
     *
     * @param inverted whether to invert the right motor
     */
    public void setRightInverted(boolean inverted) {
        this.rightInverted = inverted;
    }

    /**
     * Returns whether the left motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isLeftInverted() {
        return leftInverted;
    }

    /**
     * Returns whether the right motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isRightInverted() {
        return rightInverted;
    }

    /**
     * Returns the left motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor left() {
        return left;
    }

    /**
     * Returns the right motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor right() {
        return right;
    }

    private void write(double leftPower, double rightPower) {
        left.setPower(leftInverted ? -leftPower : leftPower);
        right.setPower(rightInverted ? -rightPower : rightPower);
    }

    private static double clamp(double power) {
        return Math.max(-1.0, Math.min(1.0, power));
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }

    @Override
    public String toString() {
        return "TankDrive[" + name + "]";
    }
}
