# CurioControl Phases & Milestones

> **Target window:** ~26 weeks (6 months) to v1.0.0  
> **Cadence:** 2-week sprints, 1 release per ~5-6 weeks  
> **Source spec:** [`spec.md`](./spec.md) §47 (Initial Release Roadmap), expanded

---

## Timeline Overview

```text
Month   1        2        3        4        5        6
Week  01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
      ├──────────────┤
      │  PHASE 0     │ Infra
      │  PHASE 1     │ v0.1.0 Foundation
      ├─────────────────────────────┤
      │        PHASE 2              │ v0.2.0 Control
      ├─────────────────────────────┤
      │        PHASE 3              │ v0.3.0 Architecture
      ├─────────────────────────────┤
      │        PHASE 4              │ v0.4.0 Vision
      ├──────────────────────────────────────┤
      │        PHASE 5              │ v1.0.0 Stable
```

---

## Phase 0 — Infrastructure (Weeks 1-3)

**Goal:** Establish the repository, build, and automation scaffolding so every subsequent phase lands on solid ground.

**Release:** none (internal only)

### Deliverables

| # | Task | Owner | Depends On | Status |
|---|------|-------|------------|--------|
| 0.1 | Create `curiooneftc/CurioControl` repo, `main` + `develop` branch policy | Lead | — | Done (repo + `develop` branch) |
| 0.2 | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` (version catalog) | Core Dev | 0.1 | Done |
| 0.3 | FTC SDK dependency wiring | Core Dev | 0.2 | Done — **Maven Central, AAR unpacked.** See ADR-001 |
| 0.4 | `checkstyle.xml`, `spotbugs-exclude.xml`, formatting rules | Core Dev | 0.2 | Done — Checkstyle + suppressions, SpotBugs, Spotless, `.editorconfig` |
| 0.5 | JUnit 5 + Mockito + JaCoCo test infrastructure | Core Dev | 0.2 | Done — plus ArchUnit (ADR-010) |
| 0.6 | GitHub Actions: `build.yml` (compile + test + static analysis on PR/push) | CI/CD | 0.2 | Done — plus `docs.yml`, `perf.yml`, Dependabot |
| 0.7 | Gradle Develocity plugin (build scan + local cache) | CI/CD | 0.2 | Done — scan on failure, no account needed; remote cache needs a paid license |
| 0.8 | GitHub Packages publish pipeline (dry-run only, no real publish yet) | CI/CD | 0.6 | Done — gated on `PUBLISH_ENABLED` |
| 0.9 | `README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `LICENSE`, `.gitignore` | Docs | 0.1 | Done — **BSD 3-Clause** |
| 0.10 | JavaDoc task + MkDocs scaffold; docs site skeleton | Docs | 0.2 | Done |
| 0.11 | Base package layout `org.curioone.control.*` (empty + `package-info.java`) | Core Dev | 0.2 | Done — renamed from `dev.curio` per ADR-013 |
| 0.12 | PR template, issue template, CODEOWNERS, branch protection rules | Lead | 0.1 | Templates + CODEOWNERS done; `BRANCH_PROTECTION.md` written — **needs applying in repo settings** |
| 0.13 | Namespace reconciliation: `org.curioone` group and package per ADR-013 | Core Dev | 0.2 | Done |

### Risks in this phase

- ~~**FTC SDK artifact availability.**~~ **Resolved.** The SDK *is* published to Maven Central under
  `org.firstinspires.ftc` (6.2.0 through 12.0.0), so no vendoring and no third-party mirror is
  needed. It ships as AARs whose metadata declares the legacy `org.gradle.libraryelements`
  attribute, which a `java-library` project cannot select — a small `@CacheableTask` extracts
  `classes.jar` from each AAR instead. Pinned to **12.0.0**. See
  [`docs/adr/001-ftc-sdk-dependency-strategy.md`](./docs/adr/001-ftc-sdk-dependency-strategy.md).
