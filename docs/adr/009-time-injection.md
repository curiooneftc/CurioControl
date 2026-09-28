# ADR-009: Time injection via a `Clock` interface

**Status:** Accepted

## Context

`MotionProfile`, `Timer`, `RateLimiter`, `Debouncer`, PID delta-time, and state-machine timeouts
all depend on the passage of time. Code that calls `System.nanoTime()` directly is hard to test:
the test either sleeps (slow and flaky) or asserts on ranges (vague and still flaky).

## Decision

Introduce a minimal `org.curioone.control.util.Clock` interface, used internally by time-dependent
components. Production code uses a real-clock implementation; tests use a fake they advance by
hand.

## Consequences

- Time-based behavior is fully deterministic and fast to test. Ten seconds of controller behavior
  is ten lines of test, with no sleeping and no flakiness.
- A small amount of indirection. An interface call in the OpMode loop is not a measurable cost.
- Aligns directly with the "testable" design principle (`spec.md` §43).

## Rejected

**Calling the system clock directly.** It is simpler to write and impossible to test properly. The
cost of the indirection is paid once; the cost of untestable timing code is paid on every bug
report.
