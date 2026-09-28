# CurioControl Project Structure

> **Source spec:** [`spec.md`](./spec.md) §5 (Package Identity), §6 (Architecture), §7 (Core Modules), §40 (Example Project)  
> **Goal:** Define the repository layout, module boundaries, and package structure up front so the build stays coherent as the framework grows.

---

## 1. Repository Layout

```text
CurioControl/
├── README.md                          # The front door: 60-second quickstart
├── spec.md                            # The source specification
├── IMPLEMENTATION_PLAN.md             # This plan set
├── PHASES.md
├── RELEASE_STRATEGY.md
├── DOCUMENTATION_PLAN.md
├── TESTING_STRATEGY.md
├── CI_CD_SETUP.md
├── PROJECT_STRUCTURE.md
├── ARCHITECTURE_DECISIONS.md
├── BRANCH_PROTECTION.md               # Required repository settings
├── CONTRIBUTING.md
├── CHANGELOG.md
├── CHANGELOG_TEMPLATE.md
├── MIGRATION.md                       # Written at each major release
├── LICENSE                            # BSD 3-Clause
│
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml             # Version catalog — the single source of tool versions
│   └── wrapper/
│
├── buildSrc/                          # Build logic (the AAR unpacker)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/kotlin/UnpackAarClasses.kt
│
├── config/
│   ├── checkstyle/
│   │   ├── checkstyle.xml
│   │   └── suppressions.xml           # Documented, deliberate exceptions only
│   └── spotbugs/exclude.xml
│
├── src/
│   ├── main/java/org/curioone/control/   # The library
│   ├── main/resources/
│   ├── test/java/org/curioone/control/   # Unit + integration + architecture tests
│   └── test/resources/                # Fixtures (CSV samples, tag detections)
│
├── benchmark/                         # JMH micro-benchmarks (hot paths) — Phase 2
│   └── src/main/java/org/curioone/control/bench/
│
├── docs/                              # MkDocs guides site (docs_dir)
│   ├── index.md
│   ├── getting-started/
│   ├── guides/
│   ├── concepts/
│   ├── reference/
│   ├── adr/                           # One file per ADR
│   └── contributing/
│
├── examples/
│   ├── README.md
│   ├── robot/                         # Full runnable FTC robot project — Phase 5
│   │   ├── build.gradle.kts
│   │   ├── settings.gradle.kts
│   │   └── TeamCode/src/main/java/org/curioone/robot/
│   │       ├── opmode/{MainTeleOp,BlueAuto}.java
│   │       ├── subsystem/{Arm,Intake,Drive}.java
│   │       └── config/RobotConfig.java
│   └── snippets/                      # Copy-paste fragments
│
├── .github/
│   ├── workflows/{build,release,docs,perf}.yml
│   ├── dependabot.yml
│   ├── CODEOWNERS
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/
│
├── requirements-docs.txt
├── mkdocs.yml
├── .markdownlint.json
├── .editorconfig
├── .gitignore
└── README.md
```

> The plan documents live at the repository root, not under `docs/`, because `docs/` is the
> MkDocs `docs_dir` and anything there becomes a published page. The spec, the plan, and the
> guides are three different audiences; keeping them apart stops a planning document from
> showing up in the user-facing quickstart.

---

## 2. Build Strategy: Single Module vs. Multi-Module

**Decision: single Gradle module with strict package boundaries (ADR-002).**

Rationale:
- Consumers add **one** dependency. A single artifact is far simpler for competition teams.
- A multi-module setup (`curiocontrol-core`, `curiocontrol-math`, …) would let a pure-Java user avoid FTC coupling — but in practice FTC competition projects always have the SDK, and the extra complexity isn't worth it for v1.0.0.
- Package-level purity is enforced by **ArchUnit tests** instead of module boundaries, which gives most of the benefit with none of the friction.

If the pure-Java story ever becomes important (e.g. desktop simulation tools), the packages can be split into modules later without changing package names.

---

## 3. Package Structure (`org.curioone.control`)

