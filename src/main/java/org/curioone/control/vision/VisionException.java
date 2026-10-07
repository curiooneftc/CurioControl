package org.curioone.control.vision;

/**
 * Thrown when vision cannot do what was asked of it.
 *
 * <p>Vision's own failure type, because the framework's {@code core.CurioException} lives behind a
 * layering wall this package may not cross: nothing outside {@code vision} may depend on it, and it
 * may not depend on {@code core} (see the layering test). Same contract as its counterpart —
 * unchecked, formatted, naming the missing piece — without the dependency.
 *
 * @since 0.4.0
 */
public final class VisionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a vision failure with a message.
     *
     * @param message what went wrong and what to attach or configure
     */
    public VisionException(String message) {
        super(message);
    }

    /**
     * Creates a vision failure wrapping the underlying cause.
     *
     * @param message what went wrong
     * @param cause the underlying failure
     */
    public VisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
