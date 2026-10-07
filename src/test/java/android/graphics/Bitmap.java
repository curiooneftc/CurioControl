package android.graphics;

/**
 * Test-only linkage stub for {@code android.graphics.Bitmap}.
 *
 * <p>The FTC vision API mentions this type in signatures Mockito must reflect over ({@code
 * CameraStreamSource.getFrameBitmap}), but the Android framework is absent on a desktop JVM. This
 * empty class satisfies linkage and reflection so SDK vision types stay mockable; nothing ever
 * instantiates it, and it never ships — test sources only.
 *
 * @since 0.4.0
 */
public final class Bitmap {

    private Bitmap() {
        throw new AssertionError("Bitmap is a linkage stub and must not be instantiated.");
    }
}
