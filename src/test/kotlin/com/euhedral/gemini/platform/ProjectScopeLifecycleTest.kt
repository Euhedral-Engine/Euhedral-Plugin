package com.euhedral.gemini.platform

import com.euhedral.gemini.bootstrap.GeminiProjectService
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ProjectScopeLifecycleTest {

    @Test
    fun projectCloseCancelsInjectedScopeAndDisposesService() = runBlocking {
        val fixtureBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("ProjectScopeLifecycleTest")
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixtureBuilder.fixture)

        runInEdtAndWait {
            fixture.setUp()
        }

        var isDisposed = false
        var isCancelled = false

        try {
            val project = fixture.project
            val service = project.getService(GeminiProjectService::class.java)

            val childStarted = CompletableDeferred<Unit>()
            val childFinished = CompletableDeferred<Unit>()

            val childJob = service.launchProjectJob {
                childStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    childFinished.complete(Unit)
                }
            }

            withTimeout(5.seconds) {
                childStarted.await()
            }

            runInEdtAndWait {
                fixture.tearDown()
            }

            withTimeout(5.seconds) {
                childFinished.await()
                childJob.join()
            }

            isCancelled = childJob.isCancelled
            isDisposed = service.isDisposed
        } catch (t: Throwable) {
            runInEdtAndWait {
                try {
                    fixture.tearDown()
                } catch (_: Throwable) {
                }
            }
            throw t
        }

        assertTrue("Child job should be cancelled on project dispose", isCancelled)
        assertTrue("Service should be marked disposed on project dispose", isDisposed)
    }
}
