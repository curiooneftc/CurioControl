# Test support

Fakes and helpers shared across the test suite (`TESTING_STRATEGY.md` §5):

| Fake | For |
|---|---|
| `FakeDcMotorEx` | Closed-loop PID and drive tests — simulates position and velocity integration |
| `FakeIMU` | A programmable heading for field-centric drive |
| `FakeClock` | Any time-dependent component, driven by hand |
| `FakeTelemetrySink` | Captures what `TelemetryManager` flushed |

Prefer a fake over a deep mock chain wherever behavior matters: a real fake can simulate a motor
integrating over a loop, which a stubbed mock chain cannot. Never mock CurioControl's own pure
classes — they are deterministic, so test them directly.

This directory is intentionally empty in Phase 0; the first fake arrives with the `hardware`
package in v0.1.0.
