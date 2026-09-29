package org.curioone.control.core;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Adapts the Robot Controller's {@link Telemetry} to a {@link TelemetrySink}.
 *
 * <p>Keeps the SDK's wide interface at the edge. Everything above this class works against the
 * two-method port, which is what makes the buffering and cadence logic testable.
 *
 * <p>It deliberately does not change {@code setAutoClear}. The SDK clears items after each update
 * by default, and {@link TelemetryManager} re-adds every value on every flush, so leaving the
 * caller's setting alone is correct in both modes.
 *
 * <p><strong>Thread safety:</strong> delegates to the SDK, so it follows the SDK's own rules. Call
 * from the OpMode thread.
 *
 * @since 0.1.0
 */
public final class SdkTelemetrySink implements TelemetrySink {

    private final Telemetry telemetry;

    /**
     * Wraps an SDK telemetry sink.
     *
     * @param telemetry the sink, typically {@code OpMode.telemetry}
     * @throws IllegalArgumentException if {@code telemetry} is {@code null}
     */
    public SdkTelemetrySink(Telemetry telemetry) {
        if (telemetry == null) {
            throw new IllegalArgumentException("telemetry must not be null");
        }
        this.telemetry = telemetry;
    }

    /**
     * Returns the underlying SDK telemetry object.
     *
     * @return the SDK telemetry, never {@code null}
     */
    public Telemetry getSdkObject() {
        return telemetry;
    }

    @Override
    public void addLine(String caption, Object value) {
        telemetry.addData(caption, value);
    }

    @Override
    public void flush() {
        telemetry.update();
    }
}
