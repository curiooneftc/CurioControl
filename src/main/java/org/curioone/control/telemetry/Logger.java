package org.curioone.control.telemetry;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.curioone.control.core.CurioConfig;
import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;

/**
 * Optional structured logging to CSV.
 *
 * <p>Disabled by default: every {@link #record} and {@link #update} returns after a single flag
 * check on {@link CurioConfig#LOGGING_ENABLED}, so a competition run with logging off pays one
 * boolean read per call and allocates nothing. Turn it on explicitly and each {@code update} emits
 * one CSV row:
 *
 * <pre>{@code
 * time,heading,armPosition,motorPower
 * 0.00,0.2,0,0.0
 * 0.02,0.4,15,0.5
 * }</pre>
 *
 * <pre>{@code
 * CurioConfig.LOGGING_ENABLED = true;
 * Logger log = Logger.toFile(new File("/sdcard/CurioControl/logs"), "auto", clock);
 * log.registerField("heading");
 * log.registerField("armPosition");
 * // ... per loop:
 * log.record("heading", imu.heading());
 * log.record("armPosition", arm.getPosition());
 * log.update();
 * log.close();
 * }</pre>
 *
 * <h2>Memory bound</h2>
 *
 * <p>Rows pass through a bounded ring buffer (default 512 rows). When the buffer is full the oldest
 * row is dropped and counted — see {@link #getDroppedRowCount()} — so a stall in storage I/O
 * degrades into gaps in the log rather than an out-of-memory kill of the control loop. Memory is
 * otherwise O(fields): one slot array plus one reusable formatting buffer.
 *
 * <p><strong>Thread safety:</strong> not thread-safe. Call from the OpMode thread only.
 *
 * @since 0.2.0
 */
public final class Logger implements Closeable {

    /** Default ring-buffer capacity, in rows. */
    public static final int DEFAULT_BUFFER_CAPACITY = 512;

    /** Default flush cadence: every row reaches storage on {@code update}. */
    public static final int DEFAULT_FLUSH_EVERY = 1;

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final BufferedWriter writer;

    private final Clock clock;

    private final List<String> fields = new ArrayList<>();

    private final Map<String, Integer> indexes = new HashMap<>();

    private final Deque<String> pending = new ArrayDeque<>();

    private final StringBuilder row = new StringBuilder();

    private String[] current;

    private boolean headerWritten;

    private boolean closed;

    private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;

    private int flushEvery = DEFAULT_FLUSH_EVERY;

    private long droppedRows;

    /**
     * Creates a logger writing to a character sink.
     *
     * @param writer where CSV goes; wrapped in a buffer internally
     * @throws IllegalArgumentException if {@code writer} is {@code null}
     */
    public Logger(Writer writer) {
        this(writer, new SystemClock());
    }

