# CurioControl Documentation Plan

> **Source spec:** [`spec.md`](./spec.md) §39 (Documentation), §40 (Example Project), §41 (Example TeleOp), §42 (Example Autonomous)  
> **Goal:** Every user, from first-time OpMode author to framework maintainer, can find accurate, version-matched documentation.

---

## 1. Documentation Philosophy

| Principle | Meaning |
|-----------|---------|
| **Two audiences** | *Users* write robot code. *Contributors* extend the framework. Docs serve both, separately. |
| **Progressive disclosure** | Quickstart → concept → reference. Nobody should read a 40-page guide to move a motor. |
| **Runnable examples** | Every code sample compiles. Prefer copy-paste-and-run over pseudo-code. |
| **Version-matched** | Docs ship per release. A reader on `0.3.0` sees `0.3.0` docs, not `latest`. |
| **Searchable** | Full-text search on the docs site; Javadoc is indexable. |
| **Living, not written-once** | Docs are updated in the same PR as the code they describe. |

---

## 2. Documentation Types

| Type | Tool | Audience | Cadence | Output |
|------|------|----------|---------|--------|
| **README** | Markdown | Everyone | Every release | 60-second quickstart + orientation |
| **Guides / Concepts** | MkDocs (Markdown) | Users | Continuous | Task-oriented how-to docs |
| **API Reference** | Javadoc | Users, contributors | Every release | Generated HTML, versioned |
| **Examples** | Runnable code in `examples/` | Users | Every phase | Compilable robot projects |
| **Tutorials** | MkDocs | New users | Major versions | Guided, narrative walkthroughs |
| **ADRs** | Markdown (`docs/adr/`) | Contributors | Per decision | Why, not what |
| **Changelog** | Keep-a-Changelog | Everyone | Every release | What changed |
| **Migration guides** | Markdown | Existing users | Major versions | Upgrade instructions |
| **Inline JavaDoc** | Javadoc comments | Everyone | Every PR | Authoritative API detail |

---

## 3. Documentation Site Structure

```text
docs/
├── index.md                    # Landing page / what is CurioControl
├── getting-started/
│   ├── installation.md         # Add dependency, Gradle config
│   ├── quickstart.md           # First robot in 10 minutes
│   ├── project-setup.md        # Full Gradle template
│   └── first-teleop.md         # Tutorial: a working TeleOp
├── guides/
│   ├── hardware-abstraction.md # Motors, servos, encoders, IMU
│   ├── drivetrains.md          # Mecanum, tank, field-centric
│   ├── control-tuning.md       # PID / PIDF tuning guide
│   ├── motion-profiles.md      # Trapezoidal profiles
│   ├── subsystems.md           # Writing your own subsystem
│   ├── commands.md             # Command-based autonomy
│   ├── state-machines.md       # Autonomous state machines
│   ├── vision.md               # AprilTag setup & use
│   ├── telemetry-and-logging.md# Telemetry + CSV logging
│   └── robot-config.md         # Separating config from code
├── concepts/
│   ├── architecture.md         # Layering, dependency rules
│   ├── philosophy.md           # Design principles (from SPEC §43)
│   ├── hardware-independence.md# Why pure Java matters
│   └── faq.md
├── reference/
│   ├── configuration.md        # CurioConfig options
│   ├── migration.md            # Version upgrade notes
│   └── compatibility.md        # Version/SDK matrix
├── adr/                        # Architecture Decision Records
│   ├── README.md
│   └── 0001-....md
└── contributing/
    ├── contributing-guide.md    # Or point to CONTRIBUTING.md
    └── testing-guide.md
```

Guides are **task-oriented** ("How do I tune a PID for an arm lift?"), not reference-oriented ("What is PIDF?").

---

## 4. Javadoc Standards (SPEC §39)

### 4.1 Requirement
- **Every public class** has JavaDoc. Enforced by CI (Phase 5).
- **Every public method** has JavaDoc, including parameters, return, and throws.
- Public enums document each constant.

### 4.2 Template

