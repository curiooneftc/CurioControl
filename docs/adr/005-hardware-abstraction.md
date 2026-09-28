# ADR-005: Wrapper, not interface, hardware abstraction

**Status:** Accepted
**Decided:** Phase 0 (previously "provisional")

## Context

Two common styles for hardware abstraction: a concrete wrapper class around the SDK object, or an
interface with an SDK-backed implementation.

## Options

- **A. Concrete wrapper** — `Motor` wraps `DcMotorEx`. Simpler for consumers, one class per device,
  easy to read. Harder to fake in a user's own tests.
- **B. Interface plus implementation** — a `Motor` interface and a `DcMotorExMotor` class. More
  swappable and testable, but doubles the type count and adds boilerplate for every device.

## Decision

**Option A** — concrete wrappers. Each holds the underlying SDK object and exposes it through
`getSdkObject()`, so direct SDK access stays possible (`spec.md` §10).

## Consequences

- Fewer types, cleaner consumer code: `Motor arm = robot.motor("arm")`.
- Users who need to fake hardware in their own tests can mock the SDK type, use a small adapter, or
  rely on wrapper methods being non-final.
- The wrappers are already the tested boundary. `hardware` has a mocked-SDK test tier, so the
  wrapper's own logic — power clamping, mode changes, unit conversion — is covered without needing
  interfaces.
- If a strong need for swappable hardware appears (simulation, post-1.0), interfaces can be
  introduced then as a breaking change, or layered on top as separate abstractions.

## Rejected

**Option B.** Doubling the type count for every device type buys testability that the existing
mocked-SDK test tier already provides. It also risks the wrappers becoming so abstract that
`getSdkObject()` is the only thing anyone uses.
