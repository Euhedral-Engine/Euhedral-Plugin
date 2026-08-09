package com.euhedral.gemini.ports

import com.euhedral.gemini.core.serialization.AdapterLocalValue
import com.euhedral.gemini.core.serialization.SerializableValue
import java.util.Arrays

@SerializableValue
enum class CredentialProvider {
    API_KEY,
    OAUTH,
}

@SerializableValue
data class CredentialReference(
    val provider: CredentialProvider,
    val accountLabel: String,
) {
    init {
        require(accountLabel.isNotBlank()) { "accountLabel cannot be blank" }
    }
}

@SerializableValue
data class CredentialStatus(
    val reference: CredentialReference,
    val available: Boolean,
    val expiresAtEpochMillis: Long? = null,
) {
    init {
        if (expiresAtEpochMillis != null) {
            require(expiresAtEpochMillis >= 0L) { "expiresAtEpochMillis must be non-negative" }
        }
    }
}

@SerializableValue
data class CredentialRequest(
    val reference: CredentialReference,
)

@AdapterLocalValue
class SecretValue(chars: CharArray) : AutoCloseable {
    private var buffer: CharArray? = chars.clone()

    fun getChars(): CharArray {
        val current = checkNotNull(buffer) { "SecretValue has been closed and cleared" }
        return current.clone()
    }

    override fun close() {
        buffer?.let { Arrays.fill(it, '\u0000') }
        buffer = null
    }

    override fun toString(): String = "[REDACTED_SECRET]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecretValue) return false
        val thisBuf = buffer
        val otherBuf = other.buffer
        if (thisBuf == null || otherBuf == null) return thisBuf === otherBuf
        return thisBuf.contentEquals(otherBuf)
    }

    override fun hashCode(): Int {
        return buffer?.contentHashCode() ?: 0
    }
}

@AdapterLocalValue
data class CredentialLease(
    val reference: CredentialReference,
    val secret: SecretValue,
    val expiresAtEpochMillis: Long? = null,
)

@AdapterLocalValue
data class StoreCredentialRequest(
    val reference: CredentialReference,
    val secret: SecretValue,
)

@SerializableValue
data class RemoveCredentialRequest(
    val reference: CredentialReference,
)

@SerializableValue
data class RevokeCredentialRequest(
    val reference: CredentialReference,
)

@SerializableValue
data class CredentialRevokeResult(
    val revoked: Boolean,
)
