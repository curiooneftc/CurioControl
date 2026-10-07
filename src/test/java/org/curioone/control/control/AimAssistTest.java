package org.curioone.control.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.curioone.control.math.Vector2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AimAssist}. */
@DisplayName("AimAssist")
class AimAssistTest {

    private static final double DELTA = 1e-9;

    private static final Pose2d ORIGIN_FACING_FORWARD = new Pose2d(0.0, 0.0, Rotation2d.ZERO);

    @Test
    @DisplayName("points at cardinal targets with the expected headings")
    void cardinalTargets() {
        assertEquals(
                0.0,
                AimAssist.desiredHeading(ORIGIN_FACING_FORWARD, new Vector2d(0.0, 24.0))
                        .getRadians(),
                DELTA);
        assertEquals(
                Math.PI / 2.0,
                AimAssist.desiredHeading(ORIGIN_FACING_FORWARD, new Vector2d(24.0, 0.0))
                        .getRadians(),
                DELTA);
        assertEquals(
                Math.PI,
                Math.abs(
                        AimAssist.desiredHeading(ORIGIN_FACING_FORWARD, new Vector2d(0.0, -24.0))
                                .getRadians()),
                DELTA);
    }

    @Test
    @DisplayName("error is zero when already facing the target")
    void zeroWhenAimed() {
        final Pose2d aimed = new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(90.0));

        assertEquals(0.0, AimAssist.headingError(aimed, new Vector2d(24.0, 0.0)), DELTA);
    }

    @Test
    @DisplayName("error takes the short way across +-pi")
    void shortWayRound() {
        // Facing just past backwards; the target is just before backwards the other way.
        // The long way is nearly a full turn; the error must be the small remainder.
        final Pose2d robot = new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(179.0));
        final Vector2d behindLeft = new Vector2d(-1.0, -100.0);

        final double error = AimAssist.headingError(robot, behindLeft);
        assertTrue(Math.abs(error) < Math.PI / 2.0, "took the long way: " + error);
    }

    @Test
    @DisplayName("distance ignores heading")
    void distance() {
        assertEquals(
                5.0, AimAssist.distanceTo(ORIGIN_FACING_FORWARD, new Vector2d(3.0, 4.0)), DELTA);
    }

    @Test
    @DisplayName("rejects nulls")
    void rejectsNulls() {
        final Vector2d target = new Vector2d(1.0, 1.0);

        assertThrows(IllegalArgumentException.class, () -> AimAssist.desiredHeading(null, target));
        assertThrows(
                IllegalArgumentException.class,
                () -> AimAssist.headingError(ORIGIN_FACING_FORWARD, null));
        assertThrows(IllegalArgumentException.class, () -> AimAssist.distanceTo(null, null));
    }
}
