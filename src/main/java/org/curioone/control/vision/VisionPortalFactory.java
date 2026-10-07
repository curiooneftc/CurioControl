package org.curioone.control.vision;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Opens vision hardware: a stream backend for a camera, tag processor included.
 *
 * <p>The SDK builds portals and processors through static factories that open real hardware, which
 * cannot run on a desktop JVM. This interface is the seam that keeps everything else testable:
 * production uses {@link #defaults()}, tests inject a fake backend, and the assembly in {@link
 * VisionManager#forCamera} is verified without a camera. Same idea as the framework's {@code
 * HardwareSource} port, one layer up.
 *
 * @since 0.4.0
 */
public interface VisionPortalFactory {

    /**
     * Creates the default factory, which opens real SDK hardware.
     *
     * <p>Builds the processor with {@code AprilTagProcessor.easyCreateWithDefaults()} and the
     * portal with {@code VisionPortal.easyCreateWithDefaults(camera, processor)} — the SDK's
     * conventional setup. Custom pipelines implement this interface instead of configuring the
     * defaults.
     *
     * @return the default factory
     */
    static VisionPortalFactory defaults() {
        return camera -> {
            final AprilTagProcessor processor = AprilTagProcessor.easyCreateWithDefaults();
            final VisionPortal portal = VisionPortal.easyCreateWithDefaults(camera, processor);
            return new SdkVisionBackend(portal, processor);
        };
    }

    /**
     * Opens a streaming backend for a camera, tag processor included.
     *
     * @param camera the camera to stream from
     * @return the backend
     */
    VisionBackend open(WebcamName camera);
}
