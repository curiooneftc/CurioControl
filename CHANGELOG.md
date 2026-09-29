# Changelog

All notable changes to CurioControl are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Foundation

First usable release: a mecanum robot that drives, and the layer everything else builds on.

### Added
- **`core` runtime.** `CurioRobot` is the composition root — hardware registry, subsystem
  registry, and the `init`/`loop`/`stop` lifecycle. `Subsystem` is the base for a mechanism
  with no-op defaults on every hook. `CurioOpMode` and `CurioAuto` are optional SDK `OpMode`
  bases; a plain `OpMode` holding a `CurioRobot` works identically (SPEC §25).
- **`HardwareSource` port.** A one-method interface over named device lookup, with
  `SdkHardwareSource` adapting the Robot Controller's `HardwareMap` at the edge. This exists
  because `HardwareMap` reaches into `android.content.Context` and cannot be mocked on a
  desktop JVM, so anything typed to it was untestable off-robot. See ADR-014.
- **`HardwareRegistry`.** Named-device resolution that returns the configured device or throws
  naming both the device and the expected type. It never substitutes another device. A wrong-type
  config is reported exactly like a missing device, because from the call site they are the
  same failure. Supports `has(...)` for genuinely optional hardware and `resolvedNames()` for
  diagnostics.
- **`TelemetryManager`.** Fluent `add(...).update()` that batches every value into one SDK call
  per flush. Categories are opt-in: `add("Voltage", v)` produces `Voltage`, while
  `add(TelemetryCategory.DRIVE, "Heading", h)` produces `[DRIVE] Heading`. `DEBUG` values are
  dropped unless `CurioConfig.DEBUG` is on, so diagnostics can stay in the code. Flushes at most
  once per `CurioConfig.TELEMETRY_PERIOD_MILLIS` (default five times a second); a call inside the
  interval leaves the buffer intact rather than dropping values.
- **`hardware` wrappers.** `Motor`, `Encoder`, `Servo`, `ContinuousServo`, `IMU`,
  `DigitalSensor`, `AnalogSensor`, `VoltageSensor`, and the `HardwareType` enum. Each validates
  arguments, names units, and delegates — and exposes `getSdkObject()` so the SDK is always one
  call away. There is deliberately no `open()`/`close()` on `Servo`: a servo can be a claw, a
  wrist, or a deploy arm, and the framework does not know which.
- **`PIDController`.** Delta-time P/I/D with an integral limit, output limits, a setpoint
  tolerance, and conditional anti-windup. Pure Java — no FTC imports.
- **`drive`.** `DriveBase` (strafe/forward/rotation), `MecanumDrive` with field-centric support,
  and `TankDrive`. Mecanum powers are scaled by one common factor so saturation reduces
  magnitude without distorting direction. A tank chassis discards the strafe axis rather than
  pretending to strafe.
- **`util`.** `Clock` and `SystemClock` (monotonic, `nanoTime`-backed, injectable for tests) and
  `Range`, the single definition of the legal motor-power and servo-position ranges that the
  wrappers and their tests both validate against.

### Decisions
- **Power and position outside their legal range throw `IllegalArgumentException` rather than
  clamping** (SPEC §11). A clamped value means a limit expression was miscalculated, and hiding
  that is how a mechanism drifts out of range without anyone noticing. The message names the
  device.
- **No standalone encoder device exists in the SDK** — an encoder is reached through the motor it
  is built into, so `robot.encoder(name)` takes the owning motor's configuration name and
  `HardwareType.ENCODER` resolves to `DcMotorEx`. This is the only way to reach one.
- **`IMU` mounting parameters come from the caller.** A silently failed calibration produces a
  heading with a constant offset, and field-centric drive built on that drifts in a way that is
  very hard to notice. `requireCalibration(...)` turns it into a bench-time error.
- **`CurioRobot` is the only class in `core` permitted to depend outward**, and `drive` may not
  depend on `core` at all — name-to-device resolution is the composition root's job. Enforced by
  ArchUnit, which caught a real attempt to add registry constructors to the drivetrains.

### Testing
- 377 unit tests, no Robot Controller required.
- Architecture guard tests: the pure-Java boundary of `math`/`control`/`util`, the package
  layering, and the `CurioRobot` composition-root exception (ADR-010, ADR-014).
- JaCoCo coverage gate enabled at 70% on pure logic only. `util` is at 100%, `control` at 98%.
  `hardware`, `drive`, and `core` are excluded deliberately: they are thin delegations, where high
  line coverage would say the delegation happened rather than that the robot works.

### Changed
- Maven coordinates are `org.curioone:curiocontrol` and the Java package is
  `org.curioone.control`, following the GitHub org rather than `spec.md` §5's
  `dev.curio`. Done before any released artifact. See ADR-013.
- `AnalogSensor.fraction()` is documented as unclamped: a reading above the configured maximum is
  returned as measured, so a misconfigured channel is visible rather than hidden.
- `CurioRobot.registerSubsystem(...)` throws if called after `init()`. A subsystem registered
  late would never receive its `init()` and would run against hardware it never configured — a
  failure that otherwise surfaces on the field.

### Documentation
- Quickstart, drivetrain guide, and hardware-abstraction guide updated to the real API.
- ADR-014: `CurioRobot` as composition root, and hardware behind a port.

## [Unreleased]

### Added
- Gradle build with a version catalog, Java 17 toolchain (auto-provisioned), and
  configuration cache.
- FTC SDK `12.0.0` wired as a compile-only dependency, resolved from Maven Central
  (ADR-001). The published artifact contains no SDK classes; the Robot Controller
  provides them at runtime.
- Static analysis: Checkstyle, SpotBugs, and Spotless (google-java-format AOSP
  style), all enforced by `./gradlew build`.
- Test infrastructure: JUnit 5, Mockito, ArchUnit, and JaCoCo.
- Architecture guard tests for the pure-Java boundary of `math`, `control`, and
  `util`, and for the package layering in `docs/PROJECT_STRUCTURE.md` (ADR-003,
  ADR-010).
- `CurioConfig` with framework-wide flags and a build-derived version constant.
- Base package layout `org.curioone.control.*` with a `package-info.java` per package.
- Develocity Build Scan publishing on build failure, with no account or API key
  required. Terms-of-service acceptance is declared in `settings.gradle.kts` and
  therefore applies to every contributor.
- GitHub Actions: `build.yml`, `release.yml` (dry-run publish until enabled),
  `docs.yml`, `perf.yml`, and Dependabot.
- Repository scaffolding docs: `README.md`, `CONTRIBUTING.md`, `LICENSE`
  (BSD 3-Clause), `MIGRATION.md`, `BRANCH_PROTECTION.md`, and a MkDocs site skeleton.

[Unreleased]: https://github.com/curiooneftc/CurioControl/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/curiooneftc/CurioControl/releases/tag/v0.1.0
