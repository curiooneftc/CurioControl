package org.curioone.control.vision;

import org.curioone.control.util.Clock;
import org.curioone.control.util.SystemClock;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;

/**
 * Owns the vision lifecycle: open the stream, poll at a bounded rate, pause, release.
 *
 * <p>Vision runs on the SDK's own thread; this class is the OpMode thread's handle on it. {@link
 * #init()} resumes streaming, {@link #process()} is the per-loop heartbeat that returns whether the
 * update interval elapsed (poll detections only then — solving poses every 5 ms loop starves
 * nothing but spends CPU for identical frames), {@link #stop()} pauses, and {@link #close()}
 * releases the camera. A portal left open across OpModes holds the camera device; closing is not
 * optional hygiene, it is what lets the next OpMode open it.
 *
 * <pre>{@code
 * vision.init();
 * // ... per loop:
 * if (vision.process()) {
 *     vision.aprilTags().getRobotPose(layout, cameraOffset).ifPresent(pose -> ...);
 * }
 * // ... on stop:
 * vision.stop();
 * vision.close();
 * }</pre>
 *
 * <p>The SDK portal itself sits behind {@link VisionBackend}: the manager never touches portal
 * hardware except through the seam, which is what keeps the lifecycle unit-testable off-robot.
 *
 * <p><strong>Thread safety:</strong> call from the OpMode thread only. The backend does its work on
 * the vision thread; the managers snapshot across it.
 *
 * @since 0.4.0
 */
public final class VisionManager {

    /** Default ceiling: vision never polls faster than this. */
    public static final double DEFAULT_MAX_UPDATE_RATE_HZ = 30.0;

    /** Nanoseconds per second, for the interval conversion. */
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final VisionBackend backend;

    private final String name;

    private final Clock clock;

    private AprilTagManager tags;

    private long maxIntervalNanos;

    private long lastPollNanos = Long.MIN_VALUE;

    /**
     * Manages a vision backend.
     *
     * @param backend the stream to manage
     * @param name the name for diagnostics, usually the camera's configuration name
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public VisionManager(VisionBackend backend, String name) {
        this(backend, name, new SystemClock());
    }

    /**
     * Manages a vision backend with an injected time source.
     *
     * @param backend the stream to manage
     * @param name the name for diagnostics, usually the camera's configuration name
     * @param clock the time source for the update-rate gate
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public VisionManager(VisionBackend backend, String name, Clock clock) {
        if (backend == null) {
            throw new IllegalArgumentException("backend must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.backend = backend;
        this.name = name;
        this.clock = clock;
        setMaxUpdateRate(DEFAULT_MAX_UPDATE_RATE_HZ);
    }

    /**
     * Builds a manager for a camera: backend, processor, and attachment in one call.
     *
     * @param camera the camera to stream from
     * @return the manager, with an AprilTag processor attached
     * @throws IllegalArgumentException if {@code camera} is {@code null}
     */
    public static VisionManager forCamera(WebcamName camera) {
        return forCamera(camera, new SystemClock(), VisionPortalFactory.defaults());
    }

    /**
     * Builds a manager for a camera with injected seams.
     *
     * @param camera the camera to stream from
     * @param clock the time source for the update-rate gate
     * @param factory the hardware edge, faked in tests
     * @return the manager, with an AprilTag processor attached
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public static VisionManager forCamera(
            WebcamName camera, Clock clock, VisionPortalFactory factory) {
        if (camera == null) {
            throw new IllegalArgumentException("camera must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        final VisionBackend backend = factory.open(camera);
        if (backend == null) {
            throw new IllegalArgumentException("factory must not return a null backend");
        }
        final VisionManager manager = new VisionManager(backend, camera.getDeviceName(), clock);
        manager.attach(new AprilTagManager(backend.processor(), camera.getDeviceName()));
        return manager;
    }

    /**
     * Returns the underlying SDK portal.
     *
     * @return the SDK portal, never {@code null} in production
     */
    public VisionPortal getSdkObject() {
        return backend.portal();
    }

