package org.curioone.robot.config;

/**
 * Every competition-specific value for this example robot.
 *
 * <p>CurioControl ships no constants: no wheel diameter, no arm position, no motor name. Those
 * values live here, in the robot project, where a team can tune them in one pass. See the
 * <a href="https://curiooneftc.github.io/CurioControl/guides/robot-config/">robot config guide</a>
 * for the reasoning.
 *
 * <p>The names match the stock FTC "Pushbot" configuration so the example runs on a fresh Robot
 * Controller with only the arm, intake, and IMU added.
 */
public final class RobotConfig {

    private RobotConfig() {
        throw new AssertionError("RobotConfig is a constant holder and must not be instantiated.");
    }

    /** Drivetrain geometry, motor names, and directions. */
    public static final class Drive {
        public static final String FRONT_LEFT = "front left motor";
        public static final String FRONT_RIGHT = "front right motor";
        public static final String BACK_LEFT = "back left motor";
        public static final String BACK_RIGHT = "back right motor";
        public static final String IMU = "imu";

        /** Free speed of the goBILDA 5203-series motor, for documentation only. */
        public static final double WHEEL_DIAMETER_MM = 96.0;

        public static final double GEAR_RATIO = 1.0;

        public static final double TICKS_PER_REV = 537.7;

        public static final double STICK_DEADBAND = 0.05;

        /** Flip these once, during setup, rather than negating inputs in the OpMode. */
        public static final boolean FRONT_LEFT_INVERTED = false;

        public static final boolean FRONT_RIGHT_INVERTED = true;

        public static final boolean BACK_LEFT_INVERTED = false;

        public static final boolean BACK_RIGHT_INVERTED = true;
    }

    /** Arm travel, gains, and the motor that drives it. */
    public static final class Arm {
        public static final String MOTOR = "arm motor";

        public static final int HOME = 0;

        public static final int LOW = 300;

        public static final int HIGH = 850;

        /** How close counts as "arrived", in encoder ticks. */
        public static final int TOLERANCE_TICKS = 10;

        public static final double POWER = 0.8;
    }

    /** Intake motor and claw positions. */
    public static final class Intake {
        public static final String MOTOR = "intake motor";

        public static final String CLAW = "claw servo";

        public static final double INTAKE_POWER = 1.0;

        public static final double OUTTAKE_POWER = -1.0;

        public static final double CLAW_OPEN = 0.65;

        public static final double CLAW_CLOSED = 0.35;
    }

    /** Autonomous distances and timings. */
    public static final class Auto {
        /** Distance from the blue starting position to the scoring position. */
        public static final double DRIVE_TO_SCORE_MM = 600.0;

        /** Time for the claw to drop a sample. */
        public static final double SCORE_SETTLE_SECONDS = 0.4;

        /** How hard the robot drives during autonomous. */
        public static final double DRIVE_POWER = 0.6;
    }
}
