# Phase 0 Platform Skeleton Implementation Plan

Status: Ready for implementation

Plan date: 2026-08-09

Source blueprint: `docs/design/phases/phase-0-platform-skeleton.md`

## 1. Outcome and scope

Phase 0 will create one Kotlin/JVM Gradle module that builds and loads an inert
IntelliJ IDEA 2026.2 plugin. It will establish the build, lifecycle, descriptor,
package, and test boundaries needed by later phases. The only runtime behavior
will be:

- A lazily created project service that owns project-lifetime coroutine jobs.
- An empty tool-window content shell.
- An empty application settings shell under `Tools`.

No agent types, ports, project intelligence, persistence, editing, policy
behavior, Gradle execution, Gemini transport, OAuth, or completion behavior will
be implemented.

The repository is currently documentation-only. There are no source files,
Gradle files, wrapper files, CI workflows, repository-local agent instructions,
or existing conventions to migrate. The implementation must preserve the
existing design documents and add only the files listed in this plan.

## 2. Resolved version matrix

Use these exact versions; do not replace them with dynamic selectors.

| Concern | Selection | Reason |
| --- | --- | --- |
| Gradle wrapper | 9.5.0, `-bin` distribution | IntelliJ Platform Gradle Plugin 2.x requires Gradle 9.0.0 or newer. Kotlin 2.4.0 is fully supported through Gradle 9.5.0. Gradle 9.6.1 is newer but is outside Kotlin 2.4.0's documented fully supported range. |
| Gradle JVM and Java toolchain | JDK 25 | IntelliJ Platform 2026.2 requires Java 25. Both Java and Kotlin bytecode targets are fixed to 25. |
| Kotlin JVM Gradle plugin | 2.4.0 | IDEA 2026.2 bundles Kotlin stdlib 2.4.0. Matching it minimizes runtime skew. |
| Kotlin stdlib | Do not package it | IDEA supplies it. Set `kotlin.stdlib.default.dependency=false`. |
| Coroutines | IDEA-bundled `kotlinx-coroutines` 1.10.2-intellij-1 | Do not declare or package `kotlinx-coroutines`; it is supplied by the platform. |
| IntelliJ Platform Gradle Plugin | 2.18.1 | Current stable 2.x version in the official SDK documentation checked on the plan date. Use the same version for the settings and project plugins. |
| IntelliJ Platform dependency | `intellijIdea("2026.2.0.1")` | Latest stable IDEA 2026.2 update in the JetBrains product feed on the plan date, full build `IU-262.8665.337`. Use the installer dependency and its bundled JBR. |
| Compatible build range | `sinceBuild = "262"`, `untilBuild = "262.*"` | The blueprint targets only the 2026.2 branch. |
| Unit/platform tests | JUnit 4.13.2 plus `TestFrameworkType.Platform` | JetBrains platform fixtures remain compatible with JUnit 4 and `BasePlatformTestCase`; all test framework dependencies are explicit. |
| Architecture tests | ArchUnit JUnit 4 1.4.1 | Current Maven Central release on the plan date and compatible with the selected JUnit runner. |
| Plugin Verifier | Version managed by IntelliJ Platform Gradle Plugin 2.18.1 | Do not pin the verifier CLI independently. Verify the built ZIP against the current target IDE. |

The implementation machine and CI must make JDK 25 available. The build will
not silently compile with an older JDK. Before other evidence, record
`./gradlew --version` and require both Launcher JVM and Daemon JVM to report 25.

## 3. Gradle layout and exact configuration

### 3.1 Files

Create:

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
.gitignore
```

`gradle-wrapper.properties` must use:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.0-bin.zip
```

Generate the wrapper from a trusted installed Gradle, then inspect the generated
files. Do not hand-author or download an unverified wrapper JAR.

Extend the existing `.gitignore` without replacing its rules. Ignore `.gradle/`,
`.kotlin/`, `.intellijPlatform/`, and `build/`, and add
`!gradle/wrapper/gradle-wrapper.jar` after the repository's current `*.jar` rule
so the wrapper JAR is intentionally tracked. This also satisfies the IntelliJ
Platform project's cache-directory configuration check.

### 3.2 Settings

`settings.gradle.kts` will:

