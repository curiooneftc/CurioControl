# ADR-002: Single Gradle module with package boundaries

**Status:** Accepted

## Context

CurioControl could ship as one artifact, or split into `curiocontrol-core`, `curiocontrol-math`,
and so on.

## Decision

A **single Gradle module** (`org.curioone:curiocontrol`) with strict **package** boundaries,
enforced by ArchUnit tests.

## Rationale

- Competition teams add one dependency. Multi-module adds consumer friction for no benefit: every
  FTC project already has the SDK.
- Package-level purity gives the architectural guarantee — no FTC imports in `math` — without
  build complexity.
- If a desktop or simulation use case ever justifies a pure-Java artifact, the packages can be
  split into modules later without renaming anything.

## Consequences

- Layering violations are caught at test time rather than compile time. Acceptable for a project
  this size, and ArchUnit fails the build either way.
- Coverage reporting and publishing are simpler: one publication, one version.
- The layering table in `PROJECT_STRUCTURE.md` is the source of truth for what may import what.

## Rejected

**Multi-module.** A team that only wants a PID controller would have to reason about which artifact
to depend on and would transitively pull the FTC SDK. The whole point of the pure-Java boundary is
that a consumer should not have to.
