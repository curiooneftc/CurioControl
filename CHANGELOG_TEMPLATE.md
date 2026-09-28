# Changelog Template

> Format and conventions for `CHANGELOG.md`. CurioControl follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/) (see `RELEASE_STRATEGY.md`).

---

## How to Use This

1. During development, add entries under the `## [Unreleased]` heading.
2. Each entry describes a **user-visible** change. Internal refactors with no behavior change don't need an entry.
3. Link each entry to its PR: `([#123](https://github.com/curiooneftc/CurioControl/pull/123))`.
4. At release time, rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD` and open a fresh `[Unreleased]` section.

---

## CHANGELOG.md Structure

```markdown
# Changelog

All notable changes to CurioControl are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
### Documentation

## [1.0.0] - YYYY-MM-DD

### Compatibility
- CurioControl: 1.0.0
- FTC SDK: X.Y
- Android SDK: NN
- Java: 17
- Gradle: X.Y

### Added
- ...

[Unreleased]: https://github.com/curiooneftc/CurioControl/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/curiooneftc/CurioControl/compare/v0.4.0...v1.0.0
```

---

## Entry Categories

| Category | Use for |
|----------|---------|
| **Added** | New features, new classes/methods, new modules |
| **Changed** | Changes in existing behavior that aren't breaking (e.g. default changed, improved performance) |
| **Deprecated** | API that still works but will be removed; include the replacement |
| **Removed** | API that no longer exists (breaking — must appear in a major release) |
| **Fixed** | Bug fixes (include the symptom and, if useful, the root cause) |
| **Security** | Security fixes or hardening |
| **Documentation** | Doc-only changes users would care about |

---

## Entry Style

**Good entries** are specific, user-focused, and mention the API by name:

```markdown
### Added
- `Motor.setVelocity(double)` for velocity-mode control.
- `MotionProfile` — trapezoidal motion profiles with `getPosition(t)`, `getVelocity(t)`, `getAcceleration(t)`, and `isFinished(t)`. ([#142](...))
- `PIDFController` combining PID with configurable feedforward. ([#151](...))

### Fixed
- `MecanumDrive` no longer exceeds ±1.0 per-motor power after normalization when all four inputs are saturated. ([#160](...))
- `Encoder.getDistance(...)` no longer loses precision for large tick counts (was using `float` internally). ([#163](...))

### Deprecated
- `Servo.open()` / `Servo.close()` are deprecated; mechanism-specific actions belong in a subsystem. Use `myClawSubsystem.open()`. Will be removed in 2.0.0.
```

**Avoid** vague entries like "fixed some bugs" or "improved performance" without specifics.

---

## Pre-1.0 vs. 1.x Notes

- **Pre-1.0 (`0.x`)**: the API is unstable. Breaking changes may appear in any release; note them clearly under **Changed** or **Removed** even without a major bump.
- **1.x**: breaking changes **must** be a major version and must have a corresponding `MIGRATION.md` section. Reference it from the changelog entry.

---

## Release Checklist (changelog-specific)

- [ ] `## [Unreleased]` renamed to the released version + date
- [ ] New `## [Unreleased]` section added
- [ ] Compatibility block filled in (`RELEASE_STRATEGY.md` §7)
- [ ] Compare links updated at the bottom of the file
- [ ] Every breaking change cross-references `MIGRATION.md`
- [ ] No vague entries; each is user-verifiable
