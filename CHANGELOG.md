# Changelog

All notable changes to CurioControl are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Gradle build with a version catalog, Java 17 toolchain (auto-provisioned), and
  configuration cache.
- FTC SDK `12.0.0` wired as a compile-only dependency, resolved from Maven Central
  (ADR-001). The published artifact contains no SDK classes; the Robot Controller
  provides them at runtime.
- Static analysis: Checkstyle, SpotBugs, and Spotless (google-java-format AOSP
  style), all enforced by `./gradlew build`.
- Test infrastructure: JUnit 5, Mockito, ArchUnit, and JaCoCo.
- Architecture guard tests for the pure-Java boundary of `math`, `control`, and
  `util`, and for the package layering in `docs/PROJECT_STRUCTURE.md` (ADR-003,
  ADR-010).
- `CurioConfig` with framework-wide flags and a build-derived version constant.
- Base package layout `org.curioone.control.*` with a `package-info.java` per package.
- Develocity Build Scan publishing on build failure, with no account or API key
  required. Terms-of-service acceptance is declared in `settings.gradle.kts` and
  therefore applies to every contributor.
- GitHub Actions: `build.yml`, `release.yml` (dry-run publish until enabled),
  `docs.yml`, `perf.yml`, and Dependabot.
- Repository scaffolding docs: `README.md`, `CONTRIBUTING.md`, `LICENSE`
  (BSD 3-Clause), `MIGRATION.md`, `BRANCH_PROTECTION.md`, and a MkDocs site skeleton.

### Changed
- Maven coordinates are `org.curioone:curiocontrol` and the Java package is
  `org.curioone.control`, following the GitHub org rather than `spec.md` §5's
  `dev.curio`. Done in Phase 0, before any released artifact. See ADR-013.

### Documentation
- Phase 0 infrastructure plan and architecture decisions, including the resolved
  FTC SDK dependency strategy and the namespace decision.

[Unreleased]: https://github.com/curiooneftc/CurioControl/compare/v0.1.0...HEAD
