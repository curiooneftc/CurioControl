# ADR-014: CurioRobot is the composition root, and hardware comes in through a port

**Status:** Accepted
**Decided:** Phase 1
**Relates to:** ADR-005 (wrapper hardware abstraction), ADR-003 (pure-Java boundary),
ADR-010 (architecture rules as tests)

## Context

Two constraints collided while writing Phase 1.

**First**, the specification's facade — `robot.drive()`, `robot.imu()`, `robot.telemetry()` — needs a
class that can reach across packages to wire those pieces together. That is a direct conflict with
the layering rule that `core` is a leaf depending on nothing (ADR-010). Either the facade does not
exist, or one class is allowed to break the rule.

**Second**, and less obviously: `com.qualcomm.robotcore.hardware.HardwareMap` **cannot be mocked on a
desktop JVM**. It is a concrete class whose hierarchy reaches into `android.content.Context`, so
Mockito's inline mock maker fails with `NoClassDefFoundError` and a misleading "Mockito cannot mock
this class". This is not a difficulty to work around with clever stubbing; it means anything that
takes a `HardwareMap` directly is untestable off-robot. Since every wrapper takes a `HardwareMap`,
the entire hardware layer and the entire composition root were untestable.

`AnalogInput` is a second instance of the same shape: it is a concrete class, not an interface, so it
cannot be implemented by a fake either — only subclassed.

## Options

For the composition root:

- **A. Drop the facade.** Subsystems look up their own hardware; there is no `CurioRobot`.
- **B. Weaken the layering rule** to permit `core` to depend on `hardware` and `drive` generally.
- **C. Name one class as the permitted exception** and enforce it in the architecture test.

For the testability problem:

- **D. Keep `HardwareMap` in the signatures** and test wrappers only through the ones that can be
  faked.
- **E. Introduce a `HardwareSource` port** and adapt `HardwareMap` to it at the edge.

## Decision

**Option C for the layering, Option E for the hardware.**

`core.CurioRobot` is the composition root and the *only* class in `core` permitted to depend on
`hardware`, `drive`, and `util`. `LayeringTest` names it explicitly, so a second exception added
later fails the build rather than quietly eroding the rule.

`core.HardwareSource` is a single-method port:

```java
public interface HardwareSource {
    <T> T get(Class<T> deviceClass, String name);
}
```

`core.SdkHardwareSource` adapts the real `HardwareMap` to it. `HardwareRegistry` depends on the port;
`CurioRobot` accepts either. The SDK class is confined to the edge of the system.

## Consequences

- The facade in the specification exists as specified, and the leaf rule survives as a rule with a
  named exception rather than as an aspiration.
- `curioone.control.core.CurioRobot` and `org.curioone.control.core.HardwareSource` are **public
  API**. A team writing its own container can depend on them. This is a deliberate cost: the port
  and the composition root are the framework's two extension seams, and a framework whose seams are
  private is a framework nobody can build on.
- `HardwareRegistry` gained a second constructor. `hardwareMap()` now returns `null` when the
  registry was not built from an SDK map; callers that need the escape hatch on-robot should keep
  using the `HardwareMap` constructors on the individual wrappers, which are unchanged.
- **`drive` may not depend on `core`.** The first cut added `HardwareRegistry` constructors to
  `MecanumDrive` and `TankDrive`; the architecture test rejected this, correctly. Name-to-device
  resolution is the composition root's job, so `CurioRobot.drive()` builds `Motor` wrappers and
  passes them to the drivetrain constructor. The drivetrain layer stays unaware of the registry.
- Two methods on the port instead of one was considered and rejected: `get` returning `null` and
  `require` throwing is the honest split, because a single method cannot express "absent" and
  "failed" at the same time, and collapsing them is what made the SDK's own `null` returns the
  reason this class exists.
- Testability is now real rather than aspirational. `FakeHardwareSource` is an ordinary `HashMap`,
  and `CurioRobotTest` covers the lifecycle, the subsystem registry, hardware resolution, and
  drivetrain construction without a hub.
- `AnalogInput` had to be subclassed rather than implemented in `FakeSdkDevices`, since the SDK
  declares it as a concrete class. The superclass constructor is called with a null controller; every
  method that could touch hardware is overridden. This is confined to test support and is
  documented as such at the point of use.
- The `HardwareMap` constructors on the individual wrappers (`new Servo(hardwareMap, "claw")`) are
  retained. They are the ergonomic path in an OpMode, and removing them would push every caller
  through `HardwareRegistry` for no benefit.