- ~~**Develocity licensing.**~~ **Resolved, but not in the direction expected.** The free tier allows
  anonymous Build Scan publishing with no account, so scans work with no setup at all. It does
  **not** license the remote build cache, so Phase 0 delivers the local build cache only and the
  remote cache is deferred until there is a paid installation. See
  [`CI_CD_SETUP.md` §3](./CI_CD_SETUP.md).
- ~~**Package namespace.**~~ **Resolved.** `spec.md` §5 specified `dev.curio`; the repository lives
  under the `curiooneftc` org, so the Maven group and Java package are now `org.curioone`. Renamed
  in Phase 0, before any released artifact made it a breaking change. See
  [`docs/adr/013-package-namespace.md`](./docs/adr/013-package-namespace.md).
- **Branch protection is a repository setting, not a file.** The templates and rules are committed,
  but the "failing test blocks a PR" criterion only holds once the rules are applied in the GitHub
  settings. See [`BRANCH_PROTECTION.md`](./BRANCH_PROTECTION.md).

### Exit criteria

- [x] `./gradlew build` runs green locally — compile, tests, Checkstyle, SpotBugs, Spotless, Javadoc
- [x] `publishToMavenLocal` succeeds — jar, sources, javadoc, `.module`, `.pom` in `~/.m2`
- [x] JavaDoc generates with no errors or warnings (`-Werror`)
- [x] Repository scaffolding docs committed
- [x] Namespace consistent end to end — coordinates, packages, imports, build metadata, docs
- [ ] A PR with a deliberately failing test is blocked by branch protection — **requires applying
      [`BRANCH_PROTECTION.md`](./BRANCH_PROTECTION.md) in the repository settings**
- [ ] A Build Scan is published for a failing CI build — **requires the first red CI run**

---

## Phase 1 — v0.1.0 Foundation (Weeks 4-8)

**Goal:** Ship a usable skeleton: robot container, core hardware wrappers, telemetry, one control loop, one drivetrain, published to GitHub Packages.

**Release:** `0.1.0` (pre-1.0, API unstable)

### Milestone breakdown

#### M1.1 — Core runtime (`core`) · Week 4
- [x] `CurioRobot` — container: hardware registry, subsystem registry, lifecycle (`init`/`loop`/`stop`)
- [x] `CurioConfig` — static config flags (`DEBUG`, telemetry cadence, feature toggles)
- [x] `CurioOpMode` / `CurioAuto` base classes extending SDK `OpMode` (optional use, per SPEC §25)
- [x] `HardwareRegistry` — named-device resolution + clear failure messages (SPEC §46), over a
      `HardwareSource` port so the SDK's unmockable `HardwareMap` stays at the edge (ADR-014)
- [x] `TelemetryManager` — `add(...).update()` fluent API, categories (SPEC §26)
- [x] Error/report formatting: `[CurioControl] ERROR / Missing hardware device: <name>` plus the
      expected type, since a wrong-type config is otherwise indistinguishable from a missing device

**Exit:** `new CurioRobot(hardwareMap)` constructs on a Robot Controller with a minimal OpMode; missing-device error message matches the spec's example output.

#### M1.2 — Hardware wrappers (`hardware`) · Weeks 4-5
- [x] `Motor` (wraps `DcMotorEx`): `setPower`, `setVelocity`, `setTargetPosition`, `getPower`, `getVelocity`, `getPosition`, `resetEncoder`, direction, zero-power behavior, run mode
- [x] Power validation: **throw** outside `[-1.0, 1.0]` (SPEC §11). Decided in Phase 1: a clamped
      value means a miscalculated limit expression, and hiding that is how a mechanism drifts out of
      range unnoticed. The message names the device.
- [x] `Servo` (wraps SDK `Servo`) — generic position only; **no** `open()`/`close()` (SPEC §12: mechanism methods belong to subsystems)
- [x] `ContinuousServo` (wraps `CRServo`)
- [x] `Encoder` — position, velocity, reset, direction, `getDistance(ticksPerRev, wheelDiameterMm)`,
      `getVelocityMmPerSecond(...)`. The SDK has no standalone encoder device, so this is a view
      onto the owning motor and takes that motor's configuration name.
