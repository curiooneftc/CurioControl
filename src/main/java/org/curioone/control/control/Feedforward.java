package org.curioone.control.control;

/**
 * An open-loop model of what a mechanism needs, independent of any feedback.
 *
 * <p>Feedback corrects for what the model gets wrong; the model carries what is predictable —
 * friction, back-EMF, inertia, gravity — so the feedback gains can stay small and the loop stays
 * stable. Every method takes the full motion state and ignores what it does not need, so models
 * compose: a {@link CombinedFeedforward} is just several of these added together.
 *
 * <p>Units are the caller's: if velocity is ticks per second, the output is in whatever unit the
 * gains were tuned in, usually motor power. Mixed units are the classic feedforward bug — keep one
 * convention per mechanism and write it down.
 *
 * <p>Implementations must be stateless and allocation-free per call.
 *
 * @since 0.2.0
 */
public interface Feedforward {

    /**
     * Computes the open-loop output for a motion state.
     *
     * @param position the mechanism position, in caller units (radians for gravity models)
     * @param velocity the mechanism velocity, in caller units per second
     * @param acceleration the mechanism acceleration, in caller units per second squared
     * @return the predicted output
     * @throws IllegalArgumentException if any argument is not finite
     */
    double calculate(double position, double velocity, double acceleration);
}
