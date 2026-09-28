<div align="center">

# CurioControl

**A modular control and development framework for FIRST Tech Challenge robots, developed by Curio One.**

[![Status](https://img.shields.io/badge/status-pre--release-orange)](https://github.com/curiooneftc/CurioControl)
[![License](https://img.shields.io/badge/license-BSD--3--Clause-blue)](LICENSE)
[![Java](https://img.shields.io/badge/java-17-orange)](https://adoptium.net/)

</div>

---

## What is CurioControl?

CurioControl is a reusable software framework for FTC robots. It gives a team a consistent,
modular architecture for robot development and removes the repetitive FTC SDK boilerplate that
otherwise gets rewritten every season.

It sits **on top of** the official FTC SDK, not in place of it. The SDK stays accessible whenever
direct SDK functionality is needed.

```text
┌─────────────────────────────┐
│       Robot Application     │
│   OpModes / Autonomous / UI │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│        CurioControl         │
│  Hardware / Control / Drive │
│  Commands / Telemetry / …   │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│          FTC SDK            │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│      Android / Hardware     │
└─────────────────────────────┘
```

The guiding idea: writing an OpMode should describe *what the robot does*, not re-implement the
infrastructure required to make a motor turn.

> **Status: pre-release.** CurioControl is under active development. Versions below `1.0.0` have an
> unstable API — pin an exact version and expect change. See [`RELEASE_STRATEGY.md`](docs/RELEASE_STRATEGY.md).

---

## 60-second quickstart

The framework ships as a normal Maven artifact. Add it to your robot project's
`build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    google()
    maven {
        url = uri("https://maven.pkg.github.com/curiooneftc/CurioControl")
        // Read-only token, supplied via a Gradle property or environment variable.
        // Never committed — see docs/RELEASE_STRATEGY.md section 9.
        credentials {
            username = providers.gradleProperty("github.actor").orNull
            password = providers.gradleProperty("github.token").orNull
        }
    }
}

dependencies {
    // Pin an exact version. Competition code must be reproducible.
    implementation("org.curioone:curiocontrol:0.1.0")
}
```

Then a TeleOp looks like this:

```java
@TeleOp(name = "Main TeleOp")
public class MainTeleOp extends CurioOpMode {

    @Override
    public void initRobot() {
        robot = new CurioRobot(hardwareMap);
    }

    @Override
    public void runRobot() {
        robot.drive()
                .mecanum(
                        gamepad1.left_stick_x,
                        gamepad1.left_stick_y,
                        gamepad1.right_stick_x);
    }
}
```

The full walkthrough — hardware config, wiring, deploy, run — is in the
[quickstart guide](docs/getting-started/quickstart.md).

> These APIs are the target design from the specification. The v0.1.0 release implements the
> `core`, `hardware`, `control`, `drive`, and `telemetry` packages; see [`PHASES.md`](PHASES.md)
> for what exists in which release.

---

## Design principles

| Principle | What it means in practice |
|---|---|
| **Simple** | Common operations need little code. |
| **Modular** | Every component is usable on its own. |
| **Testable** | Pure logic runs on a desktop JVM with no hardware. |
| **Explicit** | No hidden behavior, no magic. |
| **Lightweight** | FTC hardware is resource-constrained; hot paths allocate nothing. |
| **Extensible** | Teams can build their own components on top. |
| **Backwards-compatible** | Stable releases do not unexpectedly break robot code. |
| **Hardware-independent where possible** | Control and math never touch SDK objects. |

The last one is enforced, not just intended. `math`, `control`, and `util` contain zero imports of
`com.qualcomm.*` or `android.*`, and an ArchUnit test fails the build the moment one appears.

---

## Repository ecosystem

```text
Curio One/
├── CurioControl   ← this repository: the reusable framework
├── RobotCode      ← the current competition robot
├── CurioVision    ← optional vision tooling
└── CurioTools     ← development and scouting utilities
```

CurioControl holds **no competition constants** — no wheel diameters, no arm positions, no motor
names. Those live in the robot project's `RobotConfig` and are passed in as parameters, which is
what keeps the framework reusable across robots and seasons.

---

## Documentation

| Document | What it covers |
|---|---|
| [`PHASES.md`](PHASES.md) | The roadmap: phases, milestones, and release targets |
| [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md) | Repository layout, package boundaries, conventions |
| [`ARCHITECTURE_DECISIONS.md`](ARCHITECTURE_DECISIONS.md) | ADRs: why the framework looks the way it does |
| [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md) | Test tiers, coverage targets, mocking strategy |
| [`CI_CD_SETUP.md`](CI_CD_SETUP.md) | Workflows, static analysis gates, build cache |
| [`RELEASE_STRATEGY.md`](RELEASE_STRATEGY.md) | Versioning, publishing, compatibility matrix |
| [`BRANCH_PROTECTION.md`](BRANCH_PROTECTION.md) | Required repository settings |
| [`DOCUMENTATION_PLAN.md`](DOCUMENTATION_PLAN.md) | Docs site structure and standards |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | How to contribute, conventions, PR workflow |
| [`spec.md`](spec.md) | The source specification |
| [`docs/getting-started/`](docs/getting-started/) | Installation, project setup, first TeleOp |
| [`docs/guides/`](docs/guides/) | Task-oriented how-to guides |
| [`docs/concepts/`](docs/concepts/) | Architecture, philosophy, FAQ |
| [`docs/reference/`](docs/reference/) | Configuration, compatibility, migration |
| [`docs/adr/`](docs/adr/) | Architecture decision records |

---

## Contributing

Issues and pull requests are welcome. Start with
[`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) — it covers the branch model, coding conventions,
and what reviewers look for.

```bash
git clone https://github.com/curiooneftc/CurioControl.git
cd CurioControl
./gradlew build          # compile, test, static analysis, javadoc
./gradlew spotlessApply  # format before pushing
```

`./gradlew build` must be green before a pull request is opened.

---

## Building from source

**Prerequisites:** a JDK. The build targets Java 17 and downloads it automatically if your machine
does not have it, so nothing to install beyond Git.

```bash
./gradlew build                 # compile, test, Checkstyle, SpotBugs, Javadoc
./gradlew test                  # tests only
./gradlew javadoc               # API docs
./gradlew spotlessApply         # apply formatting
./gradlew publishToMavenLocal   # install to ~/.m2 for a scratch robot project
```

The FTC SDK is resolved from Maven Central and unpacked automatically — no Android SDK, no
vendored `.aar` files, no Robot Controller installation required.

### Compatibility

| Component | Version |
|---|---|
| CurioControl | 0.1.0-SNAPSHOT |
| FTC SDK | 12.0.0 |
| Java | 17 |
| Gradle | 8.14.5 |

---

## Security

The **library** makes **no network calls at runtime** and transmits **no robot data** externally.
It never requires connectivity while the robot is operating. Credentials for GitHub Packages are
supplied through Gradle properties or environment variables and are never committed. See
[`spec.md` §44](spec.md).

The **build** is a separate matter, and the distinction is worth being precise about. A Gradle
Build Scan is uploaded to the public Develocity server when a build *fails* — no account, no key,
terms of service accepted in `settings.gradle.kts`. That is build telemetry, not robot telemetry,
and it is scoped to failure only. To stop it entirely, delete the `termsOfUseUrl` and
`termsOfUseAgree` lines from `settings.gradle.kts`. See [`CI_CD_SETUP.md` §3](CI_CD_SETUP.md).

---

## License

[BSD 3-Clause](LICENSE) — the same license as the FTC SDK itself.
