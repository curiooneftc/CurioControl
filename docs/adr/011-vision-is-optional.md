# ADR-011: Vision is lazy and optional

**Status:** Accepted
**Source:** `spec.md` §28

## Decision

`robot.vision()` lazily constructs a `VisionManager`, and only on first access. A team that never
touches vision pays no cost: no allocation, no camera initialization.

The vision update rate is configurable so it cannot starve the control loop.

## Consequences

- Satisfies "avoid forcing vision dependencies on users who do not need them."
- The `vision` package is never imported by `core`, `control`, or `drive` — enforced by ArchUnit, so
  a non-vision user never has a vision dependency at all, not even an unused one.
- There is a test target for this in v0.4.0: a team that never touches `robot.vision()` sees zero
  added overhead, verified by allocation measurement.

## Rejected

**Initializing vision in the `CurioRobot` constructor.** Simpler, but it charges every team for a
camera they may not have and a camera-init failure that may stop a robot that did not need it.
