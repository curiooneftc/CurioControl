# Odometry and Go-To-Pose

> Status: `MecanumOdometry`, `TankOdometry`, and `PoseController` are unreleased
> additions on top of v0.4.0; see [the phases document](../../PHASES.md).

Wheel odometry answers "where am I" between tag sightings; the pose controller turns
"where am I and where to" into drive powers. Together with an occasional AprilTag
solve, they are the complete autonomous localization loop.

## Tracking the pose

```java
MecanumOdometry odometry = new MecanumOdometry(
        robot.encoder("fl"), robot.encoder("fr"),
        robot.encoder("bl"), robot.encoder("br"),
        robot.imu(), TICKS_PER_REV, WHEEL_DIAMETER_MM);
// ... per loop:
Pose2d pose = odometry.update();
```

The gyroscope is the heading authority; the wheels vote on translation only. That
division is deliberate: wheel-derived rotation needs a calibrated track width and
drifts faster, while translation from encoders is honest dead reckoning. Units are
inches everywhere — same as the vision layout, so tag solves and odometry poses mix
without conversion.

Three things to get right:

1. **Start pose.** Construct with the autonomous start (or `resetPose` immediately).
   A robot that holds the origin for one loop before teleporting plots a phantom jump.
2. **Encoder direction.** An inverted drivetrain motor inverts its encoder view too.
   Drive forward by hand and watch the pose: if it goes backwards, a wheel's direction
   is wrong, not the odometry.
3. **Slip is real.** Odometry measures where the wheels went, including pushes and
   bumps. Re-anchor with `resetPose(tagPose)` on confident tag solves; never average
   the two — a solve is absolute, odometry is relative, and the average of those is
   neither.

## Driving to a pose

```java
PoseController goTo = new PoseController(0.05, 0.02, 1.0, Math.toRadians(3.0));
// ... per loop:
PoseController.DriveSignal signal = goTo.calculate(odometry.getPose(), target);
drive.mecanum(signal.strafe(), signal.forward(), signal.rotation());
if (goTo.atTarget(odometry.getPose(), target)) {
    // next step
}
```

Proportional only: full speed from far away, easing off on arrival, saturated at full
power. When integral action or profile tracking is needed, graduate to `PIDController`
pairs on the same errors — and confirm the geometry here first, before the gains get
fancy. Note the turn sign is handled inside: a positive heading error commands
negative rotation, because framework headings increase toward field-right while
positive rotation turns counter-clockwise.

## Validation (needs a floor and a tape measure)

- [ ] Push the robot by hand along a taped square: the tracked pose follows the tape
      within a few percent per side, and returns near the start.
- [ ] Spin in place: position stays put while heading tracks the turn.
- [ ] Ten feet forward reads ten feet, not nine — else the wheel diameter or ticks
      constant is wrong.
- [ ] A tag solve mid-run and the odometry pose agree within the expected slip
      before re-anchoring.

## Next

- [Commands vs. states](commands-vs-states.md) — composing go-to-pose steps into routines
- [Vision](vision.md) — tag solves to re-anchor against
- [Bench testing](bench-testing.md) — the procedures these checks slot into
