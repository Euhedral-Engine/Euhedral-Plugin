package com.euhedral.gemini.ports

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue

@SourceBearingValue
sealed interface PortResult<out T> {
    @SourceBearingValue
    data class Success<T>(val value: T) : PortResult<T>

    @SerializableValue
    data class Failure(val error: AgentError) : PortResult<Nothing>
}
