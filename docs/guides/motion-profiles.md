# Motion Profiles

> Status: `TrapezoidalMotionProfile` landed in v0.2.0; see
> [the phases document](../../PHASES.md).

A step change in setpoint asks a mechanism for infinite acceleration at the first instant.
That is why a well-tuned PID still overshoots on long moves: the loop is chasing a target
the mechanism cannot physically reach yet. A motion profile replaces the step with a
trajectory that respects the mechanism's limits, and the loop tracks the trajectory
instead.

## The trapezoid

Accelerate at the limit, cruise at the limit, brake at the limit:

```java
TrapezoidalMotionProfile profile = new TrapezoidalMotionProfile(
        0.0, 1000.0, new TrapezoidalMotionProfile.Constraints(2000.0, 4000.0));

void run() {
    double t = elapsedSeconds();
    double position = profile.getPosition(t);
    double velocity = profile.getVelocity(t);
    if (profile.isFinished(t)) {
        // hold position, brake, or move on
    }
}
```

Short moves never reach cruise velocity. The profile degrades to a triangle with a lower
peak rather than violating the acceleration limit — there is no minimum distance, and a
zero-distance profile is finished immediately. Reverse moves mirror forward ones exactly.

## Wiring it to a loop

Feed each reference to the term that owns it: position to the feedback controller,
velocity and acceleration to the feedforward model.

```java
PIDController pid = new PIDController(kP, kI, kD);
CombinedFeedforward ff = new CombinedFeedforward(kS, kV, kA);

void run() {
    double t = elapsedSeconds();
    double output = pid.calculate(profile.getPosition(t), motor.getPosition())
            + ff.calculate(0.0, profile.getVelocity(t), profile.getAcceleration(t));
    motor.setPower(output);
}
```

The acceleration reference is the term that makes a profile track instead of lagging
behind it: without `kA`, the start of the move and the braking at the end run purely on
feedback error.

## Choosing limits

Start conservative — half the velocity and acceleration you think the mechanism can do —
and raise them until tracking degrades, then back off. The limits belong in the robot's
configuration next to the gains (`RobotConfig`), never inline at the call site: they are
physical properties of the mechanism, and two profiles for the same mechanism must agree
on them.

## Next

- [Control tuning](control-tuning.md) — PID, feedforward gains, and the tuning order
- [Robot config](robot-config.md) — where gains and physical constants belong
