package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.error.DuplicateCallIdError
import com.euhedral.gemini.core.error.ErrorCategory
import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.InvalidTransitionError
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.error.RetryClass
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
sealed interface Reduction {
    @SerializableValue
    data class Accepted(val session: AgentSession) : Reduction

    @SerializableValue
    data class Rejected(
        val unchangedSession: AgentSession,
        val error: AgentError,
    ) : Reduction
}

object AgentReducer {

    fun reduce(
        session: AgentSession,
        envelope: AgentStateEventEnvelope,
    ): Reduction {
        if (envelope.sessionId != session.id) {
            return Reduction.Rejected(
                unchangedSession = session,
                error = OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.SESSION_ID_MISMATCH,
                    safeMessage = "Session ID mismatch: event ${envelope.sessionId.value} vs session ${session.id.value}",
                ),
            )
        }

        if (envelope.expectedRevision != session.revision) {
            return Reduction.Rejected(
                unchangedSession = session,
                error = OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.STALE_SESSION_EVENT,
                    safeMessage = "Stale session event: expected revision ${envelope.expectedRevision.value} vs current revision ${session.revision.value}",
                ),
            )
        }

        if (session.state in setOf(AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED)) {
            return Reduction.Rejected(
                unchangedSession = session,
                error = InvalidTransitionError(session.state, eventName(envelope.event)),
            )
        }

        return when (val event = envelope.event) {
            is AgentStateEvent.SessionStarted -> handleSessionStarted(session, event)
            is AgentStateEvent.ModelTurnStarted -> handleModelTurnStarted(session, event)
            is AgentStateEvent.InteractionRecorded -> handleInteractionRecorded(session, event)
            is AgentStateEvent.ToolCallsAccepted -> handleToolCallsAccepted(session, event)
            is AgentStateEvent.RetryConsumed -> handleRetryConsumed(session, event)
            is AgentStateEvent.RetryScopeReset -> handleRetryScopeReset(session, event)
            is AgentStateEvent.ProcessTimingObserved -> handleProcessTimingObserved(session, event)
            is AgentStateEvent.SessionTimingObserved -> handleSessionTimingObserved(session, event)
            is AgentStateEvent.MutatingCallReserved -> handleMutatingCallReserved(session, event)
            is AgentStateEvent.MutatingCallCompleted -> handleMutatingCallCompleted(session, event)
            is AgentStateEvent.TransactionRevisionChanged -> handleTransactionRevisionChanged(session, event)
            is AgentStateEvent.ApprovalRequested -> handleApprovalRequested(session, event)
            is AgentStateEvent.ExternalModificationDetected -> handleExternalModificationDetected(session, event)
            is AgentStateEvent.ApprovalResolved -> handleApprovalResolved(session, event)
            is AgentStateEvent.VerificationStarted -> handleVerificationStarted(session, event)
            is AgentStateEvent.VerificationSucceeded -> handleVerificationSucceeded(session, event)
            is AgentStateEvent.VerificationFailed -> handleVerificationFailed(session, event)
            is AgentStateEvent.VerificationAborted -> handleVerificationAborted(session, event)
            is AgentStateEvent.WorkResumed -> handleWorkResumed(session, event)
            is AgentStateEvent.SessionCompleted -> handleSessionCompleted(session, event)
            is AgentStateEvent.SessionFailed -> handleSessionFailed(session, event)
            is AgentStateEvent.SessionCancelled -> handleSessionCancelled(session, event)
        }
    }

    private fun eventName(event: AgentStateEvent): String = event::class.simpleName ?: "AgentStateEvent"

    private fun accept(session: AgentSession): Reduction {
        return Reduction.Accepted(session.copy(revision = session.revision.next()))
    }

    private fun rejectInvalid(session: AgentSession, event: AgentStateEvent): Reduction {
        return Reduction.Rejected(session, InvalidTransitionError(session.state, eventName(event)))
    }

    private fun handleSessionStarted(session: AgentSession, event: AgentStateEvent.SessionStarted): Reduction {
        if (session.state != AgentState.IDLE) return rejectInvalid(session, event)
        if (session.revision.value != 0L || session.transaction.revision.value != 0L) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.INVALID_TRANSITION,
                    safeMessage = "SessionStarted requires revision 0 and initial transaction revision 0",
                ),
            )
        }
        val nextCounters = session.counters.copy(
            startEpochMillis = event.startEpochMillis,
            processElapsedMillis = emptyMap(),
            sessionElapsedMillis = 0L,
        )
        return accept(session.copy(state = AgentState.RUNNING, counters = nextCounters))
    }

    private fun handleModelTurnStarted(session: AgentSession, event: AgentStateEvent.ModelTurnStarted): Reduction {
        if (session.state != AgentState.RUNNING &&
            !(session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.PASSED_AWAITING_FINAL)
        ) return rejectInvalid(session, event)
        return when (val decision = session.limits.modelTurn.consumeTurn()) {
            is BudgetDecision.Allowed -> {
                val nextLimits = session.limits.copy(modelTurn = decision.nextBudget)
                val nextCounters = session.counters.copy(modelTurnsUsed = session.counters.modelTurnsUsed + 1)
                accept(session.copy(limits = nextLimits, counters = nextCounters))
            }
            is BudgetDecision.Exhausted -> Reduction.Rejected(session, decision.error)
        }
    }

    private fun handleInteractionRecorded(session: AgentSession, event: AgentStateEvent.InteractionRecorded): Reduction {
        if (session.state != AgentState.RUNNING &&
            !(session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.PASSED_AWAITING_FINAL)
        ) return rejectInvalid(session, event)
        if (session.remoteInteractionIds.contains(event.interactionId)) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.STALE_SESSION_EVENT,
                    safeMessage = "Duplicate interaction ID '${event.interactionId.value}'",
                ),
            )
        }
        val nextInteractions = session.remoteInteractionIds + event.interactionId
        return accept(session.copy(remoteInteractionIds = nextInteractions))
    }

    private fun handleToolCallsAccepted(session: AgentSession, event: AgentStateEvent.ToolCallsAccepted): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        val count = event.callIds.size
        return when (val decision = session.limits.toolCall.consumeCalls(count)) {
            is BudgetDecision.Allowed -> {
                val nextLimits = session.limits.copy(toolCall = decision.nextBudget)
                val nextCounters = session.counters.copy(toolCallsUsed = session.counters.toolCallsUsed + count)
                accept(session.copy(limits = nextLimits, counters = nextCounters))
            }
            is BudgetDecision.Exhausted -> Reduction.Rejected(session, decision.error)
        }
    }

    private fun handleRetryConsumed(session: AgentSession, event: AgentStateEvent.RetryConsumed): Reduction {
        val inValidState = session.state == AgentState.RUNNING ||
            (session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.IN_PROGRESS)
        if (!inValidState) return rejectInvalid(session, event)

        return when (event.retryClass) {
            RetryClass.HTTP -> {
                when (val decision = session.limits.httpRetry.consumeRetry(event.scopeId)) {
                    is BudgetDecision.Allowed -> {
                        val nextLimits = session.limits.copy(httpRetry = decision.nextBudget)
                        val nextCounters = session.counters.copy(httpRetriesUsed = session.counters.httpRetriesUsed + 1)
                        accept(session.copy(limits = nextLimits, counters = nextCounters))
                    }
                    is BudgetDecision.Exhausted -> Reduction.Rejected(session, decision.error)
                }
            }
            RetryClass.TRANSIENT_TOOL -> {
                when (val decision = session.limits.transientToolRetry.consumeRetry(event.scopeId)) {
                    is BudgetDecision.Allowed -> {
                        val nextLimits = session.limits.copy(transientToolRetry = decision.nextBudget)
                        val nextCounters = session.counters.copy(transientToolRetriesUsed = session.counters.transientToolRetriesUsed + 1)
                        accept(session.copy(limits = nextLimits, counters = nextCounters))
                    }
                    is BudgetDecision.Exhausted -> Reduction.Rejected(session, decision.error)
                }
            }
            RetryClass.REPAIR_CYCLE, RetryClass.NONE -> rejectInvalid(session, event)
        }
    }

    private fun handleRetryScopeReset(session: AgentSession, event: AgentStateEvent.RetryScopeReset): Reduction {
        val inValidState = session.state == AgentState.RUNNING ||
            (session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.IN_PROGRESS)
        if (!inValidState) return rejectInvalid(session, event)

        val nextLimits = when (event.retryClass) {
            RetryClass.HTTP -> session.limits.copy(httpRetry = session.limits.httpRetry.closeScope(event.scopeId))
            RetryClass.TRANSIENT_TOOL -> session.limits.copy(transientToolRetry = session.limits.transientToolRetry.closeScope(event.scopeId))
            RetryClass.REPAIR_CYCLE, RetryClass.NONE -> return rejectInvalid(session, event)
        }
        return accept(session.copy(limits = nextLimits))
    }

    private fun handleProcessTimingObserved(session: AgentSession, event: AgentStateEvent.ProcessTimingObserved): Reduction {
        val inValidState = session.state == AgentState.RUNNING ||
            (session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.IN_PROGRESS)
        if (!inValidState) return rejectInvalid(session, event)

        val previousElapsed = session.counters.processElapsedMillis[event.processInvocationId] ?: 0L
        if (event.elapsedMillis < previousElapsed) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.BUDGET,
                    code = ErrorCode.INVALID_TRANSITION,
                    safeMessage = "Process elapsed time regression observed: ${event.elapsedMillis}ms < ${previousElapsed}ms",
                ),
            )
        }

        when (val decision = session.limits.processTimeout.evaluate(event.elapsedMillis)) {
            is BudgetDecision.Exhausted -> return Reduction.Rejected(session, decision.error)
            is BudgetDecision.Allowed -> {}
        }
        val nextCounters = session.counters.copy(
            processElapsedMillis = session.counters.processElapsedMillis + (event.processInvocationId to event.elapsedMillis),
        )
        return accept(session.copy(counters = nextCounters))
    }

    private fun handleSessionTimingObserved(session: AgentSession, event: AgentStateEvent.SessionTimingObserved): Reduction {
        if (session.state !in setOf(AgentState.RUNNING, AgentState.WAITING_APPROVAL, AgentState.VERIFYING)) return rejectInvalid(session, event)
        if (event.elapsedMillis < session.counters.sessionElapsedMillis) {
            return Reduction.Rejected(session, OperationError(
                category = ErrorCategory.BUDGET,
                code = ErrorCode.INVALID_TRANSITION,
                safeMessage = "Session elapsed time regression observed: ${event.elapsedMillis}ms < ${session.counters.sessionElapsedMillis}ms",
            ))
        }
        return when (val decision = session.limits.sessionTime.evaluate(event.elapsedMillis)) {
            is BudgetDecision.Allowed -> accept(session.copy(counters = session.counters.copy(sessionElapsedMillis = event.elapsedMillis)))
            is BudgetDecision.Exhausted -> {
                val timedOut = session.copy(counters = session.counters.copy(sessionElapsedMillis = event.elapsedMillis))
                if (session.state == AgentState.WAITING_APPROVAL) {
                    accept(timedOut.copy(
                        state = AgentState.CANCELLED,
                        pendingApproval = null,
                        terminalReason = TerminalReason.Cancelled(CancellationReason.SESSION_TIMEOUT_WHILE_WAITING),
                    ))
                } else {
                    accept(timedOut.copy(
                        state = AgentState.FAILED,
                        verificationAttempt = null,
                        terminalReason = TerminalReason.Failed(decision.error),
                    ))
                }
            }
        }
    }

    private fun handleMutatingCallReserved(session: AgentSession, event: AgentStateEvent.MutatingCallReserved): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        val existing = session.mutatingCalls.entries[event.callId]
        if (existing != null) {
            val error = if (existing.fingerprint == event.fingerprint) {
                OperationError(ErrorCategory.TOOL_CALL, ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN,
                    "Mutating call '${event.callId.value}' is already recorded; use the ledger claim outcome")
            } else {
                DuplicateCallIdError(ErrorCode.DUPLICATE_CALL_ID_MISMATCH,
                    "Duplicate call ID '${event.callId.value}' used with a different fingerprint")
            }
            return Reduction.Rejected(session, error)
        }
        val entry = MutatingCallEntry(event.fingerprint, LedgerEntry.Reserved)
        val nextLedger = session.mutatingCalls.copy(entries = session.mutatingCalls.entries + (event.callId to entry))
        return accept(session.copy(mutatingCalls = nextLedger))
    }

    private fun handleMutatingCallCompleted(session: AgentSession, event: AgentStateEvent.MutatingCallCompleted): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        val existing = session.mutatingCalls.entries[event.callId]
            ?: return Reduction.Rejected(session, OperationError(
                ErrorCategory.TOOL_CALL, ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN,
                "Mutating call '${event.callId.value}' was not reserved",
            ))
        if (existing.state !is LedgerEntry.Reserved && existing.state !is LedgerEntry.EffectStarted) {
            return Reduction.Rejected(session, OperationError(
                ErrorCategory.TOOL_CALL, ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN,
                "Mutating call '${event.callId.value}' cannot be completed from ${existing.state::class.simpleName}",
            ))
        }
        val updatedLedger = session.mutatingCalls.recordCompleted(event.callId, event.result, event.resultingDigest)
        return accept(session.copy(mutatingCalls = updatedLedger))
    }

    private fun handleTransactionRevisionChanged(session: AgentSession, event: AgentStateEvent.TransactionRevisionChanged): Reduction {
        if (session.state !in setOf(AgentState.RUNNING, AgentState.WAITING_APPROVAL, AgentState.VERIFYING)) {
            return rejectInvalid(session, event)
        }
        if (event.newDigest.transactionId != session.transaction.transactionId ||
            event.newDigest.revision.value != session.transaction.revision.value + 1L) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.TRANSACTION_REVISION_MISMATCH,
                    safeMessage = "Transaction identity must be preserved and revision must advance by exactly 1",
                ),
            )
        }

        return accept(
            session.copy(
                state = AgentState.RUNNING,
                transaction = event.newDigest,
                verifiedTransaction = null,
                verificationAttempt = null,
                pendingApproval = null,
            )
        )
    }

    private fun handleApprovalRequested(session: AgentSession, event: AgentStateEvent.ApprovalRequested): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        if (session.pendingApproval != null) return rejectInvalid(session, event)
        return accept(
            session.copy(
                state = AgentState.WAITING_APPROVAL,
                pendingApproval = event.approval,
            )
        )
    }

    private fun handleExternalModificationDetected(session: AgentSession, event: AgentStateEvent.ExternalModificationDetected): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        return accept(
            session.copy(
                state = AgentState.WAITING_APPROVAL,
                pendingApproval = event.conflictApproval,
            )
        )
    }

    private fun handleApprovalResolved(session: AgentSession, event: AgentStateEvent.ApprovalResolved): Reduction {
        if (session.state != AgentState.WAITING_APPROVAL) return rejectInvalid(session, event)
        val pending = session.pendingApproval ?: return rejectInvalid(session, event)

        if (event.decision.requestId != pending.requestId) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.APPROVAL_ID_MISMATCH,
                    safeMessage = "Approval decision requestId '${event.decision.requestId.value}' does not match pending approval '${pending.requestId.value}'",
                ),
            )
        }
        if (event.decision.expectedSessionRevision != pending.expectedSessionRevision ||
            (pending.expectedTransactionDigest != null && pending.expectedTransactionDigest != session.transaction)
        ) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.APPROVAL,
                    code = ErrorCode.APPROVAL_STALE,
                    safeMessage = "Approval decision does not match the pending session or transaction revision guard",
                ),
            )
        }

        return accept(
            session.copy(
                state = AgentState.RUNNING,
                pendingApproval = null,
            )
        )
    }

    private fun handleVerificationStarted(session: AgentSession, event: AgentStateEvent.VerificationStarted): Reduction {
        if (session.state != AgentState.RUNNING) return rejectInvalid(session, event)
        if (event.frozenDigest != session.transaction) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.TRANSACTION_REVISION_MISMATCH,
                    safeMessage = "Verification started with frozen digest that does not match current transaction",
                ),
            )
        }
        if (session.verifiedTransaction != null && session.verifiedTransaction.transaction == session.transaction) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.VERIFIED_DIGEST_STALE,
                    safeMessage = "Current transaction digest is already verified",
                ),
            )
        }

        val attempt = VerificationAttempt(
            verificationRunId = event.verificationRunId,
            frozenDigest = event.frozenDigest,
            phase = VerificationPhase.IN_PROGRESS,
        )
        return accept(
            session.copy(
                state = AgentState.VERIFYING,
                verificationAttempt = attempt,
            )
        )
    }

    private fun handleVerificationSucceeded(session: AgentSession, event: AgentStateEvent.VerificationSucceeded): Reduction {
        if (session.state != AgentState.VERIFYING) return rejectInvalid(session, event)
        val attempt = session.verificationAttempt ?: return rejectInvalid(session, event)
        if (attempt.phase != VerificationPhase.IN_PROGRESS) return rejectInvalid(session, event)

        if (event.verificationRunId != attempt.verificationRunId) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.VERIFICATION_RUN_MISMATCH,
                    safeMessage = "Verification run ID mismatch",
                ),
            )
        }
        if (event.verifiedDigest.transaction != attempt.frozenDigest || event.verifiedDigest.transaction != session.transaction) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.TRANSACTION_REVISION_MISMATCH,
                    safeMessage = "Verified transaction digest does not match frozen or current digest",
                ),
            )
        }

        val updatedAttempt = attempt.copy(phase = VerificationPhase.PASSED_AWAITING_FINAL)
        return accept(
            session.copy(
                state = AgentState.VERIFYING,
                verificationAttempt = updatedAttempt,
                verifiedTransaction = event.verifiedDigest,
            )
        )
    }

    private fun handleVerificationFailed(session: AgentSession, event: AgentStateEvent.VerificationFailed): Reduction {
        if (session.state != AgentState.VERIFYING) return rejectInvalid(session, event)
        val attempt = session.verificationAttempt ?: return rejectInvalid(session, event)
        if (attempt.phase != VerificationPhase.IN_PROGRESS) return rejectInvalid(session, event)

        if (event.verificationRunId != attempt.verificationRunId) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.VERIFICATION_RUN_MISMATCH,
                    safeMessage = "Verification run ID mismatch",
                ),
            )
        }

        return when (val decision = session.limits.repairCycle.consumeCycle()) {
            is BudgetDecision.Allowed -> {
                val nextLimits = session.limits.copy(repairCycle = decision.nextBudget)
                val nextCounters = session.counters.copy(repairCyclesUsed = session.counters.repairCyclesUsed + 1)
                accept(
                    session.copy(
                        state = AgentState.RUNNING,
                        verificationAttempt = null,
                        limits = nextLimits,
                        counters = nextCounters,
                    )
                )
            }
            is BudgetDecision.Exhausted -> {
                val nextCounters = session.counters.copy(repairCyclesUsed = session.limits.repairCycle.maxCycles + 1)
                accept(
                    session.copy(
                        state = AgentState.FAILED,
                        verificationAttempt = null,
                        terminalReason = TerminalReason.Failed(event.error),
                        counters = nextCounters,
                    )
                )
            }
        }
    }

    private fun handleVerificationAborted(session: AgentSession, event: AgentStateEvent.VerificationAborted): Reduction {
        if (session.state != AgentState.VERIFYING) return rejectInvalid(session, event)
        val attempt = session.verificationAttempt ?: return rejectInvalid(session, event)
        if (attempt.phase != VerificationPhase.IN_PROGRESS) return rejectInvalid(session, event)

        if (event.verificationRunId != attempt.verificationRunId) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.VERIFICATION_RUN_MISMATCH,
                    safeMessage = "Verification run ID mismatch",
                ),
            )
        }

        return accept(
            session.copy(
                state = AgentState.RUNNING,
                verificationAttempt = null,
            )
        )
    }

    private fun handleWorkResumed(session: AgentSession, event: AgentStateEvent.WorkResumed): Reduction {
        if (session.state != AgentState.VERIFYING) return rejectInvalid(session, event)
        val attempt = session.verificationAttempt ?: return rejectInvalid(session, event)
        if (attempt.phase != VerificationPhase.PASSED_AWAITING_FINAL) return rejectInvalid(session, event)

        return accept(
            session.copy(
                state = AgentState.RUNNING,
                verificationAttempt = null,
            )
        )
    }

    private fun handleSessionCompleted(session: AgentSession, event: AgentStateEvent.SessionCompleted): Reduction {
        val inValidState = session.state == AgentState.RUNNING ||
            (session.state == AgentState.VERIFYING && session.verificationAttempt?.phase == VerificationPhase.PASSED_AWAITING_FINAL)
        if (!inValidState) return rejectInvalid(session, event)

        val verified = session.verifiedTransaction
        if (verified == null || verified.transaction != session.transaction) {
            return Reduction.Rejected(
                session,
                OperationError(
                    category = ErrorCategory.STATE,
                    code = ErrorCode.COMPLETION_NOT_VERIFIED,
                    safeMessage = "Cannot complete session without verified transaction matching current state",
                ),
            )
        }

        return accept(
            session.copy(
                state = AgentState.COMPLETED,
                verificationAttempt = null,
                terminalReason = TerminalReason.Completed(event.summary, verified),
            )
        )
    }

    private fun handleSessionFailed(session: AgentSession, event: AgentStateEvent.SessionFailed): Reduction {
        if (session.state !in setOf(AgentState.RUNNING, AgentState.VERIFYING)) {
            return rejectInvalid(session, event)
        }
        return accept(
            session.copy(
                state = AgentState.FAILED,
                verificationAttempt = null,
                terminalReason = TerminalReason.Failed(event.error),
            )
        )
    }

    private fun handleSessionCancelled(session: AgentSession, event: AgentStateEvent.SessionCancelled): Reduction {
        if (session.state !in setOf(AgentState.RUNNING, AgentState.WAITING_APPROVAL, AgentState.VERIFYING)) {
            return rejectInvalid(session, event)
        }
        return accept(
            session.copy(
                state = AgentState.CANCELLED,
                pendingApproval = null,
                verificationAttempt = null,
                terminalReason = TerminalReason.Cancelled(event.reason),
            )
        )
    }
}
