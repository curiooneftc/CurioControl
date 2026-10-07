# Benchmarks

JMH micro-benchmarks for the hot paths that `spec.md` §45 requires to be allocation-free.
The benchmarks live in `src/jmh` — a source set of the root project, not a separate module —
so the FTC SDK classpath wiring and the test fakes (`FakeDcMotor`, `FakeClock`) are shared
rather than duplicated. Run them with:

```text
./gradlew jmh
```

They run on a weekly schedule via `.github/workflows/perf.yml`, not on every pull request.
Benchmarks are slow, and a noisy benchmark is worse than none. The workflow reports; it does
not fail the build.

## What is measured

| Benchmark | Hot path | Budget |
|---|---|---|
| `PidBenchmark` | `PIDController.calculate()` on a varying measurement | Bounded, allocation-free steady state |
| `MecanumBenchmark` | `MecanumDrive.mecanum()`: wheel math, normalization, writes | Allocation-free |
| `SchedulerBenchmark.steady` | `CommandScheduler.run()` with settled commands | Flat, allocation-free on idle passes |
| `SchedulerBenchmark.churn` | `run()` scheduling and completing commands every pass | Bounded by the commands moving that pass |
| `TelemetryBenchmark` | Four buffered values plus a flush | No collection growth per call |
| `LoggerDisabledBenchmark` | `record` + `update` with logging off | ~0: a flag check, zero allocation by construction |

The target is a *budget*, not a threshold: "0 bytes allocated per `calculate()` call" is a claim
that must be measured, not assumed. A budget that regresses is a signal to look at, not a build
to fail at 2am on a Sunday. Time here is a regression tripwire; allocation is asserted
structurally in the benchmark sources and with manual `-prof gc` runs.

## Notes for authors

- Benchmarks are measurement code, not shipped code: the lint gates apply with the documented
  `src/jmh` exceptions in `config/checkstyle/suppressions.xml` and
  `config/spotbugs/exclude.xml` (JMH state fields must be accessible; literals are
  measurement parameters).
- JMH state that touches global framework flags (`CurioConfig`) saves and restores them in
  `@Setup`/`@TearDown`, and every benchmark class forks, so trials cannot leak into each
  other.
- Keep the per-class settings (`@Warmup`, `@Measurement`, `@Fork(1)`) honest for weekly CI:
  deep runs override on the command line rather than in the sources.
