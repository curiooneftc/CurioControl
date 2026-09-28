# ADR-012: The framework never contains competition constants

**Status:** Accepted
**Source:** `spec.md` §9

## Decision

The framework contains no season-specific or Curio One robot constants. No wheel diameters, no arm
positions, no motor names, no specific drivetrain dimensions. Those live in the robot project's
`RobotConfig`.

## Consequences

- CurioControl stays reusable across robots and seasons, which is the entire point of shipping a
  framework.
- APIs accept these values as parameters, for example
  `encoder.getDistance(ticksPerRev, wheelDiameterMm)`.
- Test fixtures hold the physical constants, never `src/main`. The architecture tests treat a
  real robot's numbers in framework code as a layering violation.

## Rejected

**Ship sensible defaults.** A default wheel diameter is wrong for every robot except one, and a
wrong default that silently produces a wrong distance is worse than a required parameter that the
compiler makes you supply.
