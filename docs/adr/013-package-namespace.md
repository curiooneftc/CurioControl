# ADR-013: Maven group and Java package follow the GitHub org

**Status:** Accepted
**Decided:** Phase 0
**Supersedes:** `spec.md` §5 (Package Identity)

## Context

`spec.md` §5 specifies:

```text
Group:    dev.curio
Artifact: curiocontrol
Package:  dev.curio.control
```

The repository actually lives at <https://github.com/curiooneftc/CurioControl>, in the
`curiooneftc` organisation. A `dev.curio` groupId and a `dev.curio.control` package would both
point at a namespace the project does not occupy, and neither would be derivable from the URL a
consumer is looking at when they add the dependency.

The two identifiers do not have to agree with each other. A Maven `groupId` is an independent
coordinate; many well-known libraries have a group that does not match their Java package
(`com.google.guava:guava` shipping `com.google.common` code is the standard example). The
question is which convention to follow, not whether one is possible.

## Options

- **A. Follow the spec exactly** — `dev.curio:curiocontrol` with a `dev.curio.control` package.
- **B. Follow the GitHub org** — `org.curioone:curiocontrol` with an `org.curioone.control`
  package.
- **C. Mixed** — `org.curioone:curiocontrol` with a `dev.curio.control` package.

## Decision

**Option B.** Both the Maven group and the Java package use `org.curioone`.

The rule is simple: *the repository URL is the source of truth for the namespace.* A consumer
who can read the GitHub URL can predict the dependency coordinates and the import statements.

## Consequences

- `spec.md` §5 is **superseded** for the group and package. `spec.md` itself is left unedited: it
  records what the specification says, and rewriting a spec to match its implementation destroys
  the record. This ADR is the amendment.
- Every import, directory, and package declaration in the project uses `org.curioone.control.*`.
- The example robot project uses `org.curioone.robot` (`spec.md` §40), which now shares a root
  with the framework. That is convenient but is **not** a requirement — a team's own package
  naming is entirely their business.
- The rename happened in Phase 0, before any released artifact and before any team had written
  robot code against it. After `0.1.0` the same rename would be a breaking change, and `1.0.0`
  would not be the right moment to spend that capital.
- Cost of the rename: every path in `PROJECT_STRUCTURE.md`, `CODEOWNERS`, the generated build
  metadata resource, and the ArchUnit package matchers. The ArchUnit matchers are the part worth
  remembering — a missed matcher is *silent*, because the rules use `allowEmptyShould(true)` and
  a rule matching zero classes passes. `CurioConfigTest` guards the analogous build-metadata
  path.

## Rejected

**Option A.** A namespace nobody owns is a small, permanent tax on every consumer, and it makes
the coordinates impossible to guess from the URL.

**Option C.** Legal, but it is the worst of both: neither identifier can be derived from the
other, and the mismatch invites the question this ADR exists to answer once and for all.
