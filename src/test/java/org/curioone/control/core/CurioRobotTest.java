package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.hardware.Servo;
import org.curioone.control.drive.DriveBase;
import org.curioone.control.drive.MecanumDrive;
import org.curioone.control.drive.TankDrive;
import org.curioone.control.hardware.Motor;
import org.curioone.control.support.FakeClock;
import org.curioone.control.support.FakeDcMotor;
import org.curioone.control.support.FakeHardwareSource;
import org.curioone.control.support.RecordingTelemetrySink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CurioRobot}. */
@DisplayName("CurioRobot")
class CurioRobotTest {

    private static final double DELTA = 1e-9;

    private int originalTelemetryPeriod;

    private FakeHardwareSource hardware;

    private RecordingTelemetrySink sink;

    private FakeClock clock;

    private CurioRobot robot;

    @BeforeEach
    void setUp() {
        hardware = new FakeHardwareSource();
        sink = new RecordingTelemetrySink();
        clock = new FakeClock();
        robot = new CurioRobot(hardware, clock, sink);
        // Flush on every update() so assertions do not depend on wall-clock timing. Restored in
        // teardown: CurioConfig is global, and a leak here silently breaks CurioConfigTest.
        originalTelemetryPeriod = CurioConfig.TELEMETRY_PERIOD_MILLIS;
        CurioConfig.TELEMETRY_PERIOD_MILLIS = 0;
    }

    @AfterEach
    void tearDown() {
        CurioConfig.TELEMETRY_PERIOD_MILLIS = originalTelemetryPeriod;
    }

    /** Registers the four motors {@link CurioRobot#drive()} expects. */
    private void registerDrivetrainMotors() {
        for (String wheel : new String[] {"frontLeft", "frontRight", "backLeft", "backRight"}) {
            hardware.addMotor(wheel);
        }
    }

    /**
     * A subsystem that records the lifecycle calls it receives.
     *
     * <p>The counters are private, which the enclosing test class can still read — a nested class's
     * private members are visible to its outer class, so a test double needs no accessors.
     */
    private static final class RecordingSubsystem extends Subsystem {
        private int inits;
        private int loops;
        private int stops;
        private int publishes;

        @Override
        public void init() {
            inits++;
        }

        @Override
        public void loop() {
            loops++;
        }

        @Override
        public void stop() {
            stops++;
        }

