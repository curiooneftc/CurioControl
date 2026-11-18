# Troubleshooting

## CurioControl does not resolve

Check these in order:

1. The repository is declared in `TeamCode/build.gradle`, in the same block as the dependency:

   ```groovy
   repositories {
       maven { url 'https://jitpack.io' }
   }
   ```

   The stock FTC template allows module-level repositories. If a custom project uses
   `FAIL_ON_PROJECT_REPOS`, put the same line in `settings.gradle.kts` instead.

2. The dependency uses the JitPack coordinates and an exact release tag:

   ```groovy
   implementation 'com.github.curiooneftc:CurioControl:v0.5.0'
   ```

   Do not use `org.curioone:curiocontrol` from a team project; that is the local/GitHub Packages
   coordinate. Do not use `main-SNAPSHOT`, `latest`, or `+`.

3. The tag exists in [JitPack](https://jitpack.io/com/github/curiooneftc/CurioControl). The first
   build of a new tag can take a few minutes. Open the build log for the tag if resolution keeps
   failing.

## I am being asked for a GitHub token

That means the old GitHub Packages repository URL is still present. Remove the
`https://maven.pkg.github.com/...` repository block and its `credentials` block from the robot
project. JitPack needs no username, password, personal access token, or `authToken`.

## Building CurioControl from source fails

The library build runs on JDK 17 and emits Java 8 bytecode. A stock robot project must stay on Java
8; do not raise its language level to make CurioControl compile. If Gradle itself will not start,
point `JAVA_HOME` at JDK 17.

## The robot app dexes CurioControl but fails at runtime

Confirm the Robot Controller supplies the FTC SDK. CurioControl declares the SDK as `compileOnly`,
so it never bundles it. Also confirm the SDK version is 11.1 or newer and that no ProGuard/R8 rule
strips classes reflectively used by `AprilTagManager`.

## Coverage or Javadoc gates fail

Run the full build locally before opening a pull request:

```bash
./gradlew build
```

Javadoc uses `-Xdoclint:all -Werror`, so missing `@param`, `@return`, or `@throws` tags fail the
build. Pure-logic packages (`math`, `control`, `util`) also have JaCoCo coverage gates.

## Documentation links fail

Docs are built with `mkdocs build --strict`. A broken internal link or a page missing from `nav`
fails documentation CI rather than shipping silently.
