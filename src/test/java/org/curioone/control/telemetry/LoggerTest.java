package org.curioone.control.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import org.curioone.control.core.CurioConfig;
import org.curioone.control.support.FakeClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link Logger}. */
@DisplayName("Logger")
class LoggerTest {

    private boolean loggingWasEnabled;

    private FakeClock clock;

    private StringWriter out;

    @BeforeEach
    void setUp() {
        loggingWasEnabled = CurioConfig.LOGGING_ENABLED;
        CurioConfig.LOGGING_ENABLED = true;
        clock = new FakeClock();
        out = new StringWriter();
    }

    @AfterEach
    void tearDown() {
        CurioConfig.LOGGING_ENABLED = loggingWasEnabled;
    }

    private Logger logger() {
        return new Logger(out, clock);
    }

    @Nested
    @DisplayName("disabled mode")
    class Disabled {

        @Test
        @DisplayName("writes nothing and allocates no rows when off")
        void writesNothingWhenOff() {
            CurioConfig.LOGGING_ENABLED = false;
            final Logger log = logger();

            log.registerField("heading");
            log.record("heading", 0.5);
            log.update();

            assertEquals("", out.toString());
        }
    }

    @Nested
    @DisplayName("csv output")
    class Csv {

        @Test
        @DisplayName("matches the spec example format")
        void specFormat() {
            final Logger log = logger();
            log.registerField("heading");
            log.registerField("armPosition");

            log.record("heading", 0.2);
            log.record("armPosition", 0.0);
            log.update();
            clock.advanceMillis(20);
            log.record("heading", 0.4);
            log.record("armPosition", 15.0);
            log.update();
            log.close();

            final String[] lines = out.toString().split("\n");
            assertEquals("time,heading,armPosition", lines[0]);
            assertEquals("0.0,0.2,0.0", lines[1]);
            assertEquals("0.02,0.4,15.0", lines[2]);
            assertEquals(3, lines.length);
        }

        @Test
        @DisplayName("unrecorded columns write as gaps, not shifted columns")
        void gapsForMissing() {
            final Logger log = logger();
            log.registerField("a");
            log.registerField("b");

            log.record("b", 1.0);
            log.update();
            log.close();

            final String[] lines = out.toString().split("\n");
            assertEquals("time,a,b", lines[0]);
            assertEquals("0.0,,1.0", lines[1]);
        }

        @Test
        @DisplayName("text with commas is quoted")
        void quotesText() {
            final Logger log = logger();
            log.registerField("state");

            log.record("state", "DRIVE, SCORE");
            log.update();
            log.close();

            final String[] lines = out.toString().split("\n");
            assertEquals("time,state", lines[0]);
            assertEquals("0.0,\"DRIVE, SCORE\"", lines[1]);
        }
    }

    @Nested
    @DisplayName("registration")
    class Registration {

        @Test
        @DisplayName("rejects duplicates, unknowns, and late schema changes")
        void rejectsBadSchema() {
            final Logger log = logger();
            log.registerField("heading");

            assertThrows(IllegalArgumentException.class, () -> log.registerField("heading"));
            assertThrows(IllegalArgumentException.class, () -> log.record("nope", 1.0));

            log.update();
            assertThrows(IllegalStateException.class, () -> log.registerField("late"));
        }

        @Test
        @DisplayName("rejects null writers, fields, and values")
        void rejectsNulls() {
            assertThrows(IllegalArgumentException.class, () -> new Logger(null, clock));
            assertThrows(IllegalArgumentException.class, () -> new Logger(out, null));

            final Logger log = logger();
            assertThrows(IllegalArgumentException.class, () -> log.registerField(null));
            log.registerField("state");
            assertThrows(IllegalArgumentException.class, () -> log.record("state", (String) null));
        }
    }

    @Nested
    @DisplayName("ring buffer")
    class RingBuffer {

        @Test
        @DisplayName("a full buffer drops the oldest row and counts it")
        void dropsOldest() {
            final Logger log = logger();
            log.setBufferCapacity(2);
            log.setFlushEvery(Integer.MAX_VALUE);
            log.registerField("v");

            for (int row = 0; row < 4; row++) {
                log.record("v", row);
                log.update();
            }

            assertEquals(2, log.getDroppedRowCount());
            log.flush();
            log.close();

            final String[] lines = out.toString().split("\n");
            // Header plus the two surviving rows (v = 2 and v = 3).
            assertEquals(3, lines.length);
            assertTrue(lines[1].endsWith(",2.0"), "got: " + lines[1]);
            assertTrue(lines[2].endsWith(",3.0"), "got: " + lines[2]);
        }

        @Test
        @DisplayName("rejects non-positive buffer and flush settings")
        void rejectsBadSettings() {
            final Logger log = logger();

            assertThrows(IllegalArgumentException.class, () -> log.setBufferCapacity(0));
            assertThrows(IllegalArgumentException.class, () -> log.setFlushEvery(0));
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("closed loggers fail loudly and close idempotently")
        void closedFails() {
            final Logger log = logger();
            log.close();
            log.close();

            assertThrows(IllegalStateException.class, () -> log.record("x", 1.0));
            assertThrows(IllegalStateException.class, log::update);
            assertThrows(IllegalStateException.class, log::flush);
        }

        @Test
        @DisplayName("toFile creates the directory and a timestamped file")
        void toFileCreatesFile(@TempDir Path temp) {
            final File directory = temp.resolve("logs").toFile();

            try (Logger log = Logger.toFile(directory, "auto", clock)) {
                log.registerField("heading");
                log.record("heading", 1.0);
                log.update();
            }

            final File[] files = directory.listFiles();
            assertTrue(files != null && files.length == 1, "expected one log file");
            assertTrue(files[0].getName().startsWith("auto-"), "got: " + files[0].getName());
            assertTrue(files[0].getName().endsWith(".csv"), "got: " + files[0].getName());
        }

        @Test
        @DisplayName("toFile rejects bad arguments")
        void toFileRejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> Logger.toFile(null, "auto", clock));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Logger.toFile(new File("logs"), "", clock));
        }
    }
}
