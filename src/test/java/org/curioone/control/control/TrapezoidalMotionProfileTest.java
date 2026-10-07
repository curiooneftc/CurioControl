package org.curioone.control.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TrapezoidalMotionProfile}. */
@DisplayName("TrapezoidalMotionProfile")
class TrapezoidalMotionProfileTest {

    private static final double DELTA = 1e-9;

    private static final TrapezoidalMotionProfile.Constraints LIMITS =
            new TrapezoidalMotionProfile.Constraints(100.0, 200.0);

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects bad limits and positions")
        void rejectsBadArguments() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TrapezoidalMotionProfile.Constraints(0.0, 200.0));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TrapezoidalMotionProfile.Constraints(100.0, -1.0));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new TrapezoidalMotionProfile.Constraints(
                                    Double.POSITIVE_INFINITY, 200.0));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TrapezoidalMotionProfile(0.0, 100.0, null));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new TrapezoidalMotionProfile(Double.NaN, 100.0, LIMITS));
        }

        @Test
        @DisplayName("exposes its endpoints")
        void exposesEndpoints() {
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(10.0, 110.0, LIMITS);

            assertEquals(10.0, profile.getStart(), DELTA);
            assertEquals(110.0, profile.getTarget(), DELTA);
        }
    }

    @Nested
    @DisplayName("trapezoid phases")
    class Phases {

        @Test
        @DisplayName("accelerates, cruises, then decelerates")
        void threePhases() {
            // vMax 100, aMax 200: accel distance 25 each way, so a 100-unit move cruises 50.
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(0.0, 100.0, LIMITS);

            // Mid-accel: v = a*t, s = 1/2 a t^2.
            assertEquals(50.0, profile.getVelocity(0.25), DELTA);
            assertEquals(6.25, profile.getPosition(0.25), DELTA);
            assertEquals(200.0, profile.getAcceleration(0.25), DELTA);

            // Cruise: full speed, zero acceleration.
            assertEquals(100.0, profile.getVelocity(0.75), DELTA);
            assertEquals(0.0, profile.getAcceleration(0.75), DELTA);

            // Mid-decel: slowing down.
            assertEquals(50.0, profile.getVelocity(1.25), 1e-6);
            assertEquals(-200.0, profile.getAcceleration(1.25), DELTA);
        }

        @Test
        @DisplayName("arrives exactly at the total time")
        void exactArrival() {
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(0.0, 100.0, LIMITS);

            assertEquals(1.5, profile.totalTime(), DELTA);
            assertEquals(100.0, profile.getPosition(profile.totalTime()), 1e-6);
            assertEquals(0.0, profile.getVelocity(profile.totalTime()), 1e-6);
            assertTrue(profile.isFinished(profile.totalTime()));
            assertFalse(profile.isFinished(profile.totalTime() - 0.5));
        }

        @Test
        @DisplayName("sample agrees with the individual getters")
        void sampleAgrees() {
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(0.0, 100.0, LIMITS);

            final TrapezoidalMotionProfile.State state = profile.sample(0.25);

            assertEquals(profile.getPosition(0.25), state.position(), DELTA);
            assertEquals(profile.getVelocity(0.25), state.velocity(), DELTA);
            assertEquals(profile.getAcceleration(0.25), state.acceleration(), DELTA);
            assertThrows(IllegalArgumentException.class, () -> profile.sample(Double.NaN));
        }

        @Test
        @DisplayName("short moves form a triangle without cruising")
        void triangular() {
            // 10 units at these limits can never reach 100 u/s: peak is sqrt(10*200).
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(0.0, 10.0, LIMITS);

            assertEquals(10.0, profile.getPosition(profile.totalTime()), 1e-6);
            assertTrue(profile.getVelocity(profile.totalTime() / 2.0) < 100.0);
            // A quarter of the way through a symmetric triangle the profile is still speeding
            // up at the full acceleration limit.
            assertEquals(200.0, profile.getAcceleration(profile.totalTime() / 4.0), DELTA);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class Edges {

        @Test
        @DisplayName("reverse moves mirror forward moves")
        void reverseMirrors() {
            final TrapezoidalMotionProfile forward =
                    new TrapezoidalMotionProfile(0.0, 100.0, LIMITS);
            final TrapezoidalMotionProfile reverse =
                    new TrapezoidalMotionProfile(100.0, 0.0, LIMITS);

            assertEquals(forward.totalTime(), reverse.totalTime(), DELTA);
            assertEquals(100.0 - forward.getPosition(0.25), reverse.getPosition(0.25), DELTA);
            assertEquals(-forward.getVelocity(0.25), reverse.getVelocity(0.25), DELTA);
        }

        @Test
        @DisplayName("zero distance is finished immediately")
        void zeroDistance() {
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(50.0, 50.0, LIMITS);

            assertEquals(0.0, profile.totalTime(), DELTA);
            assertEquals(50.0, profile.getPosition(0.0), DELTA);
            assertEquals(0.0, profile.getVelocity(1.0), DELTA);
            assertTrue(profile.isFinished(0.0));
        }

        @Test
        @DisplayName("time outside the profile clamps to the endpoints")
        void clampsTime() {
            final TrapezoidalMotionProfile profile =
                    new TrapezoidalMotionProfile(0.0, 100.0, LIMITS);

            assertEquals(0.0, profile.getPosition(-5.0), DELTA);
            assertEquals(100.0, profile.getPosition(profile.totalTime() + 5.0), DELTA);
            assertThrows(IllegalArgumentException.class, () -> profile.getPosition(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> profile.isFinished(Double.NaN));
        }
    }
}
