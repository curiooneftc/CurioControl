package org.curioone.control.vision;

import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * The production {@link VisionBackend}: thin delegation to a real portal and processor.
 *
 * <p>Untestable on a desktop JVM by construction — every method forwards to SDK hardware — so there
 * is deliberately nothing here to unit test. What is testable (lifecycle ordering, gating, wiring)
 * lives in {@link VisionManager} against the interface.
 */
final class SdkVisionBackend implements VisionBackend {

    private final VisionPortal portal;

    private final AprilTagProcessor processor;

    SdkVisionBackend(VisionPortal portal, AprilTagProcessor processor) {
        if (portal == null) {
            throw new IllegalArgumentException("portal must not be null");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor must not be null");
        }
        this.portal = portal;
        this.processor = processor;
    }

    @Override
    public void resume() {
        portal.resumeStreaming();
    }

    @Override
    public void pause() {
        portal.stopStreaming();
    }

    @Override
    public void close() {
        portal.close();
    }

    @Override
    public void setTagsEnabled(boolean enabled) {
        portal.setProcessorEnabled(processor, enabled);
    }

    @Override
    public VisionPortal.CameraState cameraState() {
        return portal.getCameraState();
    }

    @Override
    public float fps() {
        return portal.getFps();
    }

    @Override
    public AprilTagProcessor processor() {
        return processor;
    }

    @Override
    public VisionPortal portal() {
        return portal;
    }
}
