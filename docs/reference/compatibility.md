# Compatibility

CurioControl supports a **range** of FTC SDK versions, not just one. The library is compiled
against the oldest SDK in that range and emitted as Java 8 bytecode, so the same artifact links on
every version above the baseline.

## Current release

| Component | Version |
|---|---|
| CurioControl | 0.5.0 |
| FTC SDK (compile baseline) | 11.1.0 |
| FTC SDK (verified) | 11.1.0 and 12.0.0 |
| Library bytecode | Java 8 (class file major version 52) |
| Build JDK | 17 |
| Build Gradle | 8.14.5 |

The build JDK and the bytecode target are separate on purpose. Contributors build on JDK 17; robot
projects load Java 8 bytecode, which is all the stock Android toolchain needs.

## Verified consumer toolchains

Both rows were built end-to-end from untouched
[`FtcRobotController` v11.1](https://github.com/FIRST-Tech-Challenge/FtcRobotController/tree/v11.1)
and [v12.0](https://github.com/FIRST-Tech-Challenge/FtcRobotController/tree/v12.0) checkouts. The
only CurioControl-specific edit was the `repositories` and `dependencies` block in
`TeamCode/build.gradle`; the root build and toolchain were left alone:

| Consumer | FTC SDK | Gradle | AGP | compileSdk | Java | Result |
|---|---|---|---|---|---|---|
| Stock fork, as released | 11.1.0 | 8.9 | 8.7.0 | 30 | 8 | `:TeamCode:assembleDebug` green, 82 CurioControl classes dexed |
| Stock v12.0 fork | 12.0.0 | 9.1 | 8.13.2 | 30 | 8 | `:TeamCode:assembleDebug` green, 82 CurioControl classes dexed |

The second row is the forward-compatibility test. SDK 12 moved the Android plugin declaration out of
`build.common.gradle`, so the example `TeamCode/build.gradle` applies `com.android.application`
explicitly; that line is a no-op on SDK 11. CurioControl itself compiled and dexed unchanged on both.
The same check runs in CI via `scripts/stockfork-compat.sh`.

## Java

CurioControl ships **Java 8 bytecode**. A stock FTC project must *not* raise its language level on
CurioControl's account:

```groovy
// TeamCode/build.gradle — the stock value is already correct.
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

`compileJava` sets `options.release = 8`, so any Java 9+ API or language feature in `src/main`
fails the library build rather than producing a jar an older consumer cannot load. The published
Gradle module metadata declares `"org.gradle.jvm.version": 8`, which is what makes Gradle select
the artifact for a Java 8 consumer instead of rejecting the variant.

Building CurioControl itself needs JDK 17 (auto-provisioned if missing); that is a contributor
requirement, not a consumer one.

## FTC SDK

| SDK | Status | Notes |
|---|---|---|
| 11.1.x | Baseline — compiled against it | Stock `FtcRobotController` fork, Java 8, compileSdk 30 |
| 12.x | Verified | Same artifact; AprilTag clusters are skipped (see below) |

The SDK is `compileOnly`. CurioControl does not bring an SDK transitively, so the Robot Controller
app supplies the real classes at runtime and the SDK version stays whatever the robot project
already uses.

### AprilTag ids across SDK 11 and 12

SDK 11 declares `AprilTagDetection.id` directly. SDK 12 moves `id`, `metadata`, and `corners` onto
the `AprilTagSingleDetection` subclass, and adds `AprilTagClusterDetection`, which has no id at all.
`AprilTagManager` reads the id reflectively, so one artifact runs on both:

- SDK 11: every detection reports its id.
- SDK 12: single detections report their id; clusters report none and are skipped rather than
  failing class linkage.

That behaviour is covered by unit tests on SDK 11 and by a JVM probe against the real SDK 12
classes (`Optional[20]` for a single detection, `Optional.empty` for a cluster).

## Changing the pinned SDK

The version lives in one place, `gradle/libs.versions.toml`:

```toml
[versions]
ftcSdk = "11.1.0"
```

To move the baseline:

1. Change `ftcSdk`.
2. Run `./gradlew build` and fix any compile errors.
3. Re-run the stock-fork build check on the oldest and newest SDK you claim to support.
4. Run the bench checks in the [testing strategy](../../TESTING_STRATEGY.md) on real hardware.
5. Update the matrices above and the changelog.

A signature that changed between SDK versions shows up as a compile error, which is the good case.
A behavioral change that did not is why step 4 is not optional.

## How to check your Robot Controller version

On the Robot Controller: **Settings → About** shows the SDK version. Match it against the matrix
above before you compete.