- [x] `IMU` wrapper — heading/roll/pitch/yaw, `headingDegrees()`, `headingPositive()` wrapped to
      `[0, 2π)`, reset, and calibration (`calibrate` returns a boolean, `requireCalibration` throws)
- [x] `VoltageSensor` wrapper
- [x] `DigitalSensor`, `AnalogSensor` thin wrappers
- [x] `HardwareType` enum for registry lookups

**Exit:** Each wrapper has a JVM unit test using a mocked `DcMotorEx`/`CRServo`/etc., plus a hardware-in-the-loop smoke test on the bench.

#### M1.3 — PIDController (`control`) · Week 6
- [x] P / I / D, delta-time handling, integral accumulation + windup guard
- [x] Integral limit, output limits, error calculation, tolerance, `atSetpoint()`, `reset()`
- [x] Pure Java — zero FTC imports
- [x] Unit tests: step response, steady-state error, anti-windup under saturation, dt variation

**Exit:** Tests green; a documented "tune a PID in 10 minutes" guide exists.

#### M1.4 — MecanumDrive + TankDrive (`drive`) · Weeks 6-7
- [x] `DriveBase` abstract contract (`drive(strafe, forward, rotation)`, `stop`,
      `setZeroPowerBehavior`). Deliberately **not** a `Subsystem` — a drivetrain has no per-loop work
      of its own, and keeping it out of the hierarchy is what lets `drive` avoid depending on `core`.
- [x] `MecanumDrive`: wheel power calc, normalization (preserves magnitude under saturation), direction config
- [x] `TankDrive`
- [x] Field-centric variant taking a heading argument (SPEC §20)
- [x] Unit tests: normalization math, direction config, zero-input behavior

**Exit:** Bench test — robot drives on a real mecanum chassis, no wheel drift on straight line.

#### M1.5 — Subsystem base (`core`) · Week 7
- [x] `Subsystem` abstract class — `init()`, `loop()`, `stop()`, telemetry hook
- [x] `robot.registerSubsystem(...)`; retrieval by name (`robot.subsystem("arm")`) or by type
      (`robot.subsystem(Arm.class)`), which returns `null` when the type is ambiguous rather than
      guessing between two intakes. Registration after `init()` throws.

**Exit:** A hand-written `Arm` subsystem compiles against the framework and is registered at runtime.

#### M1.6 — Tests, docs, release · Week 8
- [x] Unit test suite for all Phase 1 modules (≥ 70% on pure logic)
- [x] JavaDoc on 100% of public Phase 1 classes
- [x] `CHANGELOG.md` entry, `README.md` quickstart
- [ ] **Publish `0.1.0` to GitHub Packages**
- [ ] Consumed by a scratch robot project to prove the artifact resolves

### v0.1.0 Definition of Done
- [x] All Phase 1 modules unit-tested (377 tests, no Robot Controller required)
- [x] `compileJava`, `test`, Checkstyle, SpotBugs, Spotless, and the JaCoCo gate all green locally
- [ ] `implementation("org.curioone:curiocontrol:0.1.0")` resolves from GitHub Packages in a fresh
      project — **requires a push and a tag**
- [ ] JavaDoc published — **requires a push**
- [ ] Bench-validated TeleOp on a real robot — **requires hardware**

The remaining unchecked items need a push, a tag, or a robot. They are not code problems.
- [x] CHANGELOG + README updated
- [ ] Release notes published to GitHub Releases with compatibility matrix — **requires a push and a tag**

---

## Phase 2 — v0.2.0 Control (Weeks 9-13)

**Goal:** Complete the control/math toolkit: PIDF, feedforward, motion profiles, pose geometry, structured logging.

**Release:** `0.2.0`

### Milestone breakdown

