# CurioControl Testing Strategy

> **Source spec:** [`spec.md`](./spec.md) §32 (Testing), §45 (Performance), §46 (Competition Safety), §49 (Definition of Done)  
> **Principle:** *Anything that does not require physical robot hardware should be unit-testable.*

---

## 1. Testing Philosophy

| Principle | Meaning |
|-----------|---------|
| **Test the pure logic hard** | `math`, `control`, `util`, `command` must have near-total coverage — they run on a desktop JVM with zero setup. |
| **Mock the hardware boundary** | `hardware` wrappers are tested against mocked SDK objects, not real devices. |
| **Validate the robot for real** | A small, deliberate set of hardware-in-the-loop checks; not a substitute for unit tests, but a necessary final gate. |
| **Enforce the architecture** | Dependency rules (e.g. `math` must not import FTC) are *tests*, not conventions. |
| **Fast feedback** | The whole unit suite should run in well under a minute. |
| **Deterministic** | No wall-clock, no randomness without a fixed seed, no network, no filesystem beyond a temp dir. |

---

## 2. Test Pyramid

```text
            ╱╲
           ╱  ╲          Hardware-in-the-loop (manual/bench)
          ╱────╲         ~10 checks, on a real robot, pre-release only
         ╱      ╲
        ╱────────╲       Integration tests (framework wiring, mocked SDK)
       ╱          ╲      ~20% of tests
      ╱────────────╲
     ╱              ╲    Unit tests (pure logic) — the bulk
    ╱                ╲   ~80% of tests
   ╱──────────────────╲
  ╱                    ╲  Static analysis: Checkstyle, SpotBugs, Error Prone
 ╱______________________╲ Architecture tests (dependency rules, public API surface)
```

---

## 3. Test Tiers

### 3.1 Unit tests (bulk of the suite)
Fast, hermetic JVM tests for everything that does not touch the SDK.

| Target | What to test | Notes |
|--------|--------------|-------|
| `PIDController` | Step response, steady-state error, integral windup under saturation, dt variation, output limits, tolerance/`atSetpoint`, `reset` | Reference implementation of "test a controller properly" |
| `PIDFController` | Same as PID plus feedforward contribution; separate verification of each term | |
| `Feedforward` | Each model returns expected value for known inputs | |
| `MotionProfile` | All three phases (accel/cruise/decel), transition times, reverse target, zero distance, `isFinished` at/after completion | |
| `Pose2d` / `Vector2d` / `Rotation2d` / `Transform2d` | Construction, rotation composition, vector rotation, transform application, plus/minus, norm | |
| `Interpolation` | Endpoints, midpoints, extrapolation | |
| `Clamp` / `Units` | Boundaries, unit round-trips | |
| `CommandScheduler` | Sequencing order, parallel completion, requirement conflict detection, cancellation, `end()` on stop, nested composition | |
| `StateMachine` | Transition traversal, entry/exit action ordering, timeouts, illegal transitions | |
| `MecanumDrive` / `TankDrive` | Power math, normalization preserves magnitude, direction config, zero input | Uses a fake motor set, not the SDK |
| `Range` / `Timer` / `RateLimiter` / `Debouncer` / `EdgeDetector` | Edge cases, timing behavior (with an injectable clock, not `System.nanoTime` directly) | |
| `Logger` (CSV) | Header, row format, buffering, disabled mode is a no-op | Temp-dir output |

### 3.2 Integration tests (mocked SDK)
Wire real framework components together, but mock the FTC SDK at the boundary.

- `Motor` wrapping a mocked `DcMotorEx`: verify calls map to the right SDK methods with the right args, including power clamping and mode changes.
- `Encoder.getDistance(...)` against a mocked encoder that returns known tick counts.
- `TelemetryManager` with a mocked telemetry sink: verify `add(...).update()` produces the expected line.
- `CurioRobot` construction with a mocked `HardwareMap`: verify subsystem registration, lifecycle calls, and **missing-hardware error behavior** (SPEC §46).
- `IMU` wrapper with a mocked `IMU`: heading source used by `drive`.

Use **Mockito** for SDK interfaces. Because the FTC SDK classes are final in places, plan for the inline mock maker (see ADR-001) so mocking works cleanly.

### 3.3 Architecture / guard tests (cheap, high value)
Tests that protect structural rules rather than behavior.

| Guard test | Enforces |
|------------|----------|
| `MathPackageIsPure` | No class in `math` imports `com.qualcomm.*` or `android.*` (SPEC §29, §31) |
| `ControlPackageIsPure` | No class in `control` imports FTC/Android (SPEC §16) |
| `NoLowerLayerDependsOnHigher` | Layering from SPEC §6 — e.g. `math` never imports `hardware` |
| `NoVisionDependencyForNonVision` | `vision` is not pulled into `core`/`control`/`drive` |
| `PublicApiSnapshot` | The set of public signatures matches a committed golden file (Phase 5) — locks the API |
| `NoAllocationInHotPath` | (Performance) JMH micro-benchmarks for scheduler/PID/drive; enforced as a budget, not a hard test |

> **Tooling:** ArchUnit (JVM, works great for these) — a Phase 1/2 deliverable.

