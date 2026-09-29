package org.curioone.control.drive;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.hardware.Motor;
import org.curioone.control.util.Range;

/**
 * Mecanum drivetrain: translation on two axes plus rotation.
 *
 * <pre>{@code
 * // robot.drive() builds a mecanum base from the conventional motor names.
 * robot.drive().drive(
 *         gamepad1.left_stick_x,    // strafe
 *         gamepad1.left_stick_y,    // forward
 *         gamepad1.right_stick_x);  // rotation
 *
 * // Or build one explicitly, to name your own motors.
 * MecanumDrive drive = robot.mecanumDrive("fl", "fr", "bl", "br");
 * drive.mecanum(x, y, r);
 * }</pre>
 *
 * <h2>Normalization</h2>
 *
 * Combining a full-power strafe with a full-power rotation asks for a corner motor needing roughly
 * 1.4 power. Clamping each motor independently distorts the requested direction: the robot stops
 * going where it was told and starts going somewhere slower and wrong. That is the single most
 * common reason a mecanum base "feels off".
 *
 * <p>So all four wheel powers are scaled by one common factor whenever any of them exceeds the
 * limit. Magnitude is reduced, direction is preserved. A full diagonal becomes a slower diagonal,
 * not a skewed one.
 *
 * <h2>Wheel order</h2>
 *
 * Motors are listed front-left, front-right, back-left, back-right. A wheel configured backwards
 * makes the robot spin instead of strafe, and the symptom looks like a controller fault rather than
 * a configuration mistake — so {@link #setInverted} exists, and getting it right belongs in the
 * robot's configuration rather than in the framework.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class MecanumDrive extends DriveBase {

    private final Motor frontLeft;

    private final Motor frontRight;

    private final Motor backLeft;

    private final Motor backRight;

    private final String name;

    private boolean frontLeftInverted;

    private boolean frontRightInverted;

    private boolean backLeftInverted;

    private boolean backRightInverted;

    /**
     * Builds a mecanum drivetrain from four configured motors.
     *
     * @param hardwareMap the configured hardware
     * @param name a name for diagnostics
     * @param frontLeftName configuration name of the front-left motor
     * @param frontRightName configuration name of the front-right motor
     * @param backLeftName configuration name of the back-left motor
     * @param backRightName configuration name of the back-right motor
     * @throws IllegalArgumentException if the hardware map or any name is {@code null}
     * @throws org.curioone.control.core.CurioException if any motor is not configured
     */
    public MecanumDrive(
            HardwareMap hardwareMap,
            String name,
            String frontLeftName,
            String frontRightName,
            String backLeftName,
            String backRightName) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("hardwareMap must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.frontLeft = new Motor(hardwareMap, frontLeftName);
        this.frontRight = new Motor(hardwareMap, frontRightName);
        this.backLeft = new Motor(hardwareMap, backLeftName);
        this.backRight = new Motor(hardwareMap, backRightName);
    }

    /**
     * Builds a mecanum drivetrain from four already-wrapped motors.
     *
     * @param name a name for diagnostics
     * @param frontLeft the front-left motor
     * @param frontRight the front-right motor
     * @param backLeft the back-left motor
     * @param backRight the back-right motor
     * @throws IllegalArgumentException if the name or any motor is {@code null}
     */
    public MecanumDrive(
            String name, Motor frontLeft, Motor frontRight, Motor backLeft, Motor backRight) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.frontLeft = requireMotor("frontLeft", frontLeft);
        this.frontRight = requireMotor("frontRight", frontRight);
        this.backLeft = requireMotor("backLeft", backLeft);
        this.backRight = requireMotor("backRight", backRight);
    }

    @Override
    public void drive(double strafe, double forward, double rotation) {
        mecanum(strafe, forward, rotation);
    }

    /**
     * Applies robot-centric mecanum input and writes motor powers.
     *
     * @param strafe left/right input in {@code [-1, 1]}, where {@code 1} is right
     * @param forward forward/backward input in {@code [-1, 1]}, where {@code 1} is forward
     * @param rotation rotation input in {@code [-1, 1]}, where {@code 1} is counter-clockwise
     * @throws IllegalArgumentException if any input is not finite
     */
    public void mecanum(double strafe, double forward, double rotation) {
        requireFinite("strafe", strafe);
        requireFinite("forward", forward);
        requireFinite("rotation", rotation);

        final double fl = forward + strafe - rotation;
        final double fr = forward - strafe - rotation;
        final double bl = forward - strafe + rotation;
        final double br = forward + strafe + rotation;

        final double scale = normalizationScale(fl, fr, bl, br);
        write(fl * scale, fr * scale, bl * scale, br * scale);
    }

    /**
     * Applies field-centric mecanum input and writes motor powers.
     *
     * <p>"Forward" is the field's forward regardless of which way the robot is pointing, which is
     * what a driver expects after the first two seconds of robot-centric driving.
     *
     * <p>The heading is passed in rather than read from an IMU inside this class. That keeps {@code
     * drive} free of SDK types, makes the rotation math testable without hardware, and lets a
     * caller substitute a fused or filtered heading (ADR-003).
     *
     * @param strafe field-relative left/right input in {@code [-1, 1]}, where {@code 1} is right
     * @param forward field-relative forward/backward input in {@code [-1, 1]}
     * @param rotation rotation input in {@code [-1, 1]}, where {@code 1} is counter-clockwise
     * @param heading the robot's field-relative heading in radians, counter-clockwise from the
     *     field's forward axis
     * @throws IllegalArgumentException if any input is not finite
     */
    public void fieldCentric(double strafe, double forward, double rotation, double heading) {
        requireFinite("strafe", strafe);
        requireFinite("forward", forward);
        requireFinite("rotation", rotation);
        requireFinite("heading", heading);

        final double cos = Math.cos(heading);
        final double sin = Math.sin(heading);

        // Field frame: +x right, +y forward, heading measured counter-clockwise from field
        // forward, which is the FTC IMU convention. The robot's forward axis therefore points
        // at (sin h, cos h) and its right axis at (cos h, -sin h).
        //
        // Decomposing the requested field velocity (vx, vy) onto those axes and inverting gives
        // the robot-frame inputs the wheel equations expect. At h = 0 this is the identity, which
        // is the check that matters: with no rotation, field-centric and robot-centric agree.
        mecanum(strafe * cos - forward * sin, strafe * sin + forward * cos, rotation);
    }

    @Override
    public void stop() {
        write(0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("behavior must not be null");
        }
        frontLeft.setZeroPowerBehavior(behavior);
        frontRight.setZeroPowerBehavior(behavior);
        backLeft.setZeroPowerBehavior(behavior);
        backRight.setZeroPowerBehavior(behavior);
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Sets what every motor does when commanded to zero power.
     *
     * <p>A convenience for the common case of naming a constant in the robot's configuration and
     * setting the mode in one place.
     *
     * @param forward direction applied to the left-side motors
     * @param right direction applied to the right-side motors
     * @throws IllegalArgumentException if either direction is {@code null}
     */
    public void setMotorDirections(DcMotorSimple.Direction forward, DcMotorSimple.Direction right) {
        frontLeft.setDirection(forward);
        backLeft.setDirection(forward);
        frontRight.setDirection(right);
        backRight.setDirection(right);
    }

    /**
     * Inverts one motor's contribution without touching the others.
     *
     * <p>This is the fix for a mirrored wheel, and it is per-wheel on purpose: a misconfigured
     * mecanum base almost always has one wheel wrong, and "which one" is much faster to diagnose
     * when each can be flipped independently.
     *
     * @param frontLeftInverted whether to invert the front-left motor
     * @param frontRightInverted whether to invert the front-right motor
     * @param backLeftInverted whether to invert the back-left motor
     * @param backRightInverted whether to invert the back-right motor
     */
    public void setInverted(
            boolean frontLeftInverted,
            boolean frontRightInverted,
            boolean backLeftInverted,
            boolean backRightInverted) {
        this.frontLeftInverted = frontLeftInverted;
        this.frontRightInverted = frontRightInverted;
        this.backLeftInverted = backLeftInverted;
        this.backRightInverted = backRightInverted;
    }

    /**
     * Inverts the front-left motor's contribution.
     *
     * @param inverted whether to invert the front-left motor
     */
    public void setFrontLeftInverted(boolean inverted) {
        this.frontLeftInverted = inverted;
    }

    /**
     * Inverts the front-right motor's contribution.
     *
     * @param inverted whether to invert the front-right motor
     */
    public void setFrontRightInverted(boolean inverted) {
        this.frontRightInverted = inverted;
    }

    /**
     * Inverts the back-left motor's contribution.
     *
     * @param inverted whether to invert the back-left motor
     */
    public void setBackLeftInverted(boolean inverted) {
        this.backLeftInverted = inverted;
    }

    /**
     * Inverts the back-right motor's contribution.
     *
     * @param inverted whether to invert the back-right motor
     */
    public void setBackRightInverted(boolean inverted) {
        this.backRightInverted = inverted;
    }

    /**
     * Returns whether the front-left motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isFrontLeftInverted() {
        return frontLeftInverted;
    }

    /**
     * Returns whether the front-right motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isFrontRightInverted() {
        return frontRightInverted;
    }

    /**
     * Returns whether the back-left motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isBackLeftInverted() {
        return backLeftInverted;
    }

    /**
     * Returns whether the back-right motor is inverted.
     *
     * @return {@code true} if inverted
     */
    public boolean isBackRightInverted() {
        return backRightInverted;
    }

    /**
     * Returns the front-left motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor frontLeft() {
        return frontLeft;
    }

    /**
     * Returns the front-right motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor frontRight() {
        return frontRight;
    }

    /**
     * Returns the back-left motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor backLeft() {
        return backLeft;
    }

    /**
     * Returns the back-right motor.
     *
     * @return the motor, never {@code null}
     */
    public Motor backRight() {
        return backRight;
    }

    /**
     * Computes the common scale factor that keeps every motor within its power limit.
     *
     * <p>Package-private for testing: the normalization is the part of a drivetrain most worth
     * asserting on directly, and driving it through mocked motors would only prove that four setter
     * calls happened.
     *
     * @param fl front-left power
     * @param fr front-right power
     * @param bl back-left power
     * @param br back-right power
     * @return a factor in {@code (0, 1]}
     */
    static double normalizationScale(double fl, double fr, double bl, double br) {
        final double max =
                Math.max(
                        Math.max(Math.abs(fl), Math.abs(fr)), Math.max(Math.abs(bl), Math.abs(br)));
        if (max <= Range.MAX_POWER || max == 0.0) {
            return 1.0;
        }
        return Range.MAX_POWER / max;
    }

    private void write(double fl, double fr, double bl, double br) {
        frontLeft.setPower(frontLeftInverted ? -fl : fl);
        frontRight.setPower(frontRightInverted ? -fr : fr);
        backLeft.setPower(backLeftInverted ? -bl : bl);
        backRight.setPower(backRightInverted ? -br : br);
    }

    private static Motor requireMotor(String label, Motor motor) {
        if (motor == null) {
            throw new IllegalArgumentException(label + " motor must not be null");
        }
        return motor;
    }

    private static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite but was " + value);
        }
    }

    @Override
    public String toString() {
        return "MecanumDrive[" + name + "]";
    }
}
