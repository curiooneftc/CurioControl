package org.curioone.control.core;

import java.util.Arrays;
import org.curioone.control.util.Clock;

/**
 * Batches telemetry values and flushes them to the Robot Controller in one call.
 *
 * <p>Values are buffered by {@code add} and written by {@link #update()}, so a loop that reports
 * eight values makes <em>one</em> SDK call rather than eight. On FTC hardware that matters: the
 * driver display is not free, and one call per loop is affordable where one per value is not
 * (ADR-006).
 *
 * <p>Example:
 *
 * <pre>{@code
 * robot.telemetry()
 *         .add(TelemetryCategory.DRIVE, "Heading", imu.heading())
 *         .add(TelemetryCategory.ARM, "Lift", lift.getPosition())
 *         .update();
 * }</pre>
 *
 * <h2>Allocation</h2>
 *
 * The key and value arrays are preallocated and never grow, so {@code add} does not resize
 * anything. Two allocations remain outside this class's control and are worth knowing about:
 *
 * <ul>
 *   <li><strong>The key.</strong> A key built by string concatenation allocates on every loop. Use
 *       a constant literal, or a {@code static final} constant, and the key is shared.
 *   <li><strong>The value.</strong> A {@code double} is boxed to pass as an {@link Object}. The
 *       alternative — formatting to a {@code String} here — allocates strictly more, so the boxing
 *       is the cheaper of the two.
 * </ul>
 *
 * <p>The whole hot path is benchmarked in Phase 2 (see {@code PHASES.md} M2.6). What is claimed
 * here is only that {@code add} does not itself allocate a collection.
 *
 * <h2>Cadence</h2>
 *
 * {@link #update()} flushes at most once per {@code CurioConfig.TELEMETRY_PERIOD_MILLIS},
 * defaulting to five times a second. A driver cannot use 100 Hz text, and a driver display
 * competing with the control loop is a bad trade. Set the period to {@code 0} to flush every loop
 * while debugging.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call it only from the OpMode thread.
 *
 * @since 0.1.0
 */
public final class TelemetryManager {

    /** Initial capacity of the key and value buffers; the robot can report more, they just grow. */
    private static final int INITIAL_CAPACITY = 32;

    private final TelemetrySink sink;

    private final Clock clock;

    private String[] keys = new String[INITIAL_CAPACITY];

    private Object[] values = new Object[INITIAL_CAPACITY];

    private TelemetryCategory[] categories = new TelemetryCategory[INITIAL_CAPACITY];

    private int size;

    private long lastFlushNanos = Long.MIN_VALUE;

    /**
     * Creates a manager writing to a telemetry sink.
     *
     * <p>In an OpMode, pass {@code new SdkTelemetrySink(telemetry)}. To discard output, pass {@link
     * TelemetrySink#discarding()}.
     *
     * @param sink where values are written
     * @param clock the time source used for the flush cadence
     * @throws IllegalArgumentException if {@code sink} or {@code clock} is {@code null}
     */
    public TelemetryManager(TelemetrySink sink, Clock clock) {
        if (sink == null) {
            throw new IllegalArgumentException("sink must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.sink = sink;
        this.clock = clock;
    }

    /**
     * Buffers a numeric value with no category prefix.
     *
     * <p>The common case. A driver reading a phone screen sees {@code Voltage}, not {@code [SYSTEM]
     * Voltage} — the prefix is noise unless a program is deliberately grouping its output.
     *
     * @param key a constant, allocation-free label such as {@code "Voltage"}
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(String key, double value) {
        return store(null, key, value);
    }

    /**
     * Buffers a whole-number value with no category prefix.
     *
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(String key, int value) {
        return store(null, key, value);
    }

    /**
     * Buffers a text value with no category prefix.
     *
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(String key, String value) {
        return store(null, key, value);
    }

    /**
     * Buffers a true/false value with no category prefix.
     *
     * <p>Sensor states, beam breaks, and "at target" flags read far better as {@code true} than as
     * {@code 1.0}.
     *
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(String key, boolean value) {
        return store(null, key, value);
    }

    /**
     * Buffers a numeric value in a category.
     *
     * <p>A {@link TelemetryCategory#DEBUG} value is discarded unless {@code CurioConfig.DEBUG} is
     * set, so diagnostic lines can be left in the code permanently.
     *
     * @param category the category, or {@code null} for no category prefix
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(TelemetryCategory category, String key, double value) {
        return store(category, key, value);
    }

    /**
     * Buffers a whole-number value in a category.
     *
     * @param category the category, or {@code null} for no category prefix
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(TelemetryCategory category, String key, int value) {
        return store(category, key, value);
    }

    /**
     * Buffers a text value in a category.
     *
     * @param category the category, or {@code null} for no category prefix
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(TelemetryCategory category, String key, String value) {
        return store(category, key, value);
    }

    /**
     * Buffers a true/false value in a category.
     *
     * @param category the category, or {@code null} for no category prefix
     * @param key a constant, allocation-free label
     * @param value the value to report
     * @return this manager, for chaining
     */
    public TelemetryManager add(TelemetryCategory category, String key, boolean value) {
        return store(category, key, value);
    }

    /**
     * Flushes buffered values if the configured interval has elapsed.
     *
     * <p>Safe to call every loop: it does nothing until the interval has passed. A call made within
     * the interval leaves the buffer intact, so values are not lost — they go out on the next call
     * that is allowed to flush.
     *
     * @return {@code true} if a flush happened
     */
    public boolean update() {
        if (size == 0) {
            return false;
        }

        final long now = clock.nowNanos();
        final long periodMillis = Math.max(0, CurioConfig.TELEMETRY_PERIOD_MILLIS);

        if (periodMillis > 0 && lastFlushNanos != Long.MIN_VALUE) {
            final long elapsedMillis = (now - lastFlushNanos) / 1_000_000L;
            if (elapsedMillis < periodMillis) {
                return false;
            }
        }

        flush(now);
        return true;
    }

    /**
     * Flushes buffered values immediately, ignoring the configured interval.
     *
     * <p>Use at the end of an OpMode so the final state is visible, or from a test.
     */
    public void updateNow() {
        flush(clock.nowNanos());
    }

    /**
     * Returns the number of values currently buffered.
     *
     * @return the buffer size
     */
    public int size() {
        return size;
    }

    private TelemetryManager store(TelemetryCategory category, String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("telemetry key must not be null");
        }
        if (category != null && category.isDebugOnly() && !CurioConfig.DEBUG) {
            return this;
        }
        if (size == keys.length) {
            grow();
        }
        categories[size] = category;
        keys[size] = key;
        values[size] = value;
        size++;
        return this;
    }

    private void grow() {
        final int newCapacity = keys.length * 2;
        keys = Arrays.copyOf(keys, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
        categories = Arrays.copyOf(categories, newCapacity);
    }

    private void flush(long nowNanos) {
        for (int i = 0; i < size; i++) {
            final String caption =
                    categories[i] == null ? keys[i] : "[" + categories[i].label() + "] " + keys[i];
            sink.addLine(caption, values[i]);
        }
        sink.flush();
        clear();
        lastFlushNanos = nowNanos;
    }

    private void clear() {
        // Null out the value slots so a flushed buffer does not pin references for the whole
        // loop. The key slots are kept: they are almost always shared string constants.
        Arrays.fill(values, 0, size, null);
        Arrays.fill(categories, 0, size, null);
        size = 0;
    }
}
