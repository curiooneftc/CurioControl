# ADR-008: Publishing to GitHub Packages first

**Status:** Accepted
**Source:** `spec.md` §34

## Decision

Distribute via **GitHub Packages** (`org.curioone:curiocontrol`). Consider **Maven Central** after
`1.0.0`, once the project is stable and has a public announcement.

## Rationale

- Free and native to the repository, with Actions-based publishing using the ephemeral
  `GITHUB_TOKEN`.
- No separate credentials infrastructure for the initial releases.
- Maven Central adds signing, a Sonatype account, and a slower cadence — not worth it before
  stability.

## Consequences

- Public read access must be configured in the repository settings; otherwise consumers need a
  **read-only** token.
- Consumers add a `maven { url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl") }`
  repository.
- All released versions stay resolvable, which satisfies the reproducibility requirement
  (`spec.md` §36).
- Publishing uses the ephemeral `GITHUB_TOKEN`, never a stored PAT (`spec.md` §44).

## Rejected

**Maven Central from the start.** Signing keys, a Sonatype account, and namespace verification
before the API has stabilized is effort spent on distribution rather than on the framework.
