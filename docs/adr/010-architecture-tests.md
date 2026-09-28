# ADR-010: Architecture rules enforced by tests

**Status:** Accepted

## Decision

The layering and purity rules in `PROJECT_STRUCTURE.md` are enforced by **ArchUnit** tests, not
merely documented.

## Consequences

- A violation — an FTC import in `math`, a cycle between packages, a pull from `vision` into
  `drive` — fails CI immediately, not in a code review months later.
- The rules are centralized in executable form that doubles as living documentation.
- Small build-time cost. ArchUnit is lightweight, and the rules run over compiled classes only.
- Rules use `allowEmptyShould(true)`, so a package with nothing in it yet is not a failure. A rule
  that *stops* checking once its package is populated still is.

## Rejected

**Convention only.** Conventions decay quietly. A reviewer has to notice the violation, understand
why it matters, and remember to reject the change. A test notices every time, including in the
contributor's first commit.
