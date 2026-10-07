package android.content;

/**
 * Test-only linkage stub for {@code android.content.Context}.
 *
 * <p>The FTC camera API takes this type in {@code CameraName.asyncRequestCameraPermission}, which
 * Mockito must reflect over to mock {@code WebcamName}. The Android framework is absent on a
 * desktop JVM, so this empty class satisfies linkage and reflection. Nothing ever instantiates it,
 * and it never ships — test sources only.
 *
 * @since 0.4.0
 */
public final class Context {

    private Context() {
        throw new AssertionError("Context is a linkage stub and must not be instantiated.");
    }
}