- Name the root project `euhedral-gemini-intellij-plugin`.
- Apply `org.jetbrains.intellij.platform.settings` version `2.18.1`.
- Use `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Declare only `mavenCentral()` and
  `intellijPlatform { defaultRepositories() }` in dependency resolution
  management.
- Keep `gradlePluginPortal()` in `pluginManagement.repositories` for plugin
  resolution.

This keeps repositories centralized and uses the JetBrains installer repository
set, which also supplies the matching bundled JBR for tests and `runIde`.

### 3.3 Project build

`build.gradle.kts` will apply:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}
```

Set:

```kotlin
group = "com.euhedral.gemini"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}
```

Declare exactly these initial dependencies:

```kotlin
dependencies {
    intellijPlatform {
        intellijIdea("2026.2.0.1")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.tngtech.archunit:archunit-junit4:1.4.1")
}
```

There is no direct stdlib or coroutines dependency, no Java plugin dependency,
and no Gradle plugin dependency in Phase 0. The Java and Gradle plugins must be
added in the later phase that first imports their APIs, in both Gradle and the
plugin descriptor as required. Phase 0 uses only the platform module API.

Set the following in `gradle.properties`:

```properties
kotlin.stdlib.default.dependency=false
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
```

Do not add `kotlin.incremental.useClasspathSnapshot=false`; current JetBrains
guidance says to remove that historical workaround.

### 3.4 Plugin metadata, build range, verifier, and IDE runner

Configure `intellijPlatform` with:

```kotlin
intellijPlatform {
    autoReload = true
    buildSearchableOptions = true

    pluginConfiguration {
        id = "com.euhedral.gemini"
        name = "Euhedral Gemini Agent"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "262"
            untilBuild = "262.*"
        }
        vendor {
            name = "Euhedral"
        }
    }

    pluginVerification {
        failureLevel = VerifyPluginTask.FailureLevel.ALL
        ides {
            current()
        }
    }
}
```

Use the plugin-managed Plugin Verifier CLI. `current()` must resolve the same
`intellijIdea("2026.2.0.1")` dependency used to compile and test. `ALL` makes
compatibility warnings, deprecated, scheduled-for-removal, experimental,
internal, override-only, non-extendable, invalid structure, missing dependency,
and non-dynamic findings fail the task. Do not add an ignored-problems file in
Phase 0.

Keep the standard `runIde` task on the selected IDEA installer and its bundled
JBR. Configure it explicitly as follows:

```kotlin
tasks.runIde {
    autoReload = true
    jvmArgs("-Xmx2g", "-Dfile.encoding=UTF-8")
}
```

Do not set a developer-specific `ideDir` or JBR path. Do not enable split mode;
Phase 0 is a classic plugin and remote-development packaging is out of scope.
Keep searchable options enabled because the plugin registers a settings page.
The sandbox remains under the Gradle plugin's project-local default and must not
be committed.

Add a `verifyJava25` task and make `check` depend on it. It must inspect all
`JavaCompile` tasks for `options.release == 25`, all `KotlinJvmCompile` tasks for
`JvmTarget.JVM_25`, and the Java toolchain language version. A deliberately
misconfigured fixture or Gradle TestKit test is unnecessary; the task failing on
any mismatch is the drift guard required by the blueprint.

## 4. Plugin descriptor

Create `src/main/resources/META-INF/plugin.xml` with this semantic content:

```xml
<idea-plugin>
  <id>com.euhedral.gemini</id>
  <name>Euhedral Gemini Agent</name>
  <vendor>Euhedral</vendor>
  <description>Inert platform shell for the Euhedral Gemini Agent.</description>

  <depends>com.intellij.modules.platform</depends>
  <depends>com.intellij.modules.idea</depends>

  <extensions defaultExtensionNs="com.intellij">
    <projectService
        serviceImplementation="com.euhedral.gemini.bootstrap.GeminiProjectService"/>
    <toolWindow
        id="Euhedral Gemini"
        anchor="right"
        factoryClass="com.euhedral.gemini.ui.AgentToolWindowFactory"
        icon="AllIcons.Toolwindows.ToolWindowPalette"/>
    <applicationConfigurable
        id="com.euhedral.gemini.settings"
        parentId="tools"
        displayName="Euhedral Gemini Agent"
        instance="com.euhedral.gemini.settings.GeminiSettingsConfigurable"/>
  </extensions>
</idea-plugin>
```

