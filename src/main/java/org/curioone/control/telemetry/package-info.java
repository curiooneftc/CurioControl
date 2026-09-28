/**
 * Telemetry and structured logging.
 *
 * <p>{@code TelemetryManager} batches values and flushes them to the Robot Controller in a single
 * call per loop, which keeps the control loop free of per-value overhead. The structured {@code
 * Logger} writes CSV for offline analysis and is <em>disabled by default</em> so that logging can
 * never surprise a team mid-match.
 *
 * <p>Neither type prints to standard output; diagnostic output belongs in telemetry so it reaches
 * the driver.
 *
 * @since 0.1.0
 */
package org.curioone.control.telemetry;
