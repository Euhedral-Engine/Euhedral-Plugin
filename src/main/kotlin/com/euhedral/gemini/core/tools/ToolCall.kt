package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.agent.ContinuationToken
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.ToolInvocationFingerprint
import com.euhedral.gemini.core.agent.ToolName
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import java.security.MessageDigest

private val CANONICAL_DECIMAL_REGEX = Regex("^-?[0-9]+(\\.[0-9]+)?$")

private fun framed(tag: String, value: String): String = "$tag:${value.length}:$value"

@SerializableValue
sealed interface ToolValue {
    fun canonicalEncoding(): String

    @SerializableValue
    object NullValue : ToolValue {
        override fun canonicalEncoding(): String = "null"
    }

    @SerializableValue
    data class BooleanValue(val value: Boolean) : ToolValue {
        override fun canonicalEncoding(): String = framed("boolean", value.toString())
    }

    @SerializableValue
    data class IntegerValue(val value: Long) : ToolValue {
        override fun canonicalEncoding(): String = framed("int", value.toString())
    }

    @SerializableValue
    data class DecimalValue(val value: String) : ToolValue {
        init {
            require(CANONICAL_DECIMAL_REGEX.matches(value)) {
                "DecimalValue must be canonical numeric string (no exponents/NaN/Infinity): '$value'"
            }
        }

        override fun canonicalEncoding(): String = framed("decimal", value)
    }

    @SerializableValue
    data class StringValue(val value: String) : ToolValue {
        override fun canonicalEncoding(): String = framed("string", value)
    }

    @SerializableValue
    data class ListValue(val items: List<ToolValue>) : ToolValue {
        override fun canonicalEncoding(): String =
            "list:${items.size}:" + items.joinToString(separator = "") { framed("item", it.canonicalEncoding()) }
    }

    @SerializableValue
    data class ObjectValue(
        val entries: Map<String, ToolValue>,
    ) : ToolValue {
        init {
            require(entries.keys.toList() == entries.keys.sorted()) {
                "ObjectValue keys must be sorted lexicographically"
            }
        }

        override fun canonicalEncoding(): String =
            "object:${entries.size}:" + entries.entries.joinToString(separator = "") {
                framed("key", it.key) + framed("value", it.value.canonicalEncoding())
            }

        companion object {
            fun of(map: Map<String, ToolValue>): ObjectValue {
                val sorted = map.toSortedMap()
                return ObjectValue(sorted)
            }
        }
    }
}

@SourceBearingValue
data class ToolCall(
    val id: ToolCallId,
    val name: ToolName,
    val arguments: ToolValue.ObjectValue,
) {
    fun computeFingerprint(effect: ToolEffect): ToolInvocationFingerprint {
        val rawPayload = framed("tool-call-v1-name", name.value) +
            framed("effect", effect.name) +
            framed("arguments", arguments.canonicalEncoding())
        val bytes = rawPayload.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(it) }
        return ToolInvocationFingerprint(hex)
    }
}
