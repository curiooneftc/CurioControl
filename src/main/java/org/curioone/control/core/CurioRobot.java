package org.curioone.control.core;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.curioone.control.drive.DriveBase;
import org.curioone.control.drive.MecanumDrive;
import org.curioone.control.drive.TankDrive;
import org.curioone.control.hardware.AnalogSensor;
import org.curioone.control.hardware.ContinuousServo;
import org.curioone.control.hardware.DigitalSensor;
import org.curioone.control.hardware.Encoder;
import org.curioone.control.hardware.HardwareType;
import org.curioone.control.hardware.Motor;
import org.curioone.control.hardware.Servo;
import org.curioone.control.hardware.VoltageSensor;
import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * The robot container: the main entry point for a CurioControl program.
 *
 * <pre>{@code
 * CurioRobot robot = new CurioRobot(hardwareMap);
 * robot.drive().drive(x, y, r);
 * robot.telemetry().add("Heading", robot.imu().heading()).update();
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>construct — resolves nothing; hardware is looked up lazily on first use
 *   <li>{@link #init()} — every registered subsystem's {@code init()}, in registration order
 *   <li>{@link #loop()} — every subsystem's {@code loop()}, then telemetry publishing
 *   <li>{@link #stop()} — every subsystem's {@code stop()}, then the drivetrain's {@code stop()}
 * </ol>
 *
 * {@link CurioOpMode} drives this for you. Call it by hand if you are using a plain SDK {@code
 * OpMode}, which the framework fully supports (spec §25).
 *
 * <h2>Why this class is different</h2>
 *
 * Every other class in {@code core} is a leaf and depends on no other CurioControl package. {@code
 * CurioRobot} is the one exception: it is the composition root, and it wires the pieces together,
 * so it is the only class allowed to reach outward to {@code hardware}, {@code drive}, and {@code
 * telemetry}. That exception is deliberate, documented in ADR-014, and enforced by an ArchUnit rule
 * that names this class as the sole permitted exception.
 *
 * <p>Without it, the facade in the specification — {@code robot.drive()}, {@code robot.imu()},
 * {@code robot.telemetry()} — could not exist.
 *
 * <h2>Hardware resolution</h2>
 *
 * Devices are resolved on first use, not in the constructor. That keeps construction cheap, makes
 * the object safe to build before the Robot Controller's hardware map is fully populated, and means
 * an unused device is never looked up. A lookup that fails throws {@link CurioException} naming the
 * device — it never substitutes a different one.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Use it from the OpMode thread.
 *
 * @since 0.1.0
 */
public final class CurioRobot {

    private final HardwareRegistry hardware;

    private final TelemetryManager telemetry;

    private final Clock clock;

    private final Map<String, Subsystem> subsystems = new LinkedHashMap<>();

    private final List<Subsystem> orderedSubsystems = new ArrayList<>();

    private DriveBase drive;

    private org.curioone.control.hardware.IMU imu;

    private String imuName;

    private org.curioone.control.vision.VisionManager vision;

    private String visionName;

    private boolean initialized;

    /**
     * Creates a robot over a Robot Controller hardware map.
     *
     * <p>Nothing is resolved yet, so this is cheap and cannot fail. Devices are looked up on first
     * use.
     *
     * @param hardwareMap the configured hardware
     * @throws IllegalArgumentException if {@code hardwareMap} is {@code null}
     */
    public CurioRobot(HardwareMap hardwareMap) {
        this(
                new SdkHardwareSource(requireNonNull(hardwareMap, "hardwareMap")),
                new SystemClock(),
                null);
    }

    /**
     * Creates a robot with an injected time source and telemetry sink.
     *
     * <p>For tests, and for the rare program that is not driven by an SDK OpMode and therefore has
     * no {@code Telemetry} to write to. Pass {@code null} for {@code telemetrySink} to discard
     * reporting.
     *
     * @param hardwareMap the configured hardware
     * @param clock the time source
     * @param telemetrySink where telemetry is written, or {@code null} to discard it
     * @throws IllegalArgumentException if {@code hardwareMap} or {@code clock} is {@code null}
     */
    public CurioRobot(HardwareMap hardwareMap, Clock clock, TelemetrySink telemetrySink) {
        this(
                new SdkHardwareSource(requireNonNull(hardwareMap, "hardwareMap")),
                clock,
                telemetrySink);
    }

    /**
     * Creates a robot over any source of named devices.
     *
     * <p>The seam that makes the whole framework testable off-robot. The Robot Controller's {@code
     * HardwareMap} reaches into Android and cannot be constructed on a desktop JVM, so tests supply
     * devices through this constructor instead.
     *
     * @param hardware where devices come from
     * @param clock the time source
     * @param telemetrySink where telemetry is written, or {@code null} to discard it
     * @throws IllegalArgumentException if {@code hardware} or {@code clock} is {@code null}
     */
    public CurioRobot(HardwareSource hardware, Clock clock, TelemetrySink telemetrySink) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.hardware = new HardwareRegistry(hardware);
        this.clock = clock;
        this.telemetry =
                new TelemetryManager(
                        telemetrySink != null ? telemetrySink : TelemetrySink.discarding(), clock);
    }

    private static <T> T requireNonNull(T value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
        return value;
    }

    // --- Lifecycle ---------------------------------------------------------------

    /**
     * Initializes every registered subsystem, in registration order.
     *
     * <p>Idempotent: calling it twice does nothing the second time, so a program that calls it
     * defensively and one that relies on {@link CurioOpMode} behave the same.
     */
    public void init() {
        if (initialized) {
            return;
        }
        for (Subsystem subsystem : orderedSubsystems) {
            subsystem.init();
        }
        initialized = true;
    }

    /**
     * Advances every registered subsystem by one iteration, then publishes telemetry.
     *
     * <p>Call once per OpMode loop. The OpMode's own input handling runs before this, so a
     * subsystem sees the commands the driver just gave.
     */
    public void loop() {
        for (Subsystem subsystem : orderedSubsystems) {
            subsystem.loop();
        }
        for (Subsystem subsystem : orderedSubsystems) {
            subsystem.publishTelemetry(telemetry);
        }
        telemetry.update();
    }

    /**
     * Stops every subsystem, then the drivetrain.
     *
     * <p>Order matters: subsystems stop first so a mechanism can still command the drivetrain into
     * a safe state before the drivetrain zeroes its own motors.
     *
     * <p>Safe to call more than once, which matters because an early OpMode exit and a normal
     * shutdown can both reach it.
     */
    public void stop() {
        for (Subsystem subsystem : orderedSubsystems) {
            subsystem.stop();
        }
        if (drive != null) {
            drive.stop();
        }
        telemetry.updateNow();
    }

    /**
     * Reports whether {@link #init()} has run.
     *
     * @return {@code true} once initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    // --- Subsystems --------------------------------------------------------------

    /**
     * Registers a subsystem under its class name.
     *
     * @param subsystem the subsystem to register
     * @throws IllegalArgumentException if {@code subsystem} is {@code null}, or already registered
     */
    public void registerSubsystem(Subsystem subsystem) {
        registerSubsystem(subsystem, null);
    }

    /**
     * Registers a subsystem under an explicit name.
     *
     * <p>The name appears in diagnostics. A team with two mechanisms of the same type — two
     * intakes, say — can tell them apart.
     *
     * <p>Must be called before {@link #init()}. A subsystem registered afterwards would never
     * receive its {@code init()}, so it would run against hardware it never configured — a failure
     * that surfaces on the field rather than on the bench. Rejecting it here turns that into a
     * startup error with a message naming the fix.
     *
     * @param subsystem the subsystem to register
     * @param name a diagnostic name, or {@code null} to use the class name
     * @throws IllegalArgumentException if {@code subsystem} is {@code null}, or already registered
     *     under that name
     * @throws IllegalStateException if the robot has already been initialized
     */
    public void registerSubsystem(Subsystem subsystem, String name) {
        if (subsystem == null) {
            throw new IllegalArgumentException("subsystem must not be null");
        }
        if (initialized) {
            throw new IllegalStateException(
                    "cannot register subsystem '"
                            + (name != null ? name : subsystem.getClass().getSimpleName())
                            + "' after init(); it would never receive init(), so its hardware "
                            + "would be unconfigured. Register every subsystem before init().");
        }
        final String key = name != null ? name : subsystem.getClass().getSimpleName();
        if (subsystems.containsKey(key)) {
            throw new IllegalArgumentException(
                    "a subsystem named '"
                            + key
                            + "' is already registered; names must be unique "
                            + "so a diagnostic identifies exactly one mechanism");
        }
        subsystem.setName(key);
        subsystems.put(key, subsystem);
        orderedSubsystems.add(subsystem);
    }

    /**
     * Returns a registered subsystem by name.
     *
     * @param name the name it was registered under
     * @param <T> the expected subsystem type
     * @return the subsystem, or {@code null} if nothing is registered under that name
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public <T extends Subsystem> T subsystem(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return castOrNull(subsystems.get(name));
    }

    /**
     * Returns the registered subsystem of a given type, or {@code null}.
     *
     * <p>Returns {@code null} when nothing of that type is registered, and when more than one is —
     * in which case the caller must use the name, because guessing which of two intakes was meant
     * is how the wrong one gets driven.
     *
     * @param <T> the subsystem type
     * @param type the class to look for
     * @return the single registered subsystem of that type, or {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public <T extends Subsystem> T subsystem(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        T found = null;
        for (Subsystem subsystem : orderedSubsystems) {
            if (type.isInstance(subsystem)) {
                if (found != null) {
                    return null;
                }
                found = type.cast(subsystem);
            }
        }
        return found;
    }

    /**
     * Returns the names of every registered subsystem, in registration order.
     *
     * @return an unmodifiable view of the registered names
     */
    public java.util.Set<String> subsystemNames() {
        return Collections.unmodifiableSet(subsystems.keySet());
    }

    // --- Hardware ----------------------------------------------------------------

    /**
     * Returns the hardware registry, for devices the convenience accessors do not cover.
     *
     * @return the registry, never {@code null}
     */
    public HardwareRegistry hardware() {
        return hardware;
    }

    /**
     * Returns the time source.
     *
     * @return the clock, never {@code null}
     */
    public Clock clock() {
        return clock;
    }

    /**
     * Returns the telemetry manager.
     *
     * @return the telemetry manager, never {@code null}
     */
    public TelemetryManager telemetry() {
        return telemetry;
    }

    /**
     * Looks up a motor by name.
     *
     * @param name the configuration name
     * @return a validated motor wrapper
     * @throws CurioException if no motor is configured under that name
     */
    public Motor motor(String name) {
        return new Motor(hardware.motor(name), name);
    }

    /**
     * Looks up a positional servo by name.
     *
     * @param name the configuration name
     * @return a validated servo wrapper
     * @throws CurioException if no servo is configured under that name
     */
    public Servo servo(String name) {
        return new Servo(hardware, name);
    }

    /**
     * Looks up a continuous rotation servo by name.
     *
     * @param name the configuration name
     * @return a validated continuous servo wrapper
     * @throws CurioException if no continuous rotation servo is configured under that name
     */
    public ContinuousServo continuousServo(String name) {
        return new ContinuousServo(hardware, name);
    }

    /**
     * Looks up the encoder of the motor configured under a name.
     *
     * @param name the owning motor's configuration name
     * @return an encoder view onto that motor
     * @throws CurioException if no motor is configured under that name
     */
    public Encoder encoder(String name) {
        return new Encoder(hardware, name);
    }

    /**
     * Looks up a digital channel by name.
     *
     * @param name the configuration name
     * @return a digital sensor wrapper
     * @throws CurioException if no digital channel is configured under that name
     */
    public DigitalSensor digitalSensor(String name) {
        return new DigitalSensor(hardware, name);
    }

    /**
     * Looks up an analog input by name.
     *
     * @param name the configuration name
     * @return an analog sensor wrapper
     * @throws CurioException if no analog input is configured under that name
     */
    public AnalogSensor analogSensor(String name) {
        return new AnalogSensor(hardware, name);
    }

    /**
     * Looks up a voltage sensor by name.
     *
     * @param name the configuration name
     * @return a voltage sensor wrapper
     * @throws CurioException if no voltage sensor is configured under that name
     */
    public VoltageSensor voltageSensor(String name) {
        return new VoltageSensor(hardware, name);
    }

    /**
     * Reports whether a device is configured, without throwing.
     *
     * <p>For probing optional hardware. Do not use it to guard a required device: that turns a loud
     * failure into a silent one.
     *
     * @param type the device kind
     * @param name the configuration name
     * @return {@code true} if the device is configured
     */
    public boolean has(HardwareType type, String name) {
        return hardware.has(type.sdkType(), name);
    }

    /**
     * Returns the robot's IMU, looked up under the name {@code "imu"}.
     *
     * @return the IMU wrapper
     * @throws CurioException if no IMU is configured under {@code "imu"}
     */
    public org.curioone.control.hardware.IMU imu() {
        return imu(DEFAULT_IMU_NAME);
    }

    /**
     * Returns the robot's IMU under a custom configuration name.
     *
     * <p>Resolved once and cached, so the same wrapper — and therefore the same object — is
     * returned on every call.
     *
     * @param name the IMU's configuration name
     * @return the IMU wrapper
     * @throws CurioException if no IMU is configured under that name
     */
    public org.curioone.control.hardware.IMU imu(String name) {
        if (imu == null || !name.equals(imuName)) {
            imu = new org.curioone.control.hardware.IMU(hardware, name);
            imuName = name;
        }
        return imu;
    }

    // --- Vision ------------------------------------------------------------------

    /**
     * Returns the robot's vision manager, built for the conventional camera name.
     *
     * <p>Lazy: nothing vision-related is constructed — no processor, no portal, no camera handle —
     * until this call. A robot that never calls it pays nothing, which is what keeps vision opt-in
     * rather than a tax on every program.
     *
     * <p>Built for the conventional camera name {@code "Webcam 1"} with the default AprilTag
     * pipeline. Use {@link #vision(String)} for any other camera, or {@link
     * #attachVision(org.curioone.control.vision.VisionManager)} for a custom pipeline.
     *
     * @return the vision manager, never {@code null}
     * @throws CurioException if no webcam is configured under {@code "Webcam 1"}
     */
    public org.curioone.control.vision.VisionManager vision() {
        return vision(DEFAULT_CAMERA_NAME);
    }

    /**
     * Returns the robot's vision manager for a camera under a custom configuration name.
     *
     * <p>Resolved once and cached like {@link #imu(String)}, so the same manager — and therefore
     * the same portal — is returned on every call.
     *
     * @param cameraName the camera's configuration name
     * @return the vision manager, never {@code null}
     * @throws CurioException if no webcam is configured under that name
     */
    public org.curioone.control.vision.VisionManager vision(String cameraName) {
        if (vision == null || !cameraName.equals(visionName)) {
            final org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName camera =
                    hardware.require(
                            org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
                                    .class,
                            cameraName);
            vision = org.curioone.control.vision.VisionManager.forCamera(camera);
            visionName = cameraName;
        }
        return vision;
    }

    /**
     * Installs a vision manager.
     *
     * <p>Call before {@link #vision()}, for a custom pipeline or a manager built with non-default
     * options. A later {@link #vision(String)} for a different name rebuilds from configuration as
     * usual.
     *
     * @param vision the manager to use
     * @throws IllegalArgumentException if {@code vision} is {@code null}
     */
    public void attachVision(org.curioone.control.vision.VisionManager vision) {
        if (vision == null) {
            throw new IllegalArgumentException("vision must not be null");
        }
        this.vision = vision;
        this.visionName = vision.name();
    }

    // --- Drive -------------------------------------------------------------------

    /**
     * Returns the drivetrain, building a mecanum base on first access.
     *
     * <p>Built from the conventional configuration names {@code frontLeft}, {@code frontRight},
     * {@code backLeft}, and {@code backRight}. Use {@link #setDrive(DriveBase)} for any other
     * layout.
     *
     * @return the drivetrain, never {@code null}
     * @throws CurioException if any of the four motors is not configured
     */
    public DriveBase drive() {
        if (drive == null) {
            drive = mecanumDrive("frontLeft", "frontRight", "backLeft", "backRight");
        }
        return drive;
    }

    /**
     * Installs a drivetrain.
     *
     * <p>Call before {@link #drive()}, for a tank chassis or a mecanum base with non-standard
     * configuration names.
     *
     * @param drive the drivetrain to use
     * @throws IllegalArgumentException if {@code drive} is {@code null}
     */
    public void setDrive(DriveBase drive) {
        if (drive == null) {
            throw new IllegalArgumentException("drive must not be null");
        }
        this.drive = drive;
    }

    /**
     * Builds a mecanum drivetrain from explicit configuration names without installing it.
     *
     * <p>For a base whose motor names differ from the convention, where {@link #drive()}'s defaults
     * would fail at first use.
     *
     * @param frontLeftName front-left motor name
     * @param frontRightName front-right motor name
     * @param backLeftName back-left motor name
     * @param backRightName back-right motor name
     * @return a mecanum drivetrain
     * @throws CurioException if any motor is not configured
     */
    public MecanumDrive mecanumDrive(
            String frontLeftName,
            String frontRightName,
            String backLeftName,
            String backRightName) {
        return new MecanumDrive(
                "mecanum",
                new Motor(hardware, frontLeftName),
                new Motor(hardware, frontRightName),
                new Motor(hardware, backLeftName),
                new Motor(hardware, backRightName));
    }

    /**
     * Builds a tank drivetrain from explicit configuration names without installing it.
     *
     * @param leftName left motor name
     * @param rightName right motor name
     * @return a tank drivetrain
     * @throws CurioException if either motor is not configured
     */
    public TankDrive tankDrive(String leftName, String rightName) {
        return new TankDrive("tank", new Motor(hardware, leftName), new Motor(hardware, rightName));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Subsystem> T castOrNull(Subsystem subsystem) {
        return subsystem == null ? null : (T) subsystem;
    }

    /** Conventional configuration name for the robot's IMU. */
    private static final String DEFAULT_IMU_NAME = "imu";

    /** Conventional configuration name for the robot's camera. */
    private static final String DEFAULT_CAMERA_NAME = "Webcam 1";

    @Override
    public String toString() {
        return "CurioRobot[" + subsystemNames().size() + " subsystems]";
    }
}
