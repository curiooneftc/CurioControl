package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.curioone.control.core.CurioException;
import org.curioone.control.core.HardwareRegistry;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

/**
 * A validated view onto one FTC IMU.
 *
 * <p>All angles are <strong>radians</strong>, matching the SDK. Radians are used because they are
 * what the SDK returns and what the control and drive code consumes; converting at the boundary is
 * where degree-based confusion gets introduced. A degree-based accessor is provided for code that
 * genuinely works in degrees.
 *
 * <p><strong>Heading vs. yaw.</strong> {@link #heading()} and {@link #yaw()} return the same
 * number. Heading is the field-relative value — whatever direction was current at the last {@link
 * #resetHeading()} — and that is what field-centric drive wants. The SDK's own yaw is also reset by
 * {@code resetYaw()}, so the two coincide; {@code heading} exists so the intent is visible at the
 * call site.
 *
 * <h2>Reset between runs</h2>
 *
 * An unreset IMU makes field-centric drive drift by a fixed offset, which is easy to misread as a
 * motor problem. Reset the heading at the start of every OpMode, autonomous included.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class IMU {

    /** Radians in a full turn, for degree conversion. */
    private static final double RADIANS_PER_TURN = 2.0 * Math.PI;

    private static final double DEGREES_PER_RADIAN = 180.0 / Math.PI;

    private final com.qualcomm.robotcore.hardware.IMU imu;

    private final String name;

    /**
     * Wraps an SDK IMU.
     *
     * @param imu the IMU to wrap
     * @param name the configuration name, used in diagnostics
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public IMU(com.qualcomm.robotcore.hardware.IMU imu, String name) {
        if (imu == null) {
            throw new IllegalArgumentException("imu must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.imu = imu;
        this.name = name;
    }

    /**
     * Looks an IMU up in the Robot Controller configuration and wraps it.
     *
     * @param hardwareMap the configured hardware
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws CurioException if no IMU is configured under that name
     */
    public IMU(HardwareMap hardwareMap, String name) {
        this(
                new HardwareRegistry(hardwareMap)
                        .require(com.qualcomm.robotcore.hardware.IMU.class, name),
                name);
    }

    /**
     * Looks an IMU up in a registry and wraps it.
     *
     * @param registry where devices come from
     * @param name the configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws CurioException if no IMU is configured under that name
     */
    public IMU(HardwareRegistry registry, String name) {
        this(registry.require(com.qualcomm.robotcore.hardware.IMU.class, name), name);
    }

    /**
     * Returns the underlying SDK IMU.
     *
     * @return the SDK IMU, never {@code null}
     */
    public com.qualcomm.robotcore.hardware.IMU getSdkObject() {
        return imu;
    }

    /**
     * Returns the configuration name this IMU was resolved under.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Runs the IMU's calibration.
     *
     * <p>The robot must be still while this runs. The parameters are season-specific — they
     * describe how the IMU is mounted on the control hub — so they come from the caller rather than
     * the framework, which has no way to know the mounting (ADR-012).
     *
     * <p>Prefer {@link #requireCalibration(com.qualcomm.robotcore.hardware.IMU.Parameters)}, which
     * turns a calibration failure into a clear error rather than a silently wrong heading.
     *
     * @param parameters the mounting parameters
     * @return {@code true} if calibration succeeded
     * @throws IllegalArgumentException if {@code parameters} is {@code null}
     */
    public boolean calibrate(com.qualcomm.robotcore.hardware.IMU.Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }
        return imu.initialize(parameters);
    }

    /**
     * Runs calibration and fails loudly if it does not succeed.
     *
     * <p>A failed calibration yields an offset heading, and a field-centric drive built on a bad
     * offset is the hardest kind of bug to notice during a match. Failing here makes it a
     * bench-time error instead.
     *
     * @param parameters the mounting parameters
     * @throws CurioException if calibration did not succeed
     * @throws IllegalArgumentException if {@code parameters} is {@code null}
     */
    public void requireCalibration(com.qualcomm.robotcore.hardware.IMU.Parameters parameters) {
        if (!calibrate(parameters)) {
            throw new CurioException(
                    "[CurioControl] ERROR"
                            + System.lineSeparator()
                            + "IMU calibration failed for '"
                            + name
                            + "'."
                            + System.lineSeparator()
                            + "The robot must be still while calibrating, and the mounting "
                            + "parameters must match how the IMU is physically mounted.");
        }
    }

    /**
     * Makes the current heading zero.
     *
     * <p>Call at the start of every run.
     */
    public void resetHeading() {
        imu.resetYaw();
    }

    /**
     * Returns the current field-relative heading.
     *
     * @return heading in radians, relative to the last {@link #resetHeading()}
     */
    public double heading() {
        return yaw();
    }

    /**
     * Returns the current field-relative heading in degrees.
     *
     * @return heading in degrees, relative to the last {@link #resetHeading()}
     */
    public double headingDegrees() {
        return yaw() * DEGREES_PER_RADIAN;
    }

    /**
     * Returns the current yaw.
     *
     * @return yaw in radians
     */
    public double yaw() {
        return readAngles().getYaw();
    }

    /**
     * Returns the current pitch.
     *
     * @return pitch in radians
     */
    public double pitch() {
        return readAngles().getPitch();
    }

    /**
     * Returns the current roll.
     *
     * @return roll in radians
     */
    public double roll() {
        return readAngles().getRoll();
    }

    /**
     * Returns the current heading wrapped to {@code [0, 2π)}.
     *
     * <p>Useful when a value is used as an index or compared against a bounded target. Plain {@link
     * #heading()} may be negative, and code that forgets this is how a robot ends up driving the
     * long way round.
     *
     * @return heading in radians, in {@code [0, 2π)}
     */
    public double headingPositive() {
        final double heading = heading() % RADIANS_PER_TURN;
        return heading < 0.0 ? heading + RADIANS_PER_TURN : heading;
    }

    private YawPitchRollAngles readAngles() {
        final YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
        if (angles == null) {
            throw new CurioException(
                    "[CurioControl] ERROR"
                            + System.lineSeparator()
                            + "IMU '"
                            + name
                            + "' returned no orientation."
                            + System.lineSeparator()
                            + "Call calibrate(...) during init before reading angles.");
        }
        return angles;
    }

    @Override
    public String toString() {
        return "IMU[" + name + "]";
    }
}