```java
/**
 * Closed-loop PID controller.
 *
 * <p>Computes an output from the error between a target and a measured value,
 * combining proportional, integral, and derivative terms with delta-time-aware
 * accumulation. This class is pure Java and safe to unit-test on a desktop JVM.
 *
 * <p>Example usage:
 * <pre>{@code
 * PIDController pid = new PIDController(0.01, 0.0, 0.001);
 * motor.setPower(pid.calculate(target, current));
 * }</pre>
 *
 * @param kP proportional gain
 * @param kI integral gain
 * @param kD derivative gain
 * @author Curio One
 * @see PIDFController
 * @since 0.1.0
 */
public class PIDController {
    /**
     * Computes the control output for the current loop iteration.
     *
     * @param target  desired setpoint
     * @param current measured value
     * @return the control output, clamped to the configured output limits
     */
    public double calculate(double target, double current) { ... }
}
```

### 4.3 What to document
- **Purpose** in one sentence.
- **Units** (mm? ticks? radians? degrees?) — always.
- **Thread safety** — is it safe to call from the OpMode thread only?
- **Allocation behavior** for hot-path methods.
- **Preconditions** (e.g. "requires a prior call to {@link #reset()}").
- **FTC coupling** — whether an SDK object is required.
- `@since` tag with the version the API appeared in.

### 4.4 Rules
- Javadoc must not contradict the code. Treat failing examples as bugs.
- No `{@code}` for multi-line; use `<pre>{@code ... }</pre>`.
- Every public type gets `@since`.

---

## 5. Document-by-Document Plan

### 5.1 `README.md`
The front door. Sections:
1. What is CurioControl? (one paragraph + architecture diagram from SPEC §1)
2. Status badge (pre-release / stable), latest version, compatibility line
3. **60-second quickstart** — dependency snippet + minimal TeleOp
4. Feature overview (link to guides)
5. Documentation links
6. Repository ecosystem note (SPEC §4 — CurioControl vs. RobotCode)
7. Contributing / Support / License
8. Roadmap table (link to `PHASES.md`)

Target length: one screen for the quickstart; the rest is navigational.

### 5.2 Getting Started
- **installation.md** — add the dependency block, GitHub Packages access (incl. read-only token setup), Gradle template.
- **quickstart.md** — smallest robot that moves: a mecanum base + `MainTeleOp`. Explicitly mirrors SPEC §41.
- **project-setup.md** — full Gradle config, `repositories`, `RobotConfig` layout, where things go.
- **first-teleop.md** — narrative tutorial: hardware config in the RC app, wiring, code, deploy, run.

### 5.3 Guides (one per major capability)
Each guide follows the same shape:
1. **What it is** (1-2 sentences)
2. **When you'd use it**
3. **Minimal example** (runnable)
4. **Full API tour** (key methods, not every one — Javadoc has that)
5. **Common recipes** (task-oriented sub-sections)
6. **Pitfalls** (real mistakes, e.g. forgetting to reset the IMU between autos)
7. **Related** links

Guide roster: hardware abstraction, drivetrains, control tuning, motion profiles, subsystems, commands, state machines, vision, telemetry & logging, robot config.

### 5.4 Concepts
- **architecture.md** — the layered diagram, dependency direction, what may import what (enforced by tests).
- **philosophy.md** — the eight design principles from SPEC §43, with a concrete example for each.
- **hardware-independence.md** — why `math`/`control` are pure Java; how it enables desktop unit tests; the guard test.
- **faq.md** — "Why not just use the FTC SDK directly?", "When should I use a command vs. a state machine?", "Can I use CurioControl without vision?".

### 5.5 Reference
- **configuration.md** — every `CurioConfig` field, default, and effect.
- **migration.md** — per-major-version upgrade steps with before/after code.
- **compatibility.md** — the version matrix (SPEC §37), kept current.

### 5.6 Examples (`examples/`)
- `examples/robot/` — a complete, runnable FTC robot project (SPEC §40 layout).
  - `opmode/MainTeleOp.java` (SPEC §41)
  - `opmode/BlueAuto.java` (SPEC §42)
  - `subsystem/Arm.java`, `subsystem/Intake.java`, `subsystem/Drive.java`
  - `config/RobotConfig.java`
  - `README.md` — how to clone, add the dependency, deploy.
- `examples/snippets/` — copy-paste fragments (10-line PID loop, field-centric drive, AprilTag read).
- The example project is compiled in CI against the **published** artifact to guarantee it stays correct.

### 5.7 ADRs (`docs/adr/`)
Record architectural decisions with context. Format: context → decision → consequences → status.
Planned ADRs:
- ADR-001: FTC SDK dependency & vendoring approach
- ADR-002: Package/module layout (packages-in-one-jar vs. multi-module Gradle)
- ADR-003: Command system design (scheduler threading model)
- ADR-004: Telemetry API shape (fluent vs. builder)
- ADR-005: Publishing target (GitHub Packages first)
- ADR-006: Hardware abstraction philosophy (wrapper vs. interface + impl)
- ADR-007: Pure-Java boundary for `math`/`control` (and how to enforce it)

