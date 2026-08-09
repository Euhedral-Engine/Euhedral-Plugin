package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.agent.ContinuationToken
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.ToolName
import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue

@SerializableValue
data class BoundedOutputMetadata(
    val truncated: Boolean,
    val returnedItems: Int,
    val totalItems: Int? = null,
    val returnedCharacters: Int,
    val totalCharacters: Int? = null,
    val continuationToken: ContinuationToken? = null,
) {
    init {
        require(returnedItems >= 0) { "returnedItems must be non-negative: $returnedItems" }
        require(returnedCharacters >= 0) { "returnedCharacters must be non-negative: $returnedCharacters" }
        if (totalItems != null) {
            require(totalItems >= 0) { "totalItems must be non-negative: $totalItems" }
            require(returnedItems <= totalItems) { "returnedItems ($returnedItems) cannot exceed totalItems ($totalItems)" }
        }
        if (totalCharacters != null) {
            require(totalCharacters >= 0) { "totalCharacters must be non-negative: $totalCharacters" }
            require(returnedCharacters <= totalCharacters) { "returnedCharacters ($returnedCharacters) cannot exceed totalCharacters ($totalCharacters)" }
        }
        if (truncated) {
            require(continuationToken != null) { "Truncated continuable output requires a continuationToken" }
        } else {
            require(continuationToken == null) { "Non-truncated output cannot have a continuationToken" }
        }
    }
}

@SourceBearingValue
sealed interface ToolOutcome {
    @SourceBearingValue
    data class Success(val value: ToolValue) : ToolOutcome

    @SourceBearingValue
    data class Failure(val error: AgentError) : ToolOutcome
}

@SourceBearingValue
data class ToolResult(
    val id: ToolCallId,
    val toolName: ToolName,
    val outcome: ToolOutcome,
    val boundedMetadata: BoundedOutputMetadata,
    val resultingTransaction: TransactionRevisionDigest? = null,
)
