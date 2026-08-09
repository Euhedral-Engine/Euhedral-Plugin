import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform")
}

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
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.0.1")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.tngtech.archunit:archunit-junit4:1.4.1")
}

// IDEA 2026.2.1 is verified from a local final installation until JetBrains
// publishes it to the release resolver.
val idea202621Path = providers.gradleProperty("idea202621Path")

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
            idea202621Path.orNull?.let { local(file(it)) }
        }
    }
}

tasks.runIde {
    autoReload = true
    jvmArgs("-Xmx2g", "-Dfile.encoding=UTF-8")
}

val verifyJava25 = tasks.register("verifyJava25") {
    doLast {
        val javaToolchainVersion = java.toolchain.languageVersion.get().asInt()
        check(javaToolchainVersion == 25) {
            "Java toolchain language version must be 25, found: $javaToolchainVersion"
        }
        tasks.withType<JavaCompile>().forEach { task ->
            val release = task.options.release.orNull
            check(release == 25) {
                "JavaCompile task ${task.name} release must be 25, found: $release"
            }
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach { task ->
            val target = task.compilerOptions.jvmTarget.orNull
            check(target == org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) {
                "KotlinCompile task ${task.name} target must be JVM_25, found: $target"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyJava25)
}
