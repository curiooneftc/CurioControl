package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CurioConfig}. */
@DisplayName("CurioConfig")
class CurioConfigTest {

    @Test
    @DisplayName("reports the real build version, not the missing-metadata fallback")
    void reportsBuildVersion() {
        final String version = CurioConfig.version();

        // This is the guard that keeps the generated resource path in
        // build.gradle.kts in step with CurioConfig.VERSION_RESOURCE. A rename of the root
        // package moves that path, and a mismatch shows up here as the "unknown" sentinel
        // rather than as a blank string.
        assertNotEquals(
                "unknown",
                version,
                "CurioConfig.version() fell back to 'unknown': the generated version.properties "
                        + "is not on the classpath. Check the path in build.gradle.kts matches "
                        + "CurioConfig.VERSION_RESOURCE.");
        assertTrue(!version.isBlank(), "CurioConfig.version() must never be blank");
    }

    @Test
    @DisplayName("is a static holder and cannot be instantiated")
    void cannotBeInstantiated() throws ReflectiveOperationException {
        final Constructor<CurioConfig> constructor = CurioConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // The constructor throws an AssertionError, which reflection wraps. Assert on the cause
        // so the failure message says what actually happened.
        try {
            constructor.newInstance();
            fail("CurioConfig must not be constructible");
        } catch (InvocationTargetException e) {
            assertInstanceOf(
                    AssertionError.class, e.getCause(), "the private constructor must reject use");
        }
    }

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("debug output is off")
        void debugIsOff() {
            assertFalse(CurioConfig.DEBUG, "debug mode is opt-in (SPEC 26)");
        }

        @Test
        @DisplayName("structured logging is off")
        void loggingIsOff() {
            assertFalse(CurioConfig.LOGGING_ENABLED, "logging is disabled by default (SPEC 27)");
        }

        @Test
        @DisplayName("telemetry flushes several times a second, not every loop")
        void telemetryCadence() {
            assertEquals(200, CurioConfig.TELEMETRY_PERIOD_MILLIS);
        }
    }
}
