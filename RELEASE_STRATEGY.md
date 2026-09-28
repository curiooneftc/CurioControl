# CurioControl Release Strategy

> **Source spec:** [`spec.md`](./spec.md) §33 (CI/CD), §34 (Gradle Publishing), §35 (Versioning), §36 (Version Pinning), §37 (Compatibility), §38 (API Stability)  
> **Scope:** How CurioControl is versioned, built, published, and supported.

---

## 1. Versioning Scheme

CurioControl uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`).

```text
1.2.3
│ │ └── PATCH  bug fix, no API change
│ └──── MINOR  new backwards-compatible functionality
└────── MAJOR  breaking API change
```

### 1.1 Pre-1.0 releases

| Version | Meaning | API stability |
|---------|---------|---------------|
| `0.x.y` | Development | **Unstable.** Any release may break API. |
| `0.x.y` | Published to GitHub Packages | Teams may use it; pin exact versions. |

All `0.x` releases are published so a team can adopt incrementally, but the API may change without a major bump. The `0.MINOR` component is effectively the "breaking" counter pre-1.0.

### 1.2 Post-1.0 releases

| Change | Bump | Example |
|--------|------|---------|
| Bug fix, docs, internal refactor | PATCH | `1.0.0 → 1.0.1` |
| New API, new optional feature | MINOR | `1.0.1 → 1.1.0` |
| Removed/renamed/changed-signature API | MAJOR | `1.1.0 → 2.0.0` |

> **Rule (SPEC §38):** Once released under `1.x`, public APIs are treated as **stable**. Breaking changes require a major version.

---

## 2. Release Train Cadence

- **Pre-1.0:** ship a release at the end of each phase (`0.1.0`, `0.2.0`, `0.3.0`, `0.4.0`).
- **1.0.0 and beyond:** ship on a fixed cadence, targeting roughly:
  - **PATCH** releases as needed (bug fixes) — usually within a week of a fix landing.
  - **MINOR** releases monthly (or per season) when features accumulate.
  - **MAJOR** releases rarely, only for planned breaking changes, always with a migration guide.

### Season considerations
FTC competition code must be **reproducible**. During a competition season, avoid publishing MINOR versions that a competition robot might pick up mid-competition. Teams pin exact versions anyway (see §5), and releases are additive, but patch releases should still be treated carefully during build-out.

---

## 3. Release Types

| Type | Triggered by | Published to GitHub Packages? | GitHub Release? | CHANGELOG? |
|------|--------------|-------------------------------|-----------------|------------|
| **Snapshot / dev** | Every merge to `main` | No (Develocity cache only) | No | No |
| **Pre-release** | End of a phase, pre-1.0 | Yes, tagged `-rc.N` optionally | Draft | Yes |
| **Minor / Patch** | Feature or fix complete | Yes | Yes | Yes |
| **Major** | Breaking change | Yes | Yes, with migration guide | Yes |
| **Hotfix** | P0 bug on a released version | Yes (`x.y.Z` on a maintenance branch) | Yes | Yes (hotfix section) |

---

## 4. Branching Model

```text
main        ──●──────●────────●──────►  (always releasable, SemVer via tags)
             \    /   \   /
develop      ──●──●     \ ●────────►   (integration; phases land here)
             \        \/
