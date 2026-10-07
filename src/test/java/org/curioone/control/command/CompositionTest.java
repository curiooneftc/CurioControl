package org.curioone.control.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.curioone.control.core.Subsystem;
import org.curioone.control.support.RecordingCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SequentialCommand} and {@link ParallelCommand}. */
@DisplayName("Command compositions")
class CompositionTest {

    private static Subsystem subsystem() {
        return new Subsystem() {};
    }

    @Nested
    @DisplayName("SequentialCommand")
    class Sequential {

        @Test
        @DisplayName("runs children in order with a one-cycle boundary between them")
        void runsInOrder() {
            final AtomicBoolean firstDone = new AtomicBoolean();
            final RecordingCommand first = new RecordingCommand("first", firstDone::get);
            final RecordingCommand second = new RecordingCommand("second");
            final SequentialCommand sequence = new SequentialCommand(first, second);

            sequence.initialize();
            sequence.execute();
            assertEquals(List.of("init-first", "exec-first"), first.events());
            assertTrue(second.events().isEmpty());

            // First finishes: it ends, the second initializes, but does not execute yet.
            firstDone.set(true);
            sequence.execute();
            assertEquals(
                    List.of("init-first", "exec-first", "exec-first", "end-first:false"),
                    first.events());
            assertEquals(List.of("init-second"), second.events());
            assertFalse(sequence.isFinished());

            // Next loop the second runs; it never finishes here, so the sequence holds.
            sequence.execute();
            assertEquals(List.of("init-second", "exec-second"), second.events());
            assertFalse(sequence.isFinished());
        }

        @Test
        @DisplayName("finishes when the last child finishes")
        void finishesAtEnd() {
            final RecordingCommand first = new RecordingCommand("first", () -> true);
            final RecordingCommand second = new RecordingCommand("second", () -> true);
            final SequentialCommand sequence = new SequentialCommand(first, second);

            sequence.initialize();
            sequence.execute();
            assertFalse(sequence.isFinished());
            sequence.execute();
            sequence.execute();
            assertTrue(sequence.isFinished());
            assertEquals(
                    List.of("init-second", "exec-second", "end-second:false"), second.events());
        }

        @Test
        @DisplayName("cancelling ends the running child as interrupted")
        void cancelEndsCurrent() {
            final RecordingCommand first = new RecordingCommand("first", () -> true);
            final RecordingCommand second = new RecordingCommand("second");
            final SequentialCommand sequence = new SequentialCommand(first, second);

            sequence.initialize();
            sequence.execute();
            sequence.end(true);

            assertEquals(List.of("init-first", "exec-first", "end-first:false"), first.events());
            assertEquals(List.of("init-second", "end-second:true"), second.events());
        }

        @Test
        @DisplayName("nested sequences behave like one command")
        void nested() {
            final RecordingCommand leaf = new RecordingCommand("leaf", () -> true);
            final SequentialCommand inner = new SequentialCommand(leaf);
            final SequentialCommand outer = new SequentialCommand(inner);

            outer.initialize();
            outer.execute();
            outer.execute();
            assertTrue(outer.isFinished());
            assertEquals(List.of("init-leaf", "exec-leaf", "end-leaf:false"), leaf.events());
        }

        @Test
        @DisplayName("requirements are the union of the children's")
        void unionRequirements() {
            final Subsystem arm = subsystem();
            final Subsystem intake = subsystem();
            final SequentialCommand sequence =
                    new SequentialCommand(
                            new RecordingCommand("first", arm),
                            new RecordingCommand("second", intake));

            assertTrue(sequence.requirements().contains(arm));
            assertTrue(sequence.requirements().contains(intake));
            assertEquals(2, sequence.requirements().size());
        }

        @Test
        @DisplayName("rejects empty and null children")
        void rejectsBadChildren() {
            assertThrows(IllegalArgumentException.class, () -> new SequentialCommand());
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new SequentialCommand(new RecordingCommand("ok"), null));
            assertThrows(
                    IllegalArgumentException.class, () -> new SequentialCommand((Command[]) null));
        }
    }

    @Nested
    @DisplayName("ParallelCommand")
    class Parallel {

        @Test
        @DisplayName("a finished child ends while the rest keep running")
        void earlyFinisherDropsOut() {
            final RecordingCommand quick = new RecordingCommand("quick", () -> true);
            final RecordingCommand slow = new RecordingCommand("slow");
            final ParallelCommand group = new ParallelCommand(quick, slow);

            group.initialize();
            assertEquals(List.of("init-quick"), quick.events());
            assertEquals(List.of("init-slow"), slow.events());

            group.execute();
            assertEquals(List.of("init-quick", "exec-quick", "end-quick:false"), quick.events());
            assertFalse(group.isFinished());

            // The finished child is not executed again.
            group.execute();
            assertEquals(List.of("init-quick", "exec-quick", "end-quick:false"), quick.events());
            assertEquals(2, slow.executions());
            assertFalse(group.isFinished());
        }

        @Test
        @DisplayName("finishes when the last child finishes")
        void finishesAtEnd() {
            final AtomicBoolean slowDone = new AtomicBoolean();
            final ParallelCommand group =
                    new ParallelCommand(
                            new RecordingCommand("quick", () -> true),
                            new RecordingCommand("slow", slowDone::get));

            group.initialize();
            group.execute();
            assertFalse(group.isFinished());
            slowDone.set(true);
            group.execute();
            assertTrue(group.isFinished());
        }

        @Test
        @DisplayName("cancelling ends only the unfinished children")
        void cancelEndsUnfinished() {
            final RecordingCommand quick = new RecordingCommand("quick", () -> true);
            final RecordingCommand slow = new RecordingCommand("slow");
            final ParallelCommand group = new ParallelCommand(quick, slow);

            group.initialize();
            group.execute();
            group.end(true);

            assertTrue(quick.events().stream().noneMatch(event -> event.endsWith(":true")));
            assertEquals(List.of("init-slow", "exec-slow", "end-slow:true"), slow.events());
        }

        @Test
        @DisplayName("requirements are the union of the children's")
        void unionRequirements() {
            final Subsystem arm = subsystem();
            final ParallelCommand group = new ParallelCommand(new RecordingCommand("one", arm));

            assertTrue(group.requirements().contains(arm));
        }

        @Test
        @DisplayName("rejects empty and null children")
        void rejectsBadChildren() {
            assertThrows(IllegalArgumentException.class, () -> new ParallelCommand());
            assertThrows(
                    IllegalArgumentException.class, () -> new ParallelCommand((Command[]) null));
        }
    }
}
