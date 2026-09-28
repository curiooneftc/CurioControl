# Contributing

See the full guide at
[`CONTRIBUTING.md`](https://github.com/curiooneftc/CurioControl/blob/main/CONTRIBUTING.md).

## Quick reference

```bash
git clone https://github.com/curiooneftc/CurioControl.git
cd CurioControl
./gradlew build          # must be green before a PR
./gradlew spotlessApply  # format before pushing
```

## Branch model

```text
main        always releasable; tagged to release
develop     integration; feature work lands here
feature/*   short-lived, cut from develop, PR back to develop
hotfix/*    from a release tag, merged to main and develop
release/*   stabilization for a specific version
```

## Conventions that are enforced, not suggested

| Rule | Enforced by |
|---|---|
| No wildcard or unused imports | Checkstyle |
| JavaDoc on all public API | Checkstyle |
| No `System.out` / `System.err` | Checkstyle |
| No FTC or Android imports in `math`, `control`, `util` | ArchUnit |
| No forbidden package dependencies | ArchUnit |
| No known bug patterns | SpotBugs |
| Formatting | Spotless |

If a rule genuinely does not apply, add a documented entry to
`config/checkstyle/suppressions.xml`. Do not silence a rule that is merely inconvenient.

## The review checklist

- Correctness and edge-case handling
- Adherence to layering rules
- Tests for new logic, including a regression test for a bug fix
- Clear JavaDoc: units, preconditions, thread-safety, allocation behavior
- No unnecessary allocations in hot paths
- Lightweight enough for Robot Controller hardware
- `CHANGELOG.md` updated for anything user-visible
