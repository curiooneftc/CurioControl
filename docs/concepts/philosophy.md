# Philosophy

CurioControl's design follows eight principles. Each one is a trade-off someone was willing to
make, so it is worth knowing what each buys and what it costs.

## Simple

Common operations require little code.

A mecanum TeleOp is three lines in `runRobot()`. The complexity lives in the framework, once,
tested once, rather than in forty OpModes across forty seasons.

## Modular

Every component works on its own. A PID controller is useful without a robot. A motor wrapper is
useful without a drivetrain. Nothing requires adopting the whole framework to use one piece.

## Testable

Pure logic runs on a desktop JVM with no hardware, no Robot Controller, and no simulation
environment. See [hardware independence](hardware-independence.md).

This is not a testing convenience — it is what makes thorough testing *possible*. You cannot
meaningfully test a motor on a CI server; you can trivially test a PID controller.

## Explicit

No hidden behavior. Telemetry is batched and flushed once per loop because that is documented, not
because there is a hidden buffer somewhere. Errors name the device and what was expected.

The test: you should be able to predict the behavior from reading the API.

## Lightweight

FTC Robot Controller hardware is resource-constrained. The framework avoids allocating objects
inside robot loops — the OpMode loop, the command scheduler, PID calculation, telemetry, and drive
calculations are all designed to be allocation-free per iteration.

This is measured, not assumed. See the benchmark budgets in the
[testing strategy](../../TESTING_STRATEGY.md).

## Extensible

Teams build their own subsystems, commands, and state machines on top. The extension points —
`Subsystem`, `Command`, `StateMachine` — are first-class parts of the API, not afterthoughts.

## Backwards-compatible

Public APIs are treated as stable once released under `1.x`. A breaking change requires a major
version and a migration guide. Before `1.0.0` the API may change freely, and the changelog says so
every time.

## Hardware-independent where possible

Controllers take numbers, not motors. A `PIDController` has no idea what a motor is. This is what
makes the previous point achievable, and it is enforced by tests.

## The trade-off

Every one of these has a cost. Simple means less configuration for the unusual case. Explicit means
no cleverness. Lightweight means the framework will not automatically parallelize your vision
pipeline. Stable APIs mean a bad decision made in `1.0` has to be carried for a long time.

Those costs are accepted deliberately. A framework that is clever, magical, and fast to write but
hard to debug on a competition day is worth less to a team than one that is boring and obvious.
