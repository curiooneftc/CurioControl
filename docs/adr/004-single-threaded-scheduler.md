# ADR-004: Single-threaded command scheduler

**Status:** Accepted

## Context

Command frameworks, WPILib-style, often run commands on background threads. The FTC OpMode loop is
single-threaded and timing-sensitive, and a competition match is not a good time to discover a race
condition.

## Decision

`CommandScheduler` is **single-threaded** and is invoked exactly once per OpMode loop iteration. No
background threads, no executors, no independently-running timers.

## Consequences

- Deterministic and safe to call from the OpMode thread.
- Matches how teams actually write FTC OpModes, so the mental model transfers.
- A command's `execute()` is called once per loop; `isFinished()` is polled each loop.
- Long waits are polled, not slept — `WaitCommand` never blocks the loop.
- Testing is straightforward: drive the scheduler by hand with a fake clock, and the sequence is
  fully determined.

## Rejected

**Background execution.** Faster in theory, non-deterministic in practice. A command racing a motor
write is a class of bug that is nearly impossible to reproduce on demand and catastrophic during a
match.
