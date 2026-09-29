package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.core.HardwareRegistry;

/**
 * A view onto an FTC voltage sensor, such as a control hub or expansion hub.
 *
 * <p>The SDK type is named {@code VoltageSensor} too, so it is referenced fully qualified
 * throughout this file and deliberately not imported — a same-named import does not compile.
 *
 * <p>The battery voltage is the single most useful number for spotting a problem before it becomes
 * one: a sagging pack shows up as brownouts and unexplained resets, and by the time the robot is
 * visibly slow the cause is usually already clear from the voltage trace.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class VoltageSensor {

    private final com.qualcomm.robotcore.hardware.VoltageSensor sensor;

    private final String name;

    /**
     * Wraps an SDK voltage sensor.
     *
     * @param sensor the sensor to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public VoltageSensor(com.qualcomm.robotcore.hardware.VoltageSensor sensor, String name) {
        if (sensor == null) {
            throw new IllegalArgumentException("sensor must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.sensor = sensor;
        this.name = name;
    }

    /**
     * Looks a voltage sensor up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no sensor is configured under that name
     */
    public VoltageSensor(HardwareMap hardwareMap, String name) {
        this(
                new HardwareRegistry(hardwareMap)
                        .require(com.qualcomm.robotcore.hardware.VoltageSensor.class, name),
                name);
    }

    /**
     * Looks a voltage sensor up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no voltage sensor is configured under
     *     that name
     */
    public VoltageSensor(HardwareRegistry registry, String name) {
        this(registry.require(com.qualcomm.robotcore.hardware.VoltageSensor.class, name), name);
    }

    /**
     * Returns the underlying SDK sensor.
     *
     * @return the SDK sensor, never {@code null}
     */
    public com.qualcomm.robotcore.hardware.VoltageSensor getSdkObject() {
        return sensor;
    }

    /**
     * Returns the configuration name this sensor was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the measured voltage.
     *
     * @return volts
     */
    public double getVoltage() {
        return sensor.getVoltage();
    }

    @Override
    public String toString() {
        return "VoltageSensor[" + name + "]";
    }
}
