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
 * ArchUnit rules that keep the pure-Java boundary of {@code math}, {@code control}, and {@code
 * util} honest.
 *
 * <p>These guards are the executable form of ADR-003. A violation fails the build now rather than
 * surfacing as an untestable, hardware-coupled component months later.
 */
@DisplayName("Architecture: pure-Java boundary")
class PureJavaPackagesTest {

    private static final String[] SDK_PACKAGES = {
        "com.qualcomm..", "android..", "androidx..", "org.firstinspires.."
    };

    private static final String[] PURE_PACKAGES = {
        "org.curioone.control.math..",
        "org.curioone.control.control..",
        "org.curioone.control.util.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("org.curioone.control");
    }

    private static ArchRule noSdkDependency(String source, String reason) {
        return noClasses()
                .that()
                .resideInAPackage(source)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(SDK_PACKAGES)
                .because(reason)
                .allowEmptyShould(true);
    }

    @Test
    @DisplayName("math contains no FTC or Android types")
    void mathPackageIsPure() {
        noSdkDependency(
                        "org.curioone.control.math..",
                        "math is pure Java so it is testable on a JVM (ADR-003)")
                .check(classes);
    }

    @Test
    @DisplayName("control contains no FTC or Android types")
    void controlPackageIsPure() {
        noSdkDependency(
                        "org.curioone.control.control..",
                        "controllers take numbers, not motor objects (ADR-003)")
                .check(classes);
    }

    @Test
    @DisplayName("util contains no FTC or Android types")
    void utilPackageIsPure() {
        noSdkDependency(
                        "org.curioone.control.util..",
                        "util is shared by every pure-logic module (ADR-003)")
                .check(classes);
    }

    @Test
    @DisplayName("no pure package leaks SDK or Android types to its consumers")
    void purePackagesDoNotExposeSdkTypes() {
        final String reason = "a pure package must never force an FTC SDK dependency on consumers";

        noClasses()
                .that()
                .resideInAnyPackage(PURE_PACKAGES)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(SDK_PACKAGES)
                .because(reason)
                .allowEmptyShould(true)
                .check(classes);
    }
}
