# Compatibility

CurioControl explicitly supports the FTC SDK version its Robot Controller project uses. A release
that needs a newer SDK than your team runs is a **documented incompatibility**, and the fix is to
stay on a compatible CurioControl version.

The most reliable check is the one the framework does for you:

```java
Log.d("CurioControl", "Running " + CurioConfig.version());
```

## Current release

| Component | Version |
|---|---|
| CurioControl | 0.1.0 |
| FTC SDK | 12.0.0 |
| Android SDK | 34 |
| Java | 17 |
| Gradle | 8.14.5 |

## What is in 0.1.0

| Package | Contents |
|---|---|
| `util` | `Clock`, `SystemClock`, `Range` |
| `core` | `CurioRobot`, `Subsystem`, `HardwareRegistry`, `HardwareSource`, `TelemetryManager`, `CurioConfig`, `CurioOpMode`, `CurioAuto` |
| `hardware` | `Motor`, `Encoder`, `Servo`, `ContinuousServo`, `IMU`, `DigitalSensor`, `AnalogSensor`, `VoltageSensor`, `HardwareType` |
| `control` | `PIDController` |
| `drive` | `DriveBase`, `MecanumDrive`, `TankDrive` |

`math`, `command`, `telemetry` (CSV `Logger`), and `vision` are **not** in 0.1.0. See
[the phases document](../../PHASES.md) for what arrives when.

## Support matrix

Updated at every release.

| CurioControl | FTC SDK | Android SDK | Java | Gradle |
|---|---|---|---|---|
| 0.1.0 | 12.0.0 | 34 | 17 | 8.14.5 |

## How to check your Robot Controller version

On the Robot Controller: **Settings → About** shows the SDK version. Match it against the matrix
above before you compete.

## Java

CurioControl requires Java 17. If your machine does not have a JDK 17, the Gradle build
provisions one automatically — nothing to install manually.

Your robot project must also target Java 17, which is what the current FTC SDK requires:

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

## Changing the pinned SDK

The version lives in one place, `gradle/libs.versions.toml`:

```toml
[versions]
ftcSdk = "12.0.0"
```

To compile against a different SDK:

1. Change `ftcSdk`.
2. Run `./gradlew build` and fix any compile errors.
3. Run the bench checks in the [testing strategy](../TESTING_STRATEGY.md) on real hardware.
4. Update the matrix above and the changelog.

A signature that changed between SDK versions shows up as a compile error, which is the good case.
A behavioral change that did not is why step 3 is not optional.
