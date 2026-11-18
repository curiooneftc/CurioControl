# Installation

> Status: this page describes the target v1.0.0 setup. The v0.1.0 release implements a subset of
> the API; see [the phases document](../../PHASES.md) for exactly what exists in which release.

## 1. Add the repository and dependency in TeamCode

CurioControl's public releases are served by [JitPack](https://jitpack.io/). It builds a release
directly from its Git tag, so teams need **no GitHub account, token, or other credentials**.

For a stock FTC project, the only CurioControl-specific edit is in `TeamCode/build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation project(':FtcRobotController')
    implementation 'com.github.curiooneftc:CurioControl:v0.5.0'
}
```

`google()` and `mavenCentral()` are already declared by the stock project template. If a custom
project sets `FAIL_ON_PROJECT_REPOS`, Gradle will reject the module-level repository; in that case,
move the JitPack line into `settings.gradle.kts` and leave only the dependency in `TeamCode`.

!!! note "Why not GitHub Packages?"
    Maintainers still publish to GitHub Packages from CI, but that registry can require a token for
    consumers. JitPack keeps team robot builds credential-free.

## 2. Pin an exact release tag

Use the exact release **tag**, including the leading `v`. Do not use `main-SNAPSHOT`, a commit
hash, `latest`, a `+` range, or an unreleased branch in competition code — reproducibility matters
more than convenience (`spec.md` §36).

```groovy
implementation 'com.github.curiooneftc:CurioControl:v0.5.0'
```

## 3. On a stock FtcRobotController fork

Nothing else changes. No Gradle, AGP, `compileSdk`, or Java upgrade is required: CurioControl is
Java 8 bytecode compiled against FTC SDK 11.1, which is what the stock fork already uses. The only
change is the `TeamCode/build.gradle` block above.

For local CurioControl development, add `mavenLocal()` to that same `repositories` block instead
and run `./gradlew publishToMavenLocal` in this repository. The local artifact uses the stable
coordinates `org.curioone:curiocontrol:0.5.0`; JitPack uses the Git-based coordinates shown above.
Both SDK 11.1 and 12.x paths are verified; see the
[compatibility reference](../reference/compatibility.md).

## 4. Verify the resolution

A successful dependency resolve is enough — there is no authentication step:

```bash
./gradlew :TeamCode:dependencies --configuration releaseRuntimeClasspath
```

Look for `com.github.curiooneftc:CurioControl:v0.5.0`. The first build of a tag may take a few
minutes while JitPack builds it; later builds are cached. If it fails, see the
[troubleshooting guide](../guides/troubleshooting.md).

## 5. Check the compatibility matrix

CurioControl is compiled against FTC SDK 11.1 and ships Java 8 bytecode, so it runs on the stock
fork and on 12.x without toolchain changes. Check the
[compatibility reference](../reference/compatibility.md) for the verified combinations before you
compete — a newer SDK than the baseline is supported, an older one is not.

## What's next

- [Quickstart](quickstart.md) — a working TeleOp in ten minutes
- [Project setup](project-setup.md) — the full Gradle template and where files go
