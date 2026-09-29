package org.curioone.control.core;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Adapts the Robot Controller's {@link HardwareMap} to a {@link HardwareSource}.
 *
 * <p>Keeps the SDK's Android-coupled class at the edge. Everything above works against the
 * one-method port, which is what makes the hardware registry testable without an Android runtime.
 *
 * <p><strong>Thread safety:</strong> follows the SDK's own rules. Call from the OpMode thread.
 *
 * @since 0.1.0
 */
public final class SdkHardwareSource implements HardwareSource {

    private final HardwareMap hardwareMap;

    /**
     * Wraps an SDK hardware map.
     *
     * @param hardwareMap the configured hardware
     * @throws IllegalArgumentException if {@code hardwareMap} is {@code null}
     */
    public SdkHardwareSource(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("hardwareMap must not be null");
        }
        this.hardwareMap = hardwareMap;
    }

    /**
     * Returns the underlying SDK hardware map.
     *
     * @return the SDK hardware map, never {@code null}
     */
    public HardwareMap hardwareMap() {
        return hardwareMap;
    }

    @Override
    public <T> T get(Class<T> deviceClass, String name) {
        return hardwareMap.get(deviceClass, name);
    }
}
