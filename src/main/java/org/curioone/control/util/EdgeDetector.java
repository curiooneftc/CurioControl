package org.curioone.control.util;

/**
 * Detects transitions on a boolean signal: button presses, sensor trips, command completions.
 *
 * <p>Poll once per loop with the current level; the return says what changed since the last call.
 * One call per loop — the update both answers and advances, so asking twice consumes the edge
 * twice. For "run once when the driver presses A", this is the whole implementation:
 *
 * <pre>{@code
 * if (pressA.update(gamepad1.a)) {
 *     scheduler.schedule(score);
 * }
 * }</pre>
 *
 * <p>Pure logic, no clock: edges are about order, not time. For inputs that need to be stable
 * before they count, see {@link Debouncer}.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.4.0
 */
public final class EdgeDetector {

    /** What changed since the previous poll, if anything. */
    public enum Edge {
        /** The level did not change. */
        NONE,
        /** The level rose from false to true. */
        RISING,
        /** The level fell from true to false. */
        FALLING
    }

    private boolean previous;

    /** Creates a detector starting low. */
    public EdgeDetector() {
        this(false);
    }

    /**
     * Creates a detector with a known starting level.
     *
     * <p>Match this to the signal's actual state when the detector is created — usually low — or
     * the first poll reports a phantom edge for a transition that happened before the detector
     * existed.
     *
     * @param initial the assumed level before the first poll
     */
    public EdgeDetector(boolean initial) {
        this.previous = initial;
    }

    /**
     * Polls the signal.
     *
     * @param level the current level
     * @return what changed since the previous poll
     */
    public Edge poll(boolean level) {
        final Edge edge;
        if (level == previous) {
            edge = Edge.NONE;
        } else if (level) {
            edge = Edge.RISING;
        } else {
            edge = Edge.FALLING;
        }
        previous = level;
        return edge;
    }

    /**
     * Forgets the history and assumes a level.
     *
     * @param level the level to assume going forward
     */
    public void reset(boolean level) {
        previous = level;
    }
}
