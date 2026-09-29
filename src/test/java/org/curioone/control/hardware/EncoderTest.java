package org.curioone.control.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.curioone.control.support.FakeDcMotor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Encoder}. */
@DisplayName("Encoder")
class EncoderTest {

    private static final double DELTA = 1e-9;

    private static final double TICKS_PER_REV = 537.7;

    private static final double WHEEL_DIAMETER_MM = 96.0;

    private static final double MM_PER_REV = Math.PI * WHEEL_DIAMETER_MM;

    private FakeDcMotor sdk;

    private Encoder encoder;

    @BeforeEach
    void setUp() {
        sdk = new FakeDcMotor();
        encoder = new Encoder(sdk, "leftFront/encoder");
    }

    @Test
    @DisplayName("rejects nulls")
    void rejectsNulls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Encoder((com.qualcomm.robotcore.hardware.DcMotorEx) null, "e"));
        assertThrows(IllegalArgumentException.class, () -> new Encoder(sdk, null));
    }

    @Test
    @DisplayName("reads the motor's position")
    void readsPosition() {
        sdk.setPositionForTest(500);
        assertEquals(500, encoder.getPosition());
    }

    @Test
    @DisplayName("defaults to forward counting")
    void defaultsToForward() {
        assertEquals(Encoder.Direction.FORWARD, encoder.getDirection());
    }

    @Test
    @DisplayName("inverts the reading when reversed")
    void reversesReading() {
        sdk.setPositionForTest(500);
        encoder.setDirection(Encoder.Direction.REVERSED);
        assertEquals(-500, encoder.getPosition());
    }

    @Test
    @DisplayName("inverts velocity too, so the two never disagree")
    void reversesVelocity() {
        sdk.setVelocity(200.0);
        encoder.setDirection(Encoder.Direction.REVERSED);
        assertEquals(-200.0, encoder.getVelocity(), DELTA);
    }

    @Test
    @DisplayName("rejects a null direction")
    void rejectsNullDirection() {
        assertThrows(IllegalArgumentException.class, () -> encoder.setDirection(null));
    }

    @Test
    @DisplayName("reset zeroes the reading")
    void resetZeroes() {
        sdk.setPositionForTest(900);
        encoder.reset();
        assertEquals(0, encoder.getPosition());
    }

    @Test
    @DisplayName("converts position to millimetres")
    void convertsToMillimetres() {
        sdk.setPositionForTest(100);
        assertEquals(
                100.0 * MM_PER_REV / TICKS_PER_REV,
                encoder.getDistance(TICKS_PER_REV, WHEEL_DIAMETER_MM),
                DELTA);
    }

    @Test
    @DisplayName("scales linearly with tick count")
    void scalesWithTicks() {
        sdk.setPositionForTest(537);
        assertEquals(
                537.0 * MM_PER_REV / TICKS_PER_REV,
                encoder.getDistance(TICKS_PER_REV, WHEEL_DIAMETER_MM),
                DELTA);
    }

    @Test
    @DisplayName("converts velocity to millimetres per second")
    void convertsVelocity() {
        sdk.setVelocity(100.0);
        assertEquals(
                100.0 * MM_PER_REV / TICKS_PER_REV,
                encoder.getVelocityMmPerSecond(TICKS_PER_REV, WHEEL_DIAMETER_MM),
                DELTA);
    }

    @Test
    @DisplayName("rejects non-positive physical constants")
    void rejectsBadConstants() {
        assertThrows(
                IllegalArgumentException.class, () -> encoder.getDistance(0.0, WHEEL_DIAMETER_MM));
        assertThrows(IllegalArgumentException.class, () -> encoder.getDistance(TICKS_PER_REV, 0.0));
        assertThrows(
                IllegalArgumentException.class, () -> encoder.getDistance(-1.0, WHEEL_DIAMETER_MM));
        assertThrows(
                IllegalArgumentException.class,
                () -> encoder.getVelocityMmPerSecond(TICKS_PER_REV, Double.NaN));
    }

    @Test
    @DisplayName("converts a whole number of revolutions to the wheel circumference")
    void wholeRevolutions() {
        // A real encoder reports integer ticks, so 537.7 ticks per revolution is approximated by
        // 538. The result should be within half a tick of one circumference.
        sdk.setPositionForTest(538);

        assertEquals(
                MM_PER_REV,
                encoder.getDistance(TICKS_PER_REV, WHEEL_DIAMETER_MM),
                0.5 * MM_PER_REV / TICKS_PER_REV,
                "one revolution should convert to the wheel's circumference, to within a tick");
    }
}
