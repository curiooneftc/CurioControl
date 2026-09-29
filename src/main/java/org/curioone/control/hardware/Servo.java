package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.util.Range;

/**
 * A validated view onto one positional FTC servo.
 *
 * <p><strong>This class deliberately has no {@code open()} or {@code close()}.</strong> Those are
 * mechanism semantics, and the framework cannot know which they are. A servo might be a claw, a
 * wrist, a release, or a deploy arm; {@code open()} on a deploy arm is a question, not an
 * instruction. The intent belongs on the subsystem that owns the mechanism:
 *
 * <pre>{@code
 * public class Claw extends Subsystem {
 *     private final Servo claw = new Servo(hardwareMap, "claw");
 *
 *     public void open()  { claw.setPosition(RobotConfig.Claw.OPEN);  }
 *     public void close() { claw.setPosition(RobotConfig.Claw.CLOSE); }
 * }
 * }</pre>
 *
 * <p>Guessing here would put the wrong vocabulary in the team's code, and renaming a servo would
 * then be an API change rather than a private detail.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class Servo {

    private final com.qualcomm.robotcore.hardware.Servo servo;

    private final String name;

    /**
     * Wraps an SDK servo.
     *
     * @param servo the servo to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Servo(com.qualcomm.robotcore.hardware.Servo servo, String name) {
        if (servo == null) {
            throw new IllegalArgumentException("servo must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.servo = servo;
        this.name = name;
    }

    /**
     * Looks a servo up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no servo is configured under that name
     */
    public Servo(HardwareMap hardwareMap, String name) {
        this(
                new org.curioone.control.core.HardwareRegistry(hardwareMap)
                        .require(com.qualcomm.robotcore.hardware.Servo.class, name),
                name);
    }

    /**
     * Looks a servo up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no servo is configured under that name
     */
    public Servo(org.curioone.control.core.HardwareRegistry registry, String name) {
        this(registry.require(com.qualcomm.robotcore.hardware.Servo.class, name), name);
    }

    /**
     * Returns the underlying SDK servo.
     *
     * @return the SDK servo, never {@code null}
     */
    public com.qualcomm.robotcore.hardware.Servo getSdkObject() {
        return servo;
    }

    /**
     * Returns the configuration name this servo was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Sets the target position.
     *
     * @param position target position in {@code [0, 1]}, where {@code 0} and {@code 1} are the
     *     servo's configured physical limits
     * @throws IllegalArgumentException if {@code position} is not finite and within {@code [0, 1]}
     */
    public void setPosition(double position) {
        if (!Range.isValidServoPosition(position)) {
            throw new IllegalArgumentException(
                    "position for servo '"
                            + name
                            + "' must be in ["
                            + Range.MIN_SERVO_POSITION
                            + ", "
                            + Range.MAX_SERVO_POSITION
                            + "] but was "
                            + position);
        }
        servo.setPosition(position);
    }

    /**
     * Returns the current commanded position.
     *
     * @return position in {@code [0, 1]}
     */
    public double getPosition() {
        return servo.getPosition();
    }

    /**
     * Sets the servo's direction.
     *
     * <p>Whether a direction flip is wanted depends on the linkage, so it belongs in the robot's
     * configuration rather than being guessed here.
     *
     * @param direction {@link com.qualcomm.robotcore.hardware.Servo.Direction#FORWARD} or {@link
     *     com.qualcomm.robotcore.hardware.Servo.Direction#REVERSE}
     * @throws IllegalArgumentException if {@code direction} is {@code null}
     */
    public void setDirection(com.qualcomm.robotcore.hardware.Servo.Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        servo.setDirection(direction);
    }

    /**
     * Rescales the servo's physical range.
     *
     * <p>Delegated unchanged. Rarely needed: setting a narrower range is usually a sign the servo's
     * limits in the Robot Controller configuration should be narrowed instead.
     *
     * @param min the new minimum position
     * @param max the new maximum position
     */
    public void scaleRange(double min, double max) {
        servo.scaleRange(min, max);
    }

    @Override
    public String toString() {
        return "Servo[" + name + "]";
    }
}
