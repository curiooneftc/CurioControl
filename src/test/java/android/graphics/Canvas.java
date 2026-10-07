package android.graphics;

/**
 * Test-only linkage stub for {@code android.graphics.Canvas}.
 *
 * <p>The FTC vision API takes this type in {@code VisionProcessorInternal.onDrawFrame}, which
 * Mockito must reflect over to mock {@code AprilTagProcessor}. The Android framework is absent on a
 * desktop JVM, so this empty class satisfies linkage and reflection. Nothing ever instantiates it,
 * and it never ships — test sources only.
 *
 * @since 0.4.0
 */
public final class Canvas {

    private Canvas() {
        throw new AssertionError("Canvas is a linkage stub and must not be instantiated.");
    }
}
