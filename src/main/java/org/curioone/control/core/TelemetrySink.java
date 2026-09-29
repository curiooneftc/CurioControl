package org.curioone.control.core;

/**
 * Where {@link TelemetryManager} writes its values.
 *
 * <p>An output port, not a hardware wrapper. {@link SdkTelemetrySink} adapts the Robot Controller's
 * {@code Telemetry}, and {@link #discarding()} discards everything — which is what a non-OpMode
 * program or a unit test uses.
 *
 * <p>This exists for the same reason {@link org.curioone.control.util.Clock} does: the SDK's
 * telemetry interface has two dozen methods, so testing against it directly means either mocking a
 * wide surface or asserting almost nothing. A two-method port makes the interesting behaviour —
 * what is buffered, in what order, and when it flushes — directly assertable.
 *
 * <p><strong>Thread safety:</strong> called only from the OpMode thread.
 *
 * @since 0.1.0
 */
public interface TelemetrySink {

    /**
     * Receives one caption and value pair.
     *
     * @param caption the display caption, already prefixed with its category
     * @param value the value
     */
    void addLine(String caption, Object value);

    /** Makes every line received since the last flush visible. */
    void flush();

    /**
     * Returns a sink that discards everything.
     *
     * <p>Used by a program with no SDK OpMode, and by tests that are not exercising the sink
     * itself.
     *
     * @return a sink that discards all output
     */
    static TelemetrySink discarding() {
        return new TelemetrySink() {
            @Override
            public void addLine(String caption, Object value) {
                // Deliberately empty: telemetry is optional.
            }

            @Override
            public void flush() {
                // Deliberately empty: telemetry is optional.
            }
        };
    }
}
