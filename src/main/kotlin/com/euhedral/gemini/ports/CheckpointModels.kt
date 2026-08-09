package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.CheckpointBlobId
import com.euhedral.gemini.core.agent.ProjectFingerprint
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue

@SerializableValue
enum class CheckpointStatus {
    ACTIVE,
    COMPLETED,
    ROLLED_BACK,
    FAILED,
    DISCARDED,
}

@SerializableValue
data class InitializeCheckpointRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val initialDigest: TransactionRevisionDigest,
)

@SourceBearingValue
data class CheckpointBlob(
    val id: CheckpointBlobId,
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckpointBlob) return false
        if (id != other.id) return false
        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    override fun toString(): String = "CheckpointBlob(id=${id.value}, size=${content.size} bytes)"
}

@SerializableValue
data class PendingMutationRecord(
    val callId: ToolCallId,
    val path: ProjectPath,
    val blobId: CheckpointBlobId? = null,
)

@SourceBearingValue
data class PersistPendingMutationRequest(
    val transactionId: TransactionId,
    val mutation: PendingMutationRecord,
    val blob: CheckpointBlob? = null,
)

@SerializableValue
data class CheckpointReceipt(
    val receiptId: String,
    val transactionId: TransactionId,
    val mutation: PendingMutationRecord,
) {
    init {
        require(receiptId.isNotBlank()) { "receiptId cannot be blank" }
    }
}

@SerializableValue
data class RecordAppliedMutationRequest(
    val receipt: CheckpointReceipt,
    val newDigest: TransactionRevisionDigest,
)

@SerializableValue
data class CheckpointManifest(
    val transactionId: TransactionId,
    val status: CheckpointStatus,
    val currentDigest: TransactionRevisionDigest,
    val mutations: List<PendingMutationRecord> = emptyList(),
)

@SerializableValue
data class RecoverableCheckpoint(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val status: CheckpointStatus,
    val digest: TransactionRevisionDigest,
)

@SerializableValue
data class SetCheckpointStatusRequest(
    val transactionId: TransactionId,
    val status: CheckpointStatus,
)

@SerializableValue
data class PruneCheckpointsRequest(
    val projectFingerprint: ProjectFingerprint,
    val keepLatest: Int = 10,
) {
    init {
        require(keepLatest >= 0) { "keepLatest must be non-negative: $keepLatest" }
    }
}

@SerializableValue
data class PruneCheckpointsResult(
    val prunedCount: Int,
) {
    init {
        require(prunedCount >= 0) { "prunedCount must be non-negative: $prunedCount" }
    }
}
