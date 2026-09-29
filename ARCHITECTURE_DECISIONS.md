# CurioControl Architecture Decisions

> Record of the significant architectural decisions made while building CurioControl.
> Each decision is captured as an ADR (Architecture Decision Record): context → decision →
> consequences → status.
> Source: `spec.md` §6 (Architecture), §31 (Dependency Philosophy), §38 (API Stability),
> §43 (Design Principles).
>
> Individual ADRs live in [`docs/adr/`](docs/adr/) — one file per decision, so each can be
> reviewed and referenced on its own. This file is the index and the consolidated record.

---

## ADR Index

| ADR | Title | Status |
|-----|-------|--------|
| [001](docs/adr/001-ftc-sdk-dependency-strategy.md) | FTC SDK dependency strategy | **Accepted** (resolved in Phase 0) |
| [002](docs/adr/002-single-gradle-module.md) | Single Gradle module with package boundaries | Accepted |
| [003](docs/adr/003-pure-java-boundary.md) | Pure-Java boundary for `math`, `control`, `util` | Accepted |
| [004](docs/adr/004-single-threaded-scheduler.md) | Single-threaded command scheduler (no background threads) | Accepted |
| [005](docs/adr/005-hardware-abstraction.md) | Wrapper (not interface) hardware abstraction | **Accepted** (resolved in Phase 0) |
| [006](docs/adr/006-telemetry-api.md) | Telemetry: fluent `add(...).update()` API | Accepted |
| [007](docs/adr/007-logging-defaults.md) | Logging disabled by default, CSV to RC storage | Accepted |
| [008](docs/adr/008-publishing-target.md) | Publishing to GitHub Packages first | Accepted |
| [009](docs/adr/009-time-injection.md) | Time injection via a `Clock` interface | Accepted |
| [010](docs/adr/010-architecture-tests.md) | Architecture rules enforced by tests (ArchUnit) | Accepted |
| [011](docs/adr/011-vision-is-optional.md) | Vision is lazy and optional | Accepted |
| [012](docs/adr/012-no-competition-constants.md) | Framework never contains competition constants | Accepted |
| [013](docs/adr/013-package-namespace.md) | Maven group and Java package follow the GitHub org | Accepted |
| [014](docs/adr/014-composition-root-and-hardware-source.md) | `CurioRobot` as composition root, hardware behind a port | Accepted |

---

## ADR-013: Maven group and Java package follow the GitHub org

**Status:** Accepted (supersedes `spec.md` §5)

Full text: [`docs/adr/013-package-namespace.md`](docs/adr/013-package-namespace.md)

**Context**
`spec.md` §5 specifies `org.curioone:curiocontrol` with a `org.curioone.control` package. The
repository actually lives at `github.com/curiooneftc/CurioControl`, so neither identifier can be
derived from the URL a consumer is looking at.

**Decision**
Both the Maven group and the Java package use `org.curioone`. The rule: **the repository URL is
the source of truth for the namespace.**

**Consequences**
- `spec.md` §5 is superseded for the group and package. `spec.md` is left unedited — it records
  what the specification says, and this ADR is the amendment.
- Coordinates are `org.curioone:curiocontrol`; imports are `org.curioone.control.*`.
- The rename happened in Phase 0, before any released artifact. After `0.1.0` it would be a
  breaking change, and `1.0.0` is not the moment to spend that capital.

---

## ADR-001: FTC SDK dependency strategy

**Status:** **Accepted** — *resolved in Phase 0; this was the highest-risk build decision.*

Full text: [`docs/adr/001-ftc-sdk-dependency-strategy.md`](docs/adr/001-ftc-sdk-dependency-strategy.md)

**Context**
The FTC SDK (`RobotCore`, `Hardware`, `Vision`, `Inspection`, `FtcCommon`) ships inside the Robot
Controller app rather than as a standard public Maven artifact. CurioControl must compile against
it, and CI must build without a physical device.

**What we found**
The SDK **is** published to Maven Central under `org.firstinspires.ftc`, with versions from `6.2.0`
through `12.0.0`. That removes the need to vendor binaries or depend on a mirror we do not control.

One complication: the SDK is published as Android **AARs**, and its Gradle module metadata declares
the legacy `org.gradle.libraryelements` attribute rather than `org.gradle.artifact.type`. A
`java-library` project without the Android Gradle Plugin cannot select that variant at all — an
artifact transform never gets a chance to run, because the variant is not selected in the first
place.