#### M2.1 — PIDFController + Feedforward · Weeks 9-10
- [x] `PIDFController` = `kP·e + kI·∫e + kD·ė + kF·ff`
- [x] `Feedforward` interface + implementations: `Constant`, `Velocity`, `Acceleration`, `Gravity`
- [x] Combined model `kS + kV·v + kA·a`
- [x] Pure Java, no FTC imports; fully unit tested

**Exit:** Shared-gain simulation proves PIDF converges where plain PID shows steady-state error under load.

#### M2.2 — MotionProfile · Weeks 10-11
- [x] `MotionProfile` trapezoidal: `getPosition(t)`, `getVelocity(t)`, `getAcceleration(t)`, `isFinished(t)`
- [x] Handles `target < start` (reverse direction), zero distance, zero max velocity/accel
- [x] Validation of parameters (non-negative limits, etc.)
- [x] Unit tests across the accel / cruise / decel phases, boundary times, exact-arrival tolerance

**Exit:** `TrapezoidalProfileTest` covers all three phases and the full transition times analytically.

#### M2.3 — Geometry (`math`) · Weeks 11-12
- [x] `Pose2d`, `Vector2d`, `Rotation2d`, `Transform2d`
- [x] `Interpolation`, `Clamp`, `Units`, `Geometry` helpers
- [x] Zero Android/FTC imports — hard requirement, enforced by an ArchUnit-style test
- [x] Unit tests: rotation composition, vector rotation, transform application, unit conversions, interpolation endpoints

**Exit:** A dependency-guard test fails the build if any class in `math` imports FTC/Android.

#### M2.4 — IMU abstraction hardening · Week 12
- [x] Calibration state machine, heading reset, heading source abstraction (so `math`/`control` never see an `IMU`)
- [x] Unit tests with a fake IMU driving heading changes

#### M2.5 — Structured logging (`telemetry`) · Weeks 12-13
- [x] `Logger` with `record(key, value)`; **disabled by default** (SPEC §27)
- [x] CSV writer to `/sdcard/CurioControl/logs/`
- [x] Low-overhead: preallocated buffers, no per-iteration string building
- [x] Ring-buffer/flush policy so logging can't OOM the RC
- [x] Configurable channel/field registration
- [x] Unit tests: CSV header correctness, row formatting, buffering, disabled-mode zero-cost behavior

**Exit:** CSV output matches the spec's example format exactly; logging overhead measured and documented.

#### M2.6 — Gradle Enterprise full rollout · Week 9 (spans phase)
- [ ] Build cache enabled for CI and local
- [ ] Develocity scans on CI builds
- [ ] Build scan comparison for slow-test detection

**Exit:** CI build time reduced measurably vs. Phase 1 baseline; scan artifacts available per build.

### v0.2.0 Definition of Done
- [ ] All of `math`, `control`, `util` are FTC-free and ≥ 90% covered
- [ ] Dependency-guard tests in place and green
- [ ] Logging verified on-device (writes CSV, readable, no interference with control loop)
- [ ] Control tuning guide in `docs/guides/`
- [ ] `0.2.0` published; `0.1.0` remains resolvable
- [ ] CHANGELOG, README, JavaDoc updated

---

## Phase 3 — v0.3.0 Architecture (Weeks 14-18)

**Goal:** The composition layer — subsystems, a lightweight command system, and autonomous state machines.

**Release:** `0.3.0`

### Milestone breakdown

#### M3.1 — Command system (`command`) · Weeks 14-16
- [ ] `Command` interface/abstract: `initialize`, `execute`, `end`, `isFinished`, requirements
- [ ] `CommandScheduler` — single-threaded, called once per loop from the OpMode
- [ ] `InstantCommand`, `WaitCommand(timer)`, `SequentialCommand`, `ParallelCommand`
- [ ] Subsystem ownership/interlocks: two commands needing the same subsystem cannot run concurrently
- [ ] Cancellation + clean `end()` invocation on stop
- [ ] Unit tests: sequencing order, parallel completion semantics, requirement conflicts, cancellation mid-run, nested composition

