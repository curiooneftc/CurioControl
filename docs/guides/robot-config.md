# Robot Configuration

> Status: the pattern is stable and enforced by design. The specific API lands across the releases
> in [the phases document](../../PHASES.md).

CurioControl contains no competition constants. No wheel diameter, no arm position, no motor name,
no ticks-per-revolution. Those values differ between robots and change between seasons, and baking
them in would make the framework useless the moment you reuse it.

This is not an accident — it is a rule, and the tests would fail if a constant appeared in
`src/main`.

## The pattern

Every competition-specific value lives in one `RobotConfig` class in *your* project:

```java
package org.curioone.robot.config;

public final class RobotConfig {

    private RobotConfig() {}

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
```

## Why it is a single file

A season's tuning session should not require a hunt through the codebase. Every value a team tunes
— wheel diameter, gains, joint positions, timeouts — in one file means one pass, one diff, and one
place to review at a competition.

## How the framework receives these values

As parameters. The API asks for what it needs rather than assuming an answer:

```java
double distanceMm = encoder.getDistance(
        RobotConfig.Drive.TICKS_PER_REV,
        RobotConfig.Drive.WHEEL_DIAMETER_MM);

PIDController armPid = new PIDController(
        RobotConfig.Arm.KP,
        RobotConfig.Arm.KI,
        RobotConfig.Arm.KD);

arm.moveTo(RobotConfig.Arm.HIGH);
```

## The test that keeps it honest

Physical constants belong in test fixtures, never in `src/main`. If a framework test needs a wheel
diameter, the fixture supplies it. A framework class holding a real robot's numbers would be a
layering violation, and the architecture tests treat it as one.

## Practical advice

- **Name the unit.** `WHEEL_DIAMETER_MM`, not `WHEEL_DIAMETER`. A unitless number is a bug waiting
  for the next robot.
- **Tune in one place.** Every gain the team will adjust belongs in `RobotConfig`, not inline in a
  subsystem.
- **Version it with the code.** A robot whose config changed mid-season should be reproducible
  from its git history. That only works if the config is in the repository.

## Next

- [Hardware abstraction](hardware-abstraction.md) — the APIs that take these values
- [Control tuning](control-tuning.md) — the gains you will be tuning
