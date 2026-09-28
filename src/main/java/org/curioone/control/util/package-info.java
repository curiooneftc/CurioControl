/**
 * Small, general-purpose utilities.
 *
 * <p>These exist because something in the framework repeatedly needed them — range clamping,
 * timing, rate limiting, debouncing, and edge detection. This package is deliberately not a
 * general-purpose dumping ground: a utility lands here only when it solves a recurring problem, and
 * it must earn the coverage target that the rest of the framework's pure logic gets.
 *
 * <p>No class in this package may import {@code com.qualcomm.*} or {@code android.*}. It provides
 * {@code Clock}, the time abstraction that keeps every time-dependent component in CurioControl
 * deterministically testable.
 *
 * @since 0.1.0
 */
package org.curioone.control.util;
