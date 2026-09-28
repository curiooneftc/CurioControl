# Architecture

> Status: the layering described here is enforced by tests and is stable. The specific classes
> arrive in the releases listed in [the phases document](../PHASES.md).

## The layer cake

```text
Application          OpModes, autonomous routines, driver UI
        |
        v
core                 CurioRobot, CurioConfig, Subsystem, HardwareRegistry
        |
        +--> hardware     Motor, Servo, Encoder, IMU
        +--> drive        DriveBase, MecanumDrive, TankDrive
        +--> command      Command, CommandScheduler, StateMachine
        +--> telemetry    TelemetryManager, Logger
        +--> vision       VisionManager, AprilTagManager  (optional, lazy)
        |
        v
FTC SDK              com.qualcomm.robotcore.*
        |
        v
Android / Hardware
```

The dependency direction is strictly downward. Lower layers never know about higher ones, which is
what keeps the framework understandable and each piece usable on its own.

## Package rules

```text
core, math, util   ->  (nothing internal)
control            ->  math, util
hardware           ->  core, math, util
drive              ->  hardware, control, math, util
command            ->  core, util
telemetry          ->  core, util
vision             ->  hardware, math, util
```

Two rules that are not visible in that table:

- `math`, `control`, and `util` import **no** FTC (`com.qualcomm.*`) or `android.*` types. This is
  what makes the control algorithms unit-testable on a desktop JVM.
- Nothing imports `vision` except `vision` itself. A team that never calls `robot.vision()` pays
  no vision cost.

These are not conventions. They are ArchUnit tests, and a violation fails the build immediately.

## Fail predictably

When a device is missing from the hardware configuration, the framework reports it clearly and
stops:

```text
[CurioControl] ERROR
Missing hardware device: armMotor
Expected configuration name: "arm"
```

It never silently substitutes a different device. A robot that mysteriously drives with one wheel
inverted is much harder to debug than one that refuses to initialize.

## Where to go next

- [Hardware independence](hardware-independence.md) — why the pure-Java boundary matters
- [Philosophy](philosophy.md) — the principles behind the design
- [Architecture decisions](adr/README.md) — the reasoning, recorded
