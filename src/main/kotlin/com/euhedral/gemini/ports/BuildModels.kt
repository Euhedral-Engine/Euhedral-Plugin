package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ProcessInvocationId
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.serialization.AdapterLocalValue
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.BoundedOutputMetadata

@SourceBearingValue
data class ProcessOutputChunk(
    val processInvocationId: ProcessInvocationId,
    val isStdErr: Boolean,
    val text: String,
)

@AdapterLocalValue
interface ProcessOutputSink {
    suspend fun emit(chunk: ProcessOutputChunk)
}

@SerializableValue
data class BuildProjectRequest(
    val processInvocationId: ProcessInvocationId,
    val transactionId: TransactionId,
    val timeoutMillis: Long = 900_000L,
) {
    init {
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }
    }
}

@SerializableValue
data class TestModuleRequest(
    val processInvocationId: ProcessInvocationId,
    val transactionId: TransactionId,
    val module: String,
    val timeoutMillis: Long = 900_000L,
) {
    init {
        require(module.isNotBlank()) { "module cannot be blank" }
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }
    }
}

@SerializableValue
data class TestClassRequest(
    val processInvocationId: ProcessInvocationId,
    val transactionId: TransactionId,
    val module: String,
    val className: String,
    val timeoutMillis: Long = 900_000L,
) {
    init {
        require(module.isNotBlank()) { "module cannot be blank" }
        require(className.isNotBlank()) { "className cannot be blank" }
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }
    }
}

@SerializableValue
data class TestMethodRequest(
    val processInvocationId: ProcessInvocationId,
    val transactionId: TransactionId,
    val module: String,
    val className: String,
    val methodName: String,
    val timeoutMillis: Long = 900_000L,
) {
    init {
        require(module.isNotBlank()) { "module cannot be blank" }
        require(className.isNotBlank()) { "className cannot be blank" }
        require(methodName.isNotBlank()) { "methodName cannot be blank" }
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }
    }
}

@SerializableValue
enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
}

@SerializableValue
data class CompilerDiagnostic(
    val path: ProjectPath? = null,
    val line: Int? = null,
    val column: Int? = null,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR,
) {
    init {
        require(message.isNotBlank()) { "message cannot be blank" }
        if (line != null) require(line >= 1) { "line must be >= 1" }
        if (column != null) require(column >= 1) { "column must be >= 1" }
    }
}

@SourceBearingValue
data class TestFailure(
    val className: String,
    val methodName: String,
    val message: String,
    val stackTraceSnippet: String = "",
) {
    init {
        require(className.isNotBlank()) { "className cannot be blank" }
        require(methodName.isNotBlank()) { "methodName cannot be blank" }
    }
}

@SourceBearingValue
data class BuildResult(
    val succeeded: Boolean,
    val exitCode: Int,
    val durationMillis: Long,
    val boundedOutput: BoundedOutputMetadata,
    val stdoutSnippet: String = "",
    val stderrSnippet: String = "",
    val executedTasks: List<String> = emptyList(),
    val diagnostics: List<CompilerDiagnostic> = emptyList(),
    val testFailures: List<TestFailure> = emptyList(),
    val reportPaths: List<ProjectPath> = emptyList(),
    val timedOut: Boolean = false,
) {
    init {
        require(durationMillis >= 0L) { "durationMillis must be non-negative" }
    }
}
