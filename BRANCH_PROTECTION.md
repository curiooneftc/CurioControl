# CurioControl branch protection

> Settings for the GitHub repository. Apply under **Settings → Branches → Branch protection
> rules**, or **Settings → Rules → Rulesets** if you prefer the newer UI. Phase 0 task 0.12.
>
> These are repository settings, not files. Nothing in this repository enforces them; the
> settings page does.

---

## Why this matters

The Phase 0 exit criterion is: **a pull request with a deliberately failing test is blocked.**

That only holds once these rules are actually configured. A CI workflow that reports failure is
only advisory until something refuses the merge.

## Verify it works

This is part of the exit criteria, not an optional extra:

1. Branch off `develop` and add a test that fails on purpose.
2. Open a pull request against `develop`.
3. Confirm the **Merge** button is disabled and the `build` check is red.
4. Close the pull request.

If the merge button is enabled, the rules are not in effect — regardless of what the settings
page appears to show.

---

## `develop`

The integration branch. All feature work merges here.

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅ |
| Require approvals | **1** |
| Require review from Code Owners | ✅ |
| Dismiss stale approvals on new pushes | ✅ |
| Require status checks to pass | ✅ `build` |
| Require conversation resolution before merging | ✅ |
| Require branches to be up to date before merging | ✅ |
| Block force pushes | ✅ |
| Block branch deletion | ✅ |
| Restrict who can push | Maintainers only |

## `main`

Identical to `develop`, except that a release must arrive by way of `develop`.

| Setting | Value |
|---|---|
| Require approvals | **1** |
| Everything else | Same as `develop` |

In practice: merge `develop` → `main`, then tag. A release is therefore a fast-forward of an
already-tested `develop` rather than a set of direct commits to `main`.

> **On the number of approvals.** An earlier draft of this document required 2 approvals on
> `main`, following a generic rule of thumb. That is not achievable for a team of two or three,
> and an unsatisfiable rule does not slow anything down — it simply blocks every release. If the
> team grows, raise it then, and expect the rule to be enforced from the day it lands.

---

## Required status checks

Exactly one:

- **`build`**

It runs `./gradlew build`, which covers compile, tests, Checkstyle, SpotBugs, formatting, and
Javadoc. One check means there is no ambiguity about what "green" means.

Do **not** mark `Verify the example project compiles` as required while it is gated behind
`if: false`. A required check that never runs blocks every pull request permanently. Add it as
required in the same change that enables it.

The check name must match the job's `name:` exactly. If a required check never reports, GitHub
blocks the merge and says "Expected — Waiting for status to be reported", which looks like a CI
outage rather than a typo.

---

## Ruleset alternative

The ruleset UI expresses the same policy with better defaults and a clearer audit trail. One
ruleset targeting `main` and `develop`:

```
Target:            main, develop
Enforcement:       Active

Rules:
  - Require a pull request before merging
      required approving reviews .... 1
      dismiss stale reviews ........ true
      require code owner review .... true
  - Require status checks to pass
      required checks ............... build
  - Require conversation resolution before merging
  - Require branches to be up to date before merging
  - Block force pushes
  - Restrict deletions
  - Require signed commits ........ (optional; enable when the team is ready)

Bypass list:
  - Repository admins, for recovering a bad release
```

A bypass list is worth having before you need it. Discovering you cannot merge a fix during a
competition because a rule locked you out is a bad weekend.

---

## Code Owners

`CODEOWNERS` currently assigns a single handle. Two things to know:

1. **A nonexistent owner is silently ignored.** If the handle is a typo, or a team was deleted,
   GitHub assigns nobody and "Require review from Code Owners" quietly stops doing anything while
   still showing as enabled. After any edit to that file, open a test pull request and confirm a
   reviewer is actually requested.
2. **You cannot be the required reviewer on your own pull request.** If you are the only owner
   and you open the pull request, code-owner review is unsatisfiable by anyone. This is fine
   while you are the only contributor — just be aware that the rule is not doing anything on your
   own branches yet.

## Teams, later

Splitting the single owner into teams improves routing once there is more than one person. The
suggested split, from the original plan:

| Team | Responsibility |
|---|---|
| `framework-leads` | Architecture, API design, releases — the default owner |
| `hardware` | `hardware` and `drive` packages, bench validation |
| `control` | `control`, `util`, `command` — algorithms and their tests |
| `vision` | `vision` package, field validation |
| `ci` | Build, CI, publishing, static analysis configuration |

Create them under **Settings → Teams** and update `.github/CODEOWNERS` in the same change, so the
two never disagree.

---

## Related

- [`CI_CD_SETUP.md`](CI_CD_SETUP.md) — the workflows the checks come from
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — the workflow these rules enforce
- [`PHASES.md`](PHASES.md) — Phase 0 task 0.12
