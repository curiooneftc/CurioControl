# Hardware Abstraction

> Status: this page describes the target v1.0.0 API. The `hardware` package lands in v0.1.0; see
> [the phases document](../../PHASES.md).

CurioControl wraps common FTC hardware in thin classes that validate arguments, document units,
and delegate to the SDK. They **wrap** the SDK; they never hide it.

## The universal escape hatch

Every wrapper exposes the underlying SDK object:

```java
Motor arm = robot.motor("arm");
DcMotorEx sdkMotor = arm.getSdkObject();
```

If a wrapper does not cover something you need, reach the SDK directly rather than forking the
wrapper. That is the whole point of the design: the framework reduces boilerplate, it does not
build a wall.

## Motor

```java
Motor arm = robot.motor("arm");

arm.setPower(0.5);              // [-1.0, 1.0]
arm.setVelocity(1000);          // ticks/second
arm.setTargetPosition(500);     // ticks
arm.setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);
arm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
arm.setDirection(DcMotorSimple.Direction.REVERSE);

int position = arm.getPosition();
double velocity = arm.getVelocity();
double power = arm.getPower();
arm.resetEncoder();
```

Power outside `[-1.0, 1.0]` is rejected rather than silently clamped. A typo that doubles motor
power is a bug you want to hear about, not one you want to discover on the field.

All of these are available without CurioControl — the wrapper's value is validation, consistent
units, and one place to change if the SDK shifts underneath.

## Servo

```java
Servo claw = robot.servo("claw");
claw.setPosition(0.5);   // [0.0, 1.0]
```

A generic servo wrapper has **no** `open()` or `close()`. Those are mechanism semantics, and they
belong on the subsystem that owns the mechanism:

```java
public class Claw extends Subsystem {
    public void open()  { servo.setPosition(RobotConfig.Claw.OPEN);  }
    public void close() { servo.setPosition(RobotConfig.Claw.CLOSE); }
}
```

The distinction matters: a servo can be a claw, a wrist, a release, or a deploy arm. The framework
does not know which, and guessing would put the wrong names in your code.

There is also `ContinuousServo` for `CRServo` devices that take a power instead of a position.

## Encoder

```java
Encoder encoder = robot.encoder("leftEncoder");

int ticks = encoder.getPosition();
double ticksPerSecond = encoder.getVelocity();
encoder.reset();
```

Raw ticks are rarely what you want, so conversion takes the physical constants as parameters:

```java
double distanceMm = encoder.getDistance(
        RobotConfig.Drive.TICKS_PER_REV,
        RobotConfig.Drive.WHEEL_DIAMETER_MM);
```

The parameters are arguments, not framework constants. The framework does not know your wheel
diameter, and never will — that value is what makes a robot a specific robot.

## IMU

```java
IMU imu = robot.imu();

double heading = imu.heading();   // radians, field-relative after reset
imu.resetHeading();
```

Remember to reset the heading between autonomous runs. A stale IMU offset is one of the most
common causes of an autonomous that works in practice and fails in a match.

## Fail loudly

A missing device is reported by name, with the configuration name that was expected:

```text
[CurioControl] ERROR
Missing hardware device: armMotor
Expected configuration name: "arm"
```

The framework never silently substitutes another device. A robot that drives with one motor
inverted is far harder to diagnose than one that refuses to start.

## Next

- [Drivetrains](drivetrains.md) — turning four motors into a robot that goes where you point it
- [Control tuning](control-tuning.md) — closed-loop control on a motor
