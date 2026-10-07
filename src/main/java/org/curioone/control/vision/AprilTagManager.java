package org.curioone.control.vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Transform2d;
import org.curioone.control.math.Units;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection;

/**
 * A validated view onto one FTC AprilTag processor.
 *
 * <p>Reads detections, filters by id, and solves the robot's field pose from a tag fix plus a field
 * layout. The processor runs on the SDK's vision thread and mutates its detection list there; every
 * query here snapshots first, so an OpMode-thread iteration never races the background solve.
 *
 * <h2>Pose convention</h2>
 *
 * <p>The SDK reports each tag in the camera frame as {@code ftcPose}: X right, Y forward, in
 * inches, with yaw counter-clockwise about up (per the FTC AprilTag docs). That frame matches the
 * framework's field frame axis-for-axis, so the translation transfers directly; the yaw negates
 * into our headings, which run clockwise from forward. A tag parallel to the camera face reports
 * yaw zero while facing the camera — a half turn from the camera's own facing — so the
 * camera-to-tag rotation is {@code π − yaw}, not {@code yaw}. Getting any of these three details
 * wrong mirrors or spins the solved pose, which is why they are stated here rather than left in the
 * code.
 *
 * <p><strong>Thread safety:</strong> safe to call from the OpMode thread. The processor's own
 * thread does the detecting; this class only reads snapshots.
 *
 * @since 0.4.0
 */
public final class AprilTagManager {

    private final AprilTagProcessor processor;

    private final String name;

    /**
     * Wraps an SDK AprilTag processor.
     *
     * @param processor the processor to wrap
     * @param name the name for diagnostics, usually the camera's configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AprilTagManager(AprilTagProcessor processor, String name) {
        if (processor == null) {
            throw new IllegalArgumentException("processor must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.processor = processor;
        this.name = name;
    }

    /**
     * Returns the underlying SDK processor.
     *
     * @return the SDK processor, never {@code null}
     */
    public AprilTagProcessor getSdkObject() {
        return processor;
    }

    /**
     * Returns the name this manager was created with.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns a snapshot of the current detections.
     *
     * <p>Copied under no lock the caller can see: the SDK mutates its list on the vision thread, so
     * iterating it directly risks a concurrent modification mid-loop. The copy is per call — poll
     * at the vision update rate, not faster.
     *
     * @return the detections, never {@code null}
     */
    public List<AprilTagDetection> getDetections() {
        return Collections.unmodifiableList(new ArrayList<>(processor.getDetections()));
    }

    /**
     * Finds the detection for one tag id.
     *
     * <p>Only single-tag detections carry an id; cluster detections report a virtual point with no
     * single tag behind it and never match. Unknown tags — visible but absent from the processor's
     * library — report no pose, but they still match by id: presence and pose are separate
     * questions, answered by {@code getTag} and {@code getRobotPose} respectively.
     *
     * @param id the tag id
     * @return the detection, or empty when no single-tag detection carries that id
     */
    public Optional<AprilTagDetection> getTag(int id) {
        for (AprilTagDetection detection : getDetections()) {
            if (detection instanceof AprilTagSingleDetection single && single.id == id) {
                return Optional.of(detection);
            }
        }
        return Optional.empty();
    }

    /**
     * Solves the robot's field pose from the current detections.
     *
     * <p>For each detected tag that has a solved pose <em>and</em> a layout entry, the candidate
     * robot pose is the tag's field pose pulled back through the robot-to-tag transform (camera
     * offset composed with the measured camera-to-tag fix, inverted). The closest candidate by
     * reported range wins: nearer tags subtend more pixels, and pixel count is accuracy.
     *
     * <p>Detections without a pose solve (unknown tags) and tags missing from the layout are
     * skipped, not errors — a frame that sees only unmapped tags still yields empty rather than a
     * guess.
     *
     * @param layout where the field's tags are, in inches
     * @param cameraOffset the camera's pose on the robot as a robot-frame transform, in inches
     * @return the robot's field pose, or empty when no detection is usable
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Optional<Pose2d> getRobotPose(TagFieldLayout layout, Transform2d cameraOffset) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (cameraOffset == null) {
            throw new IllegalArgumentException("cameraOffset must not be null");
        }
        AprilTagPoseFtc bestPose = null;
        Pose2d bestTagField = null;
        double bestRange = Double.POSITIVE_INFINITY;
        for (AprilTagDetection detection : getDetections()) {
            if (!(detection instanceof AprilTagSingleDetection single)) {
                continue;
            }
            if (single.ftcPose == null) {
                continue;
            }
            final Optional<Pose2d> tagField = layout.tagPose(single.id);
            if (tagField.isEmpty()) {
                continue;
            }
            if (single.ftcPose.range < bestRange) {
                bestPose = single.ftcPose;
                bestTagField = tagField.get();
                bestRange = single.ftcPose.range;
            }
        }
        if (bestPose == null || bestTagField == null) {
            return Optional.empty();
        }
        final Transform2d robotToTag = cameraOffset.plus(cameraToTag(bestPose));
        return Optional.of(bestTagField.transformBy(robotToTag.inverse()));
    }

    /**
     * Maps one solved pose into the camera-frame transform it reports.
     *
     * <p>The translation is the SDK's inches, axis for axis. The rotation is {@code π − yaw}: zero
     * yaw means tag-parallel-to-camera, i.e. facing the camera, i.e. a half turn from the camera's
     * own facing — and the FTC yaw runs counter-clockwise while framework headings run clockwise
     * from forward, hence the negation.
     *
     * @param pose the solved pose, in inches and degrees as the SDK reports
     * @return the camera-to-tag transform, in inches and radians
     */
    private static Transform2d cameraToTag(AprilTagPoseFtc pose) {
        return new Transform2d(
                pose.x, pose.y, Rotation2d.fromRadians(Math.PI - Units.degreesToRadians(pose.yaw)));
    }

    @Override
    public String toString() {
        return "AprilTagManager[" + name + "]";
    }
}
