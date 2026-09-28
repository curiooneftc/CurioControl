# FAQ

## Why not just use the FTC SDK directly?

You can, and for a small robot you probably should. CurioControl earns its place when a team is
rebuilding the same infrastructure across seasons: drivetrain math, control loops, telemetry
batching, autonomous structure.

The SDK gives you `DcMotorEx` and `HardwareMap`. CurioControl gives you a robot container, hardware
wrappers with validation, a drivetrain abstraction, a control toolkit, and a way to structure
autonomous routines. It sits on top of the SDK and never hides it.

## Can I use CurioControl without vision?

Yes, and you should. Vision is lazily constructed — nothing is allocated unless you call
`robot.vision()`. A test enforces that no other package may import `vision` at all, so a robot
that never touches vision carries no vision dependency and no vision overhead.

## Commands or state machines?

Both, and they are not competitors.

- **Commands** describe *what* to do: drive forward, move the arm, wait for the intake. They
  compose well and read like a list of steps. Use them when the routine is a sequence.
- **State machines** describe *where you are*: `DRIVE_TO_SCORE`, `SCORE`, `RETURN`, `PARK`, with
  explicit transitions and per-state timeouts. Use them when the routine branches on sensor data
  and you want the possible states to be visible in one place.

They interoperate: a state can schedule commands, and a command can request a transition. See
[ADR-004](../../ARCHITECTURE_DECISIONS.md) for the decision that both are first-class.

## Why does the OpMode loop have to be single-threaded?

Because the FTC OpMode loop is single-threaded and timing-sensitive. A background thread in a
command scheduler is a source of non-determinism that is very hard to reproduce on a competition
day. Everything is polled from the loop instead, which is deterministic and easy to reason about.

## My robot does not initialize. What now?

The framework reports the missing device by name and the configuration name it expected. Check the
device name in the Robot Controller app against the name your code passes — a typo is the usual
cause. See [fail predictably](architecture.md#fail-predictably).

## The API changed between 0.x releases. Why?

Before `1.0.0` the API is explicitly unstable. The `0.MINOR` component is effectively the breaking
counter. After `1.0.0`, public APIs are stable and a breaking change requires a major version with
a migration guide. See the [release strategy](../../RELEASE_STRATEGY.md).

## How do I know which CurioControl version I am running?

```java
CurioConfig.version();
```

The value comes from the build itself, so it cannot drift from the artifact you depend on. Log it
at the start of a match and you have a permanent record of what a run used.

## Is it safe to log during a competition?

The structured CSV logger is **disabled by default** and must be explicitly enabled. It writes to
Robot Controller storage, uses preallocated buffers, and never transmits anything off the robot.
A disabled logger costs effectively nothing.

## Does the library make network calls?

No. The library makes no network calls at runtime and transmits no robot data externally. The only
network access anywhere in the project is the build fetching dependencies, and the CI pipeline
publishing to GitHub Packages.

## Why Gradle and not the FTC SDK's own build?

Gradle is what FTC projects already use, what Gradle publishing and version catalogs come from,
and what the test and static-analysis tooling integrates with. See
[ADR-002](../../ARCHITECTURE_DECISIONS.md).
