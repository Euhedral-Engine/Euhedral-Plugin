package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ContentHash
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.agent.TransactionRevision
import com.euhedral.gemini.core.policy.ValidationFinding
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue

@SerializableValue
sealed interface ExpectedFileRevision {
    @SerializableValue
    object Absent : ExpectedFileRevision

    @SerializableValue
    data class Present(val contentHash: ContentHash) : ExpectedFileRevision
}

@SerializableValue
data class OpenTransactionRequest(
    val sessionId: SessionId,
)

@SerializableValue
data class InspectTransactionRequest(
    val transactionId: TransactionId,
)

@SerializableValue
data class TransactionSnapshot(
    val digest: TransactionRevisionDigest,
    val touchedPaths: List<ProjectPath> = emptyList(),
)

@SourceBearingValue
data class ReplaceTextRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val callId: ToolCallId,
    val expectedRevision: TransactionRevision,
    val path: ProjectPath,
    val oldText: String,
    val newText: String,
    val expectedFileRevision: ExpectedFileRevision,
)

@SourceBearingValue
data class CreateFileRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val callId: ToolCallId,
    val expectedRevision: TransactionRevision,
    val path: ProjectPath,
    val content: String,
    val expectedFileRevision: ExpectedFileRevision = ExpectedFileRevision.Absent,
)

@SerializableValue
data class DeleteFileRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val callId: ToolCallId,
    val expectedRevision: TransactionRevision,
    val path: ProjectPath,
    val expectedFileRevision: ExpectedFileRevision,
)

@SerializableValue
data class MoveFileRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val callId: ToolCallId,
    val expectedRevision: TransactionRevision,
    val source: ProjectPath,
    val destination: ProjectPath,
    val expectedFileRevision: ExpectedFileRevision,
)

@SerializableValue
data class WorkspaceMutationResult(
    val callId: ToolCallId,
    val priorDigest: TransactionRevisionDigest,
    val resultingDigest: TransactionRevisionDigest,
    val changedPaths: List<ProjectPath>,
    val priorContentHashes: Map<ProjectPath, ContentHash>,
    val resultingContentHashes: Map<ProjectPath, ContentHash>,
    val findings: List<ValidationFinding> = emptyList(),
)

@SerializableValue
data class RollbackOperationRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
    val targetCallId: ToolCallId,
)

@SerializableValue
data class RollbackTransactionRequest(
    val sessionId: SessionId,
    val transactionId: TransactionId,
)

@SerializableValue
data class RollbackResult(
    val restoredPaths: List<ProjectPath>,
    val resultingDigest: TransactionRevisionDigest,
    val conflicts: List<ValidationFinding> = emptyList(),
)

@SerializableValue
data class SaveTransactionDocumentsRequest(
    val transactionId: TransactionId,
)

@SerializableValue
data class SaveDocumentsResult(
    val savedPaths: List<ProjectPath>,
)