```text
org.curioone.control
├── core/            # CurioRobot, CurioConfig, CurioOpMode, CurioAuto, Subsystem, HardwareRegistry
├── hardware/        # Motor, Servo, ContinuousServo, Encoder, IMU, DigitalSensor, AnalogSensor
├── control/         # PIDController, PIDFController, Feedforward, MotionProfile
├── drive/           # DriveBase, MecanumDrive, TankDrive
├── command/         # Command, CommandScheduler, Sequential/Parallel/Instant/WaitCommand
├── telemetry/       # TelemetryManager, Logger (CSV), categories
├── vision/          # VisionManager, AprilTagManager
├── math/            # Pose2d, Vector2d, Rotation2d, Transform2d, Interpolation, Clamp, Units, Geometry
└── util/            # Range, Timer, RateLimiter, Debouncer, EdgeDetector, Clock
```

Each package gets a `package-info.java` with a short description (also feeds the Javadoc overview).

### 3.1 Layering rules (enforced by ArchUnit)

```text
core     → (nothing internal)
math     → (nothing internal)
util     → (nothing internal)
control  → math, util
hardware → core, math, util
drive    → hardware, control, math, util
command  → core, util
telemetry→ core, util
vision   → hardware, math, util
```

Additional hard rules:
- `math` and `util` import **no** FTC (`com.qualcomm.*`) or `android.*` types.
- `control` imports **no** FTC or Android types.
- Nothing imports `vision` except `vision` itself (and `core`'s lazy accessor, if used).
- No cycles between any packages.

---

## 4. Module → Phase Mapping

| Package | Introduced in | Key types |
|---------|---------------|-----------|
| `core` | v0.1.0 | `CurioRobot`, `CurioConfig`, `CurioOpMode`, `Subsystem`, `HardwareRegistry` |
| `hardware` | v0.1.0 | `Motor`, `Servo`, `ContinuousServo`, `Encoder`, `IMU` |
| `util` | v0.1.0 | `Range`, `Timer`, `Debouncer`, `Clock` (added as needed) |
| `math` | v0.2.0 | `Vector2d`, `Rotation2d`, `Pose2d`, `Transform2d`, `Clamp`, `Units` |
| `control` | v0.1.0 (PID), v0.2.0 (rest) | `PIDController`, `PIDFController`, `Feedforward`, `MotionProfile` |
| `telemetry` | v0.1.0 (telemetry), v0.2.0 (logging) | `TelemetryManager`, `Logger` |
| `drive` | v0.1.0 | `MecanumDrive`, `TankDrive`, `DriveBase` |
| `command` | v0.3.0 | `Command`, `CommandScheduler`, `SequentialCommand`, …, `StateMachine` |
| `vision` | v0.4.0 | `VisionManager`, `AprilTagManager` |

> `math` is listed as v0.2.0 because `v0.1.0` only needs simple `Vector2d`-free math; the full geometry stack lands with the control work. Small pieces of `util` (like `Range`, `Timer`) appear in v0.1.0 as hardware/telemetry need them.

---

## 5. Key Class Design Notes

### 5.1 `CurioRobot` (`core`)
The container and main entry point.

```java
CurioRobot robot = new CurioRobot(hardwareMap);
robot.drive();
robot.arm();
robot.imu();
```

Responsibilities: hardware registration, subsystem registration, shared services, telemetry, lifecycle.

Design notes:
- Subsystems registered via `registerSubsystem(...)` and exposed via typed accessors.
- Accessors return the subsystem or a lazily-created shared service (e.g. `telemetry()`).
- Lifecycle is explicit: `init()` → `loop()` (called each OpMode iteration) → `stop()`.

### 5.2 Hardware wrappers (`hardware`)
- Wrap, don't hide. Each wrapper holds the underlying SDK object and exposes it via an `unwrap()`/`getSdkObject()` so direct SDK access stays possible (SPEC §10).
- Missing devices fail loudly with the SPEC §46 message format — never silently substitute.
- `Servo` is generic: no `open()`/`close()`. Those belong in a subsystem (SPEC §12).

### 5.3 `Motor` (`hardware`)
Supports the full SPEC §11 surface. Validate inputs where practical (power ∈ [-1, 1]); decide and document whether out-of-range power clamps or throws.

### 5.4 `math` (`Pose2d`, `Vector2d`, `Rotation2d`, `Transform2d`)
Pure Java, no FTC dependencies, heavily unit-tested. These are building blocks used across `control`, `drive`, and `vision`.

### 5.5 `control`
- `PIDController` and `PIDFController` are hardware-independent and time-injected (`Clock` interface) for testability.
- `Feedforward` is a small family of models, combinable.
- `MotionProfile` is a pure function of time.

### 5.6 `command`
- `CommandScheduler` is single-threaded, driven from the OpMode loop; no background threads.
- `Command` declares required subsystems; the scheduler enforces exclusivity.
- `StateMachine` interoperates with commands but is independent of them.

### 5.7 `telemetry`
- `TelemetryManager` — fluent `add(...).update()`.
- `Logger` — structured CSV, **disabled by default** (SPEC §27), low overhead.

### 5.8 `vision`
- `VisionManager` lazily initializes only when `robot.vision()` is touched.
- `AprilTagManager` wraps the SDK processor and converts to `math` types.

---

## 6. Robot Configuration Pattern (consumer-side, SPEC §9)

Competition-specific constants live in the **robot project**, never in the framework:

```java
public final class RobotConfig {
    public static final class Drive {
        public static final double WHEEL_DIAMETER_MM = 96.0;
        public static final double GEAR_RATIO = 1.0;
        public static final double TICKS_PER_REV = 537.7;
    }
    public static final class Arm {
        public static final int HOME = 0;
        public static final int LOW = 300;
        public static final int HIGH = 850;
    }
}
```

The framework accepts these as parameters (e.g. `encoder.getDistance(ticksPerRev, wheelDiameterMm)`), keeping it free of competition constants.

---

## 7. Example Project Layout (SPEC §40)

`examples/robot/` mirrors a real competition repository:

```text
examples/robot/
├── build.gradle.kts
├── settings.gradle.kts
└── TeamCode/src/main/java/org/curioone/robot/
    ├── opmode/
    │   ├── MainTeleOp.java     # SPEC §41
    │   └── BlueAuto.java       # SPEC §42
    ├── subsystem/
    │   ├── Arm.java
    │   ├── Intake.java
    │   └── Drive.java
    └── config/
        └── RobotConfig.java
```

This project is **compiled in CI against the published artifact** to guarantee the documented usage stays correct.

---

## 8. Conventions

| Aspect | Convention |
|--------|------------|
| Language level | Java 17 |
| Javadoc | Required on all public API (CI-enforced) |
| Naming | `getX()` for values, `isX()`/`hasX()` for booleans |
| Nullability | Avoid `null` in the public API where possible; prefer `Optional` for "maybe" returns (e.g. `getTag`) |
| Exceptions | `IllegalArgumentException` for bad arguments; a clear framework exception for hardware/config problems |
| Logging | Never `System.out`; use the telemetry/logger abstraction |
| Imports | No wildcard imports (Checkstyle) |
| Formatting | Enforced by Checkstyle; IDE config committed (`.editorconfig`) |

---

## 9. Testing Layout

```text
src/test/java/org/curioone/control/
├── math/            # Pure geometry tests
├── control/         # PID, PIDF, feedforward, profile tests
├── util/            # Timer, debouncer, etc.
├── command/         # Scheduler + state machine tests
├── drive/           # Drive calc tests (fake motors)
├── hardware/        # Wrapper tests (mocked SDK)
├── telemetry/       # Telemetry + CSV logging tests
├── vision/          # AprilTag pose tests (mocked detections)
├── architecture/    # ArchUnit rules: layering, purity, API surface
└── support/         # Fakes: FakeDcMotorEx, FakeIMU, FakeClock, FakeTelemetrySink
```

---

## 10. Documentation Layout

See `DOCUMENTATION_PLAN.md` for the full docs site structure. Key points:
- Guides live in `docs/` as Markdown, built with MkDocs.
- Javadoc generated per release.
- `docs/adr/` holds Architecture Decision Records.

---

## 11. Related Documents

- [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md) — Single-module vs. multi-module, FTC dependency, etc.
- [PHASES.md](./PHASES.md) — When each package is introduced
- [TESTING_STRATEGY.md](./TESTING_STRATEGY.md) — Test layout and coverage targets
- [CI_CD_SETUP.md](./CI_CD_SETUP.md) — Build configuration reference
