# Migration

See the full guide at [`MIGRATION.md`](https://github.com/curiooneftc/CurioControl/blob/main/MIGRATION.md).

## Before 1.0.0

The API is unstable. Any release may break it without a major version bump, and the
`0.MINOR` component is effectively the breaking counter.

To move between `0.x` versions:

1. Read the changelog for every version between your pin and the target.
2. Update the pinned version in `build.gradle.kts`.
3. Build. The compiler finds signature changes; the architecture tests find layering changes.
4. Re-run the robot on the bench. Behavioral changes are called out under **Changed** and **Fixed**.

## From 1.0.0

Public APIs are stable. A breaking change requires a major version and a section in
`MIGRATION.md`.

## Always pin an exact version

```kotlin
implementation("org.curioone:curiocontrol:0.1.0")   // good
```

```kotlin
implementation("org.curioone:curiocontrol:latest")   // bad
implementation("org.curioone:curiocontrol:+")       // bad
```

Competition robot code has to be reproducible. A floating version breaks that guarantee in a way
you only discover when a robot behaves differently at a competition than it did at practice.
