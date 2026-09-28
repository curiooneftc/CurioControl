# CurioControl Examples

Runnable examples for CurioControl.

> **Status:** the robot project is written in Phase 5 (M5.4), once the API is stable. The
> snippets below are already accurate against the design in `spec.md`; the full project lands with
> the `1.0.0` release. See [`PHASES.md`](../PHASES.md).

## Contents

```text
examples/
├── robot/            A complete, runnable FTC robot project using CurioControl  (Phase 5)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── TeamCode/src/main/java/org/curioone/robot/
│       ├── opmode/{MainTeleOp,BlueAuto}.java
│       ├── subsystem/{Arm,Intake,Drive}.java
│       └── config/RobotConfig.java
└── snippets/         Copy-paste fragments  (Phase 1 onward)
    ├── mecanum-teleop.java
    ├── pid-loop.java
    ├── field-centric-drive.java
    └── telemetry.java
```

## Quick fragments

The snippets in `snippets/` are complete enough to paste into an OpMode.

### A mecanum TeleOp

```java
robot.drive()
        .mecanum(
                gamepad1.left_stick_x,   // strafe
                gamepad1.left_stick_y,   // forward
                gamepad1.right_stick_x); // rotation
```

### A PID loop on a lift

```java
PIDController pid = new PIDController(0.01, 0.0, 0.001);

void run() {
    motor.setPower(pid.calculate(targetTicks, motor.getPosition()));
    if (pid.atSetpoint()) {
        lift.setZeroPower();
    }
}
```

### Field-centric drive

```java
robot.drive().fieldCentric(
        gamepad1.left_stick_x,
        gamepad1.left_stick_y,
        gamepad1.right_stick_x,
        robot.imu().heading());
```

### Telemetry

```java
robot.telemetry()
        .add("Heading", robot.imu().heading())
        .add("Lift", lift.getPosition())
        .add("Battery", voltageSensor.getVoltage())
        .update();
```

## Why snippets are not a full project

Documentation that is not compiled rots. A snippet that nobody builds is a snippet that is wrong
within two releases. The full example project in `examples/robot/` is compiled in CI against the
**published artifact**, so the usage it demonstrates is verified rather than merely plausible.

## See also

- [Quickstart](https://curiooneftc.github.io/CurioControl/getting-started/quickstart/)
- [Guides](https://curiooneftc.github.io/CurioControl/guides/hardware-abstraction/)