The Gradle plugin patches version and `idea-version` into the processed
descriptor. `com.intellij.modules.platform` declares the actual API dependency.
`com.intellij.modules.idea` deliberately restricts this build to IntelliJ IDEA,
matching the locked target instead of advertising compatibility with every
platform IDE. No Java, language, VCS, Gradle, Kotlin plugin, or third-party plugin
dependency is justified by the Phase 0 code.

Use a non-light XML service because the approved deliverable explicitly requires
descriptor registration and a registration-resolution smoke test. Omit a
redundant `serviceInterface`; the implementation class is the service key.

All three registrations are dynamic-compatible. Do not add listeners,
components, startup activities, actions, extension points, or `overrides=true`.

## 5. Runtime shells and lifecycle contract

### 5.1 Project coroutine owner

Create `src/main/kotlin/com/euhedral/gemini/bootstrap/GeminiProjectService.kt`.

Final shape:

```text
internal class GeminiProjectService(
    project: Project,
    private val coroutineScope: CoroutineScope,
) : Disposable
```

The constructor form `Project, CoroutineScope` is an officially supported
project-service signature. Retain `project` only if needed for an assertion or
coroutine name; otherwise accept it without starting work. Constructor work must
remain trivial. The injected service scope already has `Dispatchers.Default` and
a service `CoroutineName`; do not create a new `SupervisorJob`, use
`GlobalScope`, access deprecated project/application scopes, or cancel the
injected scope directly.

Provide one `internal` launch boundary that returns the child `Job` and launches
the supplied suspending block in the injected scope. It exists to prove and later
preserve ownership, but Phase 0 must never call it from production startup or UI
code. Implement `Disposable.dispose()` only to set an internal atomic disposed
flag used by the lifecycle test. Do not perform manual scope cancellation in
`dispose()`; the platform owns that cancellation.

The lifecycle test will retain the returned `Job` and service reference, close
the fixture project, and independently prove that the child job is cancelled and
the service is disposed. This distinguishes coroutine cancellation from the
`Disposable` callback.

### 5.2 Tool-window shell

Create `src/main/kotlin/com/euhedral/gemini/ui/AgentToolWindowFactory.kt`.

`AgentToolWindowFactory` implements the public `ToolWindowFactory` API. In
`createToolWindowContent(Project, ToolWindow)`, create one non-closeable content
entry containing an otherwise empty `JBPanel` with empty text such as `No agent
session is active.` Use public Swing/IntelliJ UI and content APIs only. It must
not retrieve the project service, start a coroutine, install listeners, or own
resources requiring disposal.

Do not use Kotlin UI DSL for the tool window; JetBrains documents that DSL for
forms and settings, not general tool-window content.

### 5.3 Settings shell

Create
`src/main/kotlin/com/euhedral/gemini/settings/GeminiSettingsConfigurable.kt`.

Implement public `Configurable` with a no-argument constructor and return a
Kotlin UI DSL 2 `panel` containing only explanatory text such as `Configuration
will be added in a later phase.` Return `false` from `isModified`; `apply` and
`reset` are no-ops; release the panel reference from `disposeUIResources` if one
is retained. The constructor must not create Swing components. Do not add a
settings state service or persistence in Phase 0.

## 6. Package boundaries and API surface

Reserve this single-module package tree under `com.euhedral.gemini`:

```text
core
application
ports
adapters
policy
completion
ui
settings
bootstrap
```

Only `ui`, `settings`, and `bootstrap` contain runtime classes in Phase 0. Add
one minimal marker class to each otherwise empty package so compiled bytecode is
present for architecture tests. Marker classes contain no behavior and are
removed when real types arrive.

The dependency allowlist is:

| Package | May depend on Euhedral packages | May import IntelliJ APIs |
| --- | --- | --- |
| `core..` | `core..` only | No |
| `ports..` | `ports..`, `core..` | No |
| `application..` | `application..`, `policy..`, `ports..`, `core..` | No |
| `policy..` | `policy..`, `ports..`, `core..` | No |
| `adapters..` | `adapters..`, `ports..`, `core..` | Yes |
| `completion..` | `completion..`, `application..`, `ports..`, `core..` | Yes |
| `ui..` | `ui..`, `application..`, `core..` | Yes |
| `settings..` | `settings..` only in Phase 0 | Yes |
| `bootstrap..` | Any package for composition and lifecycle ownership | Yes |

