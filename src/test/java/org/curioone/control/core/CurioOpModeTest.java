package org.curioone.control.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the OpMode base classes, verified structurally. */
@DisplayName("OpMode base classes")
class CurioOpModeTest {

    @Nested
    @DisplayName("lifecycle wiring")
    class LifecycleWiring {

        @Test
        @DisplayName("init and loop are final, so a subclass cannot skip the robot's lifecycle")
        void lifecycleIsFinal() throws ReflectiveOperationException {
            // If these were overridable, a subclass could forget robot.loop() and the robot would
            // silently freeze while the OpMode looked healthy.
            assertTrue(Modifier.isFinal(CurioOpMode.class.getMethod("init").getModifiers()));
            assertTrue(Modifier.isFinal(CurioOpMode.class.getMethod("loop").getModifiers()));
        }

        @Test
        @DisplayName("requires exactly two hooks: build, then run")
        void twoAbstractHooks() {
            final long abstractCount =
                    java.util.Arrays.stream(CurioOpMode.class.getDeclaredMethods())
                            .filter(m -> Modifier.isAbstract(m.getModifiers()))
                            .count();

            assertEquals(2, abstractCount);
        }

        @Test
        @DisplayName("stop is overridable, so a subclass can add its own cleanup")
        void stopIsOverridable() throws ReflectiveOperationException {
            assertFalse(
                    Modifier.isFinal(CurioOpMode.class.getMethod("stop").getModifiers()),
                    "stop must be overridable, or a team cannot add cleanup beyond robot.stop()");
        }
    }

    @Nested
    @DisplayName("CurioAuto")
    class Auto {

        @Test
        @DisplayName("adds no behaviour of its own")
        void addsNothing() {
            // getDeclaredMethods() includes the compiler-generated synthetic constructor, so
            // exclude it rather than asserting a raw count.
            final long nonSynthetic =
                    java.util.Arrays.stream(CurioAuto.class.getDeclaredMethods())
                            .filter(m -> !m.isSynthetic())
                            .count();

            assertEquals(0, nonSynthetic, "CurioAuto must declare no methods of its own");
            assertEquals(0, CurioAuto.class.getDeclaredFields().length);
        }

        @Test
        @DisplayName("is a CurioOpMode, so the two are interchangeable")
        void isAnOpMode() {
            assertTrue(CurioOpMode.class.isAssignableFrom(CurioAuto.class));
        }

        @Test
        @DisplayName("inherits the final lifecycle methods rather than redeclaring them")
        void inheritsFinalLifecycle() throws ReflectiveOperationException {
            // getMethod returns a fresh copy per call, so compare the declaring class instead:
            // a redeclared init would still be final but would be CurioAuto's, not CurioOpMode's.
            assertEquals(
                    CurioOpMode.class,
                    CurioAuto.class.getMethod("init").getDeclaringClass(),
                    "CurioAuto must not redeclare init; the point is that it adds nothing");
            assertEquals(
                    CurioOpMode.class,
                    CurioAuto.class.getMethod("loop").getDeclaringClass(),
                    "CurioAuto must not redeclare loop");
        }
    }

    @Nested
    @DisplayName("hook visibility")
    class HookVisibility {

        @Test
        @DisplayName("the robot field is protected, so subclasses can use it")
        void robotIsProtected() throws ReflectiveOperationException {
            assertTrue(
                    Modifier.isProtected(
                            CurioOpMode.class.getDeclaredField("robot").getModifiers()));
        }

        @Test
        @DisplayName("the robot field is not private, which would make the class useless")
        void robotIsAccessible() throws ReflectiveOperationException {
            assertFalse(
                    Modifier.isPrivate(CurioOpMode.class.getDeclaredField("robot").getModifiers()));
        }
    }

    @Test
    @DisplayName("the base classes are abstract, so they cannot be run bare")
    void areAbstract() {
        assertTrue(Modifier.isAbstract(CurioOpMode.class.getModifiers()));
        assertTrue(Modifier.isAbstract(CurioAuto.class.getModifiers()));
    }

    @Test
    @DisplayName("a concrete subclass can be declared without implementing anything extra")
    void subclassable() {
        assertNotNull(new MinimalOpMode());
    }

    /** A minimal concrete OpMode, standing in for what a team would actually write. */
    private static final class MinimalOpMode extends CurioOpMode {

        private CurioRobot built;

        @Override
        public void initRobot() {
            built =
                    new CurioRobot(
                            new org.curioone.control.support.FakeHardwareSource(),
                            new org.curioone.control.support.FakeClock(),
                            new org.curioone.control.support.RecordingTelemetrySink());
        }

        @Override
        public void runRobot() {
            // Nothing to drive.
        }

        CurioRobot robot() {
            return built;
        }
    }

    @Test
    @DisplayName("the class carries a no-arg constructor, as the SDK requires")
    void hasNoArgConstructor() {
        assertEquals(
                1,
                CurioOpMode.class.getConstructors().length,
                "the Robot Controller instantiates OpModes reflectively with no arguments");
    }

    @Test
    @DisplayName("a subclass with a broken hook is a compile error, not a runtime one")
    void hooksAreRequiredAtCompileTime() {
        // Guards the abstract declarations themselves: an abstract hook that quietly became
        // concrete would let a team write an OpMode that never runs.
        assertThrows(
                NoSuchMethodException.class,
                () -> CurioOpMode.class.getDeclaredMethod("someHookNobodyDeclared"));
    }
}
