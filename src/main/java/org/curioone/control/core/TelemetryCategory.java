package org.curioone.control.core;

/**
 * A grouping for telemetry values.
 *
 * <p>Lives in {@code core} rather than {@code telemetry} because {@link TelemetryManager} needs it,
 * and {@code TelemetryManager} is a core service owned by {@link CurioRobot}. Putting the enum one
 * package out would make every core class that touches telemetry an outward dependency, which the
 * composition-root rule in ADR-014 exists to prevent. The {@code telemetry} package is reserved for
 * the structured CSV {@code Logger}, which depends on core and never the reverse.
 *
 * <p>Categories keep driver output readable as the robot grows: a line is prefixed with its
 * category, so a drive problem is distinguishable from a vision problem at a glance rather than
 * being one undifferentiated wall of text.
 *
 * <p>{@link #DEBUG} is suppressed unless {@code CurioConfig.DEBUG} is set, so diagnostic values can
 * be left in the code and cost nothing in a competition run.
 *
 * @since 0.1.0
 */
public enum TelemetryCategory {

    /** Drivetrain: heading, wheel positions, pose. */
    DRIVE("DRIVE"),

    /** Arm, lift, and other articulated mechanisms. */
    ARM("ARM"),

    /** Intake and indexing mechanisms. */
    INTAKE("INTAKE"),

    /** Vision: detections, poses, confidence. */
    VISION("VISION"),

    /** Framework and robot health: loop timing, battery, versions. */
    SYSTEM("SYSTEM"),

    /**
     * Diagnostic values, shown only when {@code CurioConfig.DEBUG} is set.
     *
     * <p>Anything added under this category is invisible during a normal run, which makes it safe
     * to leave in the code permanently rather than adding and removing lines while debugging.
     */
    DEBUG("DEBUG");

    private final String label;

    TelemetryCategory(String label) {
        this.label = label;
    }

    /**
     * Returns the prefix shown in driver telemetry.
     *
     * @return the display label, never {@code null}
     */
    public String label() {
        return label;
    }

    /**
     * Reports whether this category is suppressed unless debug mode is on.
     *
     * @return {@code true} only for {@link #DEBUG}
     */
    public boolean isDebugOnly() {
        return this == DEBUG;
    }
}
