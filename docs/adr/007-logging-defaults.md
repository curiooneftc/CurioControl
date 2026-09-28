# ADR-007: Logging disabled by default, CSV to Robot Controller storage

**Status:** Accepted
**Source:** `spec.md` §27

## Decision

- The structured `Logger` is **off by default**. It must be explicitly enabled.
- Output is CSV written to Robot Controller-accessible storage, for example
  `/sdcard/CurioControl/logs/`.
- Low overhead: preallocated buffers, batched writes, configurable flush interval.

## Consequences

- Competition-safe: no surprise I/O during a match.
- Nothing is transmitted externally, and there is no network dependency (`spec.md` §44).
- A disabled logger returns immediately and costs effectively nothing.
- Buffered writes mean a hard robot stop can lose the tail of a log. That is a deliberate
  trade-off: losing the last few rows beats an out-of-memory error mid-match.
- A bounded ring buffer keeps a long run from exhausting the Controller's memory.

## Rejected

**Logging on by default.** A library that writes to storage without being asked is a liability on a
competition day, and there is no way for a team to opt out without reading the source.
