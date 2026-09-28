# Migration Guide

How to move between CurioControl versions.

- **`0.x` releases** — the API is unstable. Breaking changes may appear in any release without a
  major bump. Read the [`CHANGELOG.md`](CHANGELOG.md) for each version you are moving between.
- **`1.x` and later** — the public API is stable. A breaking change requires a major version and
  a section in this file.

Always **pin an exact version** in a competition robot project. Floating versions break the
reproducibility guarantee that competition code depends on (`spec.md` §36).

---

## Migrating to 1.0.0

Not yet released. This section will be written during Phase 5, with before/after code for every
breaking change and a mechanical upgrade checklist.

---

## Migrating within `0.x`

There is no single mechanical procedure, because pre-1.0 changes are allowed to break the API.
What to do:

1. Read the `CHANGELOG.md` entries for every version between your current pin and the target.
2. Update the pinned version in `build.gradle.kts`.
3. Build. The compiler finds signature changes; the ArchUnit tests find layering changes.
4. Re-run the robot on the bench. Behavioural changes are called out in the changelog under
   **Changed** and **Fixed**.

If you hit something the changelog does not explain, please open an issue — that is a
documentation bug, and it is worth reporting.
