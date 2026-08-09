package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ApprovalRequestId
import com.euhedral.gemini.core.agent.ApprovalDecision

interface ApprovalPort {
    suspend fun awaitDecision(request: ApprovalRequest): PortResult<ApprovalDecision>
    suspend fun withdraw(requestId: ApprovalRequestId, reason: ApprovalWithdrawalReason): PortResult<Unit>
}
