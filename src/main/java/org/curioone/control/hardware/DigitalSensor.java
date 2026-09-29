package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.core.HardwareRegistry;

/**
 * A view onto one FTC digital channel, such as a limit switch.
 *
 * <p>The SDK's {@code DigitalChannel} is both an input and an output, so a limit switch and an LED
 * share a type. This wrapper is input-first, with output available when a mechanism actually drives
 * the line.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class DigitalSensor {

    private final DigitalChannel channel;

    private final String name;

    /**
     * Wraps an SDK digital channel.
     *
     * @param channel the channel to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public DigitalSensor(DigitalChannel channel, String name) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.channel = channel;
        this.name = name;
    }

    /**
     * Looks a digital channel up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no channel is configured under that name
     */
    public DigitalSensor(HardwareMap hardwareMap, String name) {
        this(new HardwareRegistry(hardwareMap).require(DigitalChannel.class, name), name);
    }

    /**
     * Looks a digital channel up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no digital channel is configured under
     *     that name
     */
    public DigitalSensor(HardwareRegistry registry, String name) {
        this(registry.require(DigitalChannel.class, name), name);
    }

    /**
     * Returns the underlying SDK channel.
     *
     * @return the SDK channel, never {@code null}
     */
    public DigitalChannel getSdkObject() {
        return channel;
    }

    /**
     * Returns the configuration name this channel was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the raw channel state.
     *
     * @return {@code true} if the line reads high
     */
    public boolean isHigh() {
        return channel.getState();
    }

    /**
     * Returns the inverted channel state.
     *
     * <p>Whether a switch is active-high or active-low is a wiring property, not a sensor property,
     * so the framework does not guess. A limit switch that is active low is very easy to misread as
     * broken, so this exists to make the inversion explicit at the call site rather than hidden in
     * a subclass.
     *
     * @param activeHigh {@code true} if the sensor is active when the line reads high
     * @return the logical state
     */
    public boolean isTriggered(boolean activeHigh) {
        return activeHigh ? isHigh() : !isHigh();
    }

    /**
     * Drives the line.
     *
     * <p>Only meaningful when the channel is configured as an output.
     *
     * @param state {@code true} to drive the line high
     */
    public void setHigh(boolean state) {
        channel.setState(state);
    }

    @Override
    public String toString() {
        return "DigitalSensor[" + name + "]";
    }
}
