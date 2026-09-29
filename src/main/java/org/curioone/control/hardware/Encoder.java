package org.curioone.control.hardware;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.curioone.control.core.HardwareRegistry;

/**
 * A validated view onto an encoder.
 *
 * <h2>There is no standalone encoder in the FTC SDK</h2>
 *
 * Unlike every other type in this package, an encoder is not a device a team names in the Robot
 * Controller configuration. It is the quadrature encoder inside a motor, and the SDK exposes it
 * only through {@link DcMotorEx}. So this class wraps a motor and reads that motor's encoder, and
 * {@link Motor#encoder()} is the natural way to get one:
 *
 * <pre>{@code
 * Motor leftFront = new Motor(hardwareMap, "leftFront");
 * Encoder leftFrontEncoder = leftFront.encoder();
 * }</pre>
 *
 * <h2>Direction</h2>
 *
 * {@link #setDirection(Direction)} multiplies readings by {@code +1} or {@code -1}. This is
 * independent of the motor's own direction, so a right-side drivetrain reads positive when moving
 * forward without the motor being wired backwards.
 *
 * <h2>Distance</h2>
 *
 * {@link #getDistance(double, double)} converts ticks to millimetres. The physical constants are
 * parameters, never framework defaults: a wheel diameter baked into a library would be wrong for
 * every robot except one (ADR-012).
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.1.0
 */
public final class Encoder {

    /** Which way this encoder counts. */
    public enum Direction {

        /** Counts increase when the shaft turns one way. */
        FORWARD(1),

        /** Counts decrease when the shaft turns that same way. */
        REVERSED(-1);

        private final int sign;

        Direction(int sign) {
            this.sign = sign;
        }

        /**
         * Returns the multiplier applied to raw readings.
         *
         * @return {@code 1} or {@code -1}
         */
        public int sign() {
            return sign;
        }
    }

    private final DcMotorEx motor;

    private final String name;

    private Direction direction = Direction.FORWARD;

    /**
     * Wraps a motor's built-in encoder.
     *
     * @param motor the motor whose encoder is read
     * @param name a name for diagnostics
     * @throws IllegalArgumentException if {@code motor} or {@code name} is {@code null}
     */
    public Encoder(DcMotorEx motor, String name) {
        if (motor == null) {
            throw new IllegalArgumentException("motor must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.motor = motor;
        this.name = name;
    }

    /**
     * Looks up a motor by name and wraps its encoder.
     *
     * <p>The name is the <em>motor's</em> configuration name, not an encoder name, because that is
     * what exists in the Robot Controller configuration.
     *
     * @param registry the registry to resolve through
     * @param motorName the owning motor's configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     * @throws org.curioone.control.core.CurioException if no motor is configured under that name
     */
    public Encoder(HardwareRegistry registry, String motorName) {
        this(registry.motor(motorName), motorName + "/encoder");
    }

    /**
     * Returns the motor this encoder is built into.
     *
     * @return the owning motor, never {@code null}
     */
    public DcMotorEx getSdkObject() {
        return motor;
    }

    /**
     * Returns this encoder's name.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Sets which way this encoder counts.
     *
     * <p>Independent of the motor's own direction.
     *
     * @param direction {@link Direction#FORWARD} or {@link Direction#REVERSED}
     * @throws IllegalArgumentException if {@code direction} is {@code null}
     */
    public void setDirection(Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        this.direction = direction;
    }

    /**
     * Returns which way this encoder counts.
     *
     * @return the current direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the current position.
     *
     * @return position in encoder ticks, signed by the configured direction
     */
    public int getPosition() {
        return motor.getCurrentPosition() * direction.sign();
    }

    /**
     * Returns the current velocity.
     *
     * @return velocity in encoder ticks per second, signed by the configured direction
     */
    public double getVelocity() {
        return motor.getVelocity() * direction.sign();
    }

    /**
     * Zeroes the reading.
     *
     * <p>Resets the owning motor's encoder, so do not call it while that motor is being used for
     * something else.
     */
    public void reset() {
        motor.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    /**
     * Converts the current position to a distance travelled.
     *
     * <pre>{@code
     * double mm = encoder.getDistance(
     *         RobotConfig.Drive.TICKS_PER_REV,
     *         RobotConfig.Drive.WHEEL_DIAMETER_MM);
     * }</pre>
     *
     * <p>This is the current position expressed as distance, not the distance moved since the last
     * reset. Call {@link #reset()} to zero it.
     *
     * @param ticksPerRev encoder ticks per motor revolution
     * @param wheelDiameterMm wheel or shaft diameter in millimetres
     * @return distance in millimetres
     * @throws IllegalArgumentException if either value is not finite and positive
     */
    public double getDistance(double ticksPerRev, double wheelDiameterMm) {
        requirePositive("ticksPerRev", ticksPerRev);
        requirePositive("wheelDiameterMm", wheelDiameterMm);
        return getPosition() * (Math.PI * wheelDiameterMm) / ticksPerRev;
    }

    /**
     * Converts a velocity to a linear speed.
     *
     * @param ticksPerRev encoder ticks per motor revolution
     * @param wheelDiameterMm wheel or shaft diameter in millimetres
     * @return speed in millimetres per second
     * @throws IllegalArgumentException if either value is not finite and positive
     */
    public double getVelocityMmPerSecond(double ticksPerRev, double wheelDiameterMm) {
        requirePositive("ticksPerRev", ticksPerRev);
        requirePositive("wheelDiameterMm", wheelDiameterMm);
        return getVelocity() * (Math.PI * wheelDiameterMm) / ticksPerRev;
    }

    private static void requirePositive(String label, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    label + " must be finite and positive but was " + value);
        }
    }

    @Override
    public String toString() {
        return "Encoder[" + name + "]";
    }
}
