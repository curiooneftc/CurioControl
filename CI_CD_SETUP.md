# CurioControl CI/CD Setup

> **Source spec:** [`spec.md`](./spec.md) §33 (CI/CD), §34 (Gradle Publishing), §44 (Security)  
> **Stack:** GitHub Actions + Gradle 8.14.5 + Gradle Develocity.  
> **Status:** the configuration described here is **implemented** as of Phase 0. The workflow YAML
> lives in `.github/workflows/`; the Gradle configuration lives in `settings.gradle.kts` and
> `build.gradle.kts`. Where a phase has not landed yet — the coverage gate, the benchmark module —
> that is called out explicitly rather than left to look finished.

---

## 1. Overview

```text
Push / PR
    │
    ▼
build.yml ──► compile → test (unit+integration+arch) → static analysis → javadoc → coverage gate → build
    │
    ├─ Develocity build scan uploaded
    └─ JaCoCo report uploaded (artifact)

Tag vX.Y.Z on main
    │
    ▼
release.yml ──► clean verify → publish (jar, sources, javadoc) to GitHub Packages → GitHub Release

main / schedule
    │
    ▼
perf.yml (scheduled) ──► JMH benchmarks → compare budgets
docs.yml ──► build & deploy docs site (MkDocs) + Javadoc to GitHub Pages
```

---

## 2. Workflows

All four workflows are committed under `.github/workflows/`. This section explains what each one
does and why.

### 2.1 `build.yml` — PR & push validation

Triggers: `pull_request` and `push` to `main` and `develop`, plus manual dispatch.

One job, one command. `./gradlew build` runs compile, tests, Checkstyle, SpotBugs, formatting, and
Javadoc, so there is exactly one definition of "green" and no ambiguity about what a required
status check means.

The workflow then:

- publishes a build scan, with `continue-on-error` so a missing Develocity key never fails a fork
  pull request;
- uploads the JaCoCo report, test results, and — only on failure — the Checkstyle report, so a red
  build says *why* without anyone re-running it locally.

`concurrency` cancels an in-flight build when a newer push lands on the same ref.

A second job, `verify-example`, compiles `examples/robot/` against the published artifact. It is
gated behind `if: false` until a release exists to depend on. **Do not mark it as a required
status check while it is disabled** — a required check that never runs blocks every pull request
permanently.

> No secret is required. A Build Scan is published on failure with no account; see §3.

### 2.2 `release.yml` — publish on tag

Triggers: `push` to a `v*` tag, plus manual dispatch with a `dryRun` input.

Publishing is **manual-approved** and gated on a repository variable:

```text
tag pushed + PUBLISH_ENABLED == "true"  ->  verify, publish, create a GitHub Release
tag pushed, PUBLISH_ENABLED unset        ->  verify and assemble only (Phase 0 dry run)
workflow_dispatch                       ->  verify and assemble only
```

A tag on its own is not permission to publish. Until the team sets `PUBLISH_ENABLED`, the
workflow runs a full clean verification and uploads the artifacts as a workflow artifact instead —
which is the Phase 0 dry run required by task 0.8.

A release build runs `./gradlew clean build` rather than an incremental build, deliberately not
trusting the cache. It then publishes the jar, sources jar, javadoc jar, `.module`, and `.pom` to
GitHub Packages using the ephemeral `GITHUB_TOKEN`, and creates a GitHub Release.
Publishing uses `GITHUB_TOKEN`, never a stored PAT (`spec.md` §44).

### 2.3 `perf.yml` — scheduled benchmarks

Triggers: `schedule: cron '0 5 * * 1'` (weekly, Monday 05:00 UTC) and manual dispatch.

Runs the JMH benchmark module and uploads results as an artifact. The workflow **reports, it does
not fail the build**: a benchmark regression deserves human review, not an automatic block at
2am on a Sunday.

It is kept off the pull request path on purpose — benchmarks are slow, and a noisy benchmark is
worse than no benchmark.

