# Project Setup

> Status: this page describes the target v1.0.0 layout. The v0.1.0 release implements a subset of
> the API; see [the phases document](../PHASES.md) for exactly what exists in which release.

## The Gradle template

A CurioControl robot project is a normal FTC project with one extra repository and one extra
dependency. Nothing about the FTC build changes.

```kotlin
// build.gradle.kts (TeamCode)
plugins {
    id("com.android.application")
    // ...the standard FTC plugins
}

android {
    namespace = "org.curioone.robot"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

repositories {
    mavenCentral()
    google()
    maven {
        url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl")
        credentials {
            username = providers.gradleProperty("github.actor").orNull
            password = providers.gradleProperty("github.token").orNull
        }
    }
}

dependencies {
    // Pin an exact version. Competition code must be reproducible.
    implementation("org.curioone:curiocontrol:0.1.0")
}
```

## Where files go

```text
RobotCode/
├── TeamCode/
│   └── src/main/java/
│       └── org/curioone/robot/
│           ├── opmode/
│           │   ├── MainTeleOp.java      # OpModes only: what the robot should do
│           │   └── BlueAuto.java
│           ├── subsystem/
│           │   ├── Arm.java             # mechanisms: own their hardware and logic
│           │   ├── Intake.java
│           │   └── Drive.java
│           └── config/
│               └── RobotConfig.java     # every competition constant, in one place
├── build.gradle.kts
└── settings.gradle.kts
```

Three rules of thumb:

1. **OpModes describe intent, not mechanics.** If an OpMode contains a `setTargetPosition` call,
   the mechanism logic probably belongs in a subsystem.
2. **Subsystems own their hardware.** A subsystem looks up its own devices and exposes meaning,
   not raw motor access.
3. **Constants live in `RobotConfig`.** If a physical number appears in more than one file, it
   belongs in the config class.

## Why CurioControl ships no constants

The framework contains no wheel diameter, no arm position, no motor name. Those values differ
between robots and change between seasons, and baking them in would make the library useless the
moment you reuse it.

Instead, the API accepts them as parameters:

```java
double distance = encoder.getDistance(
        RobotConfig.Drive.TICKS_PER_REV,
        RobotConfig.Drive.WHEEL_DIAMETER_MM);
```

See [robot configuration](../guides/robot-config.md) for the full pattern.

## Development workflow

| Task | Command |
|---|---|
| Build and deploy | `./gradlew :TeamCode:assembleDebug` in your robot project |
| Try the framework from source | `./gradlew publishToMavenLocal` in CurioControl, then depend on `0.1.0-SNAPSHOT` locally |

Only do the second while developing CurioControl itself. Competition code pins a released version.
