package org.curioone.control.telemetry;

/**
 * Thrown when the {@link Logger} cannot reach its storage.
 *
 * <p>An unchecked exception on purpose: logging is diagnostics, and a full disk must not force
 * every OpMode into checked-exception handling for a path that only fails on exhausted hardware.
 * Callers that need the guarantee can catch this around {@code update} and degrade to
 * telemetry-only output.
 *
 * @since 0.2.0
 */
public final class CurioIoException extends RuntimeException {

    /**
     * Creates an I/O failure with a message.
     *
     * @param message what could not be done
     */
    public CurioIoException(String message) {
        super(message);
    }

    /**
     * Creates an I/O failure wrapping the underlying cause.
     *
     * @param message what could not be done
     * @param cause the underlying failure
     */
    public CurioIoException(String message, Throwable cause) {
        super(message, cause);
    }
}