### 5.8 Changelog
- Keep a Changelog format. See `CHANGELOG_TEMPLATE.md`.
- Entries grouped by Added / Changed / Deprecated / Removed / Fixed / Security / Docs.
- Every entry links to the PR and, for breaking changes, to a migration note.

### 5.9 Migration guides
Written at each MAJOR release (and on demand for pre-1.0 jumps). Contents:
- What broke and why
- Before / after code for each breaking change
- A mechanical upgrade checklist
- Whether an automated migration is possible (usually not; be honest)

---

## 6. Versioned Documentation Strategy

| Aspect | Approach |
|--------|----------|
| Javadoc | Published per release tag to `docs/javadoc/vX.Y.Z/`; `javadoc/latest` aliases newest |
| Guides | MkDocs `mike` plugin, one version dir per release; `latest` alias |
| Navigation | Version switcher in the header |
| Unreleased | Served from `main` at `/dev/` for contributors |
| Retention | Keep all versions ≥ 1.0 indefinitely; pre-1.0 kept for the season |

> A reader on `0.4.0` must never be shown `1.0.0` instructions for APIs that changed. Versioned docs prevent the most common documentation failure: code that doesn't compile against the reader's actual dependency.

---

## 7. Documentation Quality Bar

A doc is "done" when:
- [ ] It answers a question a user actually has
- [ ] Code samples compile (ideally extracted from real, tested code)
- [ ] Units and preconditions are stated
- [ ] It links to related docs instead of duplicating content
- [ ] It was reviewed by someone who did not write it
- [ ] It lives in the versioned docs site, not only in a PR description

---

## 8. Documentation Work Per Phase

| Phase | Documentation deliverables |
|-------|---------------------------|
| **Phase 0** | README skeleton, CONTRIBUTING, CHANGELOG, LICENSE, docs site scaffold, ADR template, Javadoc pipeline |
| **Phase 1** | Quickstart, hardware-abstraction guide, control-tuning guide, drivetrains guide, robot-config guide |
| **Phase 2** | Motion-profiles guide, telemetry-and-logging guide, concepts/architecture.md, concepts/hardware-independence.md |
| **Phase 3** | commands.md, state-machines.md, subsystems.md, example TeleOp + Auto in `examples/robot` |
| **Phase 4** | vision.md (with diagrams), performance notes |
| **Phase 5** | Full docs audit, migration.md, complete API reference, tutorials, FAQ, 100% Javadoc coverage |

---

## 9. Tooling

| Purpose | Tool | Notes |
|---------|------|-------|
| Guides site | **MkDocs Material** | Versioning via `mike`, great search, GitHub Pages hosting |
| API reference | **Javadoc** (JDK tool) | Already in the JDK; no extra dependency; reliable for a plain Java library |
| Diagrams | Mermaid in Markdown | Diagrams-as-code, reviewable in PRs |
| Code style | Checkstyle + SpotBugs | See `CI_CD_SETUP.md` |
| Doc linting | `markdownlint` in CI | Catches broken links / inconsistent headings |
| Link checking | `lychee` or `markdown-link-check` in CI | Prevents rot |

Configuration sketches live in `CI_CD_SETUP.md`.

---

## 10. Documentation Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Docs drift from code | High | Docs updated in the same PR; Javadoc CI gate; example project compiled in CI |
| Example code stops compiling | High | Compile `examples/robot` against the published artifact in CI |
| Pre-1.0 API churn makes docs wrong | Medium | Mark pre-1.0 docs "unstable"; version the docs; rewrite in M5.1 |
| Versioned docs infra overkill for a small team | Low | Start unversioned, add `mike` in Phase 5 when it actually matters |
| Javadoc examples rot | Medium | Extract samples from tested code where feasible |

---

## 11. Documentation Definition of Done (per feature — SPEC §49)

- [ ] Public API is documented (JavaDoc)
- [ ] Example usage is documented (a guide or `examples/` snippet)
- [ ] `CHANGELOG.md` updated
- [ ] Guides updated if behavior/semantics changed
- [ ] Any new concept has a "what/why" section, not just "how"
