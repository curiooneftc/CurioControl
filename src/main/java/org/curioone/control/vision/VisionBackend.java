package org.curioone.control.vision;

import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * The SDK edge behind {@link VisionManager}: a stream that can be resumed, paused, and closed, with
 * a tag processor on it.
 *
 * <p>Exists because the SDK's {@code VisionPortal} cannot be constructed or mocked on a desktop JVM
 * — its static initializer reaches into the Android application context. Everything the manager
 * needs therefore goes through this interface: production uses the thin {@code SdkVisionBackend}
 * adapter, tests use a fake, and the manager itself never touches the portal class except through
 * the seam. Same idea as the framework's {@code HardwareSource} port, one layer up.
 *
 * <p>Implementations must be callable from the OpMode thread; the SDK does its work on the vision
 * thread underneath.
 *
 * @since 0.4.0
 */
public interface VisionBackend {

    /** Resumes streaming. */
    void resume();

    /** Pauses streaming, keeping the portal alive for a later resume. */
    void pause();

    /** Releases the camera. */
    void close();

    /**
     * Enables or disables the tag processor on the stream.
     *
     * @param enabled whether detections should run
     */
    void setTagsEnabled(boolean enabled);

    /**
     * Returns the camera state.
     *
     * @return the SDK camera state
     */
    VisionPortal.CameraState cameraState();

    /**
     * Returns the measured frame rate.
     *
     * @return frames per second, as reported by the SDK
     */
    float fps();

    /**
     * Returns the tag processor running on this backend.
     *
     * @return the SDK processor, never {@code null}
     */
    AprilTagProcessor processor();

    /**
     * Returns the underlying SDK portal.
     *
     * <p>The escape hatch: camera controls and anything the framework does not cover stay reachable
     * without forking a wrapper.
     *
     * @return the SDK portal, never {@code null} in production
     */
    VisionPortal portal();
}