**Exit:** A full autonomous routine runs from a command tree, and a test proves the scheduler never runs two conflicting commands.

#### M3.2 — Subsystem polish · Week 16
- [x] `Subsystem` binding to command scheduler (exclusive access semantics)
- [ ] Default `Subsystem`-provided command factories: `moveTo(pos)`, `waitUntil(tolerance)`
- [ ] Documented ownership rules

#### M3.3 — State machine · Weeks 16-18
- [ ] `StateMachine<S>` with current state, transitions, entry/update/exit actions, per-state timeouts
- [ ] Explicit vs. event-driven transition triggers
- [ ] Interop with commands (a state can schedule commands; a command can request a transition)
- [ ] Unit tests: transition graph traversal, entry/exit action ordering, timeout behavior, illegal-transition handling

**Exit:** The `AutoState` example from SPEC §24 (DRIVE_TO_SCORE → SCORE → RETURN → PARK) runs as both a state machine and a command tree, and the docs compare the two.

#### M3.4 — Telemetry command introspection · Week 17
- [ ] Scheduler state (running command, elapsed time, requirement conflicts) surfaced to telemetry
- [ ] Debug category additions

#### M3.5 — Docs + release · Week 18
- [ ] Architecture guide: "commands vs. state machines — when to use which"
- [ ] Ported example TeleOp + Autonomous in `examples/`
- [ ] `0.3.0` published

### v0.3.0 Definition of Done
- [ ] Scheduler + state machine ≥ 90% line coverage (both are pure logic)
- [ ] Example robot in `examples/` compiles against `0.3.0` and runs on a robot
- [ ] No allocation regression in the scheduler hot path (measured)
- [ ] `0.3.0` published; all prior versions resolvable

---

## Phase 4 — v0.4.0 Vision (Weeks 19-22)

**Goal:** Optional, opt-in vision support that doesn't burden teams who don't need it.

**Release:** `0.4.0`

### Milestone breakdown

#### M4.1 — VisionManager · Weeks 19-20
- [ ] `VisionManager` — lifecycle (`init`, `process`, `stop`), processor registration
- [ ] Lazy init: no vision object allocated unless the team touches `robot.vision()`
- [ ] Configurable update rate (vision must not starve the control loop)

#### M4.2 — AprilTagManager · Weeks 20-21
- [ ] `AprilTagManager` wrapping SDK AprilTag processor
- [ ] `getTag(int id)` → `Optional<AprilTagDetection>` (SPEC §28)
- [ ] `getRobotPose()` → `Pose2d`, mapping `math.Pose2d` ⇄ SDK pose types
- [ ] Field-relative calibration helpers
- [ ] Unit tests with mocked detections: tag filtering, pose conversion round-trip, empty-result handling

#### M4.3 — Vision + control integration · Week 21
- [ ] Aim assist / pose-based target heading helper built on `math` + `control` (no vision imports in `control`)
- [ ] Guarded so a `control` user never needs a vision dependency

#### M4.4 — Docs + release · Week 22
- [ ] Vision setup guide (hardware config, camera calibration, field layout)
- [ ] Performance notes (frame rate, dropped-frame tolerance)
- [ ] `0.4.0` published

### v0.4.0 Definition of Done
- [ ] A team that never touches `robot.vision()` sees zero added overhead (verified by allocation/timing measurement)
- [ ] AprilTag pose pipeline validated on a real field
- [ ] Vision guide complete with screenshots/diagrams
- [ ] `0.4.0` published

---

## Phase 5 — v1.0.0 Stable (Weeks 23-26)

**Goal:** Freeze the API, finish documentation, harden quality, ship the example project and migration docs.

**Release:** `1.0.0`

### Milestone breakdown

#### M5.1 — API audit & stabilization · Weeks 23-24
- [ ] Full public-API review across all modules
- [ ] Naming consistency pass (`getX()` vs `x()`, boolean `isX()` vs `hasX()`)
- [ ] Package/export surface freeze
- [ ] Anything unstable promoted, deprecated, or removed
- [ ] Public API snapshot test (a golden-file/signature test) to lock the surface

