/**
 * Modular vision support.
 *
 * <p>Vision is optional and lazy: nothing in this package is constructed unless a team calls {@code
 * robot.vision()}, so teams that never use vision pay nothing. No class in {@code core}, {@code
 * control}, or {@code drive} may import this package, which keeps vision dependencies out of robots
 * that do not want them.
 *
 * @since 0.4.0
 */
package org.curioone.control.vision;
