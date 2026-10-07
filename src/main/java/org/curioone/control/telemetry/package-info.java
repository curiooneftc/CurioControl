/**
 * Structured logging for the Robot Controller.
 *
 * <p>{@link org.curioone.control.telemetry.Logger} writes opt-in CSV rows to storage, gated on
 * {@code CurioConfig.LOGGING_ENABLED} so a competition run pays nothing. Telemetry itself is a
 * {@link org.curioone.control.core.TelemetryManager} in {@code core}, because {@code CurioRobot}
 * owns it and needs it without {@code core} depending on this package — the one-way rule that
 * ADR-014 enforces.
 *
 * @since 0.1.0
 */
package org.curioone.control.telemetry;