Additional hard rules:

- No package except `bootstrap..` may depend on `bootstrap..`.
- `core..`, `ports..`, `application..`, and `policy..` may not depend on
  `adapters..`, `completion..`, `ui..`, `settings..`, or `bootstrap..`.
- `application..` may use port interfaces but never concrete adapters.
- UI and completion may call application use cases but must not call adapters
  directly.
- No default-package classes are allowed.
- No package cycles are allowed.

There is no supported API for other plugins in Phase 0. Do not declare custom
extension points, service interfaces, or exported API packages. All Kotlin
classes and members are `internal` unless platform reflection or an overridden
public method requires bytecode visibility. The XML-registered classes remain
Kotlin `class` declarations, never `object`; Kotlin `internal` compiles to
JVM-visible classes while preventing Kotlin source consumers from treating them
as supported API. Public IntelliJ interfaces are implementation dependencies,
not a promise that Euhedral implementation classes are public API.

Only documented, non-deprecated IntelliJ APIs may appear in `src/main`. Reject
`@ApiStatus.Internal`, `@IntellijInternalApi`, experimental,
scheduled-for-removal, override-only misuse, and non-extendable misuse through
Plugin Verifier and IDE inspections. Test framework APIs may appear only in
`src/test` and are not shipped.

## 7. Test plan

### 7.1 Architecture tests

Create
`src/test/kotlin/com/euhedral/gemini/architecture/PackageArchitectureTest.kt`.

Use ArchUnit's JUnit 4 runner and import production classes under
`com.euhedral.gemini`. Express every allowlist row and hard rule above as code,
not comments. Add rules that:

- Prohibit `com.intellij..`, `org.jetbrains..`, `javax.swing..`, and
  `java.awt..` dependencies from `core..`, `ports..`, `application..`, and
  `policy..`.
- Reject package cycles with `slices().matching("com.euhedral.gemini.(*)..")`.
- Reject production classes outside the reserved top-level packages.
- Reject dependencies on `bootstrap..` from all other packages.

Because empty directories are not compiled, marker classes are required for the
first test run. Add one negative JUnit test that feeds ArchUnit a test-fixture
class in a deliberately forbidden package relationship and asserts the rule
fails. This proves the guard itself, not merely that the current skeleton happens
to comply. Keep the fixture under `src/test` so it cannot ship.

### 7.2 Platform smoke tests

Create
`src/test/kotlin/com/euhedral/gemini/platform/PluginRegistrationTest.kt` using
`BasePlatformTestCase`. Assert:

- The loaded plugin descriptor for `com.euhedral.gemini` exists and is enabled.
- `project.getService(GeminiProjectService::class.java)` resolves one stable
  project-scoped instance.
- The `ToolWindowEP` registration with ID `Euhedral Gemini` resolves its factory
  class.
- The application configurable EP registration with ID
  `com.euhedral.gemini.settings` resolves its implementation class.
- Instantiating each shell through its platform contract produces a component
  without starting a service job or throwing an exception.

Prefer public EP accessors and fixture APIs. If direct EP inspection requires an
internal platform symbol, instead parse the effective descriptor as a test
resource and instantiate through public managers. Do not weaken the production
API rule to make a test convenient.

Create
`src/test/kotlin/com/euhedral/gemini/platform/ProjectScopeLifecycleTest.kt` using
JUnit 4 and a manually managed `IdeaProjectTestFixture`:

1. Set up the fixture and retrieve its project service.
2. Launch a child that signals it started, suspends with `awaitCancellation`, and
   signals its `finally` block.
3. Tear down the fixture, which closes and disposes its project.
4. With a bounded timeout, assert the child job is cancelled, its `finally`
   signal completed, and the service's disposed flag is true.
5. Always tear down in `finally` so a failed assertion cannot leak a project.

Do not use sleeps. Use `CompletableDeferred`, `Job.join`, and bounded coroutine
timeouts. The test proves project close. Plugin unload remains a manual `runIde`
smoke because unloading the test plugin's own classloader from inside its test
process is not reliable.

### 7.3 Descriptor and packaging checks

