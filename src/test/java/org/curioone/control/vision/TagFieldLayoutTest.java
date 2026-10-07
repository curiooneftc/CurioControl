package org.curioone.control.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Rotation2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TagFieldLayout}. */
@DisplayName("TagFieldLayout")
class TagFieldLayoutTest {

    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @Test
        @DisplayName("returns poses by id and reports the unknown as empty")
        void lookup() {
            final TagFieldLayout layout =
                    TagFieldLayout.of(Map.of(20, new Pose2d(24.0, 12.0, Rotation2d.ZERO)));

            assertTrue(layout.tagPose(20).isPresent());
            assertEquals(24.0, layout.tagPose(20).orElseThrow().getX(), DELTA);
            assertTrue(layout.tagPose(21).isEmpty());
            assertTrue(layout.hasTag(20));
            assertFalse(layout.hasTag(21));
            assertEquals(1, layout.size());
        }

        @Test
        @DisplayName("millimetre layouts convert to inches")
        void millimetersConvert() {
            final TagFieldLayout layout =
                    TagFieldLayout.ofMillimeters(
                            Map.of(20, new Pose2d(25.4, 0.0, Rotation2d.fromDegrees(90.0))));

            assertEquals(1.0, layout.tagPose(20).orElseThrow().getX(), DELTA);
            assertEquals(90.0, layout.tagPose(20).orElseThrow().getRotation().getDegrees(), DELTA);
        }

        @Test
        @DisplayName("rejects null, empty, and negative layouts")
        void rejectsBadLayouts() {
            assertThrows(IllegalArgumentException.class, () -> TagFieldLayout.of(null));
            assertThrows(IllegalArgumentException.class, () -> TagFieldLayout.of(Map.of()));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> TagFieldLayout.of(Map.of(-1, Pose2d.ORIGIN)));

            final Map<Integer, Pose2d> nullPose = new HashMap<>();
            nullPose.put(20, null);
            assertThrows(IllegalArgumentException.class, () -> TagFieldLayout.of(nullPose));
        }
    }
}