    /**
     * Returns the name this manager was created with.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Attaches an AprilTag processor manager.
     *
     * <p>Replaces any previous attachment: one camera, one tag pipeline.
     *
     * @param tags the manager to attach
     * @throws IllegalArgumentException if {@code tags} is {@code null}
     */
    public void attach(AprilTagManager tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags must not be null");
        }
        this.tags = tags;
    }

    /**
     * Reports whether an AprilTag manager is attached.
     *
     * @return {@code true} once {@link #attach} has run
     */
    public boolean hasAprilTags() {
        return tags != null;
    }

    /**
     * Returns the attached AprilTag manager.
     *
     * @return the manager
     * @throws VisionException if nothing is attached — attach one first, or build with {@link
     *     #forCamera} which attaches by default
     */
    public AprilTagManager aprilTags() {
        if (tags == null) {
            throw new VisionException(
                    "no AprilTag processor attached to vision '"
                            + name
                            + "': "
                            + "call attach(...) first, or build with forCamera(...)");
        }
        return tags;
    }

    /**
     * Starts streaming and enables the attached processor, if any.
     *
     * <p>Idempotent across calls: resuming an already-streaming portal is a no-op in the SDK, so
     * calling {@code init()} defensively is safe.
     */
    public void init() {
        backend.resume();
        if (tags != null) {
            backend.setTagsEnabled(true);
        }
    }

    /**
     * Heartbeat for the OpMode loop.
     *
     * <p>Returns whether the update interval elapsed since the last {@code true}: poll detections
     * only then. Vision frames arrive far slower than OpMode loops, and re-solving the same frame
     * every 5 ms spends CPU for identical answers. The first call always returns {@code true}.
     *
     * @return {@code true} when the caller should poll
     */
    public boolean process() {
        final long now = clock.nowNanos();
        if (lastPollNanos == Long.MIN_VALUE || now - lastPollNanos >= maxIntervalNanos) {
            lastPollNanos = now;
            return true;
        }
        return false;
    }

    /**
     * Pauses streaming and disables the attached processor, if any.
     *
     * <p>The backend survives: {@link #init()} resumes it. Pausing without closing is for
     * mid-OpMode breaks, not for OpMode end — see {@link #close()}.
     */
    public void stop() {
        if (tags != null) {
            backend.setTagsEnabled(false);
        }
        backend.pause();
    }

    /**
     * Releases the camera.
     *
     * <p>Call when the OpMode ends. A backend left open holds the camera device, and the next
     * OpMode that opens it fails in a way that looks like a hardware fault.
     */
    public void close() {
        backend.close();
    }

    /**
     * Returns the camera state.
     *
     * <p>Useful when waiting for the stream to come up after {@link #init()} rather than polling
     * blindly.
     *
     * @return the SDK camera state
     */
    public VisionPortal.CameraState cameraState() {
        return backend.cameraState();
    }

    /**
     * Reports whether the camera is streaming.
     *
     * <p>The one-line wait-for-ready check: poll this after {@link #init()} instead of polling
     * detections into the void while the camera is still opening.
     *
     * @return {@code true} when the camera state is streaming
     */
    public boolean isStreaming() {
        return cameraState() == VisionPortal.CameraState.STREAMING;
    }

    /**
     * Returns the measured frame rate.
     *
     * @return frames per second, as reported by the SDK
     */
    public float getFps() {
        return backend.fps();
    }

    /**
     * Sets the fastest rate {@link #process()} returns {@code true}.
     *
     * <p>Vision must not starve the control loop: the default ceiling is 30 Hz, and most routines
     * need far less — pose solves at 5–10 Hz are plenty for aim assist. Lower the ceiling until the
     * loop timing stops moving.
     *
     * @param hertz the maximum poll rate; must be positive and finite
     * @throws IllegalArgumentException if {@code hertz} is not finite or not positive
     */
    public void setMaxUpdateRate(double hertz) {
        if (!Double.isFinite(hertz) || hertz <= 0.0) {
            throw new IllegalArgumentException(
                    "hertz must be finite and positive but was " + hertz);
        }
        this.maxIntervalNanos = (long) (NANOS_PER_SECOND / hertz);
    }

    @Override
    public String toString() {
        return "VisionManager[" + name + "]";
    }
}
