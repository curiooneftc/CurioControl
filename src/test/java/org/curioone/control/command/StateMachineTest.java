package org.curioone.control.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StateMachine}. */
@DisplayName("StateMachine")
class StateMachineTest {

    private enum Auto {
        DRIVE_TO_SCORE,
        SCORE,
        RETURN,
        PARK
    }

    private FakeClock clock;

    private StateMachine<Auto> machine;

    private List<String> events;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        machine = new StateMachine<>(Auto.DRIVE_TO_SCORE, clock);
        events = new ArrayList<>();
    }

    private void log(String event) {
        events.add(event);
    }

    @Nested
    @DisplayName("transitions")
    class Transitions {

        @Test
        @DisplayName("a true guard moves to the next state with exit then entry")
        void guardMoves() {
            final AtomicBoolean arrived = new AtomicBoolean();
            machine.onExit(Auto.DRIVE_TO_SCORE, () -> log("exit-drive"));
            machine.onEnter(Auto.SCORE, () -> log("enter-score"));
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, arrived::get);

            machine.update();
            assertEquals(Auto.DRIVE_TO_SCORE, machine.getState());
            arrived.set(true);
            machine.update();

            assertEquals(Auto.SCORE, machine.getState());
            assertEquals(List.of("exit-drive", "enter-score"), events);
        }

        @Test
        @DisplayName("the new state's update runs on the next pass, never the same one")
        void updateRunsNextPass() {
            machine.onUpdate(Auto.SCORE, () -> log("update-score"));
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);

            machine.update();
            assertEquals(Auto.SCORE, machine.getState());
            assertTrue(events.isEmpty());

            machine.update();
            assertEquals(List.of("update-score"), events);
        }

        @Test
        @DisplayName("guards evaluate in registration order, first true wins")
        void firstTrueWins() {
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.PARK, () -> true);

            machine.update();

            assertEquals(Auto.SCORE, machine.getState());
        }

        @Test
        @DisplayName("an explicit request beats guards and runs before them")
        void requestBeatsGuards() {
            final List<String> order = new ArrayList<>();
            machine.onUpdate(Auto.DRIVE_TO_SCORE, () -> order.add("update"));
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);

            machine.requestTransition(Auto.PARK);
            machine.update();

            assertEquals(Auto.PARK, machine.getState());
            // The request transitions immediately: no update action, no guard evaluation.
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("a full autonomous traversal visits every state in order")
        void fullTraversal() {
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);
            machine.addTransition(Auto.SCORE, Auto.RETURN, () -> true);
            machine.addTransition(Auto.RETURN, Auto.PARK, () -> true);

            machine.update();
            machine.update();
            machine.update();

            assertEquals(Auto.PARK, machine.getState());
        }
    }

    @Nested
    @DisplayName("timeouts")
    class Timeouts {

        @Test
        @DisplayName("a watchdog fires when the guard never becomes true")
        void watchdogFires() {
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> false);
            machine.setTimeout(Auto.DRIVE_TO_SCORE, 3.0, Auto.PARK);

            machine.update();
            clock.advanceSeconds(2.0);
            machine.update();
            assertEquals(Auto.DRIVE_TO_SCORE, machine.getState());
            clock.advanceSeconds(1.0);
            machine.update();
            assertEquals(Auto.PARK, machine.getState());
        }

        @Test
        @DisplayName("a satisfied guard beats an unexpired timeout")
        void guardBeatsUnexpiredTimeout() {
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);
            machine.setTimeout(Auto.DRIVE_TO_SCORE, 30.0, Auto.PARK);

            machine.update();

            assertEquals(Auto.SCORE, machine.getState());
        }

        @Test
        @DisplayName("timeInState measures the current state's tenure")
        void timeInState() {
            assertEquals(0.0, machine.timeInState(), 1e-9);
            machine.update();
            clock.advanceSeconds(1.5);
            assertEquals(1.5, machine.timeInState(), 1e-9);
        }

        @Test
        @DisplayName("rejects negative timeouts")
        void rejectsNegativeTimeout() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> machine.setTimeout(Auto.SCORE, -1.0, Auto.PARK));
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("reset returns without firing actions")
        void resetSkipsActions() {
            machine.onExit(Auto.SCORE, () -> log("exit-score"));
            machine.onEnter(Auto.DRIVE_TO_SCORE, () -> log("enter-drive"));
            machine.addTransition(Auto.DRIVE_TO_SCORE, Auto.SCORE, () -> true);
            machine.update();
            assertEquals(Auto.SCORE, machine.getState());

            events.clear();
            machine.reset(Auto.DRIVE_TO_SCORE);

            assertEquals(Auto.DRIVE_TO_SCORE, machine.getState());
            assertTrue(events.isEmpty());
            assertEquals(0.0, machine.timeInState(), 1e-9);
        }

        @Test
        @DisplayName("duplicate hooks are rejected loudly")
        void rejectsDuplicateHooks() {
            machine.onEnter(Auto.SCORE, () -> log("first"));

            assertThrows(
                    IllegalStateException.class,
                    () -> machine.onEnter(Auto.SCORE, () -> log("second")));
            assertThrows(
                    IllegalArgumentException.class, () -> machine.onUpdate(null, () -> log("x")));
            assertThrows(IllegalArgumentException.class, () -> machine.requestTransition(null));
            assertThrows(IllegalArgumentException.class, () -> new StateMachine<>(null));
        }
    }
}
