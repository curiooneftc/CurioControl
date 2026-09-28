# ADR-001: FTC SDK dependency strategy

**Status:** Accepted
**Decided:** Phase 0

## Context

The FTC SDK (`RobotCore`, `Hardware`, `Vision`, `Inspection`, `FtcCommon`) ships inside the Robot
Controller app rather than as a standard public Maven artifact. CurioControl must compile against
it, and CI must build without a physical device — or a Robot Controller installation on every
contributor's machine.

This was identified as the highest-risk build decision in the project. If it went wrong, nothing
else could land.

## What we found

The FTC SDK **is** published to Maven Central, under `org.firstinspires.ftc`, with versions from
`6.2.0` through `12.0.0`. That removes the need to vendor AARs or depend on a community mirror we
do not control.

There is one complication. The SDK is published as Android **AARs**, and its Gradle module
metadata declares the legacy `org.gradle.libraryelements` attribute rather than
`org.gradle.artifact.type`. A `java-library` project without the Android Gradle Plugin cannot
select that variant at all — an artifact transform never gets a chance to run, because the variant
is not selected in the first place.

## Options

- **A. `compileOnly` against the official SDK on Maven Central**, unpacking the AARs manually.
- **B. Vendor the AAR files** into a `libs/` directory and depend on them as files.
- **C. Depend on a community-published SDK artifact** via JitPack.
- **D. Apply the Android Gradle Plugin** so the AARs resolve natively.

## Decision

**Option A**, with a dedicated configuration and a small build task that extracts `classes.jar`
from each AAR.

```kotlin
val ftcSdkAars: Configuration by configurations.creating {
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(libraryElementsAttribute, "aar")   // what the SDK actually publishes
    }
}

val extractFtcSdkClasses by tasks.registering(UnpackAarClasses::class) {
    archives.from(ftcSdkAars)
    outputDirectory.set(layout.buildDirectory.dir("ftc-sdk-classes"))
    strict.set(true)
}
```

The extracted jars are then `compileOnly` dependencies. `buildSrc/src/main/kotlin/UnpackAarClasses.kt`
fails the build if an AAR has no `classes.jar`, so a change in how the SDK is published surfaces
immediately rather than as a confusing compile error.

`androidx.annotation` is resolved normally rather than through the unpacking configuration — it is
a multiplatform library, not an AAR.

## Consequences

- The published CurioControl artifact contains **no** SDK classes. Consumers get them from the
  Robot Controller at runtime, which is correct: shadowing SDK classes would be a serious problem.
- CI builds and runs the full test suite with only a JDK. No Android SDK, no Robot Controller, no
  vendored binaries.
- Tests compile against the real SDK types, so Mockito can mock `DcMotorEx` and friends directly.
- The exact SDK version is pinned in `gradle/libs.versions.toml` and recorded in the compatibility
  matrix.
- There is one piece of build logic (`UnpackAarClasses`) that exists only because of how the SDK is
  published. It is documented, and it fails loudly rather than silently.
- If the SDK is ever published with proper `org.gradle.artifact.type` metadata, this could be
  replaced with a plain artifact transform. That is a small, contained change.

## Rejected options

- **B (vendoring)** puts roughly 15 MB of third-party binaries in git, makes the SDK version bump a
  commit of opaque files, and leaves the provenance of those files to our own discipline.
- **C (JitPack)** adds an external dependency nobody on the team controls, and adds a build-time
  network dependency we do not otherwise need.
- **D (Android Gradle Plugin)** would mean the Android SDK on every machine and in CI, to build a
  plain Java library. The cost is real and the benefit is nil.
