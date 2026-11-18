# Example Robot

A complete FTC robot project using CurioControl, in the layout from `spec.md` §40:

```text
TeamCode/
└── src/main/java/org/curioone/robot/
    ├── opmode/
    │   ├── MainTeleOp.java     # sticks drive, buttons command mechanisms
    │   └── BlueAuto.java       # a command-based autonomous routine
    ├── subsystem/
    │   ├── Drive.java          # four motors + IMU, and drive-to-distance
    │   ├── Arm.java            # position-target arm, with moveTo(...) commands
    │   └── Intake.java         # roller + claw, with release()
    └── config/
        └── RobotConfig.java    # every competition constant
```

## Using it

The example is a **TeamCode module**, not a standalone Gradle project. Drop it into a stock FTC
SDK checkout:

1. Copy `TeamCode/` over the stock `TeamCode/` directory (or copy the Java sources and
   `build.gradle` into your own module).
2. No root build change is needed. The included `TeamCode/build.gradle` declares JitPack in the
   same file as the dependency:

   ```groovy
   repositories {
       maven { url 'https://jitpack.io' }
   }

   dependencies {
       implementation project(':FtcRobotController')
       implementation 'com.github.curiooneftc:CurioControl:v0.5.0'
   }
   ```

   No token or other credentials are needed. For local CurioControl development, publish it with
   `./gradlew publishToMavenLocal` and replace JitPack with `mavenLocal()`; the local artifact uses
   `org.curioone:curiocontrol:0.5.0`.

3. Build the robot controller app:

   ```bash
   ./gradlew :TeamCode:assembleDebug
   ```

4. Deploy and run `Main TeleOp` or `Blue Auto` from the Driver Station.

The module's `build.gradle` pins the CurioControl release tag exactly. Replace `v0.5.0` with the
release you deploy.

## Robot configuration

The example uses the stock Pushbot motor names, plus an arm, an intake, and an IMU. Create these
names on the Robot Controller:

| Device | Type | Name |
|---|---|---|
| Front-left drive motor | Motor | `front left motor` |
| Front-right drive motor | Motor | `front right motor` |
| Back-left drive motor | Motor | `back left motor` |
| Back-right drive motor | Motor | `back right motor` |
| Arm motor | Motor (encoder) | `arm motor` |
| Intake motor | Motor | `intake motor` |
| Claw | Servo | `claw servo` |
| IMU | IMU | `imu` |

Change the names — and wheel diameter, ticks-per-revolution, arm positions, and claw positions — in
[`RobotConfig.java`](TeamCode/src/main/java/org/curioone/robot/config/RobotConfig.java). Nothing in
CurioControl assumes a robot.

## What to look at

- **`MainTeleOp`** is the whole TeleOp. It applies a stick deadband, toggles field-centric drive, and
  commands subsystems. It never touches a motor or a servo.
- **`BlueAuto`** implements `configureRobot()` and `buildRoutine()`. The routine is a
  `Commands.sequence(...)`; `CurioAuto` runs the scheduler and cancels the routine when the OpMode
  stops.
- **`Drive.driveDistance(...)`** is a small custom `Command`: initialize reads the encoders,
  execute applies power, `isFinished()` checks the distance, `end()` stops the motors. It is the
  pattern for any mechanism-specific command.
- **`Arm.moveTo(...)`** composes an instant command and a `waitUntil`, showing that most mechanism
  actions need no class at all.
- **Subsystem telemetry** is published by `publishTelemetry(...)` and flushed once per loop by
  `CurioRobot.loop()`; the OpMode adds nothing.

## Verification

The sources are compiled in CI against the published artifact in a stock FTC SDK fork, for both SDK
11.1 and 12.0. See [`.github/workflows/compatibility.yml`](../../.github/workflows/compatibility.yml)
and [`scripts/stockfork-compat.sh`](../../scripts/stockfork-compat.sh). A change that stops this
example compiling is a change that breaks a team.
