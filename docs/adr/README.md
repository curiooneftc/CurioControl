# Architecture Decision Records

An ADR records **why** something is the way it is. The code shows what was decided; the ADR shows
the alternatives that were considered and the trade-off that was accepted.

The full set, with status, is in
[`ARCHITECTURE_DECISIONS.md`](https://github.com/curiooneftc/CurioControl/blob/main/ARCHITECTURE_DECISIONS.md).

## Index

| ADR | Title | Status |
|---|---|---|
| [001](001-ftc-sdk-dependency-strategy.md) | FTC SDK dependency strategy | Accepted |
| [002](002-single-gradle-module.md) | Single Gradle module with package boundaries | Accepted |
| [003](003-pure-java-boundary.md) | Pure-Java boundary for `math`, `control`, `util` | Accepted |
| [004](004-single-threaded-scheduler.md) | Single-threaded command scheduler | Accepted |
| [005](005-hardware-abstraction.md) | Wrapper, not interface, hardware abstraction | Accepted |
| [006](006-telemetry-api.md) | Fluent `add(...).update()` telemetry API | Accepted |
| [007](007-logging-defaults.md) | Logging disabled by default, CSV to RC storage | Accepted |
| [008](008-publishing-target.md) | Publishing to GitHub Packages first | Accepted |
| [009](009-time-injection.md) | Time injection via a `Clock` interface | Accepted |
| [010](010-architecture-tests.md) | Architecture rules enforced by tests | Accepted |
| [011](011-vision-is-optional.md) | Vision is lazy and optional | Accepted |
| [012](012-no-competition-constants.md) | The framework never contains competition constants | Accepted |
| [013](013-package-namespace.md) | Maven group and Java package follow the GitHub org | Accepted |

## Writing a new ADR

1. Copy an existing file and follow the format: **Context → Options → Decision → Consequences →
   Status**.
2. Get it reviewed in the same pull request as, or before, the code it governs.
3. Add it to the index above and to `ARCHITECTURE_DECISIONS.md`.

An ADR is written when the decision is made, not after. Rewriting history to make a decision look
obvious defeats the purpose of keeping the record.
