# ADR-003: Pure-Java boundary for `math`, `control`, and `util`

**Status:** Accepted
**Source:** `spec.md` §16, §29, §31

## Context

FTC code is hard to test because it needs a robot. Controllers that take motor objects cannot be
exercised on a CI server. Every FTC team ends up with the same untestable control code.

## Decision

`math`, `control`, and `util` must contain **zero** imports of `com.qualcomm.*` or `android.*`.
They are plain Java, testable on a desktop JVM.

## Consequences

- The bulk of the test suite is fast, hermetic, and needs no hardware.
- Controllers take plain numbers, not motor objects. A `MotionProfile` is a pure function of time.
- The rule is enforced by ArchUnit (`MathPackageIsPure`, `ControlPackageIsPure`, and their `util`
  equivalent), so it cannot erode one convenient import at a time.
- Some friction is accepted: an IMU-backed heading must be abstracted behind a plain interface so
  `drive` and `control` never see the SDK `IMU`. That indirection is the cost of the guarantee, and
  it is worth it.

## Rejected

**An interface-based control layer with SDK implementations.** It would move the SDK dependency to
the implementation rather than removing it, and a team writing a custom controller would still
have to implement SDK-shaped interfaces to get anything done.