    /**
     * Creates a logger with an injected time source.
     *
     * @param writer where CSV goes; wrapped in a buffer internally
     * @param clock the time source for the {@code time} column
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Logger(Writer writer, Clock clock) {
        if (writer == null) {
            throw new IllegalArgumentException("writer must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.writer =
                writer instanceof BufferedWriter
                        ? (BufferedWriter) writer
                        : new BufferedWriter(writer);
        this.clock = clock;
    }

    /**
     * Opens a logger on a timestamped CSV file inside a directory.
     *
     * <p>Creates the directory when missing. The file is named {@code <prefix>-<millis>.csv}, so
     * consecutive runs never overwrite each other.
     *
     * @param directory the log directory, for example {@code /sdcard/CurioControl/logs}
     * @param prefix the filename prefix
     * @param clock the time source for the {@code time} column
     * @return the logger
     * @throws IllegalArgumentException if any argument is {@code null} or blank
     * @throws CurioIoException if the directory or file cannot be created
     */
    public static Logger toFile(File directory, String prefix, Clock clock) {
        if (directory == null) {
            throw new IllegalArgumentException("directory must not be null");
        }
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be null or empty");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new CurioIoException("could not create log directory: " + directory);
        }
        final File file = new File(directory, prefix + "-" + System.currentTimeMillis() + ".csv");
        try {
            return new Logger(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                    clock);
        } catch (IOException e) {
            throw new CurioIoException("could not open log file: " + file, e);
        }
    }

    /**
     * Declares a column.
     *
     * <p>Columns appear in the CSV in registration order, after {@code time}. Hierarchical names
     * such as {@code "drive/heading"} are plain strings: they group visually in a spreadsheet
     * without any channel machinery in the hot path.
     *
     * @param field the column name
     * @throws IllegalArgumentException if {@code field} is {@code null}, empty, or already
     *     registered
     * @throws IllegalStateException if rows have already been written — the schema is frozen with
     *     the header
     */
    public void registerField(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("field must not be null or empty");
        }
        if (indexes.containsKey(field)) {
            throw new IllegalArgumentException("field already registered: " + field);
        }
        if (headerWritten) {
            throw new IllegalStateException("schema is frozen once rows are written: " + field);
        }
        indexes.put(field, fields.size());
        fields.add(field);
        growSlots();
    }

    /**
     * Records a numeric value for the current row.
     *
     * <p>A no-op when {@link CurioConfig#LOGGING_ENABLED} is off — one flag check, no allocation.
     * Recording the same field twice keeps the latest value.
     *
     * @param field a registered column
     * @param value the value
     * @throws IllegalArgumentException if {@code field} was never registered
     * @throws IllegalStateException if the logger is closed
     */
    public void record(String field, double value) {
        if (!CurioConfig.LOGGING_ENABLED) {
            return;
        }
        ensureOpen();
        current[indexOf(field)] = Double.toString(value);
    }

    /**
     * Records a whole-number value for the current row.
     *
     * <p>A no-op when {@link CurioConfig#LOGGING_ENABLED} is off.
     *
     * @param field a registered column
     * @param value the value
     * @throws IllegalArgumentException if {@code field} was never registered
     * @throws IllegalStateException if the logger is closed
     */
    public void record(String field, int value) {
        record(field, Integer.toString(value));
    }

    /**
     * Records a whole-number value for the current row.
     *
     * <p>A no-op when {@link CurioConfig#LOGGING_ENABLED} is off.
     *
     * @param field a registered column
     * @param value the value
     * @throws IllegalArgumentException if {@code field} was never registered
     * @throws IllegalStateException if the logger is closed
     */
    public void record(String field, long value) {
        record(field, Long.toString(value));
    }

    /**
     * Records a true/false value for the current row.
     *
     * <p>A no-op when {@link CurioConfig#LOGGING_ENABLED} is off.
     *
     * @param field a registered column
     * @param value the value
     * @throws IllegalArgumentException if {@code field} was never registered
     * @throws IllegalStateException if the logger is closed
     */
    public void record(String field, boolean value) {
        record(field, Boolean.toString(value));
    }

    /**
     * Records a text value for the current row.
     *
     * <p>Values containing commas, quotes, or line breaks are quoted per RFC 4180. A no-op when
     * {@link CurioConfig#LOGGING_ENABLED} is off.
     *
     * @param field a registered column
     * @param value the value
     * @throws IllegalArgumentException if {@code field} was never registered, or {@code value} is
     *     {@code null}
     * @throws IllegalStateException if the logger is closed
     */
    public void record(String field, String value) {
        if (!CurioConfig.LOGGING_ENABLED) {
            return;
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        ensureOpen();
        current[indexOf(field)] = quote(value);
    }

    /**
     * Ends the current row and makes it eligible for writing.
     *
     * <p>A no-op when {@link CurioConfig#LOGGING_ENABLED} is off. Unrecorded columns write as
     * empty, so a field logged only on some iterations reads as gaps rather than shifted columns.
     * Drains the ring buffer to storage once {@code flushEvery} rows are pending.
     *
     * @throws IllegalStateException if the logger is closed
     * @throws CurioIoException if the row cannot be written
     */
    public void update() {
        if (!CurioConfig.LOGGING_ENABLED) {
            return;
        }
        ensureOpen();
        if (!headerWritten) {
            writeHeader();
        }
        formatRow();
        if (pending.size() >= bufferCapacity) {
            pending.removeFirst();
            droppedRows++;
        }
        pending.addLast(row.toString());
        clearSlots();
        if (pending.size() >= flushEvery) {
            drain();
        }
    }

    /**
     * Sets how many pending rows trigger a write to storage.
     *
     * @param rows the flush cadence; must be positive
     * @throws IllegalArgumentException if {@code rows} is not positive
     */
    public void setFlushEvery(int rows) {
        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be positive but was " + rows);
        }
        this.flushEvery = rows;
    }

    /**
     * Sets the ring-buffer capacity in rows.
     *
     * <p>Shrinking below the rows currently pending drops the oldest first, counted in {@link
     * #getDroppedRowCount()}.
     *
     * @param capacity the capacity; must be positive
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public void setBufferCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive but was " + capacity);
        }
        this.bufferCapacity = capacity;
        while (pending.size() > bufferCapacity) {
            pending.removeFirst();
            droppedRows++;
        }
    }

    /**
     * Returns how many rows were dropped from a full buffer.
     *
     * @return the dropped-row count
     */
    public long getDroppedRowCount() {
        return droppedRows;
    }

    /**
     * Writes pending rows to storage.
     *
     * @throws IllegalStateException if the logger is closed
     * @throws CurioIoException if the rows cannot be written
     */
    public void flush() {
        ensureOpen();
        drain();
        try {
            writer.flush();
        } catch (IOException e) {
            throw new CurioIoException("could not flush log", e);
        }
    }

    /**
     * Drains pending rows and closes the sink.
     *
     * @throws CurioIoException if pending rows cannot be written
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        drain();
        try {
            writer.close();
        } catch (IOException e) {
            throw new CurioIoException("could not close log", e);
        }
    }

    private int indexOf(String field) {
        final Integer index = indexes.get(field);
        if (index == null) {
            throw new IllegalArgumentException("field not registered: " + field);
        }
        return index;
    }

    private void growSlots() {
        if (current == null) {
            current = new String[fields.size()];
            return;
        }
        final String[] grown = new String[fields.size()];
        System.arraycopy(current, 0, grown, 0, current.length);
        current = grown;
    }

    private void clearSlots() {
        for (int slot = 0; slot < current.length; slot++) {
            current[slot] = null;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("logger is closed");
        }
    }

    private void writeHeader() {
        row.setLength(0);
        row.append("time");
        for (String field : fields) {
            row.append(',').append(quote(field));
        }
        row.append('\n');
        try {
            writer.write(row.toString());
        } catch (IOException e) {
            throw new CurioIoException("could not write log header", e);
        }
        headerWritten = true;
    }

    private void formatRow() {
        row.setLength(0);
        row.append(clock.nowNanos() / NANOS_PER_SECOND);
        for (int slot = 0; slot < fields.size(); slot++) {
            row.append(',');
            if (current[slot] != null) {
                row.append(current[slot]);
            }
        }
        row.append('\n');
    }

    private void drain() {
        try {
            while (!pending.isEmpty()) {
                writer.write(pending.removeFirst());
            }
            writer.flush();
        } catch (IOException e) {
            throw new CurioIoException("could not write log rows", e);
        }
    }

    private static String quote(String value) {
        if (value.indexOf(',') < 0
                && value.indexOf('"') < 0
                && value.indexOf('\n') < 0
                && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
