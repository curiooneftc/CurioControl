package org.curioone.control.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Transform2d;
import org.curioone.control.math.Vector2d;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagClusterDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link AprilTagManager}. */
@DisplayName("AprilTagManager")
class AprilTagManagerTest {

    private static final double DELTA = 1e-6;

    private static final Transform2d CENTERED_CAMERA =
            new Transform2d(Vector2d.ZERO, Rotation2d.ZERO);

    private AprilTagProcessor processor;

    private AprilTagManager tags;

    @BeforeEach
    void setUp() {
        processor = Mockito.mock(AprilTagProcessor.class);
        Mockito.when(processor.getDetections()).thenReturn(new ArrayList<>());
        tags = new AprilTagManager(processor, "camera");
    }

    /** Builds a solved pose the way the SDK reports it: inches, yaw in degrees. */
    private static AprilTagPoseFtc solved(double x, double y, double yawDegrees, double range) {
        return new AprilTagPoseFtc(x, y, 0.0, yawDegrees, 0.0, 0.0, range, 0.0, 0.0);
    }

    /** Builds a single-tag detection, leaving the untouched fields null. */
    private static AprilTagSingleDetection detection(int id, AprilTagPoseFtc pose) {
        return new AprilTagSingleDetection(
                id, 0, 0.0f, null, null, null, pose, null, null, 0L, DistanceUnit.INCH);
    }

    private void givenDetections(AprilTagDetection... detections) {
        Mockito.when(processor.getDetections()).thenReturn(new ArrayList<>(List.of(detections)));
    }

    private static void assertPose(Pose2d actual, double x, double y, double headingRadians) {
        assertEquals(x, actual.getX(), DELTA);
        assertEquals(y, actual.getY(), DELTA);
        assertEquals(headingRadians, actual.getRotation().getRadians(), DELTA);
    }

    @Nested
    @DisplayName("tag lookup")
    class Lookup {

        @Test
        @DisplayName("finds a tag by id among several detections")
        void findsById() {
            givenDetections(
                    detection(20, solved(0.0, 24.0, 0.0, 24.0)),
                    detection(21, solved(0.0, 48.0, 0.0, 48.0)));

            final Optional<AprilTagDetection> found = tags.getTag(21);

            assertTrue(found.isPresent());
            assertEquals(21, ((AprilTagSingleDetection) found.get()).id);
            assertTrue(tags.getTag(22).isEmpty());
        }

        @Test
        @DisplayName("a cluster detection never matches an id")
        void clusterNeverMatches() {
            givenDetections(Mockito.mock(AprilTagClusterDetection.class));

            assertTrue(tags.getTag(20).isEmpty());
        }

        @Test
        @DisplayName("no detections means no tag")
        void emptyMeansEmpty() {
            assertTrue(tags.getTag(20).isEmpty());
        }
    }

    @Nested
    @DisplayName("robot pose")
    class RobotPose {

        @Test
        @DisplayName("square-on tag ahead solves the robot at the origin facing forward")
        void squareOnAhead() {
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(0.0, 24.0, Math.PI)));
            givenDetections(detection(20, solved(0.0, 24.0, 0.0, 24.0)));

            final Optional<Pose2d> pose = tags.getRobotPose(layout, CENTERED_CAMERA);

            assertTrue(pose.isPresent());
            assertPose(pose.get(), 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("a yawed tag still solves the same robot pose")
        void yawedTag() {
            // Same geometry, but the tag itself is turned 90 degrees: it faces field-right
            // while the robot still faces forward at the origin.
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(0.0, 24.0, Math.PI / 2.0)));
            givenDetections(detection(20, solved(0.0, 24.0, 90.0, 24.0)));

            final Optional<Pose2d> pose = tags.getRobotPose(layout, CENTERED_CAMERA);

            assertTrue(pose.isPresent());
            assertPose(pose.get(), 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("a rotated robot solves its rotated pose")
        void rotatedRobot() {
            // Robot at the origin facing field-right; tag straight ahead of the camera,
            // facing the robot.
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(24.0, 0.0, -Math.PI / 2.0)));
            givenDetections(detection(20, solved(0.0, 24.0, 0.0, 24.0)));

            final Optional<Pose2d> pose = tags.getRobotPose(layout, CENTERED_CAMERA);

            assertTrue(pose.isPresent());
            assertPose(pose.get(), 0.0, 0.0, Math.PI / 2.0);
        }

        @Test
        @DisplayName("a forward-mounted camera offsets the solution")
        void cameraOffset() {
            final Transform2d offset = new Transform2d(new Vector2d(0.0, 6.0), Rotation2d.ZERO);
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(0.0, 30.0, Math.PI)));
            givenDetections(detection(20, solved(0.0, 24.0, 0.0, 24.0)));

            final Optional<Pose2d> pose = tags.getRobotPose(layout, offset);

            assertTrue(pose.isPresent());
            assertPose(pose.get(), 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("the closest usable tag wins")
        void closestWins() {
            final TagFieldLayout layout =
                    TagFieldLayout.of(
                            Map.of(
                                    20, Pose2d.fromHeading(0.0, 100.0, Math.PI),
                                    21, Pose2d.fromHeading(0.0, 50.0, Math.PI)));
            // Tag 20 claims the robot is at the origin; tag 21 claims ten inches forward.
            // Tag 21 is nearer, so its claim wins.
            givenDetections(
                    detection(20, solved(0.0, 100.0, 0.0, 100.0)),
                    detection(21, solved(0.0, 40.0, 0.0, 40.0)));

            final Optional<Pose2d> pose = tags.getRobotPose(layout, CENTERED_CAMERA);

            assertTrue(pose.isPresent());
            assertPose(pose.get(), 0.0, 10.0, 0.0);
        }

        @Test
        @DisplayName("unsolvable frames yield empty, not guesses")
        void unusableYieldsEmpty() {
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(0.0, 24.0, Math.PI)));
            // Unknown tag, unsolved tag, NaN range, and a cluster: none usable.
            givenDetections(
                    detection(21, solved(0.0, 24.0, 0.0, 24.0)),
                    detection(20, null),
                    detection(20, solved(0.0, 24.0, 0.0, Double.NaN)),
                    Mockito.mock(AprilTagClusterDetection.class));

            assertTrue(tags.getRobotPose(layout, CENTERED_CAMERA).isEmpty());
        }

        @Test
        @DisplayName("rejects null layouts and offsets")
        void rejectsNulls() {
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, Pose2d.fromHeading(0.0, 24.0, Math.PI)));

            assertThrows(
                    IllegalArgumentException.class, () -> tags.getRobotPose(null, CENTERED_CAMERA));
            assertThrows(IllegalArgumentException.class, () -> tags.getRobotPose(layout, null));
        }
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("keeps its name, exposes the SDK object, and rejects nulls")
        void basics() {
            assertEquals("camera", tags.name());
            assertSame(processor, tags.getSdkObject());
            assertEquals("AprilTagManager[camera]", tags.toString());
            assertThrows(IllegalArgumentException.class, () -> new AprilTagManager(null, "camera"));
            assertThrows(
                    IllegalArgumentException.class, () -> new AprilTagManager(processor, null));
        }

        @Test
        @DisplayName("detections come back as an unmodifiable snapshot")
        void snapshot() {
            givenDetections(detection(20, solved(0.0, 24.0, 0.0, 24.0)));

            final List<AprilTagDetection> first = tags.getDetections();
            assertEquals(1, first.size());
            assertThrows(UnsupportedOperationException.class, first::clear);
        }
    }
}
