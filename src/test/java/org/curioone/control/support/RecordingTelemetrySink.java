package org.curioone.control.support;

import java.util.ArrayList;
import java.util.List;
import org.curioone.control.core.TelemetrySink;

/**
 * A {@link TelemetrySink} that records what it was given, for asserting on telemetry behaviour.
 *
 * <p>The SDK's {@code Telemetry} has two dozen methods, so asserting against it directly would mean
 * mocking a wide surface and checking very little. This records the two things that matter: which
 * captions and values were written, and how many times they were flushed.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. One test thread.
 *
 * @since 0.1.0
 */
public final class RecordingTelemetrySink implements TelemetrySink {

    private final List<String> captions = new ArrayList<>();

    private final List<Object> values = new ArrayList<>();

    private int flushes;

    @Override
    public void addLine(String caption, Object value) {
        captions.add(caption);
        values.add(value);
    }

    @Override
    public void flush() {
        flushes++;
    }

    /**
     * Returns every caption written since construction, in order.
     *
     * @return the recorded captions
     */
    public List<String> captions() {
        return captions;
    }

    /**
     * Returns every value written since construction, in order.
     *
     * @return the recorded values
     */
    public List<Object> values() {
        return values;
    }

    /**
     * Returns how many times {@link #flush()} was called.
     *
     * @return the flush count
     */
    public int flushes() {
        return flushes;
    }

    /**
     * Returns the caption at an index.
     *
     * @param index the index
     * @return the caption
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String captionAt(int index) {
        return captions.get(index);
    }

    /**
     * Returns the value at an index.
     *
     * @param index the index
     * @return the value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Object valueAt(int index) {
        return values.get(index);
    }

    /** Forgets everything recorded so far. */
    public void clear() {
        captions.clear();
        values.clear();
        flushes = 0;
    }
}