**Options**
- **A. `compileOnly` against the official SDK on Maven Central**, unpacking the AARs manually.
- **B. Vendor the AAR files** into the repo and depend on them as files.
- **C. Depend on a community-published SDK artifact** (e.g. via JitPack).
- **D. Apply the Android Gradle Plugin** so the AARs resolve natively.

**Decision: Option A**, with a dedicated configuration and a small build task
(`buildSrc/src/main/kotlin/UnpackAarClasses.kt`) that extracts `classes.jar` from each AAR. The
task fails the build if an AAR has no `classes.jar`, so a change in how the SDK is published
surfaces immediately rather than as a confusing compile error.

Pinned SDK version: **12.0.0**, in `gradle/libs.versions.toml`.

**Consequences**
- The published CurioControl artifact contains **no** SDK classes; consumers get them from the
  Robot Controller at runtime (correct — we must not shadow SDK classes).
- CI compiles and runs all pure-Java tests with only a JDK. No Android SDK, no Robot Controller,
  no vendored binaries.
- Tests compile against the real SDK types, so Mockito can mock `DcMotorEx` and friends directly.
- The exact SDK version is pinned in one place and recorded in the compatibility matrix.
- There is one piece of build logic that exists only because of how the SDK is published. It is
  documented, and it fails loudly.

---

## ADR-002: Single Gradle module with package boundaries

**Status:** Accepted

Full text: [`docs/adr/002-single-gradle-module.md`](docs/adr/002-single-gradle-module.md)

**Context**
We could ship one artifact, or split into `curiocontrol-core`, `curiocontrol-math`, etc.

**Decision**
A **single Gradle module** (`org.curioone:curiocontrol`) with strict **package** boundaries,
enforced by ArchUnit tests.

**Rationale**
- Competition teams add one dependency; multi-module adds consumer friction with little benefit,
  since every FTC project already has the SDK.
- Package purity gives us the architectural guarantee (no FTC imports in `math`) without build
  complexity.
- If a desktop/simulation use case ever justifies a pure-Java artifact, we can split modules later
  without renaming packages.

**Consequences**
- Layering violations are caught at test time, not compile time. Acceptable given the small team.
- Coverage and publishing are simpler (one publication).
- `PROJECT_STRUCTURE.md` layering table is the source of truth for allowed dependencies.

---

## ADR-003: Pure-Java boundary for `math`, `control`, `util`

**Status:** Accepted (source: `spec.md` §16, §29, §31)

Full text: [`docs/adr/003-pure-java-boundary.md`](docs/adr/003-pure-java-boundary.md)

**Decision**
`math`, `control`, and `util` must contain **zero** imports of `com.qualcomm.*` (FTC) or
`android.*`. They are plain Java, testable on a desktop JVM.

**Consequences**
- Enables fast, thorough unit tests (the bulk of the suite).
- Forces good design: controllers take plain numbers, not motor objects; `MotionProfile` is a
  pure function of time.
- Enforced by ArchUnit tests (`MathPackageIsPure`, `ControlPackageIsPure`, `UtilPackageIsPure`),
  implemented in `src/test/java/org/curioone/control/architecture/`.
- Small friction: e.g. an IMU-backed heading must be abstracted behind a plain interface so
  `drive`/`control` never see the SDK IMU.

---

## ADR-004: Single-threaded command scheduler

**Status:** Accepted

Full text: [`docs/adr/004-single-threaded-scheduler.md`](docs/adr/004-single-threaded-scheduler.md)

**Context**
Command frameworks (WPILib-style) often run on background threads. The FTC OpMode loop is
single-threaded and timing-sensitive.

**Decision**
`CommandScheduler` is **single-threaded** and is invoked exactly once per OpMode loop iteration.
No background threads, no executors, no timers running independently.

**Consequences**
- Deterministic, easy to reason about, and safe to call from the OpMode thread.
- Matches how teams actually write FTC OpModes.
- A command's `execute()` is called once per loop; `isFinished()` is polled each loop.
- Long waits (e.g. `WaitCommand`) are polled, not slept — no blocking the loop.
- Testing is straightforward (drive the scheduler manually with a fake clock).

---

## ADR-005: Wrapper (not interface) hardware abstraction

**Status:** **Accepted** — *resolved in Phase 0 (previously provisional).*

Full text: [`docs/adr/005-hardware-abstraction.md`](docs/adr/005-hardware-abstraction.md)