The `benchmark/` module lands in Phase 2 (M2.6). Until then the workflow's Gradle step is a no-op
and the schedule and artifact contract are already in place.

### 2.4 `docs.yml` — documentation site

Triggers: `push` to `main`, plus manual dispatch.

Builds the Javadoc with Gradle, installs the pinned MkDocs toolchain from `requirements-docs.txt`,
builds the site with `--strict`, copies the Javadoc into `site/javadoc/latest/`, and deploys to
GitHub Pages.

`--strict` matters: a broken internal link or a missing navigation entry fails the build rather
than shipping a subtly wrong docs site. Documentation is part of the product.

Enable Pages with source **GitHub Actions** in the repository settings.

### 2.5 Dependabot

Three ecosystems, all weekly:

| Ecosystem | Directory | Watches |
|---|---|---|
| `gradle` | `/` | `gradle/libs.versions.toml` — SDK, JUnit, Mockito, ArchUnit, plugins |
| `gradle` | `/buildSrc` | The build logic's own dependencies |
| `github-actions` | `/` | Action versions, which drift and cause surprising CI failures |

Gradle updates get a `build` commit prefix and a `gradle` label; Action updates get `ci`.

---

## 3. Develocity / Gradle Enterprise

### 3.1 What we actually get, on the free tier

| Feature | Free tier | Configured? |
|---|---|---|
| Build Scan | ✅ Anonymous publishing, no account | ✅ On failure only |
| Local build cache | ✅ No account needed | ✅ Always on |
| Remote build cache | ❌ **Not licensed** | ❌ Deliberately not configured |
| Test distribution / retry | ❌ Paid | ❌ |

The remote build cache was the biggest win in the original plan, and it is the one thing the
free tier does not provide. Gradle's own administration documentation is explicit that
"anonymous access to the built-in Build Cache node is not enabled by default", so configuring it
against the public server would be a no-op at best. Phase 0 therefore delivers the local cache,
and the remote cache is deferred to the point where there is a paid Develocity installation.
`settings.gradle.kts` carries a commented example for that day.

### 3.2 Build scans — no account, no secret

A Build Scan is published **only when the build fails**, to the public server
`develocity.geeksmart.com`. No account and no API key are required: anonymous scan publishing is
allowed by default.

Two consequences worth stating plainly:

1. **The terms-of-service acceptance is committed to this repository.** CI cannot answer the
   interactive prompt, so `termsOfUseUrl` and `termsOfUseAgree` are set in `settings.gradle.kts`.
   Gradle's documentation warns about exactly this. It is a team-level acceptance made on behalf
   of every contributor, not a per-developer one — which is why it is commented as such at the
   point of use, and why removing the two lines is a supported way to stop publishing.
2. **Some build data leaves the machine, on failure only.** For a public repository publishing
   open-source code this is low risk, but it is a transmission and belongs in the security
   review at v1.0.0 rather than being discovered at it.

Adding an account later is optional. To tie scans to your own project instead of publishing
anonymously:

- Sign in to Develocity, then **My settings → Access keys**, and create a key with
  **Publish Build Scans**.
- **Locally:** `./gradlew provisionDevelocityAccessKey` opens a browser and writes
  `~/.gradle/develocity/keys.properties`.
- **In CI:** repo → **Settings → Secrets and variables → Actions → New repository secret**, named
  `DEVELOCITY_ACCESS_KEY`.

**The value must be in the form `<server host>=<key>`**, e.g.
`develocity.geeksmart.com=abc123…`. The host prefix is what stops a key being sent to a
different server than intended. Never commit it (`spec.md` §44).

The build does not read that variable at all — the plugin does, along with the properties file
above. An earlier draft parsed it in `settings.gradle.kts` and handed the raw value to
`accessKey.set()`, which breaks if you follow the documented host-prefixed format.

The configuration itself:

