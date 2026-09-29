package org.curioone.control.support;

import java.util.HashMap;
import java.util.Map;
import org.curioone.control.core.HardwareSource;

/**
 * A {@link HardwareSource} backed by a plain map, for tests.
 *
 * <p>The SDK's {@code HardwareMap} reaches into Android and cannot be mocked on a desktop JVM. This
 * stands in for it: register a device under a name, and the framework finds it exactly as it would
 * on a Robot Controller.
 *
 * <pre>{@code
 * FakeHardwareSource hardware = new FakeHardwareSource();
 * hardware.add("frontLeft", new FakeDcMotor());
 * CurioRobot robot = new CurioRobot(hardwareMapOf(hardware), ...);
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.1.0
 */
public final class FakeHardwareSource implements HardwareSource {

    private final Map<String, Object> byName = new HashMap<>();

    /** Counts {@code get} calls, so a test can assert on caching. */
    private int lookups;

    /**
     * Registers a device under a name.
     *
     * @param <T> the device type
     * @param name the configuration name
     * @param device the device
     * @return this source, for chaining
     * @throws IllegalArgumentException if {@code name} or {@code device} is {@code null}
     */
    public <T> FakeHardwareSource add(String name, T device) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (device == null) {
            throw new IllegalArgumentException("device must not be null");
        }
        byName.put(name, device);
        return this;
    }

    /**
     * Registers a motor under a name, so a {@code Motor} wrapper can be built.
     *
     * @param name the configuration name
     * @return a new motor registered under that name
     */
    public FakeDcMotor addMotor(String name) {
        final FakeDcMotor motor = new FakeDcMotor();
        add(name, motor);
        return motor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> deviceClass, String name) {
        lookups++;
        final Object device = byName.get(name);
        if (device == null) {
            return null;
        }
        if (!deviceClass.isInstance(device)) {
            // The SDK's HardwareMap returns null for a type mismatch rather than throwing, and the
            // framework must behave identically.
            return null;
        }
        return (T) device;
    }

    /**
     * Returns how many lookups have been performed.
     *
     * @return the lookup count
     */
    public int lookups() {
        return lookups;
    }

    /** Forgets every registered device and resets the lookup count. */
    public void clear() {
        byName.clear();
        lookups = 0;
    }
}
