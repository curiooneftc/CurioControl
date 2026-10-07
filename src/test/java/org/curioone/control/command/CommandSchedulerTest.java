package org.curioone.control.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.curioone.control.core.CurioConfig;
import org.curioone.control.core.Subsystem;
import org.curioone.control.core.TelemetryManager;
import org.curioone.control.support.FakeClock;
import org.curioone.control.support.RecordingCommand;
import org.curioone.control.support.RecordingTelemetrySink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CommandScheduler}. */
@DisplayName("CommandScheduler")
class CommandSchedulerTest {

    private CommandScheduler scheduler;

    private static Subsystem subsystem() {
        return new Subsystem() {};
    }

    @BeforeEach
    void setUp() {
        scheduler = new CommandScheduler();
    }

    @Nested
    @DisplayName("scheduling and running")
    class Scheduling {

        @Test
        @DisplayName("a scheduled command initializes, executes, and ends on completion")
        void fullLifecycle() {
            final RecordingCommand command = new RecordingCommand("work", () -> true);

            scheduler.schedule(command);
            assertFalse(scheduler.isScheduled(command));
            scheduler.run();

            assertEquals(List.of("init-work", "exec-work", "end-work:false"), command.events());
            assertFalse(scheduler.isScheduled(command));
        }

        @Test
        @DisplayName("scheduling twice does not initialize twice")
        void noDoubleSchedule() {
            final RecordingCommand command = new RecordingCommand("work");

            scheduler.schedule(command, command);
            scheduler.run();

            assertEquals(List.of("init-work", "exec-work"), command.events());
        }

