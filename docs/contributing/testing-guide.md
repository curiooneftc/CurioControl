# Testing Guide

The full strategy is in
[`TESTING_STRATEGY.md`](https://github.com/curiooneftc/CurioControl/blob/main/TESTING_STRATEGY.md).

## Principles

- **Test the pure logic hard.** `math`, `control`, `util`, and `command` run on a desktop JVM with
  zero setup. That is where nearly all the tests belong.
- **Mock the hardware boundary.** `hardware` wrappers are tested against mocked SDK objects.
- **Enforce the architecture as tests.** The dependency rules are ArchUnit tests, not conventions.
- **Be deterministic.** No wall-clock, no unseeded randomness, no network, no filesystem beyond a
  temp directory.
- **Be fast.** The unit suite should run in well under a minute.

## Test tiers

| Tier | What | Roughly |
|---|---|---|
| Unit | Pure logic, no hardware | 80% of tests |
| Integration | Framework components wired together, SDK mocked | 20% |
| Architecture | ArchUnit rules | A dozen tests, high value |
| Hardware-in-the-loop | Manual, on a real robot | ~10 checks, pre-release only |

## Fakes over mock chains

Where behavior matters, use a shared fake rather than deep mock chains:

| Fake | For |
|---|---|
| `FakeDcMotorEx` | Closed-loop PID and drive tests — simulates position/velocity integration |
| `FakeIMU` | Programmable heading for field-centric drive |
| `FakeClock` | Any time-dependent component, driven by hand |

Never mock CurioControl's own pure classes. They are deterministic; test them directly.

## Coverage targets

| Layer | Target | Gate |
|---|---|---|
| `math` | ≥ 95% | Hard |
| `control` | ≥ 90% | Hard |
| `util` | ≥ 90% | Hard |
| `command` | ≥ 90% | Hard |
| `drive` (pure calc) | ≥ 90% | Hard |
| `hardware` (wrapper logic) | ≥ 70% | Tracked |
| `telemetry` / `vision` / `core` | ≥ 60% | Tracked |

JaCoCo measures this, and CI enforces the gates.

## Bug workflow

1. **Write the failing test first.** It becomes the regression test.
2. Classify: framework bug, user misuse, or SDK issue.
3. Fix on `develop`, unless it is a P0 on an active release, in which case use `hotfix/*`.
4. Add a note to `CHANGELOG.md` under **Fixed**.

## Running the tests

```bash
./gradlew test                                  # everything
./gradlew test --tests '*PIDControllerTest'      # one class
./gradlew jacocoTestReport                      # coverage report
./gradlew build                                 # tests plus static analysis and Javadoc
```
