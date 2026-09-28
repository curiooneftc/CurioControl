<!--
  Replace the placeholder teams with real GitHub teams before enabling required reviews.
  See PHASES.md task 0.12.
-->

name: Pull Request
description: A change to CurioControl
labels: [needs-review]

body:
  - type: markdown
    attributes:
      value: |
        Thanks for contributing to CurioControl.

        `./gradlew build` must be green before this can be merged: compile, tests, Checkstyle,
        SpotBugs, formatting, and Javadoc. If your local build is not green, CI will say so.

  - type: textarea
    id: summary
    attributes:
      label: What does this change?
      description: What the change does, and what problem it solves.
      placeholder: Adds a trapezoidal MotionProfile to the control package.
    validations:
      required: true

  - type: textarea
    id: motivation
    attributes:
      label: Why?
      description: |
        The motivation matters more than the diff. What could not be done before? What went wrong?
    validations:
      required: true

  - type: dropdown
    id: type
    attributes:
      label: Type of change
      options:
        - Bug fix
        - New feature
        - Refactor (no behavior change)
        - Documentation
        - Build, CI, or tooling
        - Performance
    validations:
      required: true

  - type: textarea
    id: tests
    attributes:
      label: Testing
      description: |
        What did you add or change? New logic needs unit tests; a bug fix needs a regression test.
        If you added a public API, is there a test that would fail if it broke?
      placeholder: Added a step-response test and an anti-windup-under-saturation test.
    validations:
      required: true

  - type: textarea
    id: docs
    attributes:
      label: Documentation
      description: |
        Did you change public API or behavior? Update the JavaDoc, the affected guide, and
        `CHANGELOG.md` in this same pull request.
      placeholder: Updated the control-tuning guide and added an Unreleased changelog entry.

  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: JavaDoc on all new or changed public API
          required: true
        - label: Unit tests for new logic; regression test for a bug fix
          required: true
        - label: No forbidden imports (the ArchUnit tests pass)
          required: true
        - label: No new allocations in a hot path
        - label: No competition constants added to the framework
        - label: `CHANGELOG.md` updated
        - label: `./gradlew build` is green locally
          required: true

  - type: textarea
    id: notes
    attributes:
      label: Anything else?
      description: Screenshots, telemetry output, or context a reviewer would want.
