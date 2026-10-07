# Telemetry and Logging

> Status: `TelemetryManager` landed in v0.1.0, the CSV `Logger` in v0.2.0; see
> [the phases document](../../PHASES.md).

Telemetry is how you see what the robot thinks. Logging is how you find out what it thought
after the fact. CurioControl provides both, and keeps them explicitly separate.

## Telemetry

```java
robot.telemetry()
        .add("Heading", heading)
        .add("Arm", armPosition)
        .add("Battery", voltage)
        .update();
```

`add()` buffers a value. `update()` flushes everything in a single call. The driver sees all of
the values together, and the loop makes one telemetry call per iteration instead of one per value
— which matters on hardware with a tight loop budget.

Values are grouped by category so the output stays readable:

```java
TelemetryCategory.DRIVE
TelemetryCategory.ARM
TelemetryCategory.INTAKE
TelemetryCategory.VISION
TelemetryCategory.SYSTEM
TelemetryCategory.DEBUG
```

`DEBUG` is only emitted when debug mode is on:

```java
CurioConfig.DEBUG = true;
```

### Cadence

Telemetry defaults to flushing a few times a second, not every loop. Flushing every iteration
spends loop budget on text formatting that nobody reads at 100 Hz.

```java
CurioConfig.TELEMETRY_PERIOD_MILLIS = 200;   // 5 Hz
CurioConfig.TELEMETRY_PERIOD_MILLIS = 0;     // every loop, when debugging
```

### Never `System.out`

The framework never prints to standard output. Diagnostic output belongs in telemetry, where the
driver sees it. A library that prints to a console it does not own is a nuisance in someone else's
program.

## Logging

The structured logger records named values over time and writes CSV, for tuning a gain or
diagnosing a failed autonomous the next morning.

```java
logger.record("heading", heading);
logger.record("armPosition", arm.getPosition());
logger.record("motorPower", motor.getPower());
```

Output:

```text
time,heading,x,y,armPosition,motorPower
0.00,0.2,0,0,0,0
0.02,0.4,0.1,0,15,0.5
0.04,0.7,0.3,0,30,0.5
```

That CSV is directly loadable into a spreadsheet — which is the point. The common tuning workflow
is: log a run, plot the arm position, see where it overshoots, adjust the gain.

### Disabled by default

```java
CurioConfig.LOGGING_ENABLED = false;   // the default
```

Logging writes to Robot Controller storage. That must be a deliberate choice, never a surprise
during a match. A disabled logger returns immediately and costs effectively nothing.

### Safe under load

Logging is designed not to get in the robot's way:

- Preallocated buffers; no string building per iteration.
- Batched writes with a configurable flush interval.
- A bounded ring buffer, so a long run cannot exhaust the Controller's memory.

The trade-off: a hard robot stop can lose the tail of a log, because writes are buffered. That is
deliberate — losing the last few rows beats an out-of-memory error during a match.

## Which one do I want?

| You want to… | Use |
|---|---|
| See values while driving | Telemetry |
| Understand what happened during an autonomous | Logging |
| Debug a loop that is misbehaving | Both, plus `CurioConfig.DEBUG = true` |
| Keep the match fast and quiet | Neither |

## Next

- [Robot config](robot-config.md) — the cadences and enable flags live in `CurioConfig`
- [Testing strategy](../../TESTING_STRATEGY.md) — how telemetry and logging are tested
