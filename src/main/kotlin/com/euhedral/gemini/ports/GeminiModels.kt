package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.InteractionId
import com.euhedral.gemini.core.agent.ModelRequestId
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.serialization.AdapterLocalValue
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.ToolCall
import com.euhedral.gemini.core.tools.ToolDescriptor
import com.euhedral.gemini.core.tools.ToolResult

@SerializableValue
data class GenerationConfig(
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
)

@SourceBearingValue
sealed interface InteractionInput {
    @SourceBearingValue
    data class UserText(val text: String) : InteractionInput

    @SourceBearingValue
    data class FunctionResults(val results: List<ToolResult>) : InteractionInput
}

@SerializableValue
data class TokenUsage(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int,
) {
    init {
        require(promptTokenCount >= 0) { "promptTokenCount must be non-negative" }
        require(candidatesTokenCount >= 0) { "candidatesTokenCount must be non-negative" }
        require(totalTokenCount >= 0) { "totalTokenCount must be non-negative" }
    }
}

@SourceBearingValue
sealed interface InteractionStep {
    @SourceBearingValue
    data class ModelText(val text: String) : InteractionStep

    @SourceBearingValue
    data class FunctionCall(val call: ToolCall) : InteractionStep

    @SerializableValue
    data class SafeUnknownStep(val stepType: String, val summary: String) : InteractionStep
}

@SourceBearingValue
data class InteractionRequest(
    val sessionId: SessionId,
    val modelRequestId: ModelRequestId,
    val modelName: String,
    val previousInteractionId: InteractionId? = null,
    val store: Boolean = false,
    val systemInstruction: String? = null,
    val inputs: List<InteractionInput>,
    val tools: List<ToolDescriptor>,
    val generationConfig: GenerationConfig = GenerationConfig(),
    val remainingHttpRetries: Int = 3,
) {
    init {
        require(modelName.isNotBlank()) { "modelName cannot be blank" }
        require(remainingHttpRetries >= 0) { "remainingHttpRetries must be non-negative" }
    }
}

@SourceBearingValue
data class InteractionResponse(
    val interactionId: InteractionId,
    val steps: List<InteractionStep>,
    val tokenUsage: TokenUsage? = null,
    val httpRetriesUsed: Int = 0,
) {
    init {
        require(httpRetriesUsed >= 0) { "httpRetriesUsed must be non-negative" }
    }
}

@SerializableValue
data class DeleteInteractionResult(
    val deleted: Boolean,
)

@SourceBearingValue
sealed interface InteractionStreamEvent {
    @SerializableValue
    data class Created(val interactionId: InteractionId) : InteractionStreamEvent

    @SourceBearingValue
    data class TextDelta(val text: String) : InteractionStreamEvent

    @SourceBearingValue
    data class FunctionCallReady(val call: ToolCall) : InteractionStreamEvent

    @SerializableValue
    data class SafeUnknown(val stepType: String, val summary: String) : InteractionStreamEvent

    @SourceBearingValue
    data class Completed(val response: InteractionResponse) : InteractionStreamEvent
}

@AdapterLocalValue
interface InteractionStreamSink {
    suspend fun emit(event: InteractionStreamEvent)
}
