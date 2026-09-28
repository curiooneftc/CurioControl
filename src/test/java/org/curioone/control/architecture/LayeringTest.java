package org.curioone.control.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit rules that enforce the package layering from {@code docs/PROJECT_STRUCTURE.md}.
 *
 * <p>The allowed direction is:
 *
 * <pre>{@code
 * core, math, util  ->  (nothing internal)
 * control           ->  math, util
 * hardware          ->  core, math, util
 * drive             ->  hardware, control, math, util
 * command           ->  core, util
 * telemetry         ->  core, util
 * vision            ->  hardware, math, util
 * }</pre>
 *
 * <p>Anything not listed as a dependency is forbidden, which also forbids cycles.
 */
@DisplayName("Architecture: package layering")
class LayeringTest {

    private static final String LAYERING = "docs/PROJECT_STRUCTURE.md section 3.1";

    private static final String[] ALL_PACKAGES = {
        "org.curioone.control.core..",
        "org.curioone.control.math..",
        "org.curioone.control.util..",
        "org.curioone.control.control..",
        "org.curioone.control.hardware..",
        "org.curioone.control.drive..",
        "org.curioone.control.command..",
        "org.curioone.control.telemetry..",
        "org.curioone.control.vision..",
    };

    private static final String[] ABSTRACTIONS_ONLY = {
        "org.curioone.control.core..",
        "org.curioone.control.control..",
        "org.curioone.control.drive..",
        "org.curioone.control.command..",
        "org.curioone.control.telemetry..",
        "org.curioone.control.vision..",
    };

    private static final String[] EVERYTHING_BUT_CORE_AND_UTIL = {
        "org.curioone.control.math..",
        "org.curioone.control.control..",
        "org.curioone.control.hardware..",
        "org.curioone.control.drive..",
        "org.curioone.control.command..",
        "org.curioone.control.telemetry..",
        "org.curioone.control.vision..",
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("org.curioone.control");
    }

    /**
     * Builds a "no class in {@code source} may depend on {@code forbidden}" rule.
     *
     * <p>Empty results are allowed. Several packages are still empty in Phase 0, and a rule that
     * has nothing to check yet is not a failure — a rule that stops checking once the package is
     * populated is, and {@code allowEmptyShould} keeps that distinction honest.
     */
    private static ArchRule noDependencyOn(String source, String[] forbidden, String reason) {
        return noClasses()
                .that()
                .resideInAPackage(source)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(forbidden)
                .because(reason)
                .allowEmptyShould(true);
    }

    @Test
    @DisplayName("core, math and util depend on no other framework package")
    void foundationPackagesAreIndependent() {
        final String reason = "core, math and util are the foundation layer (" + LAYERING + ")";

        for (String source :
                new String[] {
                    "org.curioone.control.core..",
                    "org.curioone.control.math..",
                    "org.curioone.control.util.."
                }) {
            noDependencyOn(source, ALL_PACKAGES, reason).check(classes);
        }
    }

    @Test
    @DisplayName("control depends only on math and util")
    void controlDependsOnlyOnMathAndUtil() {
        noDependencyOn(
                        "org.curioone.control.control..",
                        ABSTRACTIONS_ONLY,
                        "control is hardware-independent and sits above math/util only (ADR-003)")
                .check(classes);
    }

    @Test
    @DisplayName("hardware depends only on core, math and util")
    void hardwareDependsOnlyOnCoreMathAndUtil() {
        noDependencyOn(
                        "org.curioone.control.hardware..",
                        EVERYTHING_BUT_CORE_AND_UTIL,
                        "hardware wraps the SDK and must not reach upward (" + LAYERING + ")")
                .check(classes);
    }

    @Test
    @DisplayName("drive depends only on hardware, control, math and util")
    void driveDependsOnlyOnLowerLayers() {
        noDependencyOn(
                        "org.curioone.control.drive..",
                        new String[] {
                            "org.curioone.control.core..",
                            "org.curioone.control.command..",
                            "org.curioone.control.telemetry..",
                            "org.curioone.control.vision.."
                        },
                        "drive sits above hardware and control only (" + LAYERING + ")")
                .check(classes);
    }

    @Test
    @DisplayName("command depends only on core and util")
    void commandDependsOnlyOnCoreAndUtil() {
        noDependencyOn(
                        "org.curioone.control.command..",
                        EVERYTHING_BUT_CORE_AND_UTIL,
                        "commands reference subsystems by interface, never by hardware ("
                                + LAYERING
                                + ")")
                .check(classes);
    }

    @Test
    @DisplayName("telemetry depends only on core and util")
    void telemetryDependsOnlyOnCoreAndUtil() {
        noDependencyOn(
                        "org.curioone.control.telemetry..",
                        EVERYTHING_BUT_CORE_AND_UTIL,
                        "telemetry reports values; it does not reach into mechanisms ("
                                + LAYERING
                                + ")")
                .check(classes);
    }

    @Test
    @DisplayName("vision depends only on hardware, math and util")
    void visionDependsOnlyOnLowerLayers() {
        noDependencyOn(
                        "org.curioone.control.vision..",
                        new String[] {
                            "org.curioone.control.core..",
                            "org.curioone.control.control..",
                            "org.curioone.control.drive..",
                            "org.curioone.control.command..",
                            "org.curioone.control.telemetry.."
                        },
                        "vision maps detections onto math types; it never drives ("
                                + LAYERING
                                + ")")
                .check(classes);
    }

    @Test
    @DisplayName("nothing outside vision imports vision")
    void visionIsNotPulledIntoOtherLayers() {
        final String reason =
                "vision must stay opt-in, so a robot that never uses it pays nothing (ADR-011)";

        noClasses()
                .that()
                .resideOutsideOfPackage("org.curioone.control.vision..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.curioone.control.vision..")
                .because(reason)
                .allowEmptyShould(true)
                .check(classes);
    }
}
