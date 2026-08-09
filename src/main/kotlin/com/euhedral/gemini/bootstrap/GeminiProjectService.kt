package com.euhedral.gemini.bootstrap

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class GeminiProjectService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) : Disposable {
    private val disposedFlag = AtomicBoolean(false)

    val isDisposed: Boolean
        get() = disposedFlag.get()

    internal fun launchProjectJob(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch(block = block)
    }

    override fun dispose() {
        disposedFlag.set(true)
    }
}
