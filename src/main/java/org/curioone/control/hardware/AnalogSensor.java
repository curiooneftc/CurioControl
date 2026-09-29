package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.core.HardwareRegistry;

/**
 * A view onto one FTC analog input, such as a potentiometer.
 *
 * <p><strong>Raw voltage is rarely what you want.</strong> A potentiometer's useful range is a
 * mechanical angle, not 0–5 V, and the two differ by a factor and an offset that depend on the
 * linkage. {@link #fraction()} maps the reading onto {@code [0, 1]} of the channel's configured
 * range, which is the step that makes the value meaningful without the framework knowing any
 * physical constant.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class AnalogSensor {

    private final AnalogInput input;

    private final String name;

    /**
     * Wraps an SDK analog input.
     *
     * @param input the input to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AnalogSensor(AnalogInput input, String name) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.input = input;
        this.name = name;
    }

    /**
     * Looks an analog input up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no input is configured under that name
     */
    public AnalogSensor(HardwareMap hardwareMap, String name) {
        this(new HardwareRegistry(hardwareMap).require(AnalogInput.class, name), name);
    }

    /**
     * Looks an analog input up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no analog input is configured under that
     *     name
     */
    public AnalogSensor(HardwareRegistry registry, String name) {
        this(registry.require(AnalogInput.class, name), name);
    }

    /**
     * Returns the underlying SDK input.
     *
     * @return the SDK input, never {@code null}
     */
    public AnalogInput getSdkObject() {
        return input;
    }

    /**
     * Returns the configuration name this input was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the raw reading.
     *
     * @return volts
     */
    public double getVoltage() {
        return input.getVoltage();
    }

    /**
     * Returns the reading as a fraction of the channel's configured range.
     *
     * <p>The SDK's {@code AnalogInput} exposes a maximum voltage but no minimum, so the range is
     * taken as {@code [0, max]}. A channel that does not start at zero is out of spec anyway.
     *
     * <p>Not clamped. A reading above the configured maximum is returned as measured — above {@code
     * 1} — so a misconfigured or overdriven channel is visible rather than hidden. A reading below
     * zero volts is a broken sensor and should be investigated, not smoothed away.
     *
     * @return the reading as a fraction of the configured range, or {@code 0} if the configured
     *     maximum is zero or negative
     */
    public double fraction() {
        final double max = input.getMaxVoltage();
        if (max <= 0.0) {
            return 0.0;
        }
        return getVoltage() / max;
    }

    /**
     * Returns the reading mapped onto a caller-supplied range.
     *
     * <p>The general form of {@link #fraction()}, for a sensor whose useful range is narrower than
     * the electrical one. Both physical endpoints are parameters, because the framework does not
     * know them (ADR-012).
     *
     * @param atMin the value the reading should map to at zero volts
     * @param atMax the value the reading should map to at the channel maximum
     * @return the mapped value
     */
    public double mapTo(double atMin, double atMax) {
        return atMin + fraction() * (atMax - atMin);
    }

    @Override
    public String toString() {
        return "AnalogSensor[" + name + "]";
    }
}
