package org.curioone.control.core;

/**
 * A source of named hardware devices.
 *
 * <p>Exists for the same reason {@link TelemetrySink} does. The Robot Controller's {@code
 * HardwareMap} is a concrete class whose hierarchy reaches into Android ({@code
 * android.content.Context} and friends), so it cannot be mocked on a desktop JVM — and neither can
 * anything built directly on it. A two-parameter port makes the whole hardware registry testable
 * with an ordinary map.
 *
 * <p>In an OpMode, wrap the SDK map with {@link SdkHardwareSource}.
 *
 * <p><strong>Thread safety:</strong> called only from the OpMode thread.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface HardwareSource {

    /**
     * Returns the configured device of a given type, or {@code null} if it is not configured.
     *
     * <p>Returning {@code null} here is correct and expected; turning that into a loud failure is
     * {@link HardwareRegistry}'s job, not this one's. The two-method contract is what {@link
     * TelemetrySink} should have been, and the {@code null} is the one case a single method cannot
     * express.
     *
     * @param <T> the SDK device type
     * @param deviceClass the SDK device class
     * @param name the configuration name
     * @return the device, or {@code null} if not configured
     */
    <T> T get(Class<T> deviceClass, String name);
}
