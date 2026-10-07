package org.curioone.control.control;

/**
 * A trapezoidal motion profile: accelerate, cruise, decelerate.
 *
 * <p>A step change in setpoint asks a mechanism for infinite acceleration at the first instant,
 * which is why a tuned PID still overshoots on long moves. The profile replaces the step with a
 * feasible reference — bounded velocity, bounded acceleration — so the feedback loop tracks a
 * trajectory the mechanism can actually follow instead of chasing an impossible one. Feed the
 * reference position to a {@link PIDController} and the reference velocity and acceleration to an
 * {@link AccelerationFeedforward} or {@link CombinedFeedforward}.
 *
 * <pre>{@code
 * TrapezoidalMotionProfile profile =
 *         new TrapezoidalMotionProfile(0.0, 1000.0, new Constraints(2000.0, 4000.0));
 * double position = profile.getPosition(elapsedSeconds);
 * double velocity = profile.getVelocity(elapsedSeconds);
 * if (profile.isFinished(elapsedSeconds)) { ... }
 * }</pre>
 *
 * <p>Short moves never reach cruise velocity: when the distance is too small for a trapezoid, the
 * profile degrades to a triangle with a lower peak velocity rather than violating the acceleration
 * limit. Reverse moves ({@code target < start}) mirror the forward computation exactly.
 *
 * <p>Time before zero clamps to the start, time past the end clamps to the target. A profile over
 * zero distance is finished at {@code t = 0}.
 *
 * <p>Immutable and thread-safe. Pure Java: takes numbers, not hardware.
 *
 * @since 0.2.0
 */
public final class TrapezoidalMotionProfile {

    /** One half, for the constant-acceleration distance formula. */
    private static final double HALF = 0.5;

    /** Velocity and acceleration limits a profile is built under. */
    public static final class Constraints {

        private final double maxVelocity;

        private final double maxAcceleration;

        /**
         * Creates motion constraints.
         *
         * @param maxVelocity the highest permitted velocity, in caller units per second; must be
         *     positive
         * @param maxAcceleration the highest permitted acceleration, in caller units per second
         *     squared; must be positive
         * @throws IllegalArgumentException if either limit is not finite or not positive
         */
        public Constraints(double maxVelocity, double maxAcceleration) {
            if (!Double.isFinite(maxVelocity) || maxVelocity <= 0.0) {
                throw new IllegalArgumentException(
                        "maxVelocity must be finite and positive but was " + maxVelocity);
            }
            if (!Double.isFinite(maxAcceleration) || maxAcceleration <= 0.0) {
                throw new IllegalArgumentException(
                        "maxAcceleration must be finite and positive but was " + maxAcceleration);
            }
            this.maxVelocity = maxVelocity;
            this.maxAcceleration = maxAcceleration;
        }

        /**
         * Returns the velocity limit.
         *
         * @return the maximum velocity
         */
        public double getMaxVelocity() {
            return maxVelocity;
        }

        /**
         * Returns the acceleration limit.
         *
         * @return the maximum acceleration
         */
        public double getMaxAcceleration() {
            return maxAcceleration;
        }
    }

    private final double start;

    private final double target;

    private final double direction;

    private final double peakVelocity;

    private final double accelerationTime;

    private final double cruiseTime;

    private final double totalTime;

    /**
     * Creates a profile from a start to a target under the given limits.
     *
     * @param start the starting position
     * @param target the target position
     * @param constraints the motion limits
     * @throws IllegalArgumentException if {@code constraints} is {@code null}, or a position is not
     *     finite
     */
    public TrapezoidalMotionProfile(double start, double target, Constraints constraints) {
        if (!Double.isFinite(start)) {
            throw new IllegalArgumentException("start must be finite but was " + start);
        }
        if (!Double.isFinite(target)) {
            throw new IllegalArgumentException("target must be finite but was " + target);
        }
        if (constraints == null) {
            throw new IllegalArgumentException("constraints must not be null");
        }
        this.start = start;
        this.target = target;

        final double distance = Math.abs(target - start);
        this.direction = Math.signum(target - start);

        if (distance == 0.0) {
            this.peakVelocity = 0.0;
            this.accelerationTime = 0.0;
            this.cruiseTime = 0.0;
            this.totalTime = 0.0;
            return;
        }

        final double maxVelocity = constraints.getMaxVelocity();
        final double maxAcceleration = constraints.getMaxAcceleration();
        final double fullAccelDistance = maxVelocity * maxVelocity / (2.0 * maxAcceleration);

        if (2.0 * fullAccelDistance <= distance) {
            this.peakVelocity = maxVelocity;
            this.accelerationTime = maxVelocity / maxAcceleration;
            this.cruiseTime = (distance - 2.0 * fullAccelDistance) / maxVelocity;
        } else {
            this.peakVelocity = Math.sqrt(distance * maxAcceleration);
            this.accelerationTime = peakVelocity / maxAcceleration;
            this.cruiseTime = 0.0;
        }
        this.totalTime = 2.0 * accelerationTime + cruiseTime;
    }