#### M5.2 — Documentation completion · Weeks 23-25
- [ ] Every public class/method has JavaDoc (enforced by a CI check that fails on missing docs)
- [ ] `docs/` site complete: quickstart, architecture, each-module guides, tuning, troubleshooting
- [ ] API reference published
- [ ] `MIGRATION.md` (0.x → 1.0) with before/after snippets
- [ ] `README.md` as the front door: 60-second quickstart

#### M5.3 — Quality hardening · Weeks 24-25
- [ ] ≥ 80% line coverage on all pure logic; ≥ 70% on hardware wrappers
- [ ] Error-message audit against SPEC §46 ("fail predictably, never silently substitute hardware")
- [ ] Performance pass: allocation-free hot paths in OpMode loop, scheduler, PID, drive, telemetry (SPEC §45)
- [ ] Security review against SPEC §44 (no credentials, no network, no telemetry transmission)
- [ ] Dependency audit (only FTC SDK + test libs)

#### M5.4 — Example project · Weeks 24-26
- [ ] `examples/robot/` — a complete, runnable FTC robot project using `1.0.0`
- [ ] Matches SPEC §40 layout: `opmode/`, `subsystem/`, `config/RobotConfig.java`
- [ ] Example TeleOp (SPEC §41) and example Autonomous (SPEC §42)
- [ ] `examples/README.md` with setup instructions
- [ ] Builds and runs on Robot Controller; verified on a real robot

#### M5.5 — Release engineering · Week 26
- [ ] Version pinned to `1.0.0` everywhere
- [ ] Compatibility matrix filled in (CurioControl / FTC SDK / Android SDK / Java / Gradle) — SPEC §37
- [ ] `1.0.0` published to GitHub Packages
- [ ] GitHub Release with full notes, migration guide, compatibility table
- [ ] Announcement post
- [ ] Post-1.0 support policy published (see `RELEASE_STRATEGY.md`)

### v1.0.0 Definition of Done
- [ ] API frozen; snapshot test green
- [ ] 100% public API documented (CI-verified)
- [ ] Coverage thresholds met and enforced
- [ ] `examples/robot` runs on real hardware
- [ ] `MIGRATION.md`, `README.md`, docs site, CHANGELOG all current
- [ ] `1.0.0` published and consumed successfully by a clean project
- [ ] Compatibility matrix published
- [ ] No open P0/P1 bugs

---

## Backlog — Post-v1.0.0

Per SPEC §48, explicitly deferred until core is stable:

- [ ] Simulation harness
- [ ] Path planning
- [ ] Localization / odometry fusion
- [ ] Trajectory following
- [ ] Dashboard / configuration UI
- [ ] Robot log viewer
- [ ] Tuning tools
- [ ] FTC scouting integration
- [ ] Maven Central publishing
- [ ] Kotlin extension APIs

---

## Cross-Phase Workstreams

These run continuously across all phases rather than in any single one:

| Workstream | Activities |
|------------|------------|
| **Documentation** | JavaDoc per PR, guide updates, changelog upkeep |
| **Testing** | Coverage growth, new test targets per module |
| **CI/CD** | Pipeline reliability, cache tuning, publish hardening |
| **Benchmarking** | Hot-path allocation/time tracking (starts Phase 2) |
| **Robot time** | Bench/HIL validation slots for hardware-dependent work |
| **Community** | Issue triage, contributor onboarding, examples |

---

## Phase Exit Checklist (applies to every phase)

- [ ] All milestone deliverables complete
- [ ] Tests written and passing; coverage at or above phase target
- [ ] Static analysis clean (Checkstyle, SpotBugs, Error Prone)
- [ ] CI green on `main`
- [ ] JavaDoc complete for all new public APIs
- [ ] Docs site updated with any new concepts
- [x] `CHANGELOG.md` updated
- [ ] Version bumped appropriately per SemVer
- [ ] Release notes drafted
- [ ] Phase retrospective held; action items filed
