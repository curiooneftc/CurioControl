/**
 * Reusable, hardware-independent control algorithms.
 *
 * <p>Controllers in this package take plain numbers and time, never SDK objects, so they are fully
 * unit-testable on a desktop JVM. Time is supplied through {@code org.curioone.control.util.Clock}
 * rather than read from the system clock directly.
 *
 * <p>No class in this package may import {@code com.qualcomm.*} or {@code android.*}. That rule is
 * enforced by the {@code ControlPackageIsPure} architecture test.
 *
 * @since 0.1.0
 */
package org.curioone.control.control;