The Gradle `verifyPluginProjectConfiguration`, `verifyPluginStructure`,
`verifyPlugin`, and `buildPlugin` tasks cover project configuration, descriptor
shape, API compatibility, and packaging. Inspect the built ZIP to confirm:

- Kotlin stdlib and coroutines JARs are absent.
- Test classes, JUnit, and ArchUnit are absent.
- The patched descriptor contains plugin version `0.0.1`, `since-build="262"`,
  and `until-build="262.*"`.
- Only Phase 0 runtime classes and resources are present.

## 8. Implementation sequence and file inventory

Implement in this order:

1. Add the Gradle wrapper, settings, build, and properties.
2. Add `plugin.xml` and package marker classes.
3. Add `GeminiProjectService` and its lifecycle boundary.
4. Add the inert tool-window and settings shells.
5. Add architecture, registration, and lifecycle tests.
6. Run automated verification, then the manual `runIde` load/unload smoke.
7. Update this document's implementation record with actual evidence and any
   deviations before committing Phase 0 implementation.

Expected Phase 0 production paths:

```text
src/main/kotlin/com/euhedral/gemini/core/CorePackage.kt
src/main/kotlin/com/euhedral/gemini/application/ApplicationPackage.kt
src/main/kotlin/com/euhedral/gemini/ports/PortsPackage.kt
src/main/kotlin/com/euhedral/gemini/adapters/AdaptersPackage.kt
src/main/kotlin/com/euhedral/gemini/policy/PolicyPackage.kt
src/main/kotlin/com/euhedral/gemini/completion/CompletionPackage.kt
src/main/kotlin/com/euhedral/gemini/ui/AgentToolWindowFactory.kt
src/main/kotlin/com/euhedral/gemini/settings/GeminiSettingsConfigurable.kt
src/main/kotlin/com/euhedral/gemini/bootstrap/GeminiProjectService.kt
src/main/resources/META-INF/plugin.xml
```

Expected build and repository-support paths:

