package org.curioone.control.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * A lightweight state machine for autonomous routines that read better as states than as trees.
 *
 * <p>States are values — usually an enum — with entry, update, and exit actions, guarded
 * transitions, and per-state timeouts. Call {@link #update()} once per loop; at most one transition
 * happens per call, so the traversal is deterministic and a test can assert the exact path through
 * the graph.
 *
 * <pre>{@code
 * enum Auto { DRIVE_TO_SCORE, SCORE, RETURN, PARK }
 *
 * StateMachine<Auto> auto = new StateMachine<>(Auto.DRIVE_TO_SCORE);
 * auto.onEnter(Auto.SCORE, () -> arm.moveTo(HIGH));
 * auto.addTransition(Auto.SCORE, Auto.RETURN, arm::atTarget);
 * auto.setTimeout(Auto.SCORE, 3.0, Auto.RETURN);
 * // ... per loop:
 * auto.update();
 * }</pre>
 *
 * <h2>One transition per update</h2>
 *
 * <p>Each {@code update()} runs the current state's update action, then evaluates at most one
 * transition: an explicit {@link #requestTransition} first, then the timeout, then the guards in
 * registration order. The new state's update action runs on the <em>next</em> call, never the same
 * one — entry and update never interleave within a pass, which is what keeps multi-step traversals
 * assertable.
 *
 * <h2>Commands and states together</h2>
 *
 * <p>The machine never touches the scheduler itself: an update action may {@code
 * scheduler.schedule(...)} a command, and a command's end action may {@code
 * requestTransition(...)}. That one-directional wiring — states know commands, commands know
 * states, neither owns the other — is what lets a routine mix both without a cycle. See the
 * commands guide for the full autonomous example written both ways.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @param <S> the state type, typically an enum
 * @since 0.3.0
 */
public final class StateMachine<S> {

    /** Nanoseconds per second, for the elapsed-time conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Clock clock;

    private final Map<S, Runnable> entries = new HashMap<>();

    private final Map<S, Runnable> updates = new HashMap<>();

    private final Map<S, Runnable> exits = new HashMap<>();

    private final Map<S, List<Guard<S>>> transitions = new HashMap<>();

    private final Map<S, Timeout<S>> timeouts = new HashMap<>();

    private S state;

    private S requested;

    private long stateStartNanos;

    private boolean started;

    /**
     * Creates a machine with the system clock.
     *
     * @param initial the starting state
     * @throws IllegalArgumentException if {@code initial} is {@code null}
     */
    public StateMachine(S initial) {
        this(initial, new SystemClock());
    }

    /**
     * Creates a machine with an injected time source.
     *
     * @param initial the starting state
     * @param clock the time source for timeouts and {@link #timeInState()}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public StateMachine(S initial, Clock clock) {
        if (initial == null) {
            throw new IllegalArgumentException("initial must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.state = initial;
        this.clock = clock;
    }

    /**
     * Runs one machine pass.
     *
     * <p>Applies a requested transition, runs the current update action, then evaluates at most one
     * transition — timeout before guards. Entry actions run when their state is entered; update
     * actions run every pass their state is current, including the pass that entered it on a
     * previous call but never the pass that enters it now.
     */
    public void update() {
        if (!started) {
            started = true;
            stateStartNanos = clock.nowNanos();
        }
        if (requested != null) {
            final S target = requested;
            requested = null;
            transitionTo(target);
            return;
        }
        runAction(updates, state);
        final Timeout<S> timeout = timeouts.get(state);
        if (timeout != null && elapsedSeconds() >= timeout.seconds) {
            transitionTo(timeout.target);
            return;
        }
        final List<Guard<S>> guards = transitions.get(state);
        if (guards != null) {
            for (Guard<S> guard : guards) {
                if (guard.condition.getAsBoolean()) {
                    transitionTo(guard.target);
                    return;
                }
            }
        }
    }

    /**
     * Returns the current state.
     *
     * @return the state
     */
    public S getState() {
        return state;
    }

    /**
     * Returns how long the machine has been in the current state.
     *
     * @return seconds in state, or {@code 0} before the first {@link #update()}
     */
    public double timeInState() {
        if (!started) {
            return 0.0;
        }
        return (clock.nowNanos() - stateStartNanos) / NANOS_PER_SECOND;
    }

    /**
     * Requests a transition applied at the next {@link #update()}.
     *
     * <p>Explicit transitions beat guards and timeouts: a requested state is entered before
     * anything else is evaluated. A second request overwrites the first — the machine moves once
     * per pass, so only the latest request can be honoured.
     *
     * @param target the state to enter
     * @throws IllegalArgumentException if {@code target} is {@code null}
     */
    public void requestTransition(S target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        requested = target;
    }

    /**
     * Returns to a state without running exit or entry actions.
     *
     * <p>For OpMode initialization: point the machine at its start before the loop begins. The
     * timer restarts on the next {@link #update()}. Transitions taken during the run always fire
     * their actions; only this explicit reset skips them, because initialization is not a
     * transition.
     *
     * @param state the state to return to
     * @throws IllegalArgumentException if {@code state} is {@code null}
     */
    public void reset(S state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        this.state = state;
        this.requested = null;
        this.started = false;
    }

    /**
     * Registers the action run when a state is entered.
     *
     * @param state the state
     * @param action the entry action
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws IllegalStateException if the state already has an entry action
     */
    public void onEnter(S state, Runnable action) {
        putOnce(entries, state, action, "entry");
    }

    /**
     * Registers the action run every pass while a state is current.
     *
     * @param state the state
     * @param action the update action
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws IllegalStateException if the state already has an update action
     */
    public void onUpdate(S state, Runnable action) {
        putOnce(updates, state, action, "update");
    }

    /**
     * Registers the action run when a state is left.
     *
     * @param state the state
     * @param action the exit action
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws IllegalStateException if the state already has an exit action
     */
    public void onExit(S state, Runnable action) {
        putOnce(exits, state, action, "exit");
    }

    /**
     * Registers a guarded transition out of a state.
     *
     * <p>Guards for one state are evaluated in registration order, first true wins. Several guards
     * may target the same state — they are alternatives, evaluated top to bottom.
     *
     * @param from the state the transition leaves
     * @param to the state the transition enters
     * @param condition the guard, polled every pass while {@code from} is current
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public void addTransition(S from, S to, BooleanSupplier condition) {
        if (from == null) {
            throw new IllegalArgumentException("from must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("to must not be null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        transitions
                .computeIfAbsent(from, ignored -> new ArrayList<>())
                .add(new Guard<>(to, condition));
    }

    /**
     * Registers a per-state timeout.
     *
     * <p>When the machine has been in {@code state} for {@code seconds}, it transitions to {@code
     * target} — a watchdog against a guard that never becomes true. Timeouts are evaluated before
     * guards, and a zero timeout transitions on the first pass after entering. Replaces any
     * previous timeout for the state.
     *
     * @param state the state the timeout applies to
     * @param seconds the deadline in seconds; must not be negative
     * @param target the state to enter on expiry
     * @throws IllegalArgumentException if {@code state} or {@code target} is {@code null}, or
     *     {@code seconds} is negative or NaN
     */
    public void setTimeout(S state, double seconds, S target) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (Double.isNaN(seconds) || seconds < 0.0) {
            throw new IllegalArgumentException(
                    "seconds must be non-negative and not NaN but was " + seconds);
        }
        timeouts.put(state, new Timeout<>(seconds, target));
    }

    private void transitionTo(S target) {
        runAction(exits, state);
        state = target;
        stateStartNanos = clock.nowNanos();
        runAction(entries, state);
    }

    private void runAction(Map<S, Runnable> actions, S forState) {
        final Runnable action = actions.get(forState);
        if (action != null) {
            action.run();
        }
    }

    private void putOnce(Map<S, Runnable> actions, S state, Runnable action, String kind) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (actions.containsKey(state)) {
            throw new IllegalStateException(
                    "state " + state + " already has an " + kind + " action");
        }
        actions.put(state, action);
    }

    private double elapsedSeconds() {
        return (clock.nowNanos() - stateStartNanos) / NANOS_PER_SECOND;
    }

    /** A guarded edge to another state. */
    private static final class Guard<S> {

        private final S target;

        private final BooleanSupplier condition;

        Guard(S target, BooleanSupplier condition) {
            this.target = target;
            this.condition = condition;
        }
    }

    /** A deadline that moves the machine to another state. */
    private static final class Timeout<S> {

        private final double seconds;

        private final S target;

        Timeout(double seconds, S target) {
            this.seconds = seconds;
            this.target = target;
        }
    }
}