### 3.4 Hardware-in-the-loop (bench) validation
Small, purposeful, and manual. These do **not** replace unit tests. Run before each minor/major release and on the real robot.

| Check | What it validates |
|-------|-------------------|
| Boot a `MainTeleOp` with a mecanum base | `CurioRobot` constructs, drive initializes, no crash on real hardware config |
| Drive straight, strafe, rotate | Power math and direction config are physically correct |
| Field-centric drive with IMU | Heading source and rotation math are correct |
| Arm moves to configured positions, holds | `setTargetPosition` / run modes behave as expected |
| Missing-device error path | Rename a device in the config and confirm the SPEC §46 error message appears clearly |
| Telemetry + CSV logging on-device | Logs write, are readable off the RC, and don't stall the loop |
| (Phase 4) AprilTag pose | Pose conversion is physically correct on a known tag layout |
| Autonomous routine end-to-end | The composed command/state-machine routine completes reliably (run it several times) |

### 3.5 Performance testing
Per SPEC §45 (avoid allocations in robot loops), use **JMH micro-benchmarks** for the hot paths, run in CI on a schedule (not every PR, to save time):

- `CommandScheduler.run()` per-iteration allocations
- `PIDController.calculate()` allocations
- `MecanumDrive` power calc allocations
- `TelemetryManager` update allocations
- Logging in disabled mode (should be ~0)

Track these as **budgets** (e.g. "0 bytes/op for PID calculate") and alert on regression. This turns the "no unnecessary allocations" requirement into something measurable.

---

## 4. Test Coverage Targets

Coverage is measured with **JaCoCo** and enforced in CI.

| Layer | Target (line coverage) | Gate |
|-------|------------------------|------|
| `math` | ≥ 95% | Hard gate |
| `control` | ≥ 90% | Hard gate |
| `util` | ≥ 90% | Hard gate |
| `command` | ≥ 90% | Hard gate |
| `drive` (pure calc) | ≥ 90% | Hard gate |
| `hardware` (wrapper logic) | ≥ 70% | Warning, tracked |
| `telemetry` / `vision` / `core` (wiring) | ≥ 60% | Warning, tracked |

Per-phase minimums (from `PHASES.md`):
- v0.1.0: ≥ 70% on pure logic
- v0.2.0: ≥ 90% on `math`/`control`/`util`
- v0.3.0: ≥ 90% on `command`
- v1.0.0: hit the table above across the board

---

## 5. Mocking Strategy

- **JUnit 5** for tests, **Mockito** (with inline mock maker) for SDK objects.
- Create shared **fakes** rather than deep mock chains where behavior matters:
  - `FakeDcMotorEx` — simulates position/velocity integration so drive/PID tests can run a closed loop deterministically.
  - `FakeIMU` — programmable heading.
  - `FakeClock` — injectable time for `Timer`, `RateLimiter`, `Debouncer`, `MotionProfile`, `StateMachine` timeouts.
- Inject time everywhere via an interface (`org.curioone.control.util.Clock`) so no test depends on real time. This is a small design constraint adopted in Phase 0/1 to keep everything testable.
- Never mock CurioControl's own pure classes — test them directly; they're deterministic.

---

## 6. CI Test Integration

Every pull request runs (see `CI_CD_SETUP.md`):
1. Compile
2. Unit + integration + architecture tests (JaCoCo report uploaded)
3. Static analysis (Checkstyle, SpotBugs)
4. Javadoc build (fail on errors)
5. Coverage gate check (fail if below target for gated packages)
6. Build artifact

A PR is **not mergeable** if the library does not compile or unit tests fail (SPEC §33). Coverage regressions on gated packages also block.

`main`-branch and release builds run additionally:
7. Example project compile check (against the published artifact)
8. Performance benchmarks (scheduled)
9. Publish (release builds only, see `RELEASE_STRATEGY.md`)

---

## 7. Test Data & Fixtures

- **Deterministic seed** for any randomized test (e.g. mecanum noise simulation) — `Random(seed)` with a fixed seed constant.
- **Physical constants** (wheel diameter, ticks/rev, gear ratio) live in test fixtures, never in `main`. The framework must not contain competition constants (SPEC §9).
- Sample CSV logs for logging tests committed under `src/test/resources/`.
- AprilTag detection fixtures (serialized detections) for vision pose tests.

---

## 8. Bug Workflow

1. **Reproduce in a test first.** Every bug fix lands with a failing test that becomes the regression test.
2. Classify: framework bug vs. user misuse vs. SDK issue.
3. If it's a P0 (robot won't init, crash in a loop, silent wrong behavior), follow the hotfix path in `RELEASE_STRATEGY.md`.
4. Fix on `develop` unless it's an active-release P0 (then `hotfix/*`).
5. Add a note to `CHANGELOG.md` under `Fixed`.

---

## 9. Definition of Done — Testing (per feature, SPEC §49)

- [ ] Unit tests exist where applicable (logic that runs on JVM)
- [ ] Hardware-dependent code has a mocked test where practical
- [ ] Architecture guard tests still pass (no layering violations introduced)
- [ ] Coverage at or above the gated target for the affected package
- [ ] A bug fix includes a regression test
- [ ] Performance-sensitive changes include a benchmark measurement
- [ ] CI passes (tests green, coverage gate green)