**Context**
Two common styles: (a) a concrete wrapper class around the SDK object, or (b) an interface with an
SDK-backed implementation.

**Options**
- **A. Concrete wrapper** (`Motor` wraps `DcMotorEx`): simpler for consumers, one class, easy to
  read; harder to fake in user tests.
- **B. Interface + impl** (`Motor` interface, `DcMotorExMotor` impl): more swappable/testable, but
  doubles the type count and adds boilerplate for every device.

**Decision: Option A** — concrete wrappers, each exposing the underlying SDK object via
`getSdkObject()` so direct SDK access remains possible (`spec.md` §10). Testing the wrappers uses
mocked SDK objects (Mockito), not interfaces.

**Consequences**
- Fewer types; cleaner consumer code (`Motor arm = robot.motor("arm")`).
- Users who need to fake hardware in their own tests can implement a small adapter or rely on
  wrapper methods being non-final.
- The mocked-SDK test tier already covers the wrappers' own logic, so the extra indirection of an
  interface layer would buy little.
- If a strong need for swappable hardware emerges (e.g. simulation, post-1.0), interfaces can be
  introduced then as a **breaking** change with a major version, or as separate abstractions
  layered on top.

---

## ADR-006: Telemetry: fluent `add(...).update()` API

**Status:** Accepted (source: `spec.md` §26)

Full text: [`docs/adr/006-telemetry-api.md`](docs/adr/006-telemetry-api.md)

**Decision**

```java
robot.telemetry()
    .add("Heading", heading)
    .add("Arm", armPosition)
    .update();
```

A single batched `update()` flushes the values to the SDK in one call per loop.

**Consequences**
- One `Telemetry.sendTelemetryPacket()` per loop instead of N — better for the control loop
  (`spec.md` §45).
- Support categories (`DRIVE`, `ARM`, `INTAKE`, `VISION`, `SYSTEM`, `DEBUG`) to keep output
  readable; `DEBUG` only when `CurioConfig.DEBUG` is on.
- Keys/values are buffered internally, so `add` is cheap.

---

## ADR-007: Logging disabled by default, CSV to Robot Controller storage

**Status:** Accepted (source: `spec.md` §27)

Full text: [`docs/adr/007-logging-defaults.md`](docs/adr/007-logging-defaults.md)

**Decision**
- The `Logger` is **off by default**; it must be explicitly enabled.
- Output is CSV written to Robot Controller-accessible storage (e.g. `/sdcard/CurioControl/logs/`).
- Low overhead: preallocated buffers, batched writes, configurable flush interval.

**Consequences**
- Competition-safe (`spec.md` §27): no surprise I/O during a match.
- Never transmits data externally; no network dependency (`spec.md` §44).
- Buffered writes mean a hard robot stop can lose the tail of a log — acceptable trade-off,
  documented.
- A disabled logger costs effectively nothing (early return).

---

## ADR-008: Publishing to GitHub Packages first

**Status:** Accepted (source: `spec.md` §34)

Full text: [`docs/adr/008-publishing-target.md`](docs/adr/008-publishing-target.md)

**Decision**
Distribute via **GitHub Packages** (`org.curioone:curiocontrol`). Consider **Maven Central**
later, post-v1.0.0.

**Rationale**
- Free and native to the repo, with Actions-based publishing using the ephemeral `GITHUB_TOKEN`.
- No separate credentials infrastructure for the initial releases.
- Maven Central adds signing, a Sonatype account, and slower cadence — not worth it pre-stability.

**Consequences**
- Public read access for the package must be configured in repo settings; otherwise consumers need
  a **read-only** token.
- Consumers add a `maven { url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl") }`
  repository.
- All released versions remain resolvable (GitHub Packages keeps them), satisfying reproducibility
  (`spec.md` §36).

---

## ADR-009: Time injection via a `Clock` interface

**Status:** Accepted

Full text: [`docs/adr/009-time-injection.md`](docs/adr/009-time-injection.md)

**Context**
`MotionProfile`, `Timer`, `RateLimiter`, `Debouncer`, PID delta-time, and state-machine timeouts
all depend on the passage of time. Tests that call `System.nanoTime()` are flaky and slow.

**Decision**
Introduce a minimal `org.curioone.control.util.Clock` interface (e.g. `nowNanos()`), used internally
by time-dependent components. Production code uses a real-clock implementation; tests use a
`FakeClock` they advance manually.

