import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

/**
 * Unpacks the `classes.jar` out of every Android AAR in a collection into a single output
 * directory, renaming each one to match the AAR it came from.
 *
 * The FTC SDK is published to Maven Central as Android AARs whose Gradle module metadata
 * declares the legacy `org.gradle.libraryelements` attribute. A `java-library` project without
 * the Android Gradle Plugin cannot select that variant, so an artifact transform never gets a
 * chance to run. Resolving the AARs into a dedicated configuration and unpacking them here is
 * explicit, works identically on every machine, and keeps the Robot Controller as the source of
 * truth for the SDK at runtime.
 *
 * Nothing is lost: an AAR is a zip holding `classes.jar` next to resources, a manifest, and
 * native libraries. When compiling Java against the SDK the classes are the entire relevant
 * surface, and the Robot Controller app supplies everything else at runtime.
 *
 * See `docs/ARCHITECTURE_DECISIONS.md` ADR-001.
 */
@CacheableTask
abstract class UnpackAarClasses : DefaultTask() {

    /** The AARs (and any plain jars) to unpack. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archives: ConfigurableFileCollection

    /** Where the extracted jars land. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** Fail the build if an AAR has no `classes.jar` instead of silently producing nothing. */
    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun unpack() {
        val target = outputDirectory.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        val missing = mutableListOf<String>()

        for (archive in archives.files) {
            if (archive.extension != "aar") {
                // A dependency that is already a jar (androidx.annotation) is used as-is.
                archive.copyTo(File(target, archive.name), overwrite = true)
                continue
            }

            val outputName = archive.name.removeSuffix(".aar") + "-classes.jar"
            val outputFile = File(target, outputName)

            ZipFile(archive).use { zip ->
                val entry = zip.getEntry("classes.jar")
                if (entry == null) {
                    missing += archive.name
                    return@use
                }
                zip.getInputStream(entry).use { input ->
                    outputFile.outputStream().use { out -> input.copyTo(out) }
                }
            }

            logger.info("Unpacked {} -> {}", archive.name, outputName)
        }

        check(missing.isEmpty() || !strict.get()) {
            "These AARs contained no classes.jar entry: ${missing.joinToString()}. " +
                "CurioControl expects the FTC SDK to be published as standard Android AARs."
        }
    }
}
