# Drivetrains

CurioControl provides a generic drivetrain contract and two implementations. The abstraction covers
kinematics and power distribution; it does not decide what kind of robot you have.

## Mecanum

`robot.drive()` returns a mecanum base built from the conventional motor names `frontLeft`,
`frontRight`, `backLeft`, and `backRight`. Call `drive(...)` each loop:

```java
robot.drive().drive(
        gamepad1.left_stick_x,   // strafe  (-1 left,  1 right)
        gamepad1.left_stick_y,   // forward (-1 back,  1 forward)
        gamepad1.right_stick_x); // rotate (-1 left,  1 right)
```

That is the common case: translation in two axes plus rotation, from three sticks.

If your motors are named differently, build the base explicitly and keep the reference:

```java
MecanumDrive drive = robot.mecanumDrive("fl", "fr", "bl", "br");
drive.drive(x, y, r);
```

`drive(...)` and `mecanum(...)` are the same method under two names; the type-specific name is
what you reach for when you hold a `MecanumDrive` directly.

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
MecanumDrive drive = robot.mecanumDrive("fl", "fr", "bl", "br");
drive.setFrontLeftInverted(true);
```

Direction lives in your robot's configuration, not in the framework.

## Tank

Install a `TankDrive` before the first call to `robot.drive()`:

```java
robot.setDrive(robot.tankDrive("left", "right"));
robot.drive().drive(0.0, gamepad1.left_stick_y, gamepad1.right_stick_y);
```

Or drive it directly, which is clearer when you have the reference:

```java
TankDrive drive = robot.tankDrive("left", "right");
drive.tank(gamepad1.left_stick_y, gamepad1.right_stick_y);
```

Straightforward: left power and right power. Differential steering handles the turning.

A tank chassis cannot strafe. `drive(strafe, forward, rotation)` **discards** the strafe axis rather
than pretending to strafe — you get a turn where you asked for a slide, which is a visible bug
rather than a silent one.

## Field-centric

Robot-centric means "forward is where the robot is pointing". Field-centric means "forward is where
the field is". The second is what most drivers expect.

```java
MecanumDrive drive = robot.mecanumDrive("fl", "fr", "bl", "br");

drive.fieldCentric(
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
