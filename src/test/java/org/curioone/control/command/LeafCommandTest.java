package org.curioone.control.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.curioone.control.core.Subsystem;
import org.curioone.control.support.FakeClock;
import org.curioone.control.support.RecordingCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the leaf commands and the {@link Commands} factories. */
@DisplayName("Leaf commands and factories")
class LeafCommandTest {

    private static Subsystem subsystem() {
        return new Subsystem() {};
    }

    @Nested
    @DisplayName("InstantCommand")
    class Instant {

        @Test
        @DisplayName("runs the action once and finishes immediately")
        void runsOnce() {
            final AtomicInteger runs = new AtomicInteger();
            final InstantCommand command = new InstantCommand(runs::incrementAndGet);

            command.initialize();
            command.execute();

            assertEquals(1, runs.get());
            assertTrue(command.isFinished());
        }

        @Test
        @DisplayName("carries its requirements and name")
        void carriesRequirements() {
            final Subsystem arm = subsystem();
            final InstantCommand command = new InstantCommand("Stop", () -> {}, arm);

            assertEquals("Stop", command.name());
            assertTrue(command.requirements().contains(arm));
        }

        @Test
        @DisplayName("rejects nulls and blank names")
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> new InstantCommand(null));
            assertThrows(IllegalArgumentException.class, () -> new InstantCommand("", () -> {}));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new InstantCommand(() -> {}, (Subsystem) null));
        }
    }

    @Nested
    @DisplayName("WaitCommand")
    class Wait {

        @Test
        @DisplayName("finishes once the duration has passed")
        void finishesAfterDuration() {
            final FakeClock clock = new FakeClock();
            final WaitCommand command = new WaitCommand(1.0, clock);

            command.initialize();
            clock.advanceMillis(999);
            assertFalse(command.isFinished());
            clock.advanceMillis(1);
            assertTrue(command.isFinished());
        }

        @Test
        @DisplayName("is not finished before it starts")
        void notFinishedBeforeStart() {
            final WaitCommand command = new WaitCommand(0.0, new FakeClock());

            assertFalse(command.isFinished());
        }

        @Test
        @DisplayName("rejects negative durations and nulls")
        void rejectsBadArguments() {
            final FakeClock clock = new FakeClock();

            assertThrows(IllegalArgumentException.class, () -> new WaitCommand(-1.0, clock));
            assertThrows(IllegalArgumentException.class, () -> new WaitCommand(Double.NaN, clock));
            assertThrows(IllegalArgumentException.class, () -> new WaitCommand(1.0, null));
        }
    }

    @Nested
    @DisplayName("Commands factories")
    class Factories {

        @Test
        @DisplayName("waitUntil polls the condition")
        void waitUntilPolls() {
            final AtomicBoolean ready = new AtomicBoolean();
            final Command command = Commands.waitUntil(ready::get);

            command.initialize();
            assertFalse(command.isFinished());
            ready.set(true);
            assertTrue(command.isFinished());
        }

        @Test
        @DisplayName("sequence and parallel build the compositions")
        void compositions() {
            final RecordingCommand first = new RecordingCommand("first");
            final RecordingCommand second = new RecordingCommand("second");

            final SequentialCommand sequence = Commands.sequence(first, second);
            sequence.initialize();
            assertEquals(List.of("init-first"), first.events());

            final ParallelCommand parallel = Commands.parallel("Both", first, second);
            assertEquals("Both", parallel.name());
        }

        @Test
        @DisplayName("list overloads build the same compositions")
        void listOverloads() {
            final RecordingCommand first = new RecordingCommand("first");
            final RecordingCommand second = new RecordingCommand("second");

            final SequentialCommand sequence = Commands.sequence(List.of(first, second));
            sequence.initialize();
            sequence.execute();
            assertEquals(List.of("init-first", "exec-first"), first.events());

            final ParallelCommand parallel = Commands.parallel("Both", List.of(first, second));
            assertEquals("Both", parallel.name());
            assertThrows(IllegalArgumentException.class, () -> Commands.sequence(List.of()));
            assertThrows(
                    IllegalArgumentException.class, () -> Commands.sequence((List<Command>) null));
        }

        @Test
        @DisplayName("factories reject null actions and conditions")
        void rejectsNulls() {
            assertThrows(IllegalArgumentException.class, () -> Commands.instant(null));
            assertThrows(IllegalArgumentException.class, () -> Commands.waitUntil(null));
            assertThrows(IllegalArgumentException.class, () -> Commands.waitSeconds(-1.0));
            assertThrows(IllegalArgumentException.class, () -> Commands.run(null));
        }
    }

    @Nested
    @DisplayName("Commands.run")
    class Run {

        @Test
        @DisplayName("executes every loop and never finishes on its own")
        void runsUntilCancelled() {
            final AtomicInteger runs = new AtomicInteger();
            final Command command = Commands.run("Hold", runs::incrementAndGet);

            command.initialize();
            command.execute();
            command.execute();

            assertEquals(2, runs.get());
            assertFalse(command.isFinished());
            assertEquals("Hold", command.name());
        }

        @Test
        @DisplayName("carries its requirements")
        void carriesRequirements() {
            final Subsystem arm = subsystem();
            final Command command = Commands.run(() -> {}, arm);

            assertTrue(command.requirements().contains(arm));
        }
    }
}