        @Override
        protected void publishTelemetry(TelemetryManager telemetry) {
            publishes++;
            telemetry.add(TelemetryCategory.SYSTEM, "recording", loops);
        }
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects a null hardware source")
        void rejectsNullSource() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurioRobot((HardwareSource) null, clock, sink));
        }

        @Test
        @DisplayName("rejects a null clock")
        void rejectsNullClock() {
            assertThrows(
                    IllegalArgumentException.class, () -> new CurioRobot(hardware, null, sink));
        }

        @Test
        @DisplayName("rejects a null SDK hardware map")
        void rejectsNullHardwareMap() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurioRobot((com.qualcomm.robotcore.hardware.HardwareMap) null));
        }

        @Test
        @DisplayName("resolves nothing until asked")
        void lazyHardwareResolution() {
            new CurioRobot(hardware, clock, sink);

            assertEquals(0, hardware.lookups(), "construction must not touch hardware");
        }

        @Test
        @DisplayName("tolerates a null telemetry sink by discarding output")
        void nullSinkDiscards() {
            final CurioRobot discarding = new CurioRobot(hardware, clock, null);

            assertNotNull(discarding.telemetry());
            discarding.telemetry().add("a", 1.0).update();
        }
    }

    @Nested
    @DisplayName("subsystem lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("init runs each subsystem's init once")
        void initRunsOnce() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a);

            robot.init();
            robot.init();

            assertEquals(1, a.inits, "init must be idempotent");
            assertTrue(robot.isInitialized());
        }

        @Test
        @DisplayName("reports not initialized before init runs")
        void notInitializedYet() {
            assertFalse(robot.isInitialized());
        }

        @Test
        @DisplayName("loop runs each subsystem's loop and telemetry hook")
        void loopRunsBoth() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a);
            robot.init();

            robot.loop();

            assertEquals(1, a.loops);
            assertEquals(1, a.publishes);
        }

        @Test
        @DisplayName("loop flushes telemetry")
        void loopFlushesTelemetry() {
            robot.registerSubsystem(new RecordingSubsystem());
            robot.init();

            robot.loop();

            assertTrue(sink.flushes() > 0, "robot.loop() must flush telemetry");
            assertEquals("[SYSTEM] recording", sink.captions().get(0));
        }

        @Test
        @DisplayName("stop runs each subsystem's stop")
        void stopRunsSubsystems() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a);
            robot.init();

            robot.stop();

            assertEquals(1, a.stops);
        }

        @Test
        @DisplayName("stop is safe to call twice")
        void stopIsIdempotent() {
            robot.registerSubsystem(new RecordingSubsystem());

            robot.stop();
            robot.stop();
        }

        @Test
        @DisplayName("subsystems run in registration order")
        void registrationOrder() {
            final StringBuilder order = new StringBuilder();
            robot.registerSubsystem(
                    new Subsystem() {
                        @Override
                        public void loop() {
                            order.append("a");
                        }
                    },
                    "a");
            robot.registerSubsystem(
                    new Subsystem() {
                        @Override
                        public void loop() {
                            order.append("b");
                        }
                    },
                    "b");

            robot.loop();

            assertEquals("ab", order.toString());
        }
    }

    @Nested
    @DisplayName("subsystem registry")
    class SubsystemRegistry {

        @Test
        @DisplayName("rejects null subsystems")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> robot.registerSubsystem(null));
        }

        @Test
        @DisplayName("rejects a duplicate name")
        void rejectsDuplicate() {
            robot.registerSubsystem(new RecordingSubsystem(), "arm");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> robot.registerSubsystem(new RecordingSubsystem(), "arm"));
        }

        @Test
        @DisplayName("rejects registration after init, which would skip the subsystem's init")
        void rejectsAfterInit() {
            robot.init();

            final RecordingSubsystem late = new RecordingSubsystem();
            final IllegalStateException thrown =
                    assertThrows(
                            IllegalStateException.class,
                            () -> robot.registerSubsystem(late, "late"));

            assertTrue(
                    thrown.getMessage().contains("init()"),
                    "the message should say what went wrong and what to do: "
                            + thrown.getMessage());
        }

        @Test
        @DisplayName("does not register a subsystem that was rejected after init")
        void rejectedAfterInitIsNotRegistered() {
            robot.init();

            assertThrows(
                    IllegalStateException.class,
                    () -> robot.registerSubsystem(new RecordingSubsystem(), "late"));

            assertNull(robot.subsystem("late"));
            assertFalse(robot.subsystemNames().contains("late"));
        }

        @Test
        @DisplayName("rejects null in a lookup")
        void rejectsNullLookup() {
            assertThrows(IllegalArgumentException.class, () -> robot.subsystem((String) null));
            assertThrows(
                    IllegalArgumentException.class, () -> robot.subsystem((Class<Subsystem>) null));
        }

        @Test
        @DisplayName("retrieves by name")
        void retrievesByName() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a, "arm");

            assertSame(a, robot.subsystem("arm"));
        }

        @Test
        @DisplayName("retrieves by type when unambiguous")
        void retrievesByType() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a);

            assertSame(a, robot.subsystem(RecordingSubsystem.class));
        }

        @Test
        @DisplayName("returns null by type when two match, rather than guessing")
        void ambiguousByType() {
            robot.registerSubsystem(new RecordingSubsystem(), "left");
            robot.registerSubsystem(new RecordingSubsystem(), "right");

            assertNull(
                    robot.subsystem(RecordingSubsystem.class),
                    "two intakes of the same type must not be silently resolved to one of them");
        }

        @Test
        @DisplayName("returns null for an unknown name")
        void unknownName() {
            assertNull(robot.subsystem("nope"));
        }

        @Test
        @DisplayName("names a subsystem by its class name by default")
        void defaultName() {
            final RecordingSubsystem a = new RecordingSubsystem();
            robot.registerSubsystem(a);

            assertEquals("RecordingSubsystem", a.name());
        }

        @Test
        @DisplayName("lists names in registration order")
        void listsNamesInOrder() {
            robot.registerSubsystem(new RecordingSubsystem(), "zulu");
            robot.registerSubsystem(new RecordingSubsystem(), "alpha");

            assertEquals("[zulu, alpha]", robot.subsystemNames().toString());
        }

        @Test
        @DisplayName("does not expose a mutable view of the registry")
        void namesAreUnmodifiable() {
            assertThrows(
                    UnsupportedOperationException.class, () -> robot.subsystemNames().add("x"));
        }
    }

    @Nested
    @DisplayName("hardware accessors")
    class HardwareAccessors {

        @Test
        @DisplayName("wraps a motor by name")
        void wrapsMotor() {
            final FakeDcMotor motor = hardware.addMotor("arm");

            final Motor wrapped = robot.motor("arm");

            assertEquals("arm", wrapped.name());
            assertSame(motor, wrapped.getSdkObject());
        }

        @Test
        @DisplayName("fails loudly on a missing device")
        void missingDeviceFails() {
            assertThrows(CurioException.class, () -> robot.motor("arm"));
        }

        @Test
        @DisplayName("wraps a servo by name")
        void wrapsServo() {
            hardware.add("claw", org.mockito.Mockito.mock(Servo.class));

            assertNotNull(robot.servo("claw"));
        }

        @Test
        @DisplayName("caches the IMU across calls")
        void cachesImu() {
            hardware.add(
                    "imu", org.mockito.Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class));

            assertSame(robot.imu(), robot.imu());
        }

        @Test
        @DisplayName("caches the IMU only per name, not globally")
        void imuCacheIsPerName() {
            hardware.add(
                    "imu", org.mockito.Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class));
            hardware.add(
                    "imu2", org.mockito.Mockito.mock(com.qualcomm.robotcore.hardware.IMU.class));

            assertNotSame(robot.imu("imu"), robot.imu("imu2"));
            assertSame(robot.imu("imu2"), robot.imu("imu2"));
        }

        @Test
        @DisplayName("exposes the registry, the clock, and telemetry")
        void exposesServices() {
            assertNotNull(robot.hardware());
            assertSame(clock, robot.clock());
            assertNotNull(robot.telemetry());
        }
    }

    @Nested
    @DisplayName("drivetrain")
    class Drivetrain {

        @Test
        @DisplayName("builds a mecanum base from the conventional names")
        void buildsMecanum() {
            registerDrivetrainMotors();

            final DriveBase drive = robot.drive();

            assertNotNull(drive);
            assertTrue(drive instanceof MecanumDrive);
        }

        @Test
        @DisplayName("returns the same drivetrain across calls")
        void drivetrainIsCached() {
            registerDrivetrainMotors();

            assertSame(robot.drive(), robot.drive());
        }

        @Test
        @DisplayName("accepts an installed drivetrain")
        void acceptsInstalledDrive() {
            final TankDrive tank =
                    new TankDrive(
                            "tank",
                            new Motor(hardware.addMotor("l"), "l"),
                            new Motor(hardware.addMotor("r"), "r"));
            robot.setDrive(tank);

            assertSame(tank, robot.drive());
        }

        @Test
        @DisplayName("rejects a null drivetrain")
        void rejectsNullDrive() {
            assertThrows(IllegalArgumentException.class, () -> robot.setDrive(null));
        }

        @Test
        @DisplayName("stop stops the drivetrain too")
        void stopStopsDrive() {
            final FakeDcMotor left = hardware.addMotor("l");
            final FakeDcMotor right = hardware.addMotor("r");
            robot.setDrive(new TankDrive("tank", new Motor(left, "l"), new Motor(right, "r")));

            robot.drive();
            ((TankDrive) robot.drive()).tank(1.0, 1.0);
            assertEquals(1.0, left.lastPower(), DELTA, "precondition: the drivetrain is moving");

            robot.stop();

            assertEquals(0.0, left.lastPower(), DELTA, "robot.stop() must stop the drivetrain");
            assertEquals(0.0, right.lastPower(), DELTA);
        }

        @Test
        @DisplayName("a missing wheel fails loudly rather than driving three motors")
        void missingWheelFailsLoudly() {
            hardware.addMotor("frontLeft");

            assertThrows(CurioException.class, () -> robot.drive());
        }
    }

    @Test
    @DisplayName("two robots do not share state")
    void robotsAreIndependent() {
        final CurioRobot other = new CurioRobot(hardware, clock, new RecordingTelemetrySink());
        robot.registerSubsystem(new RecordingSubsystem(), "arm");

        assertTrue(other.subsystemNames().isEmpty());
        assertNull(other.subsystem("arm"));
        assertFalse(other.isInitialized());
    }
}
