package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
enum class ToolEffect {
    READ_ONLY,
    MUTATING,
    PROCESS,
    CONTROL,
}
