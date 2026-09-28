# Control Tuning

> Status: this page describes the target v1.0.0 API. `PIDController` lands in v0.1.0, `PIDF` and
> motion profiles in v0.2.0; see [the phases document](../../PHASES.md).

A PID loop turns "I want position X" into "apply this much power, re-evaluated every loop". It is
the workhorse of FTC: arms, lifts, shooters, and anything that has to hold a position while the
robot is moving.

## The idea

Each loop, compute the error between target and current, and combine three terms:

| Term | Asks | Symptom if too high |
|---|---|---|
| **P** — proportional | "Close the gap now" | Oscillation, overshoot |
| **I** — integral | "Cancel out anything I keep missing" | Windup, sluggish recovery from load |
| **D** — derivative | "Slow down as I approach" | Noise amplification, twitchy behavior |

```java
PIDController pid = new PIDController(0.01, 0.0, 0.001);

void run() {
    double output = pid.calculate(targetTicks, motor.getPosition());
    motor.setPower(output);
}
```

The controller needs two numbers. It has no idea what a motor is — that is what makes it
testable and reusable.

## Tuning order

Tune in this order. It is the fastest path to something that works, because each term's job
depends on the one before it being right.

**1. P only.** Set `kI` and `kD` to zero. Increase `kP` until the mechanism overshoots and
oscillates, then back off by half. It should move briskly without shaking.

**2. Add I.** The mechanism now reaches the target under load but slowly gets there — a lift
sags, an arm creeps. Add a small `kI` and increase until it settles briskly. Stop when the
mechanism starts overshooting *after* settling; that is windup.

**3. Add D.** A little `kD` damps overshoot and makes the approach smoother. Small values only.
Too much amplifies encoder noise into visible jitter.

Then set a tolerance:

```java
pid.setOutputLimits(-1.0, 1.0);
pid.setIntegralLimit(0.25);     // anti-windup bound
pid.setTolerance(2.0);          // ticks

if (pid.atSetpoint()) {
    // hold position, brake, or move on
}
```

## The two things that bite

**Integral windup.** While the motor is saturated at full power, the error keeps accumulating.
By the time the mechanism reaches the target, the integrator holds an enormous charge and drives
it straight past. `setIntegralLimit` bounds this. Set it if your mechanism ever overshoots badly
after a long move.

**Variable loop timing.** FTC loop times vary — often substantially. A controller that assumes a
fixed `dt` produces an integral term that scales with how busy the loop is, so a PID tuned on an
idle robot behaves differently during a match. CurioControl's controllers take delta time from a
`Clock` so the behavior is the same regardless of loop rate.

## Time injection

Time-dependent components read time through `org.curioone.control.util.Clock` rather than calling
`System.nanoTime()` directly. In production that is a real clock; in tests it is a fake you
advance by hand, so a test can exercise ten seconds of controller behavior instantly and without
flakiness.

```java
FakeClock clock = new FakeClock();
PIDController pid = new PIDController(kP, kI, kD, clock);

pid.calculate(100, 0);
clock.advanceMillis(20);
pid.calculate(100, 5);   // exactly 20ms of integral, every time
```

## PIDF

`PIDFController` adds a feedforward term — a prediction of the output needed before any error
exists:

```text
output = kP·error + kI·integral + kD·derivative + kF·feedforward
```

This eliminates steady-state error that feedforward can address, and it is usually the single
biggest tuning improvement available on a drivetrain follower or a flywheel. See
[ADR and the phases document](../../PHASES.md) for the v0.2.0 milestone.

## Next

- [Motion profiles](motion-profiles.md) — for smooth, speed-limited motion
- [Robot config](robot-config.md) — where gains and physical constants belong
