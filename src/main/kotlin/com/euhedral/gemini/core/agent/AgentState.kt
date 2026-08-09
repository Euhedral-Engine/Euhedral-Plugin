package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.revision.VerifiedTransactionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.ToolEffect

@SerializableValue
enum class AgentState {
    IDLE,
    RUNNING,
    WAITING_APPROVAL,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@SerializableValue
enum class VerificationPhase {
    IN_PROGRESS,
    PASSED_AWAITING_FINAL,
}

@SerializableValue
data class VerificationAttempt(
    val verificationRunId: VerificationRunId,
    val frozenDigest: TransactionRevisionDigest,
    val phase: VerificationPhase = VerificationPhase.IN_PROGRESS,
)

@SerializableValue
data class PendingApproval(
    val requestId: ApprovalRequestId,
    val callId: ToolCallId? = null,
    val effect: ToolEffect,
    val actionSummary: String,
    val targetSummary: String,
    val policyReasonCode: String,
    val expectedSessionRevision: SessionRevision,
    val expectedTransactionDigest: TransactionRevisionDigest? = null,
)

@SerializableValue
enum class CancellationReason {
    USER_REQUESTED,
    PROJECT_CLOSED,
    PLUGIN_UNLOADED,
    APPROVAL_CANCELLED,
    SESSION_TIMEOUT_WHILE_WAITING,
    SUPERSEDED,
}

@SerializableValue
sealed interface TerminalReason {
    @SerializableValue
    data class Completed(
        val summary: String,
        val verifiedTransaction: VerifiedTransactionDigest,
    ) : TerminalReason

    @SerializableValue
    data class Failed(
        val error: AgentError,
    ) : TerminalReason

    @SerializableValue
    data class Cancelled(
        val cancellationReason: CancellationReason,
    ) : TerminalReason
}

@SourceBearingValue
data class AgentSession(
    val id: SessionId,
    val revision: SessionRevision = SessionRevision(0L),
    val state: AgentState = AgentState.IDLE,
    val projectFingerprint: ProjectFingerprint,
    val transaction: TransactionRevisionDigest,
    val verifiedTransaction: VerifiedTransactionDigest? = null,
    val verificationAttempt: VerificationAttempt? = null,
    val pendingApproval: PendingApproval? = null,
    val remoteInteractionIds: List<InteractionId> = emptyList(),
    val limits: AgentLimits = AgentLimits(),
    val counters: AgentCounters = AgentCounters(),
    val mutatingCalls: MutatingCallLedger = MutatingCallLedger(),
    val terminalReason: TerminalReason? = null,
) {
    init {
        when (state) {
            AgentState.IDLE -> {
                require(pendingApproval == null) { "IDLE state cannot have pending approval" }
                require(verificationAttempt == null) { "IDLE state cannot have verification attempt" }
                require(verifiedTransaction == null) { "IDLE state cannot have verified transaction" }
                require(remoteInteractionIds.isEmpty()) { "IDLE state cannot have remote interactions" }
                require(terminalReason == null) { "IDLE state cannot have terminal reason" }
            }
            AgentState.RUNNING -> {
                require(pendingApproval == null) { "RUNNING state cannot have pending approval" }
                require(verificationAttempt == null) { "RUNNING state cannot have verification attempt" }
                require(terminalReason == null) { "RUNNING state cannot have terminal reason" }
            }
            AgentState.WAITING_APPROVAL -> {
                require(pendingApproval != null) { "WAITING_APPROVAL state must have pending approval" }
                require(verificationAttempt == null) { "WAITING_APPROVAL state cannot have verification attempt" }
                require(terminalReason == null) { "WAITING_APPROVAL state cannot have terminal reason" }
            }
            AgentState.VERIFYING -> {
                require(verificationAttempt != null) { "VERIFYING state must have verification attempt" }
                require(pendingApproval == null) { "VERIFYING state cannot have pending approval" }
                require(terminalReason == null) { "VERIFYING state cannot have terminal reason" }
                if (verificationAttempt.phase == VerificationPhase.PASSED_AWAITING_FINAL) {
                    require(verifiedTransaction != null) { "PASSED_AWAITING_FINAL subphase must have verified transaction" }
                    require(verifiedTransaction.transaction == verificationAttempt.frozenDigest) { "PASSED_AWAITING_FINAL verified transaction must match frozen digest" }
                    require(verifiedTransaction.transaction == transaction) { "PASSED_AWAITING_FINAL verified transaction must match current transaction" }
                }
            }
            AgentState.COMPLETED -> {
                require(terminalReason is TerminalReason.Completed) { "COMPLETED state must have Completed terminal reason" }
                require(verifiedTransaction != null) { "COMPLETED state must have verified transaction" }
                require(verifiedTransaction.transaction == transaction) { "COMPLETED verified transaction must match current transaction" }
                require(pendingApproval == null) { "COMPLETED state cannot have pending approval" }
                require(verificationAttempt == null) { "COMPLETED state cannot have verification attempt" }
            }
            AgentState.FAILED -> {
                require(terminalReason is TerminalReason.Failed) { "FAILED state must have Failed terminal reason" }
                require(pendingApproval == null) { "FAILED state cannot have pending approval" }
                require(verificationAttempt == null) { "FAILED state cannot have verification attempt" }
            }
            AgentState.CANCELLED -> {
                require(terminalReason is TerminalReason.Cancelled) { "CANCELLED state must have Cancelled terminal reason" }
                require(pendingApproval == null) { "CANCELLED state cannot have pending approval" }
                require(verificationAttempt == null) { "CANCELLED state cannot have verification attempt" }
            }
        }
    }

    fun toRecoveryMetadata(): SessionRecoveryMetadata = SessionRecoveryMetadata(
        id = id,
        revision = revision,
        state = state,
        projectFingerprint = projectFingerprint,
        transaction = transaction,
        verifiedTransaction = verifiedTransaction,
        verificationAttempt = verificationAttempt,
        pendingApproval = pendingApproval,
        remoteInteractionIds = remoteInteractionIds,
        limits = limits,
        counters = counters,
        mutatingCalls = mutatingCalls,
        terminalReason = terminalReason,
    )
}

@SourceBearingValue
data class SessionRecoveryMetadata(
    val id: SessionId,
    val revision: SessionRevision,
    val state: AgentState,
    val projectFingerprint: ProjectFingerprint,
    val transaction: TransactionRevisionDigest,
    val verifiedTransaction: VerifiedTransactionDigest?,
    val verificationAttempt: VerificationAttempt?,
    val pendingApproval: PendingApproval?,
    val remoteInteractionIds: List<InteractionId>,
    val limits: AgentLimits,
    val counters: AgentCounters,
    val mutatingCalls: MutatingCallLedger,
    val terminalReason: TerminalReason?,
)