```text
.gitignore
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Expected test paths:

```text
src/test/kotlin/com/euhedral/gemini/architecture/PackageArchitectureTest.kt
src/test/kotlin/com/euhedral/gemini/architecture/fixtures/ForbiddenDependencyFixture.kt
src/test/kotlin/com/euhedral/gemini/platform/PluginRegistrationTest.kt
src/test/kotlin/com/euhedral/gemini/platform/ProjectScopeLifecycleTest.kt
```

No other production file is authorized by this phase without recording the
reason in the implementation record.

## 9. Commands and exit evidence

Run from the repository root with `JAVA_HOME` pointing to JDK 25:

```text
./gradlew --version
./gradlew clean verifyJava25 test
./gradlew verifyPluginProjectConfiguration verifyPluginStructure
./gradlew buildPlugin verifyPlugin
unzip -l build/distributions/euhedral-gemini-intellij-plugin-0.0.1.zip
./gradlew runIde
git diff --check
```

For `runIde`, use the development instance to perform this checklist before
closing it normally:

1. Confirm the plugin is enabled and no plugin-load error appears.
2. Open a disposable project.
3. Open `Euhedral Gemini` from the right tool-window stripe and confirm the inert
   empty state renders.
4. Open `Settings | Tools | Euhedral Gemini Agent` and confirm the inert panel
   renders and Apply remains disabled.
5. Close the project, reopen another disposable project, and confirm the plugin
   remains usable.
6. Disable or uninstall the plugin in the sandbox when supported, without an IDE
   restart prompt, then inspect `idea.log` for unload errors, leaked plugin class
   references, coroutine failures, and disposal exceptions.
7. Close the development instance so `runIde` exits successfully.

The Phase 0 implementation exit record added to this file must include:

- Exact Gradle, JVM, Kotlin, platform plugin, IDEA, and full IDE build versions.
- The commit hash being tested and a clean `git status` before publication.
- Pass/fail and duration for every command above.
- Test counts and names, including the negative architecture guard.
- Plugin Verifier target and zero findings at the configured failure level.
- Built ZIP path and an explicit statement that stdlib, coroutines, and test
  libraries are absent.
- `runIde` observations for load, UI registrations, project close, and dynamic
  unload, plus the inspected sandbox log path.
- Final production file list, final type names, deviations, and remaining risks.

Phase 0 is complete only when all automated commands pass, the manual smoke is
recorded, the project-scope child is cancelled on project close, the service is
disposed, the plugin dynamically unloads without leaked jobs, and only inert UI
shells are exposed.

## 10. Risks and controls

| Risk | Control and decision |
| --- | --- |
| IDEA 2026.2 uses Java 25 while a developer runs Gradle on an older JDK | Require JDK 25 in the wrapper evidence, set Java and Kotlin toolchains and targets to 25, and bind `verifyJava25` into `check`. |
| Newer Gradle is mistaken for safer | Pin 9.5.0 because it is the newest version in Kotlin 2.4.0's fully supported range; revisit only in a dedicated version update. |
| Kotlin stdlib or coroutines are accidentally bundled | Disable the default stdlib dependency, declare no coroutines dependency, inspect the runtime dependency graph and built ZIP, and fail verifier/package review on duplicates. |
| A floating IDEA dependency changes evidence | Pin `2026.2.0.1` and record full build `262.8665.337`; use `current()` for verifier parity. |
| XML service registration is flagged as convertible to a light service | Keep it intentionally because descriptor wiring is a locked deliverable; do not add a service interface or eager preload. |
| Project jobs leak on project close or plugin unload | Launch only through the injected service scope; test project-close cancellation and manually exercise dynamic unload. |
| A test passes without exercising its guard | Include a deliberately forbidden test fixture and assert ArchUnit rejects it. |
| Single-module boundaries erode as phases add dependencies | Encode the allowlist, IntelliJ-free inner packages, cycle rule, and bootstrap isolation in ArchUnit from the first compiled classes. |
| Internal or experimental IntelliJ APIs enter through convenience code | Use public APIs only, fail Plugin Verifier at `ALL`, run IDE inspections, and redesign tests rather than shipping an internal API. |
| `runIde` success is mistaken for an unload test | Record the manual disable/uninstall action and inspect the sandbox log; normal process exit alone is insufficient. |
| `com.intellij.modules.idea` narrows product compatibility | This is deliberate because the blueprint locks IntelliJ IDEA 2026.2 only. Reconsider product reach in a later approved design change, not incidentally. |

## 11. Official references checked

The following official documentation was checked on 2026-08-09:

- IntelliJ Platform Gradle Plugin 2.x:
  https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- IntelliJ Platform dependency helpers:
  https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
- IntelliJ Platform Gradle Plugin extension and Plugin Verifier DSL:
  https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
- IntelliJ Platform testing extension:
  https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-testing-extension.html
- IntelliJ 2026 API and Java requirements:
  https://plugins.jetbrains.com/docs/intellij/api-changes-list-2026.html
- Build number ranges:
  https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
- Kotlin and bundled coroutine versions:
  https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
- Coroutine service scopes:
  https://plugins.jetbrains.com/docs/intellij/coroutine-scopes.html
- Services:
  https://plugins.jetbrains.com/docs/intellij/plugin-services.html
- Tool windows:
  https://plugins.jetbrains.com/docs/intellij/tool-windows.html
- Settings guide and Kotlin UI DSL 2:
  https://plugins.jetbrains.com/docs/intellij/settings-guide.html
  https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
- Tests and fixtures:
  https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html
- Plugin compatibility and dependencies:
  https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html
- Verifying plugin compatibility:
  https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html
- IDE development instances:
  https://plugins.jetbrains.com/docs/intellij/ide-development-instance.html
- Kotlin Gradle compatibility:
  https://kotlinlang.org/docs/gradle-configure-project.html
- Gradle releases:
  https://gradle.org/releases/
- JetBrains product release feed used to resolve IDEA 2026.2.0.1:
  https://www.jetbrains.com/updates/updates.xml

## 12. Plan review checklist

- [x] Scope contains no Phase 1 domain or port implementation.
- [x] All version-sensitive build and platform selections are exact.
- [x] Java 25 is enforced for toolchain and emitted bytecode.
- [x] Platform-bundled stdlib and coroutines behavior is explicit.
- [x] `plugin.xml`, service lifecycle, tool window, and settings shells are exact.
- [x] Package allowlists and negative architecture evidence are defined.
- [x] Public versus internal API policy is defined.
- [x] Automated, manual, packaging, verifier, and exit evidence are defined.
- [x] All plan text is ASCII-only.
