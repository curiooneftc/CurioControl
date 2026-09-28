# CurioControl

**A modular control and development framework for FIRST Tech Challenge robots, developed by
Curio One.**

CurioControl gives an FTC team a consistent, modular architecture for robot development, and
removes the repetitive FTC SDK boilerplate that otherwise gets rewritten every season. It sits on
top of the official SDK, not in place of it.

```text
Robot Application
        |
        v
   CurioControl
        |
        v
     FTC SDK
        |
        v
  Android / Hardware
```

## Where to start

| If you want to… | Go to |
|---|---|
| Get a robot running in ten minutes | [Quickstart](getting-started/quickstart.md) |
| Add the dependency to an existing project | [Installation](getting-started/installation.md) |
| Understand how the framework is layered | [Architecture](concepts/architecture.md) |
| Move a motor | [Hardware abstraction](guides/hardware-abstraction.md) |
| Tune a PID loop | [Control tuning](guides/control-tuning.md) |
| Know why something is the way it is | [Architecture decisions](adr/README.md) |
| See what is planned and when | [Phases and milestones](../PHASES.md) |

## Status

CurioControl is under active development. Versions below `1.0.0` have an unstable API: pin an
exact version and expect change between minor releases. See the
[release strategy](../RELEASE_STRATEGY.md).

## Design principles

Simple · Modular · Testable · Explicit · Lightweight · Extensible · Backwards-compatible ·
Hardware-independent where possible

[Philosophy in detail](concepts/philosophy.md)