    /**
     * Returns the starting position.
     *
     * @return the start
     */
    public double getStart() {
        return start;
    }

    /**
     * Returns the target position.
     *
     * @return the target
     */
    public double getTarget() {
        return target;
    }

    /**
     * Returns the total duration of the profile.
     *
     * @return the time from start to exact arrival, in seconds
     */
    public double totalTime() {
        return totalTime;
    }

    /**
     * Returns the reference position at a time.
     *
     * @param time seconds since the profile started; clamped to {@code [0, totalTime]}
     * @return the reference position
     * @throws IllegalArgumentException if {@code time} is NaN
     */
    public double getPosition(double time) {
        return start + direction * distanceAt(clampTime(time));
    }

    /**
     * Returns the reference velocity at a time.
     *
     * <p>The sign follows the direction of travel: negative on a reverse move.
     *
     * @param time seconds since the profile started; clamped to {@code [0, totalTime]}
     * @return the reference velocity, in caller units per second
     * @throws IllegalArgumentException if {@code time} is NaN
     */
    public double getVelocity(double time) {
        return direction * velocityAt(clampTime(time));
    }

    /**
     * Returns the reference acceleration at a time.
     *
     * <p>Positive while speeding up, zero while cruising, negative while braking — the sign follows
     * the direction of travel.
     *
     * @param time seconds since the profile started; clamped to {@code [0, totalTime]}
     * @return the reference acceleration, in caller units per second squared
     * @throws IllegalArgumentException if {@code time} is NaN
     */
    public double getAcceleration(double time) {
        return direction * accelerationAt(clampTime(time));
    }

    /**
     * Reports whether the profile has arrived.
     *
     * @param time seconds since the profile started
     * @return {@code true} once {@code time} reaches the total duration
     * @throws IllegalArgumentException if {@code time} is NaN
     */
    public boolean isFinished(double time) {
        if (Double.isNaN(time)) {
            throw new IllegalArgumentException("time must not be NaN");
        }
        return time >= totalTime;
    }

    private double clampTime(double time) {
        if (Double.isNaN(time)) {
            throw new IllegalArgumentException("time must not be NaN");
        }
        return Math.max(0.0, Math.min(totalTime, time));
    }

    private double distanceAt(double time) {
        final double maxAcceleration = accelerationMagnitude();
        if (time < accelerationTime) {
            return HALF * maxAcceleration * time * time;
        }
        final double accelDistance = HALF * maxAcceleration * accelerationTime * accelerationTime;
        if (time < accelerationTime + cruiseTime) {
            return accelDistance + peakVelocity * (time - accelerationTime);
        }
        final double decelTime = time - accelerationTime - cruiseTime;
        return accelDistance
                + peakVelocity * cruiseTime
                + peakVelocity * decelTime
                - HALF * maxAcceleration * decelTime * decelTime;
    }

    private double velocityAt(double time) {
        final double maxAcceleration = accelerationMagnitude();
        if (time < accelerationTime) {
            return maxAcceleration * time;
        }
        if (time < accelerationTime + cruiseTime) {
            return peakVelocity;
        }
        return Math.max(
                0.0, peakVelocity - maxAcceleration * (time - accelerationTime - cruiseTime));
    }

    private double accelerationAt(double time) {
        final double maxAcceleration = accelerationMagnitude();
        if (time < accelerationTime) {
            return maxAcceleration;
        }
        if (time < accelerationTime + cruiseTime) {
            return 0.0;
        }
        if (time < totalTime) {
            return -maxAcceleration;
        }
        return 0.0;
    }

    private double accelerationMagnitude() {
        if (accelerationTime == 0.0) {
            return 0.0;
        }
        return peakVelocity / accelerationTime;
    }

    @Override
    public String toString() {
        return "TrapezoidalMotionProfile[start="
                + start
                + ", target="
                + target
                + ", totalTime="
                + totalTime
                + "]";
    }
}
