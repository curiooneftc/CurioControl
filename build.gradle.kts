import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage

/** The legacy artifact-kind attribute that the FTC SDK's published metadata declares. */
val libraryElementsAttribute: Attribute<String> =
    Attribute.of("org.gradle.libraryelements", String::class.java)

plugins {
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jmh)
    `java-library`
    `maven-publish`
    `jacoco`
    checkstyle
}

group = "org.curioone"
version = "0.3.2"

description = "CurioControl — a modular control and development framework for FIRST Tech Challenge robots."

// --- Toolchain -----------------------------------------------------------------
java {
    toolchain {
        languageVersion =
            JavaLanguageVersion.of(
                libs.versions.java
                    .get()
                    .toInt(),
            )
    }
    withSourcesJar()
    withJavadocJar()
}

// --- FTC SDK ------------------------------------------------------------------
// The FTC SDK is published to Maven Central as Android AARs, and its module metadata declares
// the legacy `org.gradle.libraryelements` attribute rather than `org.gradle.artifact.type`.
// A `java-library` project has no Android Gradle Plugin and therefore cannot consume that
// variant at all.
//
// The fix: resolve the AARs into their own configuration and unzip the `classes.jar` out of
// each one, so the Java sources can compile against them. The Robot Controller app supplies the
// real SDK at runtime, so the published CurioControl artifact contains none of it.
// See docs/ARCHITECTURE_DECISIONS.md ADR-001.
val ftcSdkAars: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    isVisible = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(libraryElementsAttribute, "aar")
    }
}

dependencies {
    ftcSdkAars(libs.ftc.robotcore)
    ftcSdkAars(libs.ftc.hardware)
    ftcSdkAars(libs.ftc.vision)
    ftcSdkAars(libs.ftc.inspection)
    ftcSdkAars(libs.ftc.ftccommon)
    // EasyOpenCV for the org.opencv types in AprilTagDetection's constructor (test fakes
    // only — unpacked like the SDK above, never published).
    ftcSdkAars(libs.easyopencv)
    ftcSdkAars(libs.opencv)

    // androidx.annotation is a normal multiplatform library, not an AAR, so it resolves
    // through the ordinary compile classpath rather than the AAR-unpacking configuration.
    compileOnly(libs.androidx.annotation)
    testCompileOnly(libs.androidx.annotation)

    // gson appears in an annotation on one SDK class. It is compileOnly like the rest of the SDK:
    // the Robot Controller supplies it at runtime.
    compileOnly(libs.gson)
    testCompileOnly(libs.gson)
}

// The unpacked FTC SDK classes, used as a compile classpath entry.
//
// Two traps avoided here, both of which fail silently until a class actually imports an SDK type:
//
//  1. `extractFtcSdkClasses.map { it.outputDirectory }` yields a provider *of a property
//     object*, not of a directory, and resolves to an empty classpath entry.
//  2. Putting the output *directory* on the classpath does not work either: javac treats a
//     directory as a package root and never looks inside it for jars. The entry has to be the
//     jar files themselves.
//
// `builtBy` carries the task dependency, and matching the jars keeps the whole thing lazy.
val ftcSdkClassesDir = layout.buildDirectory.dir("ftc-sdk-classes")

val extractFtcSdkClasses by tasks.registering(UnpackAarClasses::class) {
    description = "Unpacks classes.jar out of the FTC SDK AARs so the library can compile against them."
    group = "build setup"
    strict.set(true)
    archives.from(ftcSdkAars)
    outputDirectory.set(ftcSdkClassesDir)
}

val ftcSdkClasses: FileCollection =
    objects
        .fileCollection()
        .from(files(ftcSdkClassesDir).asFileTree.matching { include("**/*.jar") })
        .builtBy(extractFtcSdkClasses)

dependencies {
    compileOnly(ftcSdkClasses)

    // The tests need the SDK at *runtime*, not just at compile time: a wrapper class that
    // implements DcMotorEx cannot even be loaded without it, let alone exercised. testRuntimeOnly
    // rather than testImplementation because neither is published, and the compile/runtime split
    // keeps the intent explicit: the SDK is on the test classpath, never in the artifact.
    testCompileOnly(ftcSdkClasses)
    testRuntimeOnly(ftcSdkClasses)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.archunit.junit5)

    testRuntimeOnly(libs.junit.platform.launcher)
}

// --- Benchmarks (JMH) ------------------------------------------------------------
// Micro-benchmarks for the hot paths spec.md §45 requires to be allocation-free. They live in
// `src/jmh` (created by the JMH plugin), run via `./gradlew jmh` on a schedule — never as part
// of `check` — and upload JSON results for review. See `benchmark/README.md`.
//
// Two classpath notes, both following the test wiring above:
//   * The SDK rides along exactly like it does for tests: compile-only to build against,
//     runtime-only so wrapper classes load.
//   * Benchmarks reuse the test fakes (FakeDcMotor, FakeClock) via the test output rather than
//     duplicating them: fixtures in one place, or they drift apart.
jmh {
    resultFormat.set("JSON")
}

