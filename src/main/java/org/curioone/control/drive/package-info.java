/**
 * Generic drivetrain abstractions.
 *
 * <p>{@code DriveBase} defines the contract shared by every drivetrain. Implementations translate
 * driver inputs into per-motor powers; they do not own hardware configuration beyond what the
 * chosen kinematics requires.
 *
 * <p>Physical constants — wheel diameter, gear ratio, ticks per revolution — always come from the
 * robot project's configuration class. This package never contains a competition-specific number.
 *
 * @since 0.1.0
 */
package org.curioone.control.drive;
