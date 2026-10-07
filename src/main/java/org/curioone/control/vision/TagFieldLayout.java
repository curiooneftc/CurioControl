package org.curioone.control.vision;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.curioone.control.math.Pose2d;
import org.curioone.control.math.Units;

/**
 * Where the field's AprilTags are.
 *
 * <p>An immutable map from tag id to the tag's field pose: its position, plus the direction its
 * face points (into the field, toward the robots that read it). The pose solver reports where a tag
 * is relative to the camera; this layout is what turns that relative fix into a field pose for the
 * robot. It comes from the caller — season field, custom practice field, tape on a classroom wall —
 * because the framework ships no season data and never will.
 *
 * <p>Units are inches throughout: tag positions, like the SDK's {@code ftcPose}, in the same inches
 * the field is measured in. Build from millimetres with {@link #ofMillimeters} if the layout was
 * surveyed metric; mixing units silently scales the answer by 25.4.
 *
 * <p>Immutable and thread-safe.
 *
 * @since 0.4.0
 */
public final class TagFieldLayout {

    private final Map<Integer, Pose2d> tags;

    private TagFieldLayout(Map<Integer, Pose2d> tags) {
        this.tags = Collections.unmodifiableMap(tags);
    }

    /**
     * Creates a layout from tag poses in inches.
     *
     * @param tags tag id to field pose, in inches
     * @return the layout
     * @throws IllegalArgumentException if {@code tags} is {@code null} or empty, holds a {@code
     *     null} key or pose, or holds a negative id
     */
    public static TagFieldLayout of(Map<Integer, Pose2d> tags) {
        return new TagFieldLayout(copyChecked(tags));
    }

    /**
     * Creates a layout from tag poses in millimetres, converting to inches.
     *
     * @param tags tag id to field pose, with positions in millimetres
     * @return the layout, in inches
     * @throws IllegalArgumentException if {@code tags} is {@code null} or empty, holds a {@code
     *     null} key or pose, or holds a negative id
     */
    public static TagFieldLayout ofMillimeters(Map<Integer, Pose2d> tags) {
        final Map<Integer, Pose2d> checked = copyChecked(tags);
        final Map<Integer, Pose2d> inches = new HashMap<>(checked.size());
        for (Map.Entry<Integer, Pose2d> entry : checked.entrySet()) {
            final Pose2d metric = entry.getValue();
            inches.put(
                    entry.getKey(),
                    new Pose2d(
                            Units.millimetersToInches(metric.getX()),
                            Units.millimetersToInches(metric.getY()),
                            metric.getRotation()));
        }
        return new TagFieldLayout(Collections.unmodifiableMap(inches));
    }

    /**
     * Looks up a tag's field pose.
     *
     * @param id the tag id
     * @return the pose, or empty when the layout does not know that tag
     */
    public Optional<Pose2d> tagPose(int id) {
        return Optional.ofNullable(tags.get(id));
    }

    /**
     * Reports whether the layout knows a tag.
     *
     * @param id the tag id
     * @return {@code true} if the tag has a pose
     */
    public boolean hasTag(int id) {
        return tags.containsKey(id);
    }

    /**
     * Returns how many tags the layout knows.
     *
     * @return the tag count
     */
    public int size() {
        return tags.size();
    }

    private static Map<Integer, Pose2d> copyChecked(Map<Integer, Pose2d> tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags must not be null");
        }
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("tags must contain at least one tag");
        }
        final Map<Integer, Pose2d> copy = new HashMap<>(tags.size());
        for (Map.Entry<Integer, Pose2d> entry : tags.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0) {
                throw new IllegalArgumentException("tag ids must not be null or negative");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "tag " + entry.getKey() + " must not map to null");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
