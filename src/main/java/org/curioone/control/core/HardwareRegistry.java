package org.curioone.control.core;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves named devices from the Robot Controller configuration.
 *
 * <p>Every lookup either returns the configured device or throws. It never substitutes a different
 * device, and it never returns {@code null} (ADR-012, spec §46). A robot that drives with one wheel
 * inverted because a name was mistyped is far harder to diagnose than one that refuses to
 * initialize.
 *
 * <h2>Why the registry exists</h2>
 *
 * The SDK returns {@code null} for a missing device, so every lookup site needs its own null check
 * and its own error message. Centralising that means the message is the same everywhere, and a test
 * can assert on it.
 *
 * <h2>Caching</h2>
 *
 * Resolved devices are cached by name and type, so asking twice returns the same instance rather
 * than looking it up again. The same name under two types is two entries.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Use it from the OpMode thread during
 * initialization.
 *
 * @since 0.1.0
 */
public final class HardwareRegistry {

    private final HardwareSource source;

    private final Map<CacheKey, Object> cache = new LinkedHashMap<>();

    private final HardwareMap sdkMap;

    /**
     * Creates a registry over a Robot Controller hardware map.
     *
     * @param hardwareMap the configured hardware
     * @throws IllegalArgumentException if {@code hardwareMap} is {@code null}
     */
    public HardwareRegistry(HardwareMap hardwareMap) {
        this(new SdkHardwareSource(hardwareMap));
    }

    /**
     * Creates a registry over any source of named devices.
     *
     * <p>For tests, and for anything that wants to supply devices from somewhere other than the
     * Robot Controller configuration.
     *
     * @param source where devices come from
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public HardwareRegistry(HardwareSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.source = source;
        this.sdkMap = source instanceof SdkHardwareSource sdk ? sdk.hardwareMap() : null;
    }

    /**
     * Returns the underlying Robot Controller hardware map.
     *
     * <p>Exposed so a team can reach a device the framework does not wrap. Wrapping is a
     * convenience, not a wall (ADR-005). Returns {@code null} when the registry was not built from
     * an SDK hardware map.
     *
     * @return the SDK hardware map, or {@code null}
     */
    public HardwareMap hardwareMap() {
        return sdkMap;
    }

    /**
     * Looks up a device by name and type.
     *
     * @param <T> the SDK device type
     * @param type the SDK device class to look up
     * @param name the configuration name
     * @return the configured device, never {@code null}
     * @throws CurioException if no device with that name and type is configured
     * @throws IllegalArgumentException if {@code type} or {@code name} is {@code null}
     */
    public <T> T require(Class<T> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        final CacheKey key = new CacheKey(type, name);
        final Object cached = cache.get(key);
        if (cached != null) {
            return type.cast(cached);
        }

        final T device = source.get(type, name);
        if (device == null) {
            throw CurioException.missingDevice(name, type.getSimpleName());
        }
        cache.put(key, device);
        return device;
    }

    /**
     * Looks up a motor by name.
     *
     * @param name the configuration name
     * @return the configured motor, never {@code null}
     * @throws CurioException if no motor with that name is configured
     */
    public DcMotorEx motor(String name) {
        return require(DcMotorEx.class, name);
    }

    /**
     * Reports whether a device is configured, without throwing.
     *
     * <p>Use this to probe optional hardware — a second camera, a sensor some robots have and
     * others do not. Do not use it to guard a required device: that turns a loud failure into a
     * silent one.
     *
     * @param type the SDK device class
     * @param name the configuration name
     * @return {@code true} if the device is configured
     */
    public boolean has(Class<?> type, String name) {
        return name != null && type != null && source.get(type, name) != null;
    }

    /**
     * Returns the names the registry has successfully resolved, in first-use order.
     *
     * <p>Intended for a "what is on this robot" diagnostic during debugging.
     *
     * @return an unmodifiable view of the resolved device names
     */
    public Set<String> resolvedNames() {
        final Set<String> names = new LinkedHashSet<>();
        for (CacheKey key : cache.keySet()) {
            names.add(key.name);
        }
        return Collections.unmodifiableSet(names);
    }

    /** Cache identity: the same name looked up as two different types is two entries. */
    private static final class CacheKey {
        private final Class<?> type;
        private final String name;

        private CacheKey(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey)) {
                return false;
            }
            final CacheKey that = (CacheKey) other;
            return type.equals(that.type) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + name.hashCode();
        }
    }
}
