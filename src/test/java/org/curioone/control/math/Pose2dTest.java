package org.curioone.control.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Pose2d} and {@link Transform2d}. */
@DisplayName("Pose2d and Transform2d")
class Pose2dTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("transform application")
    class Application {
        @Test
        @DisplayName("at zero heading the transform applies unchanged")
        void zeroHeadingIsIdentity() {
            final Pose2d start = Pose2d.fromHeading(1.0, 2.0, 0.0);
            final Pose2d end =
                    start.transformBy(new Transform2d(24.0, 12.0, Rotation2d.fromDegrees(30.0)));

            assertEquals(25.0, end.getX(), DELTA);
            assertEquals(14.0, end.getY(), DELTA);
            assertEquals(30.0, end.getRotation().getDegrees(), DELTA);
        }

        @Test
        @DisplayName("driving forward moves along the heading, even when rotated")
        void forwardFollowsHeading() {
            // Facing field-right: robot-forward is field +x, so 24 of robot-forward lands on
            // +x — the same answer field-centric drive gives the wheels.
            final Pose2d start = Pose2d.fromHeading(0.0, 0.0, Math.PI / 2.0);
            final Pose2d end = start.transformBy(new Transform2d(0.0, 24.0, Rotation2d.ZERO));

            assertEquals(24.0, end.getX(), DELTA);
            assertEquals(0.0, end.getY(), DELTA);
        }

        @Test
        @DisplayName("a robot-frame strafe rotates into the field frame")
        void strafeRotatesIntoFieldFrame() {
            // Facing field-right: robot-right points field-backward, so 24 of robot-right
            // lands on -y.
            final Pose2d start = Pose2d.fromHeading(0.0, 0.0, Math.PI / 2.0);
            final Pose2d end = start.transformBy(new Transform2d(24.0, 0.0, Rotation2d.ZERO));

            assertEquals(0.0, end.getX(), DELTA);
            assertEquals(-24.0, end.getY(), DELTA);
        }

        @Test
        @DisplayName("a pure rotation leaves the position alone")
        void pureRotation() {
            final Pose2d start = new Pose2d(5.0, 6.0, Rotation2d.ZERO);
            final Pose2d end =
                    start.transformBy(new Transform2d(Vector2d.ZERO, Rotation2d.fromDegrees(90.0)));

            assertEquals(5.0, end.getX(), DELTA);
            assertEquals(6.0, end.getY(), DELTA);
            assertEquals(90.0, end.getRotation().getDegrees(), DELTA);
        }

        @Test
        @DisplayName("transform then inverse returns to the start")
        void transformThenInverse() {
            final Pose2d start = new Pose2d(3.0, 4.0, Rotation2d.fromDegrees(30.0));
            final Transform2d motion = new Transform2d(10.0, -5.0, Rotation2d.fromDegrees(45.0));

            final Pose2d roundTripped = start.transformBy(motion).transformBy(motion.inverse());

            assertEquals(start.getX(), roundTripped.getX(), DELTA);
            assertEquals(start.getY(), roundTripped.getY(), DELTA);
            assertEquals(
                    start.getRotation().getRadians(),
                    roundTripped.getRotation().getRadians(),
                    DELTA);
        }
    }

    @Nested
    @DisplayName("relative poses")
    class Relative {

        @Test
        @DisplayName("minus maps the reference pose onto this one")
        void minusMapsReference() {
            final Pose2d reference = Pose2d.fromHeading(10.0, 0.0, 0.0);
            final Pose2d self = Pose2d.fromHeading(34.0, 0.0, 0.0);

            final Transform2d relative = self.minus(reference);

            assertEquals(24.0, relative.getX(), DELTA);
            assertEquals(0.0, relative.getY(), DELTA);
        }

        @Test
        @DisplayName("relativeTo is consistent with transformBy")
        void relativeToConsistent() {
            final Pose2d reference = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(45.0));
            final Pose2d self =
                    reference.transformBy(new Transform2d(5.0, 3.0, Rotation2d.fromDegrees(10.0)));

            final Pose2d recovered = reference.transformBy(self.minus(reference));

            assertEquals(self.getX(), recovered.getX(), DELTA);
            assertEquals(self.getY(), recovered.getY(), DELTA);
            assertEquals(
                    self.getRotation().getRadians(), recovered.getRotation().getRadians(), DELTA);
        }

        @Test
        @DisplayName("composing transforms applies them in order")
        void compositionOrder() {
            final Transform2d first = new Transform2d(10.0, 0.0, Rotation2d.ZERO);
            final Transform2d second = new Transform2d(0.0, 10.0, Rotation2d.fromDegrees(90.0));

            final Pose2d sequential = Pose2d.ORIGIN.transformBy(first).transformBy(second);
            final Pose2d composed = Pose2d.ORIGIN.transformBy(first.plus(second));

            assertEquals(sequential, composed);
        }
    }

    @Nested
    @DisplayName("interpolation and distance")
    class InterpolationAndDistance {

        @Test
        @DisplayName("hits both endpoints")
        void endpoints() {
            final Pose2d start = new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(0.0));
            final Pose2d end = new Pose2d(24.0, 0.0, Rotation2d.fromDegrees(90.0));

            assertEquals(start, start.interpolate(end, 0.0));
            assertEquals(end, start.interpolate(end, 1.0));
        }

        @Test
        @DisplayName("midpoint splits translation and heading")
        void midpoint() {
            final Pose2d middle =
                    new Pose2d(0.0, 0.0, Rotation2d.ZERO)
                            .interpolate(new Pose2d(24.0, 0.0, Rotation2d.fromDegrees(90.0)), 0.5);

            assertEquals(12.0, middle.getX(), DELTA);
            assertEquals(45.0, middle.getRotation().getDegrees(), DELTA);
        }

        @Test
        @DisplayName("distance ignores heading")
        void distanceIgnoresHeading() {
            assertEquals(
                    5.0,
                    new Pose2d(0.0, 0.0, Rotation2d.ZERO)
                            .distance(new Pose2d(3.0, 4.0, Rotation2d.fromDegrees(180.0))),
                    DELTA);
        }

        @Test
        @DisplayName("rejects nulls")
        void rejectsNulls() {
            // The concatenation consumes the result: discarding a freshly created value
            // trips the unused-return check, inside a lambda or a try block alike.
            try {
                fail("transformBy(null) must throw, got " + Pose2d.ORIGIN.transformBy(null));
            } catch (IllegalArgumentException expected) {
                assertEquals("transform must not be null", expected.getMessage());
            }
            try {
                Pose2d.ORIGIN.minus(null);
                fail("minus(null) must throw");
            } catch (IllegalArgumentException expected) {
                assertEquals("other must not be null", expected.getMessage());
            }
            try {
                Pose2d.ORIGIN.interpolate(null, 0.5);
                fail("interpolate(null, t) must throw");
            } catch (IllegalArgumentException expected) {
                assertEquals("end must not be null", expected.getMessage());
            }
            try {
                Pose2d.ORIGIN.interpolate(Pose2d.ORIGIN, Double.NaN);
                fail("interpolate(end, NaN) must throw");
            } catch (IllegalArgumentException expected) {
                assertEquals("t must not be NaN", expected.getMessage());
            }
        }
    }
}
