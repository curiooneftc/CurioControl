package org.curioone.control.hardware;

/**
 * A source of field-relative heading, without the IMU attached.
 *
 * <p>Drive code needs a heading number every loop; it does not need to know the number came from an
 * IMU, a fused estimator, or a test lambda. Depending on this interface instead of {@link IMU}
 * keeps that seam explicit: {@code math} and {@code control} never see an IMU at all, and a test
 * drives heading changes with a one-line lambda instead of a mocked SDK object.
 *
 * <pre>{@code
 * drive.fieldCentric(x, y, rotation, () -> Math.PI / 2.0);
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> implementations called from the OpMode loop must be safe on
 * that thread; {@link IMU} is, like the rest of the framework, OpMode-thread-only.
 *
 * @since 0.2.0
 */
@FunctionalInterface
public interface HeadingSource {

    /**
     * Returns the current field-relative heading.
     *
     * @return heading in radians, relative to the last reset
     */
    double heading();
}
