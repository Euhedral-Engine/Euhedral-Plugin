package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ApprovalRequestId
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.agent.SessionRevision
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.tools.ToolEffect

@SerializableValue
enum class ApprovalWithdrawalReason {
    USER_CANCELLED,
    TIMEOUT,
    STALE_REVISION,
    SESSION_CANCELLED,
}

@SerializableValue
data class ApprovalRequest(
    val requestId: ApprovalRequestId,
    val sessionId: SessionId,
    val callId: ToolCallId? = null,
    val effect: ToolEffect,
    val actionSummary: String,
    val targetSummary: String,
    val policyReasonCode: String,
    val consequences: List<String> = emptyList(),
    val expectedSessionRevision: SessionRevision,
    val expectedTransactionDigest: TransactionRevisionDigest? = null,
) {
    init {
        require(actionSummary.isNotBlank()) { "actionSummary cannot be blank" }
        require(targetSummary.isNotBlank()) { "targetSummary cannot be blank" }
        require(policyReasonCode.isNotBlank()) { "policyReasonCode cannot be blank" }
    }
}