        @Test
        @DisplayName("a command scheduled from inside a hook runs on the next pass")
        void stagedFromHook() {
            final RecordingCommand late = new RecordingCommand("late", () -> true);
            final Command early = new InstantCommand("early", () -> scheduler.schedule(late));

            scheduler.schedule(early);
            scheduler.run();

            assertTrue(late.events().isEmpty());
            scheduler.run();
            assertEquals(List.of("init-late", "exec-late", "end-late:false"), late.events());
        }

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            assertThrows(
                    IllegalArgumentException.class, () -> scheduler.schedule((Command[]) null));
            assertThrows(IllegalArgumentException.class, () -> scheduler.schedule((Command) null));
            assertThrows(IllegalArgumentException.class, () -> scheduler.cancel((Command) null));
            assertThrows(IllegalArgumentException.class, () -> scheduler.isScheduled(null));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> scheduler.setDefaultCommand(null, new RecordingCommand("x")));
        }
    }

    @Nested
    @DisplayName("ownership and preemption")
    class Ownership {

        @Test
        @DisplayName("scheduling a conflicting command preempts the running one")
        void preemptsOnConflict() {
            final Subsystem arm = subsystem();
            final RecordingCommand first = new RecordingCommand("first", arm);
            final RecordingCommand second = new RecordingCommand("second", arm);

            scheduler.schedule(first);
            scheduler.run();
            scheduler.schedule(second);
            scheduler.run();

            assertEquals(List.of("init-first", "exec-first", "end-first:true"), first.events());
            assertEquals(List.of("init-second", "exec-second"), second.events());
            assertFalse(scheduler.isScheduled(first));
            assertTrue(scheduler.isScheduled(second));
        }

        @Test
        @DisplayName("commands on different subsystems run concurrently")
        void noConflictRunsTogether() {
            final RecordingCommand first = new RecordingCommand("first", subsystem());
            final RecordingCommand second = new RecordingCommand("second", subsystem());

            scheduler.schedule(first, second);
            scheduler.run();

            assertTrue(scheduler.isScheduled(first));
            assertTrue(scheduler.isScheduled(second));
            assertEquals(List.of("init-first", "exec-first"), first.events());
            assertEquals(List.of("init-second", "exec-second"), second.events());
        }

        @Test
        @DisplayName("a requirement-free command never preempts")
        void requirementFreeNeverPreempts() {
            final RecordingCommand owner = new RecordingCommand("owner", subsystem());
            final RecordingCommand free = new RecordingCommand("free");

            scheduler.schedule(owner);
            scheduler.run();
            scheduler.schedule(free);
            scheduler.run();

            assertTrue(scheduler.isScheduled(owner));
            assertTrue(scheduler.isScheduled(free));
            assertEquals(2, owner.executions());
        }
    }

    @Nested
    @DisplayName("cancellation")
    class Cancellation {

        @Test
        @DisplayName("cancelling before the next run means it never starts")
        void cancelBeforeStart() {
            final RecordingCommand command = new RecordingCommand("work");

            scheduler.schedule(command);
            scheduler.cancel(command);
            scheduler.run();

            assertTrue(command.events().isEmpty());
            assertFalse(scheduler.isScheduled(command));
        }

        @Test
        @DisplayName("cancelling a running command ends it as interrupted")
        void cancelRunning() {
            final RecordingCommand command = new RecordingCommand("work");

            scheduler.schedule(command);
            scheduler.run();
            scheduler.cancel(command);
            scheduler.run();

            assertEquals(List.of("init-work", "exec-work", "end-work:true"), command.events());
        }

        @Test
        @DisplayName("cancelAll ends everything as interrupted")
        void cancelAll() {
            final RecordingCommand first = new RecordingCommand("first");
            final RecordingCommand second = new RecordingCommand("second");

            scheduler.schedule(first, second);
            scheduler.run();
            scheduler.cancelAll();
            scheduler.run();

            assertEquals(List.of("init-first", "exec-first", "end-first:true"), first.events());
            assertEquals(List.of("init-second", "exec-second", "end-second:true"), second.events());
            assertTrue(scheduler.scheduledCommands().isEmpty());
        }

        @Test
        @DisplayName("cancelling an unscheduled command does nothing")
        void cancelUnscheduled() {
            final RecordingCommand command = new RecordingCommand("work");

            scheduler.cancel(command);
            scheduler.run();

            assertTrue(command.events().isEmpty());
        }
    }

    @Nested
    @DisplayName("default commands")
    class Defaults {

        @Test
        @DisplayName("a default starts when its subsystem goes free")
        void defaultStartsWhenFree() {
            final Subsystem drive = subsystem();
            final RecordingCommand hold = new RecordingCommand("hold", drive);
            scheduler.setDefaultCommand(drive, hold);

            // Initialized on this pass, first executes on the next one.
            scheduler.run();
            assertEquals(List.of("init-hold"), hold.events());
            scheduler.run();
            assertEquals(List.of("init-hold", "exec-hold"), hold.events());
            assertTrue(scheduler.isScheduled(hold));
        }

        @Test
        @DisplayName("a scheduled command preempts the default and it resumes after")
        void defaultPreemptedAndResumes() {
            final Subsystem drive = subsystem();
            final RecordingCommand hold = new RecordingCommand("hold", drive);
            final RecordingCommand auto = new RecordingCommand("auto", () -> true, drive);
            scheduler.setDefaultCommand(drive, hold);

            scheduler.run();
            scheduler.run();
            scheduler.schedule(auto);
            scheduler.run();

            // The auto preempts the default, finishes in the same pass, and the freed
            // subsystem gets its default back immediately — still within that pass.
            assertTrue(hold.events().contains("end-hold:true"));
            assertFalse(scheduler.isScheduled(auto));
            assertTrue(scheduler.isScheduled(hold));

            scheduler.run();
            assertEquals(
                    List.of("init-hold", "exec-hold", "end-hold:true", "init-hold", "exec-hold"),
                    hold.events());
        }

        @Test
        @DisplayName("removing a default cancels it if running")
        void removeDefaultCancels() {
            final Subsystem drive = subsystem();
            final RecordingCommand hold = new RecordingCommand("hold", drive);
            scheduler.setDefaultCommand(drive, hold);

            scheduler.run();
            scheduler.run();
            scheduler.removeDefaultCommand(drive);
            scheduler.run();

            assertTrue(hold.events().contains("end-hold:true"));
            assertFalse(scheduler.isScheduled(hold));
            scheduler.run();
            assertFalse(scheduler.isScheduled(hold));
        }
    }

    @Nested
    @DisplayName("telemetry")
    class Telemetry {

        private boolean debugWasOn;

        private RecordingTelemetrySink sink;

        private TelemetryManager telemetry;

        @BeforeEach
        void setUpTelemetry() {
            debugWasOn = CurioConfig.DEBUG;
            CurioConfig.DEBUG = true;
            sink = new RecordingTelemetrySink();
            telemetry = new TelemetryManager(sink, new FakeClock());
        }

        @AfterEach
        void tearDownTelemetry() {
            CurioConfig.DEBUG = debugWasOn;
        }

        @Test
        @DisplayName("reports the running count and names")
        void reportsRunning() {
            scheduler.schedule(new RecordingCommand("alpha"), new RecordingCommand("beta"));
            scheduler.run();
            scheduler.publishTelemetry(telemetry);
            telemetry.updateNow();

            assertTrue(sink.captions().contains("[DEBUG] Scheduler/running"));
            assertTrue(sink.captions().contains("[DEBUG] Scheduler/commands"));
            assertTrue(sink.values().contains("alpha (0.0s), beta (0.0s)"));
            assertTrue(sink.values().contains("0"));
            assertTrue(sink.values().contains("(none)"));
        }

        @Test
        @DisplayName("reports tenures and preemptions")
        void reportsTenureAndPreemptions() {
            final FakeClock clock = new FakeClock();
            final CommandScheduler timed = new CommandScheduler(clock);
            final Subsystem arm = subsystem();
            timed.schedule(new RecordingCommand("first", arm));
            timed.run();
            clock.advanceSeconds(2.5);
            timed.schedule(new RecordingCommand("second", arm));
            timed.run();

            assertEquals(1, timed.preemptions());
            timed.publishTelemetry(telemetry);
            telemetry.updateNow();

            assertTrue(sink.values().contains("second (0.0s)"));
            assertTrue(sink.values().contains("1"));
            assertTrue(sink.values().contains("first"));
        }

        @Test
        @DisplayName("reports an empty scheduler without failing")
        void reportsEmpty() {
            scheduler.publishTelemetry(telemetry);
            telemetry.updateNow();

            assertTrue(sink.values().contains("(none)"));
        }

        @Test
        @DisplayName("debug values are dropped unless debug mode is on")
        void debugGated() {
            CurioConfig.DEBUG = false;
            sink.clear();

            scheduler.schedule(new RecordingCommand("alpha"));
            scheduler.run();
            scheduler.publishTelemetry(telemetry);
            telemetry.updateNow();

            assertTrue(sink.captions().isEmpty());
        }

        @Test
        @DisplayName("rejects a null manager")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> scheduler.publishTelemetry(null));
        }
    }

    @Nested
    @DisplayName("snapshots")
    class Snapshots {

        @Test
        @DisplayName("scheduledCommands reflects the running set in order")
        void snapshot() {
            final RecordingCommand first = new RecordingCommand("first");
            final RecordingCommand second = new RecordingCommand("second");

            scheduler.schedule(first, second);
            scheduler.run();

            assertEquals(List.of(first, second), scheduler.scheduledCommands());
        }
    }
}