```kotlin
develocity {
    server.set(providers.gradleProperty("develocity.serverUrl")
        .getOrElse("https://develocity.geeksmart.com"))

    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }
        uploadInBackground.set(false)
    }
}
```

> Point `develocity.serverUrl` at your own installation if the team ever gets one. The server URL
> is a Gradle property precisely so that day requires no code change.

### 3.3 Cache safety

The FTC SDK is resolved from Maven Central and unpacked by the `extractFtcSdkClasses` task, which
is `@CacheableTask` with a `@PathSensitive(NAME_ONLY)` input. Its outputs are therefore
fingerprintable, so caching is reliable for every task that depends on them.

There is no local or environment-dependent artifact, so the classic "cache silently goes stale"
failure mode does not apply.

---

## 4. Gradle Build Essentials (reference)

The build is the source of truth; this section explains the parts that are not obvious.

`settings.gradle.kts` — repositories, the Java 17 toolchain resolver, and Develocity:

```kotlin
plugins {
    // Provisions JDK 17 on machines that do not have it, so a contributor needs no JDK install.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("com.gradle.develocity") version "4.2.1"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        google()   // the SDK's androidx dependencies
    }
}

develocity {
    server.set(providers.gradleProperty("develocity.serverUrl")
        .getOrElse("https://develocity.geeksmart.com"))

    buildScan {
        // Terms of service for the public free server. CI cannot answer the interactive
        // prompt, so the acceptance is declared here — and therefore committed to the repo.
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")

        // Publish only when the build fails. That is when a scan is worth reading.
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }

        // CI agents are torn down when the build ends; a background upload would be killed.
        uploadInBackground.set(false)
    }
}

buildCache {
    local { isEnabled = true }   // needs no account
    // No remote cache: the public free server does not license it, and anonymous access to
    // the built-in build cache node is disabled by default. See CI_CD_SETUP.md §3.2.
}
```

> **Nothing in the build reads `DEVELOCITY_ACCESS_KEY`.** The plugin already reads that
> environment variable and `~/.gradle/develocity/keys.properties` itself, in the documented
> `<server host>=<key>` form. An earlier draft parsed the variable and passed the raw value to
> `accessKey.set()`, which silently breaks if the value follows that host-prefixed format.
> Leave it to the plugin.

`build.gradle.kts` — the FTC SDK wiring is the part worth reading:

```kotlin
// The SDK ships as AARs whose metadata declares the legacy `org.gradle.libraryelements`
// attribute, which a java-library project cannot select. Unpack them instead (ADR-001).
val ftcSdkAars: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    isVisible = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(libraryElementsAttribute, "aar")
    }
}

val extractFtcSdkClasses by tasks.registering(UnpackAarClasses::class) {
    archives.from(ftcSdkAars)
    outputDirectory.set(layout.buildDirectory.dir("ftc-sdk-classes"))
    strict.set(true)          // an AAR with no classes.jar fails the build
}

dependencies {
    compileOnly(files(extractFtcSdkClasses.map { it.outputDirectory }))
    testCompileOnly(files(extractFtcSdkClasses.map { it.outputDirectory }))
}
```

Tool versions all live in `gradle/libs.versions.toml`, so there is exactly one place to change
them and Dependabot has a single file to watch.

```kotlin
group = "org.curioone"
version = "0.1.0-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
    withSourcesJar()
    withJavadocJar()
}
```

The version is also baked into the jar as a resource, so `CurioConfig.version()` can never drift
from the published artifact. Two halves of one contract — the resource classpath root, and the
package path under it — declared once each:

```kotlin
val versionResourceRoot = "generated/version"
val versionResourcePackage = "org/curioone/control"

val generateVersionProperties by tasks.registering(WriteProperties::class) {
    destinationFile = layout.buildDirectory
        .file("$versionResourceRoot/$versionResourcePackage/version.properties")
    property("version", project.version.toString())
}

sourceSets.main {
    resources.srcDir(generateVersionProperties.map { layout.buildDirectory.dir(versionResourceRoot) })
}
```

