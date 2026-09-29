/**
 * Structured logging for the Robot Controller.
 *
 * <p>Reserved for the CSV {@code Logger}, which lands in v0.2.0. Telemetry itself is a {@link
 * org.curioone.control.core.TelemetryManager} in {@code core}, because {@code CurioRobot} owns it
 * and needs it without {@code core} depending on this package — the one-way rule that ADR-014
 * enforces.
 *
 * <p>When {@code Logger} arrives it depends on {@code core} and {@code util}, and nothing in {@code
 * core} will depend on it, so the dependency stays one-directional.
 *
 * @since 0.1.0
 */
package org.curioone.control.telemetry;
