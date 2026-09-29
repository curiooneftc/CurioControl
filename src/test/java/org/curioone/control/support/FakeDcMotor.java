package org.curioone.control.support;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DcMotorEx} that behaves like a motor instead of recording calls.
 *
 * <p>Preferred over a Mockito mock wherever behaviour matters. A mock proves that a setter was
 * called; a fake lets a test run a closed loop — {@code power -> velocity -> position} — and assert
 * where a PID controller ends up. That is the difference between testing a controller and testing
 * that a controller was invoked.
 *
 * <p>The integration model is deliberately simple and predictable: each {@code tick()} advances
 * position by velocity and velocity by {@code power * maxTicksPerSecond / ticksPerRev}, with no
 * friction, no load, and no noise. Good enough to close a loop, and free of surprises.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.1.0
 */
// The SDK deprecates setPowerFloat and the PIDCoefficients accessors, but they are still part of
// the interface a fake must implement. Suppressing at the class is the only honest option:
// omitting them would not compile, and the deprecation is the SDK's, not ours.
@SuppressWarnings("deprecation")
public final class FakeDcMotor implements DcMotorEx {

    private static final double TICKS_PER_REV = 537.7;

    private final List<String> calls = new ArrayList<>();

    private double power;

    private double velocity;

    private int position;

    private int targetPosition;

    private DcMotor.RunMode mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER;

    private DcMotorSimple.Direction direction = DcMotorSimple.Direction.FORWARD;

    private DcMotor.ZeroPowerBehavior zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;

    private boolean busy;

    private int targetPositionTolerance;

    @Override
    public void setPower(double power) {
        this.power = power;
        calls.add("setPower(" + power + ")");
    }

    @Override
    public double getPower() {
        return power;
    }

    @Override
    public void setVelocity(double velocity) {
        this.velocity = velocity;
        calls.add("setVelocity(" + velocity + ")");
    }

    @Override
    public double getVelocity() {
        return velocity;
    }

    @Override
    public void setTargetPosition(int position) {
        this.targetPosition = position;
        calls.add("setTargetPosition(" + position + ")");
    }

    @Override
    public int getTargetPosition() {
        return targetPosition;
    }

    @Override
    public int getCurrentPosition() {
        return position;
    }

