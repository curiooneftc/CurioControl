package org.curioone.control.core;

/**
 * Base class for an autonomous OpMode.
 *
 * <p>Identical in behaviour to {@link CurioOpMode}; it exists so an autonomous can be recognised in
 * the Robot Controller and in code review as a different kind of program from a TeleOp, which is
 * worth something when a routine and a driver-mode share subsystems.
 *
 * <p>There is no behaviour difference between the two. If you find yourself overriding a method
 * that only exists here, it should move to {@link CurioOpMode}.
 *
 * @since 0.1.0
 */
public abstract class CurioAuto extends CurioOpMode {
    // Intentionally empty. See the class Javadoc.
}
