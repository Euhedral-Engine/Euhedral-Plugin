package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.InteractionId

interface GeminiTransport {
    suspend fun interact(
        request: InteractionRequest,
    ): PortResult<InteractionResponse>

    suspend fun stream(
        request: InteractionRequest,
        sink: InteractionStreamSink,
    ): PortResult<InteractionResponse>

    suspend fun deleteInteraction(
        interactionId: InteractionId,
    ): PortResult<DeleteInteractionResult>
}
