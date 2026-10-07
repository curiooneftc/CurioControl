/**
 * Modular vision support.
 *
 * <p>Vision is optional and lazy: nothing in this package is constructed unless a team calls {@code
 * robot.vision()}, so teams that never use vision pay nothing. No class in {@code core}, {@code
 * control}, or {@code drive} may import this package, with one exception — {@code CurioRobot}, the
 * composition root, which resolves the manager lazily on first use. That single reference is what
 * keeps the {@code robot.vision()} facade without taxing robots that never call it; the layering
 * test enforces the exception by class name.
 *
 * @since 0.4.0
 */
package org.curioone.control.vision;
