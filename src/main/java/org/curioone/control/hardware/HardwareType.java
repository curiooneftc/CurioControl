package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * The device kinds CurioControl can resolve from a {@link
 * org.curioone.control.core.HardwareRegistry}.
 *
 * <p>Each constant names the SDK type it resolves, so callers do not repeat a class literal and a
 * lookup cannot disagree with its own diagnostic.
 *
 * @since 0.1.0
 */
public enum HardwareType {

    /** A brushed or brushless DC motor with a full control interface. */
    MOTOR(DcMotorEx.class, "MOTOR"),

    /**
     * A positional servo.
     *
     * <p>The SDK has no standalone encoder device, so an encoder is read through the motor whose
     * encoder it is. See {@link Encoder}.
     */
    ENCODER(DcMotorEx.class, "ENCODER"),

    /** An IMU on a Lynx module, or a Rev Hub internal IMU. */
    IMU(IMU.class, "IMU"),

    /** A positional servo. */
    SERVO(Servo.class, "SERVO"),

    /** A continuous rotation servo, which takes power rather than position. */
    CONTINUOUS_SERVO(CRServo.class, "CONTINUOUS_SERVO"),

    /** A digital input or output, such as a limit switch. */
    DIGITAL_SENSOR(DigitalChannel.class, "DIGITAL_SENSOR"),

    /** An analog input, such as a potentiometer. */
    ANALOG_SENSOR(AnalogInput.class, "ANALOG_SENSOR"),

    /** A battery or expansion hub voltage sensor. */
    VOLTAGE_SENSOR(VoltageSensor.class, "VOLTAGE_SENSOR");

    private final Class<?> sdkType;

    private final String label;

    HardwareType(Class<?> sdkType, String label) {
        this.sdkType = sdkType;
        this.label = label;
    }

    /**
     * Returns the SDK class this type resolves.
     *
     * <p>{@link #ENCODER} resolves to {@link DcMotorEx}: the FTC SDK exposes an encoder only
     * through the motor it is built into, so an "encoder" lookup is a lookup of the owning motor.
     *
     * @return the SDK device class, never {@code null}
     */
    public Class<?> sdkType() {
        return sdkType;
    }

    /**
     * Returns the name used in error messages.
     *
     * @return the display label, never {@code null}
     */
    public String label() {
        return label;
    }
}