// The JMH plugin's packaging tasks hold project references and cannot store the configuration
// cache. Benchmarks run on a schedule, never on the critical path, so opting those tasks out
// costs nothing and keeps every other task cached.
tasks.named("jmh") { notCompatibleWithConfigurationCache("JMH plugin tasks are not supported") }
tasks.named("jmhJar") { notCompatibleWithConfigurationCache("JMH plugin tasks are not supported") }

dependencies {
    jmhCompileOnly(ftcSdkClasses)
    jmhRuntimeOnly(ftcSdkClasses)
    jmhImplementation(files(sourceSets.named("test").map { it.output }))
}

// --- Build metadata ------------------------------------------------------------
// CurioConfig.version() reads this file, so the reported version can never drift from the
// published artifact (docs/RELEASE_STRATEGY.md §5).
//
// Two halves of one contract, declared here once each:
//   versionResourceRoot     the directory added to the resource classpath
//   versionResourcePackage  the path under it, mirroring the Java package
// so the file lands at /org/curioone/control/version.properties. CurioConfig.VERSION_RESOURCE
// is the other end of that contract, and CurioConfigTest fails loudly if the two disagree.
val versionResourceRoot = "generated/version"
val versionResourcePackage = "org/curioone/control"

val generateVersionProperties by tasks.registering(WriteProperties::class) {
    destinationFile = layout.buildDirectory.file("$versionResourceRoot/$versionResourcePackage/version.properties")
    property("version", project.version.toString())
}

sourceSets.main {
    resources.srcDir(generateVersionProperties.map { layout.buildDirectory.dir(versionResourceRoot) })
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateVersionProperties)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// --- Compilation ---------------------------------------------------------------
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
        // Treat Javadoc warnings as errors so a malformed doc comment blocks the build.
        addBooleanOption("Werror", true)
    }
}

// --- Tests ---------------------------------------------------------------------
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // Deterministic by default: no test may depend on the ambient locale or timezone.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    systemProperty("user.timezone", "UTC")
    systemProperty("java.awt.headless", "true")
}

// --- Coverage ------------------------------------------------------------------
jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required = true
        html.required = true
    }
}

// SpotBugs is configured with ignoreFailures so its report is always written, which means the
// task's own exit code cannot gate the build. This doLast is the gate: a report containing any
// finding fails the task, so a violation is never silently tolerated and the report is on disk to
// explain it.
// The gate covers pure logic only (math, control, util) — the packages ADR-003 makes SDK-free, and
// the only ones where coverage percentage is a meaningful measure of quality. `hardware`, `drive`,
// and `core` are thin delegations to the SDK: high line coverage there says the delegation happens,
// not that the robot works, and the bench tests are what actually validate them (TESTING_STRATEGY
// §3.4). Excluding them keeps the number honest rather than gamed by counting one-line delegates.
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("org.curioone.control.math.*", "org.curioone.control.control.*", "org.curioone.control.util.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// --- Static analysis -----------------------------------------------------------
spotbugs {
    toolVersion = "4.9.3"
    excludeFilter = file("config/spotbugs/exclude.xml")
    ignoreFailures = false
    effort = com.github.spotbugs.snom.Effort.DEFAULT
    reportLevel = com.github.spotbugs.snom.Confidence.DEFAULT
    showProgress = true
}

checkstyle {
    toolVersion = "10.21.4"
}

tasks.withType<Checkstyle>().configureEach {
    configFile = file("config/checkstyle/checkstyle.xml")
    configDirectory = file("config")
    maxWarnings = 0
    exclude("generated")
}

// --- Formatting ----------------------------------------------------------------
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.25.2").aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.kts")
        ktlint("1.5.0")
    }
    format("xml") {
        target("config/**/*.xml")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("yaml") {
        target(".github/**/*.yml", ".github/**/*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// --- Publishing ----------------------------------------------------------------
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "org.curioone"
            artifactId = "curiocontrol"

            pom {
                name = "CurioControl"
                description = project.description
                url = "https://github.com/curiooneftc/CurioControl"
                inceptionYear = "2026"
                licenses {
                    license {
                        name = "BSD 3-Clause License"
                        url = "https://opensource.org/licenses/BSD-3-Clause"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "curio-one"
                        name = "Curio One"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/curiooneftc/CurioControl.git"
                    developerConnection = "scm:git:ssh://git@github.com/curiooneftc/CurioControl.git"
                    url = "https://github.com/curiooneftc/CurioControl"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/curiooneftc/CurioControl/issues"
                }
            }
        }
    }

    repositories {
        // Named "local" so `publish` in CI can stay a dry run until the team explicitly
        // opts in. See docs/CI_CD_SETUP.md §2.2.
        maven {
            name = "GitHubPackages"
            url =
                uri(
                    providers
                        .gradleProperty("curio.packagesUrl")
                        .getOrElse("https://maven.pkg.github.com/curiooneftc/CurioControl"),
                )
            credentials {
                username = providers.gradleProperty("github.actor").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("github.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
