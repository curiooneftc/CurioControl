# Vision

> Status: `VisionManager`, `AprilTagManager`, and `AimAssist` landed in v0.4.0; see
> [the phases document](../../PHASES.md).

Vision is opt-in twice over: nothing is constructed until `robot.vision()` is called, and
the portal only streams between `init()` and `close()`. A robot that never looks at a tag
carries no camera handle, no background thread, and no extra allocation.

## Wiring it up

```java
public void initRobot() {
    robot = new CurioRobot(hardwareMap);
    vision = robot.vision("Webcam 1");   // conventional name, default AprilTag pipeline
    vision.init();
}

public void runRobot() {
    if (vision.process()) {              // true at most 30 times a second by default
        vision.aprilTags().getRobotPose(layout, cameraOffset).ifPresent(pose -> {
            // drive on it, log it, aim with it
        });
    }
}

public void stopRobot() {
    vision.stop();
    vision.close();                      // releases the camera for the next OpMode
}
```

`robot.vision()` with no arguments uses the conventional `"Webcam 1"`. `attachVision`
installs a custom pipeline instead — the only way to run one, since the portal, the
processor, and the manager are built together and stay together.

## Hardware config and calibration

- **Name the camera** in the Robot Controller configuration exactly as the code asks
  for it. A missing camera fails at `vision(...)` with the expected name in the message —
  bench time, not match time.
- **Lens intrinsics** belong to the SDK, not the framework: calibrate through the SDK's
  camera calibration OpMode for the resolution being streamed. Uncalibrated (or
  wrong-resolution) intrinsics bias every pose solve, and no framework code can detect
  that — the symptom is a robot pose that is consistently off by a scale factor.
- **Mounting** is two numbers the framework cannot know: where the camera sits on the
  robot and which way it points. Measure the offset from the robot's rotation center to
  the lens in inches, and the camera's yaw relative to robot-forward:

```java
Transform2d cameraOffset = new Transform2d(
        new Vector2d(0.0, 6.0),          // six inches ahead of center...
        Rotation2d.fromDegrees(0.0));     // ...facing forward
```

A wrong offset shifts every solve by exactly the error — if the solved pose is always
six inches short in the same direction, the offset is wrong by six inches in that
direction.

## Field layout

```java
TagFieldLayout layout = TagFieldLayout.of(Map.of(
        20, Pose2d.fromHeading(72.0, 36.0, Math.PI),   // position in inches,
        21, Pose2d.fromHeading(72.0, -36.0, Math.PI))); // facing into the field
```

Each entry is the tag's position plus the direction its face points. Use
`ofMillimeters` for metric surveys. Tags the layout does not know are skipped, never
guessed: a frame of unknown tags yields empty, and `getTag(id)` still finds them by id
for bearing-style driving that needs no field pose.

The pose convention, stated once because all three details bite: the SDK reports the
tag in the camera frame as X right, Y forward, in inches, with yaw counter-clockwise —
the framework frame axis-for-axis, with the yaw negated into framework headings. A
parallel tag reports yaw zero while facing the camera, half a turn from the camera's
own facing. `AprilTagManager` documents the mapping; do not re-derive it at call sites.

## Performance

- **Poll at the vision rate, not the loop rate.** `process()` gates to 30 Hz by default;
  only solve inside the `true` branch. Frames arrive far slower than OpMode loops.
- **Lower the ceiling** with `setMaxUpdateRate` until loop timing stops moving. Pose
  solves at 5–10 Hz are plenty for aim assist; full rate is for validation, not matches.
- **Watch `getFps()` and the camera state** after `init()` rather than polling blindly.
  Dropped frames read as stale poses — timestamp-sensitive logic should compare
  `frameAcquisitionNanoTime` across polls.
- **Log, don't guess.** Record detections and solved poses with the CSV logger during
  every validation run; a drift complaint without a log is not actionable.

## Aim assist

`AimAssist` (in `control`, deliberately vision-free) turns a field pose and a target
point into a heading error for a turn loop:

```java
double error = AimAssist.headingError(robotPose, targetPoint);
turnPid.calculate(0.0, -error);   // P-loop on the error; tune like any turn controller
```

The pose can come from `getRobotPose`, from odometry, or from a fusion of both — the
helper takes numbers, so the source never matters. Bearing-style driving (turn to face
the tag, drive to range) needs no pose at all: `getTag(id)` plus the SDK's bearing and
range, straight into proportional loops.

## Validation checklist (needs hardware)

- [ ] Solved pose on a surveyed field matches tape measurements within an inch at
      several stations, facing several tags.
- [ ] Yawed tags (turned up to ~45°) solve the same robot pose as square-on tags.
- [ ] Unknown tags yield empty poses but readable ids.
- [ ] Loop timing with vision streaming matches loop timing without it, at the
      configured update ceiling.
- [ ] Closing and reopening across OpModes never fails to acquire the camera.

## Next

- [Bench testing](bench-testing.md) — the procedures these checks slot into
- [Telemetry and logging](telemetry-and-logging.md) — recording validation runs
