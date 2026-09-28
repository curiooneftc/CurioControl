# Fixtures and test resources

Sample CSV logs for the logger tests and serialized AprilTag detections for the vision pose tests
land here, per `TESTING_STRATEGY.md` §7.

This directory is intentionally empty in Phase 0. It is kept in version control because an empty
directory is not preserved by git, and the first fixture added should not have to create the
directory tree as well.

Nothing competition-specific belongs here. Physical constants — wheel diameter, ticks per
revolution, gear ratio — live in the test fixtures that need them, never in `src/main`
(ADR-012).