`CurioConfig.VERSION_RESOURCE` is the other end of that contract, and `CurioConfigTest` fails
loudly if the two ever disagree — a rename of the root package moves this path, and the failure
mode is a silent fallback to `"unknown"` rather than a compile error.

Publishing — the target URL is a property so a fork can publish elsewhere without editing the
build:

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "org.curioone"
            artifactId = "curiocontrol"
            pom { /* name, description, licence, scm, issueManagement */ }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(providers.gradleProperty("curio.packagesUrl")
                .getOrElse("https://maven.pkg.github.com/curiooneftc/CurioControl"))
            credentials {
                username = providers.gradleProperty("github.actor").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("github.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### 4.1 What `./gradlew build` actually runs

One command, and it is the single definition of "green":

| Task | What it enforces |
|---|---|
| `compileJava` / `compileTestJava` | Compiles, with `-Xlint:all -Werror` |
| `test` | JUnit 5 unit, integration, and ArchUnit architecture tests |
| `checkstyleMain` / `checkstyleTest` | Style, naming, and Javadoc presence, `maxWarnings = 0` |
| `spotbugsMain` / `spotbugsTest` | Known bug patterns |
| `spotlessCheck` | Formatting (google-java-format AOSP) |
| `javadoc` | Documentation builds cleanly, `-Werror` |
| `jar`, `sourcesJar`, `javadocJar` | The publishable artifacts |

`jacocoTestCoverageVerification` is **disabled in Phase 0**: there is no production code to cover
yet. It is enabled with the per-package targets in
[`TESTING_STRATEGY.md` §4](./TESTING_STRATEGY.md) starting at v0.1.0.

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "curiocontrol"
            pom { name = "CurioControl"; description = "..." }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl")
            credentials {
                username = "GITHUB_ACTOR"
                password = "GITHUB_TOKEN"
            }
        }
    }
}
```

> The FTC SDK dependency wiring was the highest-risk build detail. It is resolved in Phase 0 and
> recorded in [ADR-001](./docs/adr/001-ftc-sdk-dependency-strategy.md).

---

## 5. Static Analysis Gates

All of these run as part of `./gradlew build`. Nothing needs a separate command to be enforced.

| Tool | Runs on | Fails the build on | Config | Status |
|---|---|---|---|---|
| **Checkstyle** | `main`, `test` | any violation, `maxWarnings = 0` | `config/checkstyle/checkstyle.xml` | Enabled |
| **SpotBugs** | `main`, `test` | any reported bug | `config/spotbugs/exclude.xml` | Enabled |
| **ArchUnit** | tests | any rule violation | `src/test/java/.../architecture/` | Enabled |
| **Javadoc** | all | any error or warning (`-Werror`) | `build.gradle.kts` | Enabled |
| **Spotless** | all | any formatting difference | `build.gradle.kts` | Enabled |
| **markdownlint** | docs | warn → fix locally | `.markdownlint.json` | Config only |
| **Link check** | docs | warn | `lychee` | Planned |
| **Error Prone** | `main` | yes | `net.ltgt.errorprone` plugin | Not enabled |
| **Coverage gate** | test | below target on a gated package | `jacocoTestCoverageVerification` | Disabled until v0.1.0 |

Three notes on the choices:

- **Checkstyle has `maxWarnings = 0`.** A warning nobody must fix is a warning everybody ignores.
- **Exceptions live in one file.** `config/checkstyle/suppressions.xml` holds every deliberate
  exception, each with a comment explaining why. If an entry is no longer needed, delete it and fix
  the code — a suppression list that only grows is a list of rules that have stopped applying.
- **Error Prone is not enabled.** It overlaps substantially with `-Xlint:all -Werror` for a plain
  Java library, and it adds a compiler plugin dependency for little gain. Revisit if the codebase
  outgrows what the compiler catches.

The coverage gate is `jacocoTestCoverageVerification`, enabled with the per-package targets in
[`TESTING_STRATEGY.md` §4](./TESTING_STRATEGY.md). It is off in Phase 0 because there is no
production code to measure yet.

---

## 6. Branch Protection (`main`, `develop`)

The full settings, including the verification step, are in
[`BRANCH_PROTECTION.md`](./BRANCH_PROTECTION.md). In short:

- Require a pull request before merging; `develop` must be merged in before `main`.
- Require the `build` status check.
- Require review: 1 approving reviewer on `develop`, 2 on `main`, plus Code Owner review.
- Require branches to be up to date; disallow force pushes and deletion on both.
- `CODEOWNERS` auto-requests review on the package boundaries and the build configuration.

The Phase 0 exit criterion is that a pull request with a deliberately failing test is **blocked**.
That only holds once these rules are actually configured — the CI setup alone guarantees nothing.

## 7. Required GitHub Configuration

| Item | Where | Notes |
|---|---|---|
| `DEVELOCITY_ACCESS_KEY` | Actions secret | **Not required.** Only needed to tie scans to your own Develocity project instead of publishing anonymously. Value must be `<host>=<key>` |
| `GITHUB_TOKEN` | Built-in | Auto-provided; the release job requests `packages: write` |
| `PUBLISH_ENABLED` | Actions **variable** | Must be `true` for `release.yml` to actually publish |
| Pages | Settings → Pages | Source: **GitHub Actions** |
| Actions permissions | Settings → Actions | Read-only by default is fine; the release job declares its own |
| Dependabot | `.github/dependabot.yml` | Three ecosystems, weekly |
| CODEOWNERS teams | Settings → Teams | Create the placeholder teams before requiring review |
| Branch protection | Settings → Branches | See [`BRANCH_PROTECTION.md`](./BRANCH_PROTECTION.md) |

> `PUBLISH_ENABLED` is deliberately a repository **variable**, not a secret and not something set
> in the workflow. A tag pushed by anyone who can push a tag must not by itself publish a release.

## 8. Release Checklist Tied to CI (see also [`RELEASE_STRATEGY.md`](./RELEASE_STRATEGY.md))

1. Merge `develop → main` with CI green.
2. Bump `version` in `build.gradle.kts`; update `CHANGELOG.md` and the compatibility matrix.
   (`CurioConfig.version()` follows the build automatically — there is nothing to sync by hand.)
3. Push the annotated tag `vX.Y.Z`.
4. `release.yml` verifies from a clean build, then publishes and creates the GitHub Release —
   provided `PUBLISH_ENABLED` is `true`.
5. Verify the artifact resolves in a scratch project.
6. Merge `main → develop`.

## 9. CI Risks & Mitigations

| Risk | Impact | Mitigation | Status |
|---|---|---|---|
| FTC SDK not resolvable in CI | High | Resolved: the SDK is on Maven Central, unpacked by a cacheable task (ADR-001) | **Done** |
| Build cache unreliable for a non-fingerprintable dependency | Medium | No such dependency: the unpack task uses `@PathSensitive(NAME_ONLY)` | **Done** |
| Scan publish fails (network, TLS, server) | Low | The publish runs after the build and its failure never fails the build — verified by forcing one | **Done** |
| Develocity key missing on forks | None | No key is needed at all; a fork PR publishes anonymously like everything else | **Done** |
| A tag publishing without review | High | `PUBLISH_ENABLED` variable gates the publish step | **Done** |
| Slow test suite growth | Medium | Keep unit tests fast; the pure-logic suite runs in well under a minute today | Monitor |
| Release published with failing coverage | Medium | Coverage gate is part of the release verify step from v0.1.0 | Planned |
| Docs build silently broken | Medium | `mkdocs build --strict` | **Done** |
| Action version drift | Medium | Dependabot on `github-actions` | **Done** |
