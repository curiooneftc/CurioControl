# Contributing to CurioControl

> Thanks for your interest in improving CurioControl. This guide covers the workflow,
> conventions, and expectations for contributions — whether you are a Curio One developer or an
> outside contributor.

---

## 1. Ways to contribute

- **Report bugs** — open an issue with reproduction steps and logs.
- **Improve documentation** — fix unclear guides, add examples, correct JavaDoc.
- **Add features** — pick up an issue labeled `good first issue` or `help wanted`.
- **Review PRs** — comment on design, tests, and adherence to the design principles.
- **Validate on hardware** — run the bench checks in [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)
  and report results.

---

## 2. Before you start

1. **Read the spec** — [`spec.md`](../spec.md) and
   [`ARCHITECTURE_DECISIONS.md`](ARCHITECTURE_DECISIONS.md) define what CurioControl is and which
   decisions are already settled. Most "how should we…" questions are already answered there.
2. **Check the roadmap** — [`PHASES.md`](PHASES.md) shows what is in scope. Out-of-scope ideas
   belong in the backlog, not in a pull request.
3. **Open an issue first** for anything non-trivial, so the design can be discussed before code is
   written.
4. **Respect the design principles** (`spec.md` §43): simple, modular, testable, explicit,
   lightweight, extensible, backwards-compatible, and hardware-independent where possible.

---

## 3. Development setup

### Prerequisites

- **Git.**
- **Nothing else.** The build targets the Java 17 toolchain and downloads it automatically if your
  machine does not have a matching JDK. The FTC SDK is resolved from Maven Central.

### Getting started

```bash
git clone https://github.com/curiooneftc/CurioControl.git
cd CurioControl

./gradlew build            # compile, test, static analysis, javadoc
./gradlew test             # tests only
./gradlew javadoc          # generate API docs
./gradlew spotlessApply    # format before pushing
./gradlew publishToMavenLocal  # install to ~/.m2 to try it in a robot project
```

### Branches

- Work on `feature/<short-description>` branches cut from `develop`.
- Keep branches short-lived and rebase on `develop` before opening a pull request.
- Target `develop` for normal changes. `main` is reserved for releases.

---

## 4. Pull request workflow

1. **Create a feature branch** from `develop`.
2. **Make focused changes.** One logical change per pull request; keep diffs reviewable.
3. **Add tests.** New logic needs unit tests; bug fixes need a regression test.
4. **Update docs.** If you changed or added public API, update JavaDoc and any affected guide.
5. **Update `CHANGELOG.md`** under the `Unreleased` section.
6. **Run the build locally.** `./gradlew build` must be green.
7. **Open the pull request** against `develop` and fill in the template.
8. **CI must be green** before merge: compile, tests, static analysis, Javadoc.
9. **Respond to review.** At least one approving review is required, two for `main`.

### What reviewers look for

- Correctness and edge-case handling
- Adherence to layering rules — no forbidden imports, enforced by the ArchUnit tests
- Test coverage for new logic
- Clear, correct JavaDoc: units, preconditions, thread-safety, allocation behavior
- No unnecessary allocations in hot paths
- Competitive, lightweight code — this runs on constrained Robot Controller hardware

---

## 5. Coding conventions

| Aspect | Convention |
|---|---|
| Language | Java 17 |
| Formatting | Spotless with google-java-format in AOSP style (4-space indent, 100 columns) |
| Static analysis | Checkstyle and SpotBugs must be clean; `maxWarnings = 0` |
| Imports | No wildcards; no unused imports |
| Javadoc | Required on all public classes and methods |
| Naming | `getX()` for values, `isX()`/`hasX()` for booleans, `UPPER_SNAKE` for constants |
| Nullability | Prefer `Optional` for "maybe" returns; avoid `null` in the public API |
| Exceptions | `IllegalArgumentException` for bad arguments; a framework exception for hardware or config problems |
| Logging | Use the telemetry/logger abstraction — never `System.out` or `System.err` |
| Time | Inject `Clock`; never call `System.nanoTime()` directly in library code |
| Competition constants | Never in the framework — they belong in the robot project's `RobotConfig` |

Run `./gradlew spotlessApply` before pushing. `spotlessCheck` runs as part of `build`, so an
unformatted file fails CI.

### Checkstyle exceptions

If a rule genuinely does not apply, add an entry to `config/checkstyle/suppressions.xml` with a
comment explaining why. Do not silence a check that is merely inconvenient.

---

## 6. Testing expectations

From [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md):

- **Pure logic** (`math`, `control`, `util`, `command`) → unit tests, no hardware, target ≥ 90%
  coverage.
- **Hardware wrappers** → tests against mocked SDK objects.
- **Architecture rules** → keep the ArchUnit tests passing; do not introduce forbidden dependencies.
- **Bugs** → always add a failing test first, then fix it.
- **Performance-sensitive code** → add or update a JMH benchmark.

CI enforces coverage gates on the key packages. A drop below target blocks the pull request.

---

## 7. Documentation expectations

From [`DOCUMENTATION_PLAN.md`](DOCUMENTATION_PLAN.md):

- **JavaDoc on all public API**, stating purpose, units, preconditions, and thread-safety.
- **Runnable examples** — code samples should compile. Prefer real, tested code over pseudo-code.
- **Guides updated in the same pull request** when behavior or semantics change.
- **`CHANGELOG.md` updated** for any user-visible change.

---

## 8. Versioning and releases

- Versioning is **Semantic Versioning** — see [`RELEASE_STRATEGY.md`](RELEASE_STRATEGY.md).
- **Do not** bump the version in a feature pull request. Maintainers bump versions at release time.
- Breaking API changes require a **major** version and a `MIGRATION.md` entry.
- Pre-1.0 (`0.x`), the API may change more freely — but still document what changed.

---

## 9. Security

From [`spec.md`](../spec.md) §44:

- **Never** commit credentials, tokens, or secrets.
- The library makes **no network calls** and transmits **no robot data** at runtime.
- Keep GitHub authentication in environment variables or Gradle properties, never in source.
- Report security issues privately to the maintainers rather than in a public issue.

---

## 10. Code of conduct

Be respectful and constructive. Assume good intent. Critique the code, not the person. This is a
student-run open-source project; we want it to be a welcoming place to learn and improve.

---

## 11. Getting help

- Open a **Discussion** or **Issue** on GitHub.
- For a bug, include: Robot Controller version, CurioControl version, OpMode code,
  telemetry/log output, and exact reproduction steps.
- For a question, describe what you are trying to do and where you are stuck.

---

## 12. Related documents

- [`spec.md`](../spec.md) — the specification
- [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md) — project plan
- [`PHASES.md`](PHASES.md) — roadmap and scope
- [`ARCHITECTURE_DECISIONS.md`](ARCHITECTURE_DECISIONS.md) — design decisions
- [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md) — layout, layering, conventions
- [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md) — testing rules
- [`DOCUMENTATION_PLAN.md`](DOCUMENTATION_PLAN.md) — documentation rules
- [`RELEASE_STRATEGY.md`](RELEASE_STRATEGY.md) — versioning and publishing
- [`CI_CD_SETUP.md`](CI_CD_SETUP.md) — build and CI reference
