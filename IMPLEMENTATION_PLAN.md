# CurioControl Implementation Plan

> **Status:** Draft  
> **Version:** 0.1.0-planning  
> **Target:** v1.0.0 in ~6 months (Standard timeline)  
> **Last Updated:** 2026-09-28

---

## 1. Executive Summary

This document outlines the complete implementation plan for **CurioControl** — a modular control and development framework for FIRST Tech Challenge (FTC) robots. The plan is derived from the specification in [`spec.md`](./spec.md) and expands it into actionable phases, release milestones, documentation strategy, testing approach, and CI/CD configuration.

### Key Targets

| Target | Date | Notes |
|--------|------|-------|
| v0.1.0 Foundation | Month 1-2 | Core infrastructure, hardware abstractions, basic drive |
| v0.2.0 Control | Month 2-3 | PID/F, Feedforward, Motion Profiles, Pose math |
| v0.3.0 Architecture | Month 3-4 | Subsystems, Commands, State Machines |
| v0.4.0 Vision | Month 4-5 | VisionManager, AprilTag integration |
| v1.0.0 Stable | Month 5-6 | API stabilization, docs, examples, publishing |

---

## 2. Scope & Boundaries

### In Scope (v1.0.0)
- All modules listed in `spec.md` Section 7
- Gradle publishing to GitHub Packages
- GitHub Actions CI/CD with Gradle Enterprise
- Comprehensive JavaDoc + Markdown documentation
- Example robot project in `examples/`
- Unit test coverage ≥ 80% for pure logic modules

### Out of Scope (v1.0.0)
- Simulation module
- Path planning / trajectory following
- Localization (odometry fusion)
- Dashboard / configuration UI
- FTC Scouting integration
- Maven Central publishing (post-v1.0.0)

---

## 3. Team & Roles

| Role | Responsibilities |
|------|------------------|
| **Framework Lead** | Architecture decisions, API design, code review |
| **Core Developer(s)** | Module implementation, unit tests |
| **CI/CD Engineer** | GitHub Actions, Gradle Enterprise, publishing pipeline |
| **Documentation Lead** | JavaDoc, guides, examples, changelog |
| **QA/Test Engineer** | Test infrastructure, hardware-in-the-loop validation |

> **Note:** For a small team, individuals may wear multiple hats. The plan assumes 2-3 active developers.

---

## 4. Technical Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Java 17+ | FTC SDK requirement, modern language features |
| Build | Gradle 8.x (Kotlin DSL) | Standard for FTC, excellent publishing support |
| Testing | JUnit 5 + Mockito | JVM testing, mocking for hardware abstraction |
| Static Analysis | SpotBugs, Checkstyle, Error Prone | Code quality, catch bugs early |
| CI | GitHub Actions + Gradle Enterprise | Spec requirement + build caching/analytics |
| Publishing | Maven Publish → GitHub Packages | Spec requirement, free for public repos |
| Documentation | JavaDoc + MkDocs (or GitHub Pages) | API docs + narrative guides |

---

## 5. Module Dependency Graph

```
curiocontrol (root)
├── core           ← No internal deps (foundation)
├── math           ← No internal deps (pure Java)
├── util           ← No internal deps (pure Java)
├── hardware       → core, math, util
├── control        → math, util
├── drive          → hardware, control, math
├── command        → core, util
├── telemetry      → core, util
└── vision         → hardware, math, util
```

**Rule:** Lower layers never depend on higher layers. Cycles are forbidden.

---

## 6. Definition of Ready (DoR) for Issues

Before work begins on any issue:

- [ ] Clear acceptance criteria
- [ ] API design reviewed (for new public APIs)
- [ ] Dependencies identified
- [ ] Test strategy defined
- [ ] Documentation impact assessed
- [ ] Estimated effort (story points or days)

---

## 7. Definition of Done (DoD) for Features

Per `spec.md` Section 49:

- [ ] Public API documented with JavaDoc
- [ ] Unit tests exist where applicable (≥ 80% coverage for pure logic)
- [ ] No unnecessary FTC SDK coupling
- [ ] Code follows project conventions (Checkstyle/SpotBugs clean)
- [ ] Existing functionality remains compatible (regression tests pass)
- [ ] CI passes (compile, test, static analysis)
- [ ] Example usage documented (in guides or examples/)
- [ ] CHANGELOG.md updated
- [ ] Version bump appropriate for change (SemVer)

---

## 8. Risk Assessment & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| FTC SDK version incompatibility | Medium | High | Pin SDK version per release; test against target RC app |
| API churn pre-1.0.0 | High | Medium | Timebox API reviews; use `@ApiStatus.Experimental` |
| Hardware testing bottleneck | High | High | Prioritize pure-Java unit tests; use mocks; schedule robot time |
| Gradle Enterprise setup complexity | Low | Medium | Start with GitHub Actions only; add GE in v0.2.0 |
| Scope creep | Medium | High | Strict v1.0.0 scope; future modules in backlog |

---

## 9. Communication & Cadence

| Cadence | Purpose | Participants |
|---------|---------|--------------|
| Weekly sync (30 min) | Progress, blockers, priorities | All developers |
| Sprint planning (1 hr) | Issue selection, estimation | All developers |
| Sprint review (30 min) | Demo completed work, gather feedback | All + stakeholders |
| Release retrospective (1 hr) | Process improvement | All developers |

---

## 10. Success Metrics (v1.0.0)

| Metric | Target |
|--------|--------|
| API stability | Zero breaking changes post-v1.0.0 without major version |
| Test coverage (pure logic) | ≥ 80% line coverage |
| Build time (CI) | < 10 minutes |
| Publish success rate | 100% (automated) |
| Documentation completeness | 100% public APIs have JavaDoc |
| Example project | Compiles and runs on Robot Controller |

---

## 11. Related Documents

- [PHASES.md](./PHASES.md) — Detailed phase breakdown with milestones
- [RELEASE_STRATEGY.md](./RELEASE_STRATEGY.md) — Versioning, publishing, release process
- [DOCUMENTATION_PLAN.md](./DOCUMENTATION_PLAN.md) — Documentation structure and content
- [TESTING_STRATEGY.md](./TESTING_STRATEGY.md) — Unit/integration testing approach
- [CI_CD_SETUP.md](./CI_CD_SETUP.md) — GitHub Actions + Gradle Enterprise config
- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) — Directory layout and modules
- [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md) — Key architectural decisions
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Contribution guidelines
- [CHANGELOG_TEMPLATE.md](./CHANGELOG_TEMPLATE.md) — Changelog format