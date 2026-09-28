# Drivetrains

> Status: this page describes the target v1.0.0 API. The `drive` package lands in v0.1.0; see
> [the phases document](../../PHASES.md).

CurioControl provides a generic drivetrain contract and two implementations. The abstraction covers
kinematics and power distribution; it does not decide what kind of robot you have.

## Mecanum

```java
robot.drive().mecanum(
        gamepad1.left_stick_x,   // strafe  (-1 left,  1 right)
        gamepad1.left_stick_y,   // forward (-1 back,  1 forward)
        gamepad1.right_stick_x); // rotate (-1 left,  1 right)
```

That is the common case: translation in two axes plus rotation, from three sticks.

### Normalization

Raw wheel-power math produces a number that can exceed `[-1.0, 1.0]` when you combine a full-power
strafe with a full-power rotation. Clamping each motor independently distorts the requested
direction — the robot drifts.

CurioControl scales all four wheel powers by a common factor so the *direction* is preserved and
only the magnitude is reduced. A full diagonal becomes a slower diagonal rather than a skewed one.

This is the single most common quality difference between a drivetrain that "feels right" and one
that fights the driver.

### Direction configuration

Mecanum wheels are directional. A mirrored motor makes the robot spin instead of strafe, and the
symptom looks like a broken controller rather than a configuration mistake.

```java
MecanumDrive drive = robot.drive().mecanum();
drive.setLeftFrontInverted(true);
```

Direction lives in your robot's configuration, not in the framework.

## Tank

```java
robot.drive().tank(
        gamepad1.left_stick_y,
        gamepad1.right_stick_y);
```

Straightforward: left power and right power. Differential steering handles the turning.

## Field-centric

Robot-centric means "forward is where the robot is pointing". Field-centric means "forward is where
the field is". The second is what most drivers expect.

```java
robot.drive().fieldCentric(
        gamepad1.left_stick_x,
        gamepad1.left_stick_y,
        gamepad1.right_stick_x,
        robot.imu().heading());
```

The heading is passed in rather than read from an IMU inside the drive class. That keeps `drive`
free of SDK types and makes the rotation math testable without hardware — see
[hardware independence](../concepts/hardware-independence.md).

!!! warning "Reset the IMU between runs"
    Field-centric drive is only as good as the heading it is given. Reset it at the start of every
    autonomous. A stale offset shows up as a robot that consistently drifts a few degrees, which is
    easy to misread as a motor problem.

## Always stop cleanly

```java
robot.drive().stop();
```

Call it when the OpMode ends or a subsystem takes exclusive control. A drivetrain left at full
power after an OpMode stops is the difference between a robot that sits still and one that keeps
driving into the wall.

## Next

- [Control tuning](control-tuning.md) — for arms, lifts, and anything that needs a P loop
- [Hardware abstraction](hardware-abstraction.md) — the motors underneath