feature/*    ──●──●──●              (short-lived, PR → develop)
hotfix/*     ──●──●                  (from a release tag, PR → main + develop)
release/*    ──●──●                  (stabilization for a specific version)
```

### Branch rules
- `main` is protected: must build green, must be tagged to release.
- `develop` is the default integration branch; features merge here.
- Feature branches are short-lived (< 1 sprint) and rebased on `develop`.
- `hotfix/*` branches off the release tag, merge to both `main` and `develop`.
- `release/*` is used when a version needs extra stabilization before publish.

---

## 5. Version Pinning (Consumer Guidance — SPEC §36)

Teams **must** depend on a specific version:

```kotlin
dependencies {
    implementation("org.curioone:curiocontrol:1.2.0")  // good
}
```

Avoid:

```kotlin
implementation("org.curioone:curiocontrol:latest")     // bad
implementation("org.curioone:curiocontrol:+")         // bad
```

And do not depend on unreleased `main`/`develop` builds. Competition robot code must remain reproducible; floating versions break that guarantee.

The framework should provide a **version constant** so robot code can log exactly which CurioControl it built against:

```java
CurioConfig.VERSION  // e.g. "1.2.0"
```

---

## 6. Publishing Pipeline

### 6.1 Target repository
- **Primary:** GitHub Packages (`https://maven.pkg.github.com/curiooneftc/CurioControl`)
- **Future:** Maven Central (post-v1.0.0, once the project is stable and has a public announcement)

### 6.2 Consuming dependency (team robot projects)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl")
            credentials {
                username = providers.gradleProperty("github.actor").orNull
                password = providers.gradleProperty("github.token").orNull
            }
        }
    }
}
```

For a public repo, GitHub Packages allows anonymous read for public packages when configured; otherwise a read-only token is supplied via Gradle properties. For competition robots on a school network, a **read-only** PAT is recommended (see §9).

### 6.3 Publishing (from CurioControl CI)
Publishing is **manual-approved** and only runs from `main` on a version tag:

1. A maintainer creates and pushes an annotated tag (e.g. `v1.2.0`).
2. The `release.yml` GitHub Actions workflow runs:
   - Full verification build (clean, no cache trust issues)
   - Unit tests + static analysis
   - Generates and uploads sources + javadoc JARs
   - `publish` to GitHub Packages with a `GITHUB_TOKEN` scoped to `packages: write`
   - Creates a GitHub Release from the tag with notes from `CHANGELOG.md`
3. Never publish from a fork PR. Never commit credentials.

### 6.4 Artifacts published per release
- `curiocontrol-<version>.jar` (main)
- `curiocontrol-<version>-sources.jar`
- `curiocontrol-<version>-javadoc.jar`
- `curiocontrol-<version>.module` / `.pom` (Gradle Module Metadata)

---

## 7. Compatibility Matrix (spec.md §37)

Every release documents the exact toolchain. The matrix is stored in
[`docs/reference/compatibility.md`](./docs/reference/compatibility.md) and reproduced in each
GitHub Release.

| CurioControl | FTC SDK | Android SDK | Java | Gradle |
|--------------|----------|-------------|------|--------|
| 0.1.0 | 12.0.0 | 34 | 17 | 8.14.5 |
| 0.2.0 | 12.0.0 | 34 | 17 | 8.14.5 |
| 0.3.0 | 12.0.0 | 34 | 17 | 8.14.5 |
| 0.4.0 | 12.0.0 | 34 | 17 | 8.14.5 |
| 1.0.0 | 12.0.0 | 34 | 17 | 8.14.5 |

> The FTC SDK version was **pinned in Phase 0 to `12.0.0`** (ADR-001), the latest release
> published to Maven Central. Rows below `0.1.0` assume the team stays on that SDK; if the
> team's Robot Controller is upgraded first, the row is updated at that release.
>
> The FTC SDK version is pinned in exactly one place, `gradle/libs.versions.toml` under
> `[versions] ftcSdk`. Nothing else in the build hardcodes it.

### Compatibility policy
- CurioControl explicitly supports the FTC SDK version used by the team's Robot Controller project.
- A CurioControl release that requires a newer SDK than the team uses is a **documented
  incompatibility**; teams must stay on a compatible CurioControl version.
- Breaking the minimum SDK requirement is a **MAJOR** change.

---

## 8. Release Checklist (per release)

**Pre-release**
- [ ] All work for the version is merged to `develop`
- [ ] Feature-complete; no open P0 bugs
- [ ] Test suite green; coverage at/above target
- [ ] Static analysis clean
- [ ] JavaDoc complete; docs site updated
- [ ] `CHANGELOG.md` entry written
- [ ] Compatibility matrix updated

**Release**
- [ ] Merge `develop → main` (PR with full green CI)
- [ ] Bump version in build file(s) and `CurioConfig.VERSION`
- [ ] Create and push annotated tag `vX.Y.Z`
- [ ] `release.yml` publishes to GitHub Packages
- [ ] Verify artifact resolves in a scratch project
- [ ] GitHub Release created with notes + compatibility table
- [ ] Merge `main → develop` (sync)

**Post-release**
- [ ] Announce in team channels
- [ ] Tag-and-branch for `main`
- [ ] File any follow-up issues discovered during validation
- [ ] Update this document's compatibility table if needed

---

## 9. Security & Credentials (SPEC §44)

- **Never** commit GitHub tokens, PATs, or passwords to the repository.
- Publishing uses the ephemeral `GITHUB_TOKEN` provided by Actions (scoped `packages: write`).
- Consumers store read tokens in `~/.gradle/gradle.properties` or CI secrets — never in version-controlled files.
- `.gitignore` must cover `local.properties`, `*.keystore`, `gradle.properties` (if it holds secrets), and build outputs.
- The library makes **no network calls at runtime** and transmits **no robot data** externally. This is a hard architectural constraint, not just a policy.
- Dependabot enabled to catch FTC SDK / library CVEs.

---

## 10. Deprecation Policy

Pre-1.0:
- Deprecated APIs may be removed in the next `0.MINOR` release with a note in the CHANGELOG.

1.x:
- A deprecated API remains for **at least two MINOR releases** before removal.
- `@Deprecated(forRemoval = true)` plus JavaDoc pointing to the replacement.
- Removal happens in a MAJOR release with a `MIGRATION.md` entry.

---

## 11. Release Communication

- **Changelog:** Keep a Changelog format, updated per release. See `CHANGELOG_TEMPLATE.md`.
- **GitHub Releases:** Human-readable summary, compatibility matrix, migration notes if breaking.
- **Tag format:** `vX.Y.Z` (SemVer). Pre-release tags may use `vX.Y.Z-rc.N`.
- **Docs site:** Versioned docs per release; a `/latest` alias always points at the newest.

---

## 12. Related Documents

- [PHASES.md](./PHASES.md) — When each release is targeted
- [CI_CD_SETUP.md](./CI_CD_SETUP.md) — Workflow definitions
- [DOCUMENTATION_PLAN.md](./DOCUMENTATION_PLAN.md) — Docs site & versioning
- [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md) — ADR for publishing choice
