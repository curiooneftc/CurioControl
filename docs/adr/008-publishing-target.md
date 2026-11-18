# ADR-008: Publishing target

**Status:** Accepted; amended for credential-free public reads.

**Source:** `spec.md` §34

## Decision

Maintainers publish releases with GitHub Actions using the ephemeral `GITHUB_TOKEN`
(`org.curioone:curiocontrol`). Public consumers read those releases through
[JitPack](https://jitpack.io/) using the Git-based coordinates
`com.github.curiooneftc:CurioControl:<tag>`. Maven Central remains the post-1.0.0 option.

## Rationale

- GitHub Packages stays free and native to the repository, and CI already has a scoped token.
- JitPack builds a tag from the public repository, so teams need **no token** to add CurioControl.
- Public read access on GitHub Packages has been unreliable across repository settings; asking
  every FTC team to manage a PAT for a public library was friction without benefit.
- Maven Central still adds signing, a Sonatype account, and a slower cadence — not worth it before
  API stability.

## Consequences

- Consumers add only `maven { url = uri("https://jitpack.io") }`; no `credentials` block is needed.
- Consumer versions are exact release tags, including the leading `v`, so builds stay reproducible
  (`spec.md` §36).
- Maintainers still publish to GitHub Packages with `GITHUB_TOKEN`, never a stored PAT. That registry
  is an implementation detail for maintainers, not the documented team dependency source.
- JitPack builds from source on first request, so the first resolve of a new tag may take a few
  minutes.

## Rejected

- **GitHub Packages as the only public registry.** It can require a read token and gave teams a
  worse experience for a public, BSD-licensed library.
- **Maven Central from the start.** Signing keys, a Sonatype account, and namespace verification
  before the API has stabilized is effort spent on distribution rather than on the framework.