**Consequences**
- Time-based behavior is fully deterministic and fast to test.
- A tiny amount of indirection; no performance concern (an interface call in the OpMode loop is
  negligible).
- Aligns with the "testable" design principle (`spec.md` §43).

---

## ADR-010: Architecture rules enforced by tests (ArchUnit)

**Status:** Accepted

Full text: [`docs/adr/010-architecture-tests.md`](docs/adr/010-architecture-tests.md)

**Decision**
The layering and purity rules in `PROJECT_STRUCTURE.md` are enforced by **ArchUnit** tests, not
just convention.

**Consequences**
- A violation (e.g. importing FTC in `math`) fails CI immediately, not in a code review months
  later.
- Centralizes the architectural rules in executable form that doubles as living documentation.
- Rules use `allowEmptyShould(true)`, so a package with nothing in it yet is not a failure. A rule
  that *stops* checking once its package is populated still is.
- Slight build-time cost (fast; ArchUnit is lightweight).

---

## ADR-011: Vision is lazy and optional

**Status:** Accepted (source: `spec.md` §28)

Full text: [`docs/adr/011-vision-is-optional.md`](docs/adr/011-vision-is-optional.md)

**Decision**
`robot.vision()` lazily constructs a `VisionManager` only when first accessed. Teams that never
touch vision pay no cost (no allocation, no camera init).

**Consequences**
- Satisfies "avoid forcing vision dependencies on users who do not need them."
- Vision update rate is configurable so it cannot starve the control loop.
- The `vision` package is never imported by `core`/`control`/`drive` (enforced by ArchUnit).

---

## ADR-012: Framework never contains competition constants

**Status:** Accepted (source: `spec.md` §9)

Full text: [`docs/adr/012-no-competition-constants.md`](docs/adr/012-no-competition-constants.md)

**Decision**
The framework contains no season-specific or Curio One robot constants (no wheel diameters, no arm
positions, no specific motor names). Those live in the robot project's `RobotConfig`.

**Consequences**
- CurioControl stays reusable across robots and seasons.
- APIs accept these values as parameters (e.g. `encoder.getDistance(ticksPerRev, wheelDiameterMm)`).
- Test fixtures hold physical constants, never `src/main`.

---

## ADR-014: `CurioRobot` as composition root, hardware behind a port

**Status:** Accepted

Full text: [`docs/adr/014-composition-root-and-hardware-source.md`](docs/adr/014-composition-root-and-hardware-source.md)

**Context**
Two problems collided in Phase 1. The specification's facade (`robot.drive()`, `robot.imu()`)
requires a class that reaches across packages, which conflicts with `core` being a leaf. Separately,
`HardwareMap` reaches into `android.content.Context` and **cannot be mocked on a desktop JVM** — so
any signature mentioning it is untestable off-robot, which is every hardware wrapper and the
composition root itself.

**Decision**
`core.CurioRobot` is the composition root and the only class in `core` permitted to depend outward;
`LayeringTest` names it as the sole exception. Hardware reaches the framework through a one-method
`HardwareSource` port, with `SdkHardwareSource` adapting the real `HardwareMap` at the edge.

**Consequences**
- The specification's facade exists, and the leaf rule survives as a rule with one named exception
  rather than as an aspiration.
- `CurioRobot` and `HardwareSource` are public API — they are the framework's two extension seams.
- `drive` may not depend on `core`, so name-to-device resolution happens in the composition root and
  drivetrains are constructed from already-wrapped `Motor`s. The architecture test enforced this.
- `FakeHardwareSource` is a plain `HashMap`, so the lifecycle, subsystem registry, hardware
  resolution, and drivetrain construction are all covered without a hub.

---

## How to add a new ADR

1. Create `docs/adr/NNNN-short-title.md`.
2. Use the format: **Context → Options → Decision → Consequences → Status**.
3. Get it reviewed in the same PR as (or before) the code it governs.
4. Update the ADR Index in this file and in `docs/adr/README.md`.

An ADR is written when the decision is made, not after. Rewriting history to make a decision look
obvious defeats the purpose of keeping the record.

---

## Related documents

- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) — layout and layering rules governed by these ADRs
- [TESTING_STRATEGY.md](./TESTING_STRATEGY.md) — architecture guard tests
- [RELEASE_STRATEGY.md](./RELEASE_STRATEGY.md) — publishing governed by ADR-008
- [CI_CD_SETUP.md](./CI_CD_SETUP.md) — the build and CI these ADRs describe
- [spec.md](./spec.md) — the source specification
