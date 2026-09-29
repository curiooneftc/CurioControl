package org.curioone.control.core;

/**
 * Thrown when CurioControl cannot build a working robot.
 *
 * <p>The framework fails loudly and immediately rather than substituting hardware or degrading
 * quietly, so that a misconfiguration is discovered during initialization instead of on the field
 * as a robot that drives with one wheel inverted.
 *
 * <p>The message is always formatted the same way, so a driver or a scout reading telemetry can
 * recognise it:
 *
 * <pre>{@code
 * [CurioControl] ERROR
 * Missing hardware device: armMotor
 * Expected configuration name: "arm" of type MOTOR
 * }</pre>
 *
 * <p>This is an unchecked exception: a caller cannot reasonably be expected to recover from a robot
 * that cannot be built, and a checked one would force a {@code try}/{@code catch} around every
 * OpMode's initialization.
 *
 * @since 0.1.0
 */
public class CurioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Prefix on every CurioControl error line, so failures are identifiable in the log. */
    private static final String PREFIX = "[CurioControl] ERROR";

    /**
     * Creates an exception with a fully formatted message.
     *
     * @param message the message; CurioControl does not add the prefix itself
     */
    public CurioException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a fully formatted message and an underlying cause.
     *
     * @param message the message
     * @param cause the underlying failure
     */
    public CurioException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Reports a device that is not present in the Robot Controller configuration.
     *
     * <p>The returned exception is not thrown; the caller decides. The message names both the key
     * that was requested and the type that was expected, because a lookup most often fails for one
     * of two reasons and the pair disambiguates them: the name is misspelled, or the device is
     * configured as a different type.
     *
     * @param name the configuration name that was requested
     * @param type the hardware type that was expected
     * @return an exception describing the missing device
     */
    public static CurioException missingDevice(String name, String type) {
        return new CurioException(
                PREFIX
                        + System.lineSeparator()
                        + "Missing hardware device: "
                        + name
                        + System.lineSeparator()
                        + "Expected configuration name: \""
                        + name
                        + "\" of type "
                        + type);
    }
}
