/**
 * The lightweight command-based architecture.
 *
 * <p>A command describes one robot action. The scheduler runs commands from the OpMode loop, is
 * single-threaded, and enforces exclusive subsystem ownership so two commands that need the same
 * subsystem can never run concurrently.
 *
 * <p>Commands and state machines coexist; neither is required. See {@code PHASES.md} for the v0.3.0
 * milestone that introduces this package.
 *
 * @since 0.3.0
 */
package org.curioone.control.command;
