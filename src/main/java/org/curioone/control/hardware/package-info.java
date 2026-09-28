/**
 * Lightweight wrappers around common FTC hardware.
 *
 * <p>Every wrapper in this package holds the underlying SDK object and exposes it through {@code
 * getSdkObject()}, so direct SDK access stays possible when a team needs something the wrapper does
 * not cover. Wrappers validate arguments where practical and never silently substitute a missing
 * device — a missing device is a loud, actionable error.
 *
 * <p>The wrappers wrap the SDK; they do not replace it. This package is the main place where {@code
 * com.qualcomm.robotcore} types appear, and it is the boundary that mocked tests substitute for.
 *
 * @since 0.1.0
 */
package org.curioone.control.hardware;
