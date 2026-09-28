# Installation

> Status: this page describes the target v1.0.0 setup. The v0.1.0 release implements a subset of
> the API; see [the phases document](../PHASES.md) for exactly what exists in which release.

## 1. Add the repository

CurioControl is distributed through GitHub Packages. Add the repository to your robot project's
`settings.gradle.kts` (or `build.gradle.kts`, depending on your template):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
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

`google()` is required because the FTC SDK depends on `androidx` artifacts.

## 2. Add the dependency

```kotlin
dependencies {
    implementation("org.curioone:curiocontrol:0.1.0")
}
```

**Always pin an exact version.** Never use `latest`, a `+` range, or a snapshot of an unreleased
branch — competition robot code has to be reproducible (`spec.md` §36).

## 3. Provide a read token

Anonymous read access works only if the package has been made public in the repository settings.
Otherwise, supply a **read-only** personal access token. Do not commit it.

Put it in `~/.gradle/gradle.properties` on your own machine:

```properties
github.actor=<your-github-username>
github.token=<read-only-token>
```

Or supply it from the environment in CI:

```bash
export GITHUB_TOKEN=<read-only-token>
```

!!! warning "Never commit credentials"
    Tokens in a version-controlled file are a security incident, not a convenience. See
    [`spec.md` §44](../spec.md) and [the release strategy](../RELEASE_STRATEGY.md).

## 4. Check the compatibility matrix

Every CurioControl release pins a specific FTC SDK version. Make sure it matches the Robot
Controller version you run in the room — a mismatch is a documented incompatibility, not something
to discover during a match.

See the [compatibility reference](compatibility.md).

## What's next

- [Quickstart](quickstart.md) — a working TeleOp in ten minutes
- [Project setup](project-setup.md) — the full Gradle template and where files go
