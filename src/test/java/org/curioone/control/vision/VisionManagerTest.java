package org.curioone.control.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.support.FakeClock;
import org.curioone.control.support.FakeVisionBackend;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link VisionManager}. */
@DisplayName("VisionManager")
class VisionManagerTest {

    private FakeVisionBackend backend;

    private AprilTagManager tags;

    private FakeClock clock;

    private VisionManager manager;

    @BeforeEach
    void setUp() {
        backend = new FakeVisionBackend();
        tags = new AprilTagManager(backend.processor(), "camera");
        clock = new FakeClock();
        manager = new VisionManager(backend, "camera", clock);
    }

    @Nested
    @DisplayName("attachment")
    class Attachment {

        @Test
        @DisplayName("starts empty and reports absence loudly")
        void startsEmpty() {
            assertFalse(manager.hasAprilTags());
            assertThrows(VisionException.class, manager::aprilTags);
        }

        @Test
        @DisplayName("attach makes the pipeline available")
        void attach() {
            manager.attach(tags);

            assertTrue(manager.hasAprilTags());
            assertSame(tags, manager.aprilTags());
            assertThrows(IllegalArgumentException.class, () -> manager.attach(null));
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("init resumes the stream and enables the processor")
        void init() {
            manager.attach(tags);

            manager.init();

            assertTrue(backend.resumed());
            assertTrue(backend.tagsEnabled());
        }

        @Test
        @DisplayName("init without attachment only resumes")
        void initWithoutAttachment() {
            manager.init();

            assertTrue(backend.resumed());
            assertFalse(backend.tagsEnabled());
        }

        @Test
        @DisplayName("stop disables the processor and pauses the stream")
        void stop() {
            manager.attach(tags);

            manager.stop();

            assertFalse(backend.tagsEnabled());
            assertTrue(backend.paused());
        }

        @Test
        @DisplayName("close releases the backend")
        void close() {
            manager.close();

            assertTrue(backend.closed());
        }

        @Test
        @DisplayName("state and frame rate delegate to the backend")
        void delegates() {
            backend.setCameraState(VisionPortal.CameraState.STREAMING);
            backend.setFps(30.0f);

            assertEquals(VisionPortal.CameraState.STREAMING, manager.cameraState());
            assertEquals(30.0f, manager.getFps());
        }

        @Test
        @DisplayName("isStreaming reflects the camera state")
        void isStreaming() {
            assertTrue(manager.isStreaming());

            backend.setCameraState(VisionPortal.CameraState.OPENING_CAMERA_DEVICE);
            assertFalse(manager.isStreaming());
        }
    }

    @Nested
    @DisplayName("update rate")
    class UpdateRate {

        @Test
        @DisplayName("the first process always polls, then the gate holds")
        void gateHolds() {
            manager.attach(tags);

            assertTrue(manager.process());
            assertFalse(manager.process());
            clock.advanceMillis(34);
            assertTrue(manager.process());
        }

        @Test
        @DisplayName("a slower ceiling holds longer")
        void slowerCeiling() {
            manager.setMaxUpdateRate(10.0);

            assertTrue(manager.process());
            clock.advanceMillis(99);
            assertFalse(manager.process());
            clock.advanceMillis(1);
            assertTrue(manager.process());
        }

        @Test
        @DisplayName("rejects non-positive rates")
        void rejectsBadRates() {
            assertThrows(IllegalArgumentException.class, () -> manager.setMaxUpdateRate(0.0));
            assertThrows(IllegalArgumentException.class, () -> manager.setMaxUpdateRate(-1.0));
            assertThrows(
                    IllegalArgumentException.class, () -> manager.setMaxUpdateRate(Double.NaN));
        }
    }

    @Nested
    @DisplayName("forCamera")
    class ForCamera {

        @Test
        @DisplayName("builds backend, processor, and attachment through the factory")
        void buildsThroughFactory() {
            final WebcamName camera = Mockito.mock(WebcamName.class);
            Mockito.when(camera.getDeviceName()).thenReturn("Webcam 1");
            final FakeVisionBackend built = new FakeVisionBackend();
            final VisionPortalFactory factory = Mockito.mock(VisionPortalFactory.class);
            Mockito.when(factory.open(camera)).thenReturn(built);

            final VisionManager assembled =
                    VisionManager.forCamera(camera, new FakeClock(), factory);

            Mockito.verify(factory).open(camera);
            assertEquals("Webcam 1", assembled.name());
            assertTrue(assembled.hasAprilTags());
            assertSame(built.processor(), assembled.aprilTags().getSdkObject());
        }

        @Test
        @DisplayName("a null backend from the factory fails loudly")
        void nullBackendFails() {
            final WebcamName camera = Mockito.mock(WebcamName.class);
            final VisionPortalFactory factory = Mockito.mock(VisionPortalFactory.class);
            Mockito.when(factory.open(camera)).thenReturn(null);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> VisionManager.forCamera(camera, clock, factory));
        }

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            final VisionPortalFactory factory = Mockito.mock(VisionPortalFactory.class);
            final WebcamName camera = Mockito.mock(WebcamName.class);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> VisionManager.forCamera(null, clock, factory));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> VisionManager.forCamera(camera, null, factory));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> VisionManager.forCamera(camera, clock, null));
            assertThrows(IllegalArgumentException.class, () -> new VisionManager(null, "camera"));
            assertThrows(IllegalArgumentException.class, () -> new VisionManager(backend, null));
        }
    }
}
