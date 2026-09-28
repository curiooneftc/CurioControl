# Benchmarks

JMH micro-benchmarks for the hot paths that `spec.md` §45 requires to be allocation-free:

- `PIDController.calculate()`
- `MecanumDrive` power calculation
- `CommandScheduler.run()`
- `TelemetryManager` update
- The logger in disabled mode (should be ~0)

These run on a weekly schedule via `.github/workflows/perf.yml`, not on every pull request.
Benchmarks are slow, and a noisy benchmark is worse than none.

**This module lands in Phase 2 (M2.6).** The workflow already exists and treats the module's
absence as a no-op, so enabling it later is a matter of adding the build file and switching the
Gradle step on.

The target is a *budget*, not a threshold: "0 bytes allocated per `calculate()` call" is a claim
that must be measured, not assumed. A budget that regresses is a signal to look at, not a build to
fail at 2am on a Sunday.

The directory is kept in version control because git does not preserve empty directories.
