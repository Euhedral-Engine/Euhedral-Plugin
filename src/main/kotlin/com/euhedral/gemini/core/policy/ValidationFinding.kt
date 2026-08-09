package com.euhedral.gemini.core.policy

import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
enum class FindingSeverity {
    ERROR,
    WARNING,
    ADVISORY,
}

@SerializableValue
data class LineRange(
    val startLine: Int,
    val endLine: Int,
) {
    init {
        require(startLine >= 1) { "startLine must be >= 1" }
        require(endLine >= startLine) { "endLine must be >= startLine" }
    }
}

@SerializableValue
data class ValidationFinding(
    val code: String,
    val severity: FindingSeverity,
    val path: ProjectPath? = null,
    val range: LineRange? = null,
    val explanation: String,
    val suggestedRecovery: String? = null,
) {
    init {
        require(code.isNotBlank()) { "Finding code cannot be blank" }
        require(explanation.isNotBlank()) { "Finding explanation cannot be blank" }
    }
}
