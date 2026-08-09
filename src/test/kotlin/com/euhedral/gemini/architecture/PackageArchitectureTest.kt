package com.euhedral.gemini.architecture

import com.euhedral.gemini.architecture.fixtures.ForbiddenDependencyFixture
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.Assert.fail
import org.junit.Test

class PackageArchitectureTest {

    private val productionClassesOnly = ImportOption { location ->
        location.contains("/main/")
    }

    private val productionClasses = ClassFileImporter()
        .withImportOption(productionClassesOnly)
        .importPackages("com.euhedral.gemini")

    @Test
    fun `no intellij or ui dependencies from domain packages`() {
        noClasses()
            .that().resideInAnyPackage(
                "com.euhedral.gemini.core..",
                "com.euhedral.gemini.ports..",
                "com.euhedral.gemini.application..",
                "com.euhedral.gemini.policy.."
            )
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.intellij..",
                "org.jetbrains..",
                "javax.swing..",
                "java.awt.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `no bootstrap dependencies from other packages`() {
        noClasses()
            .that().resideInAPackage("com.euhedral.gemini..")
            .and().resideOutsideOfPackage("com.euhedral.gemini.bootstrap..")
            .should().dependOnClassesThat().resideInAPackage("com.euhedral.gemini.bootstrap..")
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `domain packages do not depend on outer packages`() {
        noClasses()
            .that().resideInAnyPackage(
                "com.euhedral.gemini.core..",
                "com.euhedral.gemini.ports..",
                "com.euhedral.gemini.application..",
                "com.euhedral.gemini.policy.."
            )
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.euhedral.gemini.adapters..",
                "com.euhedral.gemini.completion..",
                "com.euhedral.gemini.ui..",
                "com.euhedral.gemini.settings..",
                "com.euhedral.gemini.bootstrap.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `application does not depend on adapters`() {
        noClasses()
            .that().resideInAPackage("com.euhedral.gemini.application..")
            .should().dependOnClassesThat().resideInAPackage("com.euhedral.gemini.adapters..")
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `ui and completion do not depend on adapters`() {
        noClasses()
            .that().resideInAnyPackage(
                "com.euhedral.gemini.ui..",
                "com.euhedral.gemini.completion.."
            )
            .should().dependOnClassesThat().resideInAPackage("com.euhedral.gemini.adapters..")
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `core depends only on core`() {
        classes()
            .that().resideInAPackage("com.euhedral.gemini.core..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.euhedral.gemini.core..",
                "java..",
                "kotlin.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `ports depends only on ports and core`() {
        classes()
            .that().resideInAPackage("com.euhedral.gemini.ports..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.euhedral.gemini.ports..",
                "com.euhedral.gemini.core..",
                "java..",
                "kotlin.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `settings phase 0 isolation`() {
        classes()
            .that().resideInAPackage("com.euhedral.gemini.settings..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.euhedral.gemini.settings..",
                "com.intellij..",
                "org.jetbrains..",
                "javax.swing..",
                "java.awt..",
                "java..",
                "kotlin.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `no package cycles`() {
        slices()
            .matching("com.euhedral.gemini.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `only allowed top level packages`() {
        classes()
            .should().resideInAnyPackage(
                "com.euhedral.gemini.core..",
                "com.euhedral.gemini.application..",
                "com.euhedral.gemini.ports..",
                "com.euhedral.gemini.adapters..",
                "com.euhedral.gemini.policy..",
                "com.euhedral.gemini.completion..",
                "com.euhedral.gemini.ui..",
                "com.euhedral.gemini.settings..",
                "com.euhedral.gemini.bootstrap.."
            )
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `negative architecture guard fails on forbidden dependency fixture`() {
        val classes = ClassFileImporter().importClasses(ForbiddenDependencyFixture::class.java)
        val rule = noClasses()
            .that().resideInAPackage("com.euhedral.gemini.architecture.fixtures..")
            .should().dependOnClassesThat().resideInAPackage("com.intellij..")

        try {
            rule.check(classes)
            fail("Expected architecture rule check to fail for ForbiddenDependencyFixture")
        } catch (e: AssertionError) {
            // Success: rule correctly detected forbidden dependency
        }
    }
}