    @Override
    public void setMode(DcMotor.RunMode mode) {
        this.mode = mode;
        calls.add("setMode(" + mode + ")");
        if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER) {
            position = 0;
            velocity = 0.0;
        }
    }

    @Override
    public DcMotor.RunMode getMode() {
        return mode;
    }

    @Override
    public void setDirection(DcMotorSimple.Direction direction) {
        this.direction = direction;
        calls.add("setDirection(" + direction + ")");
    }

    @Override
    public DcMotorSimple.Direction getDirection() {
        return direction;
    }

    @Override
    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        this.zeroPowerBehavior = behavior;
        calls.add("setZeroPowerBehavior(" + behavior + ")");
    }

    @Override
    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior() {
        return zeroPowerBehavior;
    }

    @Override
    public void setPowerFloat() {
        calls.add("setPowerFloat()");
    }

    @Override
    public boolean getPowerFloat() {
        return false;
    }

    @Override
    public boolean isBusy() {
        return busy;
    }

    @Override
    public boolean isOverCurrent() {
        return false;
    }

    @Override
    public void setMotorEnable() {
        // Not modelled.
    }

    @Override
    public void setMotorDisable() {
        // Not modelled.
    }

    @Override
    public boolean isMotorEnabled() {
        return true;
    }

    @Override
    public void setVelocity(
            double velocity, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit unit) {
        this.velocity = velocity;
    }

    @Override
    public double getVelocity(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit unit) {
        return velocity;
    }

    @Override
    public void setPIDCoefficients(
            DcMotor.RunMode runMode, com.qualcomm.robotcore.hardware.PIDCoefficients coefficients) {
        // Not modelled: the framework does not set SDK PID gains.
    }

    @Override
    public void setPIDFCoefficients(
            DcMotor.RunMode runMode,
            com.qualcomm.robotcore.hardware.PIDFCoefficients coefficients) {
        // Not modelled: the framework does not set SDK PIDF gains.
    }

    @Override
    public void setVelocityPIDFCoefficients(double a, double b, double c, double d) {
        // Not modelled: the framework uses its own PIDController.
    }

    @Override
    public void setPositionPIDFCoefficients(double coefficient) {
        // Not modelled: the framework uses its own PIDController.
    }

    @Override
    public com.qualcomm.robotcore.hardware.PIDCoefficients getPIDCoefficients(
            DcMotor.RunMode runMode) {
        return new com.qualcomm.robotcore.hardware.PIDCoefficients(0, 0, 0);
    }

    @Override
    public com.qualcomm.robotcore.hardware.PIDFCoefficients getPIDFCoefficients(
            DcMotor.RunMode runMode) {
        return new com.qualcomm.robotcore.hardware.PIDFCoefficients(0, 0, 0, 0);
    }

    @Override
    public void setTargetPositionTolerance(int tolerance) {
        this.targetPositionTolerance = tolerance;
    }

    @Override
    public int getTargetPositionTolerance() {
        return targetPositionTolerance;
    }

    @Override
    public double getCurrent(org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit unit) {
        return 0.0;
    }

    @Override
    public double getCurrentAlert(
            org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit unit) {
        return 0.0;
    }

    @Override
    public void setCurrentAlert(
            double amps, org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit unit) {
        // Not modelled.
    }

    @Override
    public HardwareDevice.Manufacturer getManufacturer() {
        return HardwareDevice.Manufacturer.Other;
    }

    @Override
    public String getDeviceName() {
        return "FakeDcMotor";
    }

    @Override
    public String getConnectionInfo() {
        return "";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        // Nothing to reset.
    }

    @Override
    public void close() {
        // Nothing to close.
    }

    @Override
    public MotorConfigurationType getMotorType() {
        throw new UnsupportedOperationException("the fake does not model motor type");
    }

    @Override
    public void setMotorType(MotorConfigurationType motorType) {
        // Not modelled.
    }

    @Override
    public com.qualcomm.robotcore.hardware.DcMotorController getController() {
        return null;
    }

    @Override
    public int getPortNumber() {
        return 0;
    }

    // --- Fake-specific behaviour ------------------------------------------------

    /**
     * Advances the simulated motor by one loop iteration.
     *
     * <p>In {@link DcMotor.RunMode#RUN_TO_POSITION} the motor drives toward its target instead of
     * following the commanded power, which is what makes a position loop testable.
     *
     * @param seconds the loop interval
     */
    public void tick(double seconds) {
        if (mode == DcMotor.RunMode.RUN_TO_POSITION) {
            final int remaining = targetPosition - position;
            // A fixed, deliberately large step so a test converges in a handful of ticks rather
            // than simulating a real motor's acceleration curve.
            final int step = (int) Math.copySign(Math.max(1, Math.abs(remaining) / 2.0), remaining);
            position += step;
            velocity = step / Math.max(seconds, 1e-9);
            busy = remaining != 0;
        } else {
            final double ticksPerSecond = power * TICKS_PER_REV;
            position += (int) Math.round(ticksPerSecond * seconds);
            velocity = ticksPerSecond;
            busy = power != 0.0;
        }
    }

    /**
     * Returns the last power commanded.
     *
     * @return the commanded power
     */
    public double lastPower() {
        return power;
    }

    /**
     * Returns every call made, in order, for asserting on delegation.
     *
     * @return the recorded calls
     */
    public List<String> calls() {
        return calls;
    }

    /**
     * Sets the simulated position directly, for arranging a test's starting state.
     *
     * @param position the position in ticks
     */
    public void setPositionForTest(int position) {
        this.position = position;
    }
}
