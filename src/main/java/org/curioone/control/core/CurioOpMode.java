package org.curioone.control.core;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

/**
 * Optional base class for an OpMode driven by CurioControl.
 *
 * <p>Extending this is a convenience, not a requirement — a plain SDK {@code OpMode} with a {@link
 * CurioRobot} field works identically, and the framework does nothing that requires this class
 * (spec §25).
 *
 * <p>Implement {@link #initRobot()} and {@link #runRobot()}; the SDK's lifecycle methods are wired
 * up for you:
 *
 * <pre>{@code
 * @TeleOp(name = "Main TeleOp")
 * public class MainTeleOp extends CurioOpMode {
 *     private CurioRobot robot;
 *
 *     &#64;Override
 *     public void initRobot() {
 *         robot = new CurioRobot(hardwareMap);
 *         robot.registerSubsystem(new Arm(hardwareMap));
 *     }
 *
 *     &#64;Override
 *     public void runRobot() {
 *         robot.drive().drive(
 *                 gamepad1.left_stick_x,
 *                 gamepad1.left_stick_y,
 *                 gamepad1.right_stick_x);
 *     }
 * }
 * }</pre>
 *
 * <h2>Loop order</h2>
 *
 * {@link #runRobot()} runs <em>before</em> the robot's own {@code loop()}, so a subsystem sees the
 * commands the driver just gave on this iteration rather than last iteration's.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. The SDK calls these methods on the OpMode
 * thread.
 *
 * @since 0.1.0
 */
public abstract class CurioOpMode extends OpMode {

    /** The robot container, available once {@link #initRobot()} has returned. */
    protected CurioRobot robot;

    /**
     * Builds the robot and registers its subsystems.
     *
     * <p>Called once from {@link #init()}. Construct the {@link CurioRobot} and register subsystems
     * here. Do not call {@code robot.loop()} — this class does that.
     */
    public abstract void initRobot();

    /**
     * Runs one iteration of driver logic.
     *
     * <p>Called once per OpMode loop, before the registered subsystems' own {@code loop()}.
     */
    public abstract void runRobot();

    @Override
    public final void init() {
        robot =
                new CurioRobot(
                        hardwareMap,
                        new org.curioone.control.util.SystemClock(),
                        new SdkTelemetrySink(telemetry));
        initRobot();
        robot.init();
    }

    @Override
    public final void loop() {
        runRobot();
        robot.loop();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
        super.stop();
    }
}
