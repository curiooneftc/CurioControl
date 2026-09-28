Absolutely — here’s the full CurioControl specification in Markdown, updated around the new name and structured so you could drop it directly into SPEC.md or the repository README.

# CurioControl
> A modular control and development framework for FIRST Tech Challenge robots, developed by Curio One.
**Status:** Pre-release / Development  
**Target Platform:** FTC Robot Controller  
**Language:** Java  
**Build System:** Gradle  
**License:** TBD
---
## 1. Overview
CurioControl is a reusable software framework for FIRST Tech Challenge (FTC) robots.
Its purpose is to provide Curio One with a consistent, modular, and maintainable architecture for robot development while reducing repetitive FTC SDK boilerplate.
CurioControl sits on top of the official FTC SDK rather than replacing it.
```text
┌─────────────────────────────┐
│       Robot Application     │
│                             │
│  OpModes / Autonomous / UI  │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│        CurioControl         │
│                             │
│ Hardware / Control / Drive  │
│ Commands / Telemetry / etc. │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│          FTC SDK            │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│       Android / Hardware    │
└─────────────────────────────┘

The framework should be opinionated about software architecture, but not about how a team’s robot must function.

⸻

2. Goals

CurioControl should:

* Reduce repetitive FTC SDK boilerplate.
* Provide a consistent architecture across Curio One robot projects.
* Make robot code easier for new team members to understand.
* Separate robot logic from hardware implementation.
* Provide reusable control algorithms.
* Make autonomous routines easier to construct.
* Make debugging and telemetry easier.
* Make numerical/control code unit-testable outside the Robot Controller.
* Support versioned releases through Gradle.
* Allow components to be used independently.
* Remain lightweight enough for FTC Robot Controller hardware.

⸻

3. Non-Goals

CurioControl should not:

* Replace the FTC SDK.
* Force teams to use a specific drivetrain.
* Contain season-specific robot logic.
* Contain Curio One’s competition robot configuration.
* Depend on unnecessary external services.
* Require an internet connection while the robot is operating.
* Hide the FTC SDK completely.

The FTC SDK should remain accessible when direct SDK functionality is necessary.

⸻

4. Repository

Recommended GitHub repository:

Curio-One/CurioControl

Recommended project ecosystem:

Curio-One/
│
├── CurioControl
│   └── Reusable FTC framework
│
├── RobotCode
│   └── Current competition robot
│
├── CurioVision
│   └── Optional vision tooling
│
└── CurioTools
    └── Development/scouting utilities

Initially, only CurioControl should be created.

Additional repositories should only be introduced when there is a genuine separation of responsibilities.

⸻

5. Package Identity

Recommended Maven coordinates:

Group:
dev.curio
Artifact:
curiocontrol

Gradle dependency:

implementation("dev.curio:curiocontrol:1.0.0")

Recommended Java package:

dev.curio.control

Subpackages:

dev.curio.control
├── core
├── hardware
├── control
├── drive
├── command
├── telemetry
├── vision
├── math
└── util

⸻

6. Architecture

CurioControl should follow a layered architecture.

Application
    │
    ▼
OpModes / Autonomous
    │
    ▼
Robot
    │
    ├── Subsystems
    │      │
    │      ▼
    │   Controllers
    │
    ├── Drive
    │
    ├── Vision
    │
    └── Telemetry
           │
           ▼
        Hardware
           │
           ▼
        FTC SDK

The general dependency direction should be:

Application
    ↓
CurioControl
    ↓
FTC SDK

Lower-level components should not depend on robot-specific implementations.

⸻

7. Core Modules

CurioControl should eventually contain the following modules.

curiocontrol
│
├── core
│
├── hardware
│
├── control
│
├── drive
│
├── command
│
├── telemetry
│
├── vision
│
├── math
│
└── util

⸻

8. Core

The core package provides the main framework abstractions.

8.1 Robot

The main robot container:

BioBuzzRobot robot = new BioBuzzRobot(hardwareMap);

Final API should use:

CurioRobot robot = new CurioRobot(hardwareMap);

Responsibilities:

* Hardware registration
* Subsystem registration
* Shared services
* Telemetry
* Robot lifecycle

Example:

CurioRobot robot = new CurioRobot(hardwareMap);
robot.drive();
robot.arm();
robot.intake();
robot.imu();

⸻

9. Robot Configuration

Robot-specific configuration should be separated from framework code.

Example:

public final class RobotConfig {
    public static final class Drive {
        public static final double WHEEL_DIAMETER_MM = 96.0;
        public static final double GEAR_RATIO = 1.0;
        public static final double TICKS_PER_REV = 537.7;
    }
    public static final class Arm {
        public static final int HOME = 0;
        public static final int LOW = 300;
        public static final int HIGH = 850;
    }
}

CurioControl itself should never contain competition-specific constants.

⸻

10. Hardware Abstraction

CurioControl should provide lightweight wrappers around common FTC hardware.

Initial hardware support:

Motor
Servo
ContinuousServo
Encoder
IMU
DigitalSensor
AnalogSensor

Example:

Motor arm = robot.motor("arm");
arm.setPower(0.5);
arm.setVelocity(1000);
int position = arm.getPosition();

The wrappers should internally use FTC SDK classes such as:

DcMotorEx
Servo
CRServo
IMU

Direct SDK access should remain possible.

⸻

11. Motor API

The Motor abstraction should support:

setPower(double power);
setVelocity(double velocity);
setTargetPosition(int position);
getPower();
getVelocity();
getPosition();
resetEncoder();
setDirection(...);
setZeroPowerBehavior(...);
setRunMode(...);

The API should validate invalid values where practical.

For example:

Power:
[-1.0, 1.0]

⸻

12. Servo API

Example:

ServoEx claw = robot.servo("claw");
claw.setPosition(0.5);

Optional convenience functionality:

claw.open();
claw.close();

Mechanism-specific methods such as open() and close() should preferably live in a subsystem rather than the generic servo wrapper.

⸻

13. Encoders

Provide a consistent encoder abstraction.

Example:

Encoder encoder = robot.encoder("leftEncoder");
int ticks = encoder.getPosition();
double velocity = encoder.getVelocity();

Support:

* Position
* Velocity
* Reset
* Direction
* Conversion to distance

Example:

double distance = encoder.getDistance(
    RobotConfig.Drive.TICKS_PER_REV,
    RobotConfig.Drive.WHEEL_DIAMETER_MM
);

⸻

14. Control

The control package contains reusable control algorithms.

Initial components:

PIDController
PIDFController
Feedforward
MotionProfile

⸻

15. PID Controller

Example:

PIDController pid = new PIDController(
    0.01,
    0.0,
    0.001
);

Usage:

double output = pid.calculate(
    targetPosition,
    currentPosition
);
motor.setPower(output);

Required features:

* P
* I
* D
* Delta time
* Integral accumulation
* Integral limits
* Output limits
* Error calculation
* Tolerance
* Reset

Example:

pid.setOutputLimits(-1.0, 1.0);
pid.setIntegralLimit(0.25);
if (pid.atSetpoint()) {
    ...
}

⸻

16. PIDF Controller

Support:

PID + Feedforward

Conceptually:

output =
    kP * error
  + kI * integral
  + kD * derivative
  + kF * feedforward

The implementation should be independent of FTC hardware.

This allows it to be unit-tested on a normal JVM.

⸻

17. Feedforward

Provide reusable feedforward models.

Initial support:

Constant
Velocity
Acceleration
Gravity

Potential combined model:

kS + kV * velocity + kA * acceleration

⸻

18. Motion Profiles

Provide basic motion profiling.

Initial target:

Trapezoidal motion profile

Parameters:

MotionProfile profile = new MotionProfile(
    start,
    target,
    maxVelocity,
    maxAcceleration
);

The profile should provide:

getPosition(time);
getVelocity(time);
getAcceleration(time);
isFinished(time);

⸻

19. Drive

The drive package provides generic drivetrain abstractions.

Initial implementations:

DriveBase
MecanumDrive
TankDrive

⸻

20. Mecanum Drive

Example:

robot.drive().mecanum(
    strafe,
    forward,
    rotation
);

The implementation should handle:

* Wheel power calculation
* Power normalization
* Direction configuration
* Optional field-centric control

Example:

robot.drive().fieldCentric(
    strafe,
    forward,
    rotation,
    robot.imu().heading()
);

⸻

21. Pose and Geometry

Provide basic robotics mathematics.

Initial types:

Pose2d
Vector2d
Rotation2d
Transform2d

Example:

Pose2d pose = new Pose2d(
    24.0,
    12.0,
    Math.toRadians(90)
);

These classes should contain no FTC-specific dependencies.

⸻

22. Subsystems

Every major robot mechanism should be represented by a subsystem.

Example:

public class Arm extends Subsystem {
    private final Motor motor;
    public Arm(HardwareMap hardwareMap) {
        motor = new Motor(hardwareMap, "arm");
    }
    public void moveTo(int position) {
        motor.setTargetPosition(position);
    }
}

Usage:

robot.arm().moveTo(RobotConfig.Arm.HIGH);

Subsystems should:

* Own their hardware.
* Own mechanism-specific logic.
* Expose a clean public API.
* Avoid leaking implementation details.

⸻

23. Command System

CurioControl should eventually provide a lightweight command-based architecture.

Core types:

Command
CommandScheduler
SequentialCommand
ParallelCommand
InstantCommand
WaitCommand

Example:

new SequentialCommand(
    new MoveArmCommand(robot.arm(), 500),
    new IntakeCommand(robot.intake(), 1.0),
    new MoveArmCommand(robot.arm(), 0)
);

The command system should support:

* Scheduling
* Cancellation
* Completion
* Sequential execution
* Parallel execution
* Requirements
* Subsystem ownership

⸻

24. Autonomous State Machines

CurioControl should provide a lightweight state-machine abstraction.

Example:

enum AutoState {
    DRIVE_TO_SCORE,
    SCORE,
    RETURN,
    PARK
}

State machines should support:

Current state
Transitions
Entry actions
Update actions
Exit actions
Timeouts

Commands and state machines may coexist.

Neither should be mandatory.

⸻

25. OpMode Integration

CurioControl should provide optional base classes.

Example:

@TeleOp
public class MainTeleOp extends CurioOpMode {
    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);
    }
    @Override
    public void runRobot() {
        robot.drive().mecanum(
            gamepad1.left_stick_x,
            gamepad1.left_stick_y,
            gamepad1.right_stick_x
        );
    }
}

However, users should still be able to use normal FTC OpMode and LinearOpMode classes.

⸻

26. Telemetry

CurioControl should provide a telemetry abstraction.

Example:

robot.telemetry()
    .add("Heading", heading)
    .add("Arm", armPosition)
    .add("Battery", voltage)
    .update();

Support categories:

DRIVE
ARM
INTAKE
VISION
SYSTEM
DEBUG

Optional debug mode:

CurioConfig.DEBUG = true;

⸻

27. Logging

CurioControl should provide optional structured logging.

Example:

logger.record("heading", heading);
logger.record("armPosition", arm.getPosition());
logger.record("motorPower", motor.getPower());

Output should support CSV.

Example:

time,heading,x,y,armPosition,motorPower
0.00,0.2,0,0,0,0
0.02,0.4,0.1,0,15,0.5
0.04,0.7,0.3,0,30,0.5

Logging must be:

* Optional
* Low overhead
* Disabled by default unless enabled
* Safe for competition use

⸻

28. Vision

Vision should be modular.

Initial abstraction:

VisionManager
AprilTagManager

Example:

AprilTagManager tags =
    robot.vision().aprilTags();

Query:

Optional<AprilTagDetection> tag =
    tags.getTag(20);

Future functionality:

Pose2d pose = tags.getRobotPose();

Vision implementations should avoid forcing vision dependencies on users who do not need them.

⸻

29. Math

The math package should contain pure Java implementations wherever possible.

Examples:

Pose2d
Vector2d
Rotation2d
Interpolation
Clamp
Units
Geometry

These classes should have no Android or FTC SDK dependencies.

This allows extensive JVM unit testing.

⸻

30. Utility Functions

Useful generic utilities may include:

Range
Timer
RateLimiter
Debouncer
EdgeDetector
Units

Utilities should only be added when they solve recurring problems.

Avoid turning util into a dumping ground.

⸻

31. Dependency Philosophy

CurioControl should minimize dependencies.

Preferred dependency hierarchy:

CurioControl
    ↓
FTC SDK

Additional dependencies should only be introduced when there is a clear benefit.

Pure control/math functionality should avoid Android dependencies wherever possible.

⸻

32. Testing

Anything that does not require physical robot hardware should be unit-testable.

Priority test targets:

PIDController
PIDFController
MotionProfile
Feedforward
Pose2d
Vector2d
CommandScheduler
StateMachine
Drive calculations
Utility classes

Example:

@Test
void pidReachesTarget() {
    ...
}

Hardware-dependent functionality should use mocks where practical.

⸻

33. CI/CD

GitHub Actions should automatically run on:

Pull Request
Push
Release

Pipeline:

Checkout
   ↓
Compile
   ↓
Unit Tests
   ↓
Static Checks
   ↓
Build
   ↓
Publish

A pull request should not be mergeable if the library does not compile or its unit tests fail.

⸻

34. Gradle Publishing

CurioControl should be distributed as a Maven-compatible package.

Initial repository:

GitHub Packages

Future option:

Maven Central

Example dependency:

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Curio-One/CurioControl")
    }
}
dependencies {
    implementation("dev.curio:curiocontrol:1.0.0")
}

Authentication should be handled through Gradle properties/environment variables and should never be committed to the repository.

⸻

35. Versioning

CurioControl uses Semantic Versioning:

MAJOR.MINOR.PATCH

Example:

1.2.3
│ │ │
│ │ └── Bug fix
│ └──── Backwards-compatible feature
└────── Breaking API change

Examples:

1.0.0 → 1.0.1

Bug fix.

1.0.1 → 1.1.0

New backwards-compatible functionality.

1.1.0 → 2.0.0

Breaking API change.

⸻

36. Version Pinning

Robot projects must depend on a specific version.

Recommended:

implementation("dev.curio:curiocontrol:1.2.0")

Avoid:

implementation("dev.curio:curiocontrol:latest")

or dependencies on unreleased main builds.

Competition robot code should remain reproducible.

⸻

37. Compatibility

Every release must document:

CurioControl version
FTC SDK version
Android SDK version
Java version
Gradle version

Example:

CurioControl 1.0.0
FTC SDK: <version>
Java: <version>
Android SDK: <version>
Gradle: <version>

CurioControl should explicitly support the FTC SDK version used by the team’s Robot Controller project.

⸻

38. API Stability

Public APIs should be treated as stable once released under 1.x.

Before 1.0.0:

API may change freely.

After 1.0.0:

Breaking API changes require a major version.

Internal implementation details should not be exposed unnecessarily.

⸻

39. Documentation

Every public class should have JavaDoc.

Example:

/**
 * PID controller for closed-loop control.
 *
 * @param kP proportional coefficient
 * @param kI integral coefficient
 * @param kD derivative coefficient
 */
public class PIDController {
    ...
}

The repository should contain:

README.md
SPEC.md
CHANGELOG.md
CONTRIBUTING.md
LICENSE

Optional:

docs/
examples/

⸻

40. Example Project

A CurioControl-based robot should ideally look like:

RobotCode/
│
├── TeamCode/
│   └── src/main/java/
│       └── org/curioone/robot/
│
│           ├── opmode/
│           │   ├── MainTeleOp.java
│           │   └── BlueAuto.java
│           │
│           ├── subsystem/
│           │   ├── Arm.java
│           │   ├── Intake.java
│           │   └── Drive.java
│           │
│           └── config/
│               └── RobotConfig.java
│
└── build.gradle

The competition repository should contain robot-specific code, while CurioControl contains reusable framework code.

⸻

41. Example TeleOp

A finished TeleOp should ideally be relatively small.

@TeleOp(name = "Main TeleOp")
public class MainTeleOp extends CurioOpMode {
    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);
        robot.registerSubsystem(
            new Arm(hardwareMap),
            new Intake(hardwareMap)
        );
    }
    @Override
    public void runRobot() {
        robot.drive().mecanum(
            gamepad1.left_stick_x,
            gamepad1.left_stick_y,
            gamepad1.right_stick_x
        );
        if (gamepad1.a) {
            robot.arm().moveTo(RobotConfig.Arm.HIGH);
        }
        if (gamepad1.b) {
            robot.arm().moveTo(RobotConfig.Arm.HOME);
        }
    }
}

The goal is for an OpMode to describe what the robot should do, rather than how every individual motor and sensor works.

⸻

42. Example Autonomous

Example:

@Autonomous(name = "Blue Auto")
public class BlueAuto extends CurioAuto {
    @Override
    public void buildRoutine() {
        schedule(
            new SequentialCommand(
                new DriveForwardCommand(robot.drive(), 24),
                new MoveArmCommand(robot.arm(), RobotConfig.Arm.HIGH),
                new ScoreCommand(robot.intake()),
                new MoveArmCommand(robot.arm(), RobotConfig.Arm.HOME)
            )
        );
    }
}

⸻

43. Design Principles

CurioControl should follow these principles:

Simple

Common operations should require little code.

Modular

Components should be usable independently.

Testable

Pure logic should be testable without robot hardware.

Explicit

Avoid excessive magic or hidden behavior.

Lightweight

FTC hardware is resource-constrained.

Extensible

Teams should be able to build their own components on top of CurioControl.

Backwards-compatible

Stable releases should not unexpectedly break robot code.

Hardware-independent where possible

Control and math logic should not depend directly on FTC hardware.

⸻

44. Security

The library should:

* Never store credentials.
* Never transmit robot data externally by default.
* Never require cloud connectivity during operation.
* Avoid unnecessary network dependencies.
* Keep GitHub authentication outside source code.
* Never commit GitHub tokens or credentials.

⸻

45. Performance

CurioControl should avoid unnecessary allocations inside robot loops.

Particular attention should be given to:

OpMode loop
CommandScheduler
PID calculations
Telemetry
Vision processing
Drive calculations

Real-time robot loops should avoid creating unnecessary objects every iteration.

⸻

46. Competition Safety

CurioControl should fail predictably.

Hardware initialization errors should be reported clearly.

Example:

[CurioControl] ERROR
Missing hardware device: armMotor
Expected configuration name: "arm"

The framework should not silently substitute missing hardware.

⸻

47. Initial Release Roadmap

v0.1.0 — Foundation

[ ] Project structure
[ ] Gradle publishing
[ ] CurioRobot
[ ] Motor
[ ] Servo
[ ] Encoder
[ ] TelemetryManager
[ ] PIDController
[ ] MecanumDrive
[ ] Basic unit tests
[ ] GitHub Actions

v0.2.0 — Control

[ ] PIDFController
[ ] Feedforward
[ ] MotionProfile
[ ] IMU abstraction
[ ] Pose2d
[ ] Vector2d
[ ] Logging

v0.3.0 — Architecture

[ ] Subsystem
[ ] Command
[ ] CommandScheduler
[ ] SequentialCommand
[ ] ParallelCommand
[ ] StateMachine

v0.4.0 — Vision

[ ] VisionManager
[ ] AprilTagManager
[ ] AprilTag pose utilities

v1.0.0 — Stable

[ ] Stable public API
[ ] Complete documentation
[ ] Full unit-test coverage of pure logic
[ ] CI/CD
[ ] Versioned Maven package
[ ] Example robot project
[ ] Migration documentation

⸻

48. Future Possibilities

Potential future modules:

CurioControl
├── Simulation
├── Path Planning
├── Localization
├── Trajectory Following
├── Dashboard
├── Configuration UI
├── Robot Log Viewer
├── Tuning Tools
└── FTC Scouting Integration

These should not be implemented until the core framework is stable.

⸻

49. Definition of Done

A CurioControl feature is considered complete when:

* [ ]	Public API is documented.
* [ ]	Unit tests exist where applicable.
* [ ]	No unnecessary FTC coupling exists.
* [ ]	Code follows project conventions.
* [ ]	Existing functionality remains compatible.
* [ ]	CI passes.
* [ ]	Example usage is documented.
* [ ]	Changelog is updated.
* [ ]	Version is appropriate for the change.

⸻

50. Guiding Principle

CurioControl should make FTC programming feel like building a robot rather than repeatedly building the software infrastructure required to control one.

┌─────────────────────────────────────┐
│             CURIO ONE               │
│                                     │
│          Competition Robot          │
│                 │                   │
│                 ▼                   │
│           CurioControl              │
│                 │                   │
│        ┌────────┼────────┐          │
│        ▼        ▼        ▼          │
│     Control  Hardware  Vision       │
│        │        │        │          │
│        └────────┼────────┘          │
│                 ▼                   │
│              FTC SDK                │
│                 ▼                   │
│              ROBOT                  │
└─────────────────────────────────────┘

CurioControl should be the common software foundation that lets Curio One’s developers focus on robot behavior, strategy, and engineering rather than rewriting the same infrastructure every season.