# Bench Testing

> Status: procedures for the v0.1.0 drivetrains and the v0.2.0 IMU work. No special
> hardware beyond the robot, a flat floor, and tape.

Unit tests prove the math. These procedures prove the robot. Run them in order: each one
assumes the earlier ones pass, so a failure points at the step you just ran rather than at
"the drive" in general. Budget thirty minutes and a charged battery — a brownout halfway
through invalidates the drift checks.

## Drivetrain

### 1. Wheels off the ground: direction

Lift the robot. Push each stick axis to full and confirm every wheel spins the way the
wheel equations say: forward drives all four the same way, strafe-right spins
front-left and back-right forward with the other pair reversed, rotation spins the left
pair against the right pair. A wheel going the wrong way is a `setInverted` fix in the
robot's configuration, never in the framework.

### 2. Wheels off the ground: saturation

Hold full strafe plus full rotation. Every motor must stay within power — the
normalization scales the whole diagonal down instead of clamping wheels individually.
Listen for it: a saturated diagonal sounds uniformly loaded, not like one motor
fighting the rest.

### 3. Straight line, robot-centric

Tape a start line. Drive forward ten feet, robot-centric, no steering correction. Mark
where it stops, repeat three times. Consistent drift to one side is a weight imbalance
or a dragging wheel; random spread is normal mecanum slip. If the robot rotates instead
of translating, one wheel is inverted — back to step 1.

### 4. Field-centric sanity

Reset the heading facing field-forward. Push forward: the robot drives field-forward
regardless of which way it starts pointing. Rotate the robot ninety degrees by hand
(power off, or in a disabled OpMode), push forward again: still field-forward. If it
drives sideways instead, the heading sign reaching `fieldCentric` is inverted — check
what the IMU reports against a known rotation before touching the drive code.

## IMU

### 5. Calibration

Keep the robot still and run `requireCalibration` with the season's mounting parameters.
A failure here is mounting parameters that do not match the physical install, or a
robot that was not still — both bench-time problems, which is the point of failing
loudly instead of driving on a bad offset.

### 6. Heading reset discipline

Reset the heading at the start of every OpMode, autonomous included. Verify by
resetting, rotating the robot a quarter turn, and reading `heading()`: it must report
the quarter turn, not zero and not the pre-reset value. An unreset IMU makes
field-centric drive drift by a fixed offset, which misreads exactly like a motor
problem.

### 7. Wrap and continuity

Spin the robot slowly through a full turn while logging `heading()` and
`headingPositive()`. The raw heading passes through ±π without jumping; the positive
variant wraps cleanly into `[0, 2π)`. A discontinuity mid-turn is a mounting or
calibration fault, not a software one.

### 8. Drift check

Leave the robot still for two minutes and log the heading. A few degrees of walk is
normal IMU drift; tens of degrees means recalibrate, and persistent fast drift means
the hub mount is loose or the parameters are wrong.

## Recording results

Log every procedure with the CSV `Logger`: heading, wheel powers, and time, at minimum.
A drift complaint without a log is not actionable — with one, it is a plot. See
[telemetry and logging](telemetry-and-logging.md).
