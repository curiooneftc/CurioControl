/**
 * Core runtime for CurioControl.
 *
 * <p>This package holds the robot container ({@code CurioRobot}), the framework configuration
 * surface ({@code CurioConfig}), the optional OpMode base classes ({@code CurioOpMode}, {@code
 * CurioAuto}), the hardware registry that resolves named devices with actionable error messages,
 * and the {@code Subsystem} base class.
 *
 * <p>Nothing in this package may depend on {@code drive}, {@code command}, {@code telemetry}, or
 * {@code vision} — the dependency direction is defined in {@code docs/PROJECT_STRUCTURE.md} and
 * enforced by ArchUnit tests.
 *
 * @since 0.1.0
 */
package org.curioone.control.core;
