package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeClock;
import org.curioone.control.support.FakeHardwareSource;
import org.curioone.control.support.FakeVisionBackend;
import org.curioone.control.vision.VisionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@code vision} accessors on {@link CurioRobot}. */
@DisplayName("CurioRobot vision accessors")
class CurioRobotVisionTest {

    private FakeClock clock;

    private FakeHardwareSource hardware;

    private CurioRobot robot;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        hardware = new FakeHardwareSource();
        robot = new CurioRobot(hardware, clock, null);
    }

    private static VisionManager managerNamed(String name) {
        return new VisionManager(new FakeVisionBackend(), name, new FakeClock());
    }

    @Nested
    @DisplayName("attachVision")
    class Attach {

        @Test
        @DisplayName("an attached manager is returned as-is and cached")
        void attachedIsCached() {
            final VisionManager attached = managerNamed("cam");
            robot.attachVision(attached);

            assertSame(attached, robot.vision("cam"));
            assertSame(attached, robot.vision("cam"));
        }

        @Test
        @DisplayName("rejects a null manager")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> robot.attachVision(null));
        }
    }

    @Nested
    @DisplayName("resolution")
    class Resolution {

        @Test
        @DisplayName("a missing camera fails loudly instead of building half a pipeline")
        void missingCameraFails() {
            assertThrows(CurioException.class, () -> robot.vision("Webcam 1"));
        }

        @Test
        @DisplayName("a cached name resolves without touching hardware")
        void cacheHitTouchesNoHardware() {
            robot.attachVision(managerNamed("front"));

            assertSame(robot.vision("front"), robot.vision("front"));
            assertEquals(0, hardware.lookups());
        }

        @Test
        @DisplayName("another name re-resolves through hardware, like the IMU accessor")
        void renameReResolves() {
            robot.attachVision(managerNamed("front"));

            // "back" is not the cached name, so the robot goes back to configuration —
            // where nothing is configured, hence loudly rather than half-built.
            assertThrows(CurioException.class, () -> robot.vision("back"));
            assertTrue(hardware.lookups() > 0);
        }
    }
}
