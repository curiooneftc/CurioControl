# ADR-006: Fluent `add(...).update()` telemetry API

**Status:** Accepted
**Source:** `spec.md` §26

## Decision

```java
robot.telemetry()
        .add("Heading", heading)
        .add("Arm", armPosition)
        .update();
```

A single batched `update()` flushes every buffered value to the SDK in one call per loop.

## Consequences

- One telemetry call per loop instead of one per value. Better for the control loop
  (`spec.md` §45).
- Keys and values are buffered internally, so `add()` is cheap.
- Values are grouped by category — `DRIVE`, `ARM`, `INTAKE`, `VISION`, `SYSTEM`, `DEBUG` — so
  output stays readable. `DEBUG` is only emitted when `CurioConfig.DEBUG` is on.
- `update()` is an explicit, visible call site. A reader can see exactly when data reaches the
  driver.

## Rejected

**Sending each value immediately.** Simpler to call, but one SDK call per value per loop. On FTC
hardware that is a measurable slice of the loop budget spent on the driver display.
