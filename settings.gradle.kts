pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions the Java 17 toolchain on machines that do not have it installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.2.1"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        // The FTC SDK is published to Maven Central (ADR-001). google() is required for the
        // androidx artifacts the SDK declares, and for the Robot Controller project template.
        google()
    }
}

// --- Develocity / Gradle Enterprise (Phase 0 task 0.7) -------------------------
// Opt-in by design. With no Develocity account and no secret, the build works exactly as it
// does today: nothing is transmitted, and a fork pull request or a contributor's first
// `git clone` can never fail on a missing key (spec.md §44).
//
// A Build Scan is published only when the build FAILS — that is when a scan is worth reading —
// and only the parts a scan needs. It is the cheapest useful diagnostic CurioControl has: it
// shows exactly which task got slow, what the cache did, and what the test timings were.
//
// Nothing here reads DEVELOCITY_ACCESS_KEY. The plugin already reads that environment variable
// and ~/.gradle/develocity/keys.properties itself, in the documented `<server host>=<key>` form.
// Parsing it here previously passed a raw key straight to accessKey.set(), which breaks if the
// value follows the documented host-prefixed format. Leave it to the plugin.
val develocityServer: String =
    providers.gradleProperty("develocity.serverUrl").getOrElse("https://develocity.geeksmart.com")

develocity {
    server.set(develocityServer)

    buildScan {
        // Terms of service for the public free server. CI cannot answer the interactive
        // prompt, so the acceptance has to be declared here — which means it is committed to
        // this repository. That is a deliberate, team-level acceptance on behalf of every
        // contributor, not a per-developer one. Remove both lines to stop publishing entirely.
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")

        // A scan is most valuable when something went wrong, so publish exactly then. A green
        // build's scan is a trend line nobody reads.
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }

        // CI agents are torn down the moment the build ends, so a background upload would be
        // killed mid-flight.
        uploadInBackground.set(false)

        tag("CurioControl")
    }
}

buildCache {
    // The built-in local cache needs no account and helps every contributor immediately.
    local {
        isEnabled = true
    }
    // The remote cache is deliberately NOT configured. The public free server does not license
    // it, and "anonymous access to the built-in build cache node" is disabled by default, so
    // enabling it here would be a no-op at best. With a paid Develocity installation, add:
    //
    //   remote(develocity.buildCache) {
    //       isEnabled = true
    //       isPush = true
    //   }
}

rootProject.name = "curiocontrol"
