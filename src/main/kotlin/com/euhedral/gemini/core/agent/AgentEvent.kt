package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.error.RetryClass
import com.euhedral.gemini.core.policy.ValidationFinding
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.revision.VerifiedTransactionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.ToolEffect
import com.euhedral.gemini.core.tools.ToolResult

@SerializableValue
enum class ApprovalDecisionStatus {
    GRANTED,
    DENIED,
    CANCELLED,
    STALE,
}

@SerializableValue
data class ApprovalDecision(
    val requestId: ApprovalRequestId,
    val status: ApprovalDecisionStatus,
    val expectedSessionRevision: SessionRevision,
    val explanation: String? = null,
)

@SerializableValue
data class AgentStateEventEnvelope(
    val sessionId: SessionId,
    val expectedRevision: SessionRevision,
    val event: AgentStateEvent,
)

@SourceBearingValue
sealed interface AgentStateEvent {
    @SerializableValue
    data class SessionStarted(val startEpochMillis: Long) : AgentStateEvent

    @SerializableValue
    data class ModelTurnStarted(val modelRequestId: ModelRequestId) : AgentStateEvent

    @SerializableValue
    data class InteractionRecorded(val modelRequestId: ModelRequestId, val interactionId: InteractionId) : AgentStateEvent

    @SerializableValue
    data class ToolCallsAccepted(val modelRequestId: ModelRequestId, val callIds: List<ToolCallId>) : AgentStateEvent

    @SerializableValue
    data class RetryConsumed(val retryClass: RetryClass, val scopeId: String) : AgentStateEvent

    @SerializableValue
    data class RetryScopeReset(val retryClass: RetryClass, val scopeId: String) : AgentStateEvent

    @SerializableValue
    data class ProcessTimingObserved(val processInvocationId: ProcessInvocationId, val elapsedMillis: Long) : AgentStateEvent

    /** Elapsed time since SessionStarted; emitted for every orchestration wait, not only processes. */
    @SerializableValue
    data class SessionTimingObserved(val elapsedMillis: Long) : AgentStateEvent

    @SerializableValue
    data class MutatingCallReserved(val callId: ToolCallId, val fingerprint: ToolInvocationFingerprint) : AgentStateEvent

    @SourceBearingValue
    data class MutatingCallCompleted(val callId: ToolCallId, val result: ToolResult, val resultingDigest: TransactionRevisionDigest?) : AgentStateEvent

    @SerializableValue
    data class TransactionRevisionChanged(val newDigest: TransactionRevisionDigest) : AgentStateEvent

    @SerializableValue
    data class ApprovalRequested(val approval: PendingApproval) : AgentStateEvent

    @SerializableValue
    data class ExternalModificationDetected(val conflictApproval: PendingApproval) : AgentStateEvent

    @SerializableValue
    data class ApprovalResolved(val decision: ApprovalDecision) : AgentStateEvent

    @SerializableValue
    data class VerificationStarted(val verificationRunId: VerificationRunId, val frozenDigest: TransactionRevisionDigest) : AgentStateEvent

    @SerializableValue
    data class VerificationSucceeded(val verificationRunId: VerificationRunId, val verifiedDigest: VerifiedTransactionDigest) : AgentStateEvent

    @SerializableValue
    data class VerificationFailed(val verificationRunId: VerificationRunId, val error: AgentError) : AgentStateEvent

    @SerializableValue
    data class VerificationAborted(val verificationRunId: VerificationRunId) : AgentStateEvent

    @SerializableValue
    object WorkResumed : AgentStateEvent

    @SerializableValue
    data class SessionCompleted(val summary: String) : AgentStateEvent

    @SerializableValue
    data class SessionFailed(val error: AgentError) : AgentStateEvent

    @SerializableValue
    data class SessionCancelled(val reason: CancellationReason) : AgentStateEvent
}

@SerializableValue
data class FileChangeSummary(
    val path: ProjectPath,
    val changeType: String,
)

@SerializableValue
data class ProcessOutputChunkInfo(
    val processInvocationId: ProcessInvocationId,
    val isStdErr: Boolean,
    val text: String,
)

@SourceBearingValue
data class AgentEventEnvelope(
    val sessionId: SessionId,
    val eventSequence: EventSequence,
    val sessionRevision: SessionRevision,
    val occurredAtEpochMillis: Long,
    val payload: AgentEventPayload,
)

@SourceBearingValue
sealed interface AgentEventPayload {
    @SerializableValue
    data class SessionStarted(val sessionId: SessionId) : AgentEventPayload

    @SerializableValue
    data class ModelRequestStarted(val modelRequestId: ModelRequestId) : AgentEventPayload

    @SourceBearingValue
    data class ModelOutputDelta(val modelRequestId: ModelRequestId, val boundedText: String) : AgentEventPayload

    @SerializableValue
    data class InteractionRecorded(val modelRequestId: ModelRequestId, val interactionId: InteractionId) : AgentEventPayload

    @SerializableValue
    data class ToolBatchAccepted(val modelRequestId: ModelRequestId, val callIds: List<ToolCallId>) : AgentEventPayload

    @SerializableValue
    data class ToolStarted(val callId: ToolCallId, val toolName: ToolName, val effect: ToolEffect) : AgentEventPayload

    @SourceBearingValue
    data class ToolCompleted(val callId: ToolCallId, val toolName: ToolName, val result: ToolResult, val executionDisposition: String) : AgentEventPayload

    @SerializableValue
    data class RetryScheduled(val retryClass: RetryClass, val scopeId: String, val retryNumber: Int) : AgentEventPayload

    @SerializableValue
    data class ApprovalRequested(val request: PendingApproval) : AgentEventPayload

    @SerializableValue
    data class ApprovalResolved(val decision: ApprovalDecision) : AgentEventPayload

    @SerializableValue
    data class FilesChanged(val callId: ToolCallId?, val changes: List<FileChangeSummary>, val transactionDigest: TransactionRevisionDigest) : AgentEventPayload

    @SerializableValue
    data class ExternalModificationDetected(val conflict: PendingApproval) : AgentEventPayload

    @SerializableValue
    data class ValidationFailed(val callId: ToolCallId, val findings: List<ValidationFinding>) : AgentEventPayload

    @SerializableValue
    data class ProcessStarted(val processInvocationId: ProcessInvocationId, val logicalOperation: String) : AgentEventPayload

    @SourceBearingValue
    data class ProcessOutput(val processInvocationId: ProcessInvocationId, val chunk: ProcessOutputChunkInfo) : AgentEventPayload

    @SerializableValue
    data class ProcessFinished(val processInvocationId: ProcessInvocationId, val resultSummary: String) : AgentEventPayload

    @SerializableValue
    data class VerificationStarted(val verificationRunId: VerificationRunId, val frozenDigest: TransactionRevisionDigest) : AgentEventPayload

    @SerializableValue
    data class VerificationPassed(val verificationRunId: VerificationRunId, val verifiedDigest: VerifiedTransactionDigest) : AgentEventPayload

    @SerializableValue
    data class VerificationFailed(val verificationRunId: VerificationRunId, val error: AgentError, val repairCyclesUsed: Int) : AgentEventPayload

    @SerializableValue
    data class RepairStarted(val repairCycleNumber: Int) : AgentEventPayload

    @SerializableValue
    data class SessionCompleted(val reason: TerminalReason.Completed) : AgentEventPayload

    @SerializableValue
    data class SessionFailed(val reason: TerminalReason.Failed) : AgentEventPayload

    @SerializableValue
    data class SessionCancelled(val reason: CancellationReason) : AgentEventPayload
}
