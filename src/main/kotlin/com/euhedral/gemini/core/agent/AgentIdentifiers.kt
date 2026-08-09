package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class SessionRevision(val value: Long) {
    init {
        require(value >= 0L) { "SessionRevision must be non-negative: $value" }
    }

    fun next(): SessionRevision {
        require(value < Long.MAX_VALUE) { "SessionRevision overflow" }
        return SessionRevision(value + 1L)
    }
}

@SerializableValue
@JvmInline
value class EventSequence(val value: Long) {
    init {
        require(value >= 0L) { "EventSequence must be non-negative: $value" }
    }

    fun next(): EventSequence {
        require(value < Long.MAX_VALUE) { "EventSequence overflow" }
        return EventSequence(value + 1L)
    }
}

@SerializableValue
@JvmInline
value class ModelRequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "ModelRequestId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class InteractionId(val value: String) {
    init {
        require(value.isNotBlank()) { "InteractionId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "TransactionId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class TransactionRevision(val value: Long) {
    init {
        require(value >= 0L) { "TransactionRevision must be non-negative: $value" }
    }

    fun next(): TransactionRevision {
        require(value < Long.MAX_VALUE) { "TransactionRevision overflow" }
        return TransactionRevision(value + 1L)
    }
}

@SerializableValue
@JvmInline
value class VerificationRunId(val value: String) {
    init {
        require(value.isNotBlank()) { "VerificationRunId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class ToolCallId(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolCallId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class ToolName(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolName cannot be blank" }
    }
}

private val LOWERCASE_HEX_64_REGEX = Regex("^[0-9a-f]{64}$")

@SerializableValue
@JvmInline
value class ToolInvocationFingerprint(val value: String) {
    init {
        require(LOWERCASE_HEX_64_REGEX.matches(value)) {
            "ToolInvocationFingerprint must be 64 lowercase hex characters: '$value'"
        }
    }
}

@SerializableValue
@JvmInline
value class ApprovalRequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "ApprovalRequestId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class ProcessInvocationId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProcessInvocationId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class CheckpointBlobId(val value: String) {
    init {
        require(value.isNotBlank()) { "CheckpointBlobId cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class ProjectFingerprint(val value: String) {
    init {
        require(value.isNotBlank()) { "ProjectFingerprint cannot be blank" }
    }
}

@SerializableValue
@JvmInline
value class ProjectPath(val value: String) {
    init {
        require(value.isNotBlank()) { "ProjectPath cannot be blank" }
        require(!value.startsWith("/")) { "ProjectPath must be relative: '$value'" }
        require(!value.contains("\\")) { "ProjectPath must use forward slashes: '$value'" }
        require(!value.split("/").contains("..")) { "ProjectPath cannot contain directory traversal: '$value'" }
    }
}

@SerializableValue
@JvmInline
value class ContentHash(val value: String) {
    init {
        require(LOWERCASE_HEX_64_REGEX.matches(value)) {
            "ContentHash must be 64 lowercase hex characters: '$value'"
        }
    }
}

@SerializableValue
@JvmInline
value class Sha256Digest(val value: String) {
    init {
        require(LOWERCASE_HEX_64_REGEX.matches(value)) {
            "Sha256Digest must be 64 lowercase hex characters: '$value'"
        }
    }
}

@SerializableValue
@JvmInline
value class ContinuationToken(val value: String) {
    init {
        require(value.isNotBlank()) { "ContinuationToken cannot be blank" }
    }
}
