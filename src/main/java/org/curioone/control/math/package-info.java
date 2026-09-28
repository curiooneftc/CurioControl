/**
 * Robotics mathematics: geometry, units, and interpolation.
 *
 * <p>Everything here is plain Java. No class in this package may import {@code com.qualcomm.*} or
 * {@code android.*}, which is what makes the geometry stack testable to near-total coverage on a
 * desktop JVM. The rule is enforced by the {@code MathPackageIsPure} architecture test.
 *
 * <p>Conventions used throughout this package:
 *
 * <ul>
 *   <li>angles are radians unless a name or JavaDoc says otherwise;
 *   <li>distances are in inches or millimetres, named explicitly;
 *   <li>types are immutable — every operation returns a new instance.
 * </ul>
 *
 * @since 0.2.0
 */
package org.curioone.control.math;
