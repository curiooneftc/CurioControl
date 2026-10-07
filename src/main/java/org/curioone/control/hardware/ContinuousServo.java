package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.core.HardwareRegistry;
import org.curioone.control.util.Range;

/**
 * A validated view onto one continuous rotation servo.
 *
 * <p>A {@code CRServo} takes signed <em>power</em> rather than a position, so it is a different
 * type from {@link Servo} rather than a subclass: mixing them up is a common and confusing mistake,
 * and the compiler should catch it.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class ContinuousServo {

    private final CRServo servo;

    private final String name;

    /**
     * Wraps an SDK continuous rotation servo.
     *
     * @param servo the servo to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public ContinuousServo(CRServo servo, String name) {
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
     * Looks a continuous rotation servo up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no continuous rotation servo is
     *     configured under that name
     */
    public ContinuousServo(HardwareMap hardwareMap, String name) {
        this(new HardwareRegistry(hardwareMap).require(CRServo.class, name), name);
    }

    /**
     * Looks a continuous rotation servo up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no continuous rotation servo is
     *     configured under that name
     */
    public ContinuousServo(HardwareRegistry registry, String name) {
        this(registry.require(CRServo.class, name), name);
    }

    /**
     * Returns the underlying SDK servo.
     *
     * @return the SDK servo, never {@code null}
     */
    public CRServo getSdkObject() {
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
     * Sets output power.
     *
     * @param power power in {@code [-1, 1]}; the sign selects rotation direction
     * @throws IllegalArgumentException if {@code power} is not finite and within {@code [-1, 1]}
     */
    public void setPower(double power) {
        if (!Range.isValidPower(power)) {
            throw new IllegalArgumentException(
                    "power for continuous servo '"
                            + name
                            + "' must be in ["
                            + Range.MIN_POWER
                            + ", "
                            + Range.MAX_POWER
                            + "] but was "
                            + power);
        }
        servo.setPower(power);
    }

    /**
     * Returns the current output power.
     *
     * @return power in {@code [-1, 1]}
     */
    public double getPower() {
        return servo.getPower();
    }

    /**
     * Stops the servo.
     *
     * <p>Sets power to zero through the validated path.
     */
    public void stop() {
        setPower(0.0);
    }

    /**
     * Sets the servo's direction.
     *
     * @param direction {@link DcMotorSimple.Direction#FORWARD} or {@link
     *     DcMotorSimple.Direction#REVERSE}
     * @throws IllegalArgumentException if {@code direction} is {@code null}
     */
    public void setDirection(DcMotorSimple.Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        servo.setDirection(direction);
    }

    @Override
    public String toString() {
        return "ContinuousServo[" + name + "]";
    }
}
