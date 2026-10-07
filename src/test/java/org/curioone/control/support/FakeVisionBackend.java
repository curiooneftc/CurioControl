package org.curioone.control.support;

import org.curioone.control.vision.VisionBackend;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.mockito.Mockito;

/**
 * A {@link VisionBackend} that records lifecycle calls for assertions.
 *
 * <p>The SDK's {@code VisionPortal} cannot be constructed or mocked on a desktop JVM — its static
 * initializer reaches into the Android application context — so tests never touch it. This stands
 * in for the whole backend: stream calls are recorded, stats are stubbed, and the tag processor is
 * a plain mock (which, unlike the portal, mocks cleanly).
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.4.0
 */
public final class FakeVisionBackend implements VisionBackend {

    private final AprilTagProcessor processor;

    private boolean resumed;

    private boolean paused;

    private boolean closed;

    private boolean tagsEnabled;

    private VisionPortal.CameraState cameraState = VisionPortal.CameraState.STREAMING;

    private float fps;

    /** Creates a backend with a fresh mock processor. */
    public FakeVisionBackend() {
        this(Mockito.mock(AprilTagProcessor.class));
    }

    /**
     * Creates a backend around a processor.
     *
     * @param processor the tag processor
     * @throws IllegalArgumentException if {@code processor} is {@code null}
     */
    public FakeVisionBackend(AprilTagProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("processor must not be null");
        }
        this.processor = processor;
    }

    @Override
    public void resume() {
        resumed = true;
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void setTagsEnabled(boolean enabled) {
        tagsEnabled = enabled;
    }

    @Override
    public VisionPortal.CameraState cameraState() {
        return cameraState;
    }

    @Override
    public float fps() {
        return fps;
    }

    @Override
    public AprilTagProcessor processor() {
        return processor;
    }

    @Override
    public VisionPortal portal() {
        return null;
    }

    /**
     * Reports whether {@link #resume()} ran.
     *
     * @return {@code true} after resume
     */
    public boolean resumed() {
        return resumed;
    }

    /**
     * Reports whether {@link #pause()} ran.
     *
     * @return {@code true} after pause
     */
    public boolean paused() {
        return paused;
    }

    /**
     * Reports whether {@link #close()} ran.
     *
     * @return {@code true} after close
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Reports the last enabled state.
     *
     * @return the last value passed to {@link #setTagsEnabled}
     */
    public boolean tagsEnabled() {
        return tagsEnabled;
    }

    /**
     * Sets the reported camera state.
     *
     * @param cameraState the state
     */
    public void setCameraState(VisionPortal.CameraState cameraState) {
        this.cameraState = cameraState;
    }

    /**
     * Sets the reported frame rate.
     *
     * @param fps the frame rate
     */
    public void setFps(float fps) {
        this.fps = fps;
    }
}
