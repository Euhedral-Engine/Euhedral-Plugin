package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.ErrorCategory
import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.InvalidTransitionError
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.revision.VerifiedTransactionDigest
import com.euhedral.gemini.core.tools.ToolEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentReducerTest {

    private val sessionId = SessionId("sess-1")
    private val projectFingerprint = ProjectFingerprint("proj-1")
    private val t0Digest = Sha256Digest("0".repeat(64))
    private val t1Digest = Sha256Digest("1".repeat(64))
    private val t2Digest = Sha256Digest("2".repeat(64))
    private val initialTransaction = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(0L), t0Digest)
    private val nextTransaction = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(1L), t1Digest)

    private fun createInitialSession(): AgentSession {
        return AgentSession(
            id = sessionId,
            revision = SessionRevision(0L),
            state = AgentState.IDLE,
            projectFingerprint = projectFingerprint,
            transaction = initialTransaction,
        )
    }

    private fun createRunningSession(): AgentSession {
        val idle = createInitialSession()
        val envelope = AgentStateEventEnvelope(sessionId, SessionRevision(0L), AgentStateEvent.SessionStarted(1000L))
        val reduction = AgentReducer.reduce(idle, envelope) as Reduction.Accepted
        return reduction.session
    }

    private fun createWaitingApprovalSession(): Pair<AgentSession, PendingApproval> {
        val running = createRunningSession()
        val approval = PendingApproval(
            requestId = ApprovalRequestId("app-1"),
            effect = ToolEffect.MUTATING,
            actionSummary = "replace text",
            targetSummary = "src/Main.kt",
            policyReasonCode = "REQUIRE_MUTATION_APPROVAL",
            expectedSessionRevision = running.revision,
        )
        val envelope = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.ApprovalRequested(approval))
        val reduction = AgentReducer.reduce(running, envelope) as Reduction.Accepted
        return Pair(reduction.session, approval)
    }

    private fun createVerifyingSession(): Pair<AgentSession, VerificationRunId> {
        val running = createRunningSession()
        val runId = VerificationRunId("vrun-1")
        val envelope = AgentStateEventEnvelope(
            sessionId,
            running.revision,
            AgentStateEvent.VerificationStarted(runId, initialTransaction),
        )
        val reduction = AgentReducer.reduce(running, envelope) as Reduction.Accepted
        return Pair(reduction.session, runId)
    }

    private fun createPassedAwaitingFinalSession(): Pair<AgentSession, VerifiedTransactionDigest> {
        val (verifying, runId) = createVerifyingSession()
        val verifiedDigest = VerifiedTransactionDigest(initialTransaction, runId)
        val envelope = AgentStateEventEnvelope(
            sessionId,
            verifying.revision,
            AgentStateEvent.VerificationSucceeded(runId, verifiedDigest),
        )
        val reduction = AgentReducer.reduce(verifying, envelope) as Reduction.Accepted
        return Pair(reduction.session, verifiedDigest)
    }

    @Test
    fun sessionStartedMovesIdleToRunning() {
        val session = createInitialSession()
        val envelope = AgentStateEventEnvelope(sessionId, SessionRevision(0L), AgentStateEvent.SessionStarted(1000L))
        val reduction = AgentReducer.reduce(session, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertEquals(1000L, newSession.counters.startEpochMillis)
        assertEquals(SessionRevision(1L), newSession.revision)
    }

    @Test
    fun approvalRequestMovesRunningToWaitingApproval() {
        val running = createRunningSession()
        val approval = PendingApproval(
            requestId = ApprovalRequestId("app-1"),
            effect = ToolEffect.MUTATING,
            actionSummary = "replace text",
            targetSummary = "src/Main.kt",
            policyReasonCode = "REQUIRE_MUTATION_APPROVAL",
            expectedSessionRevision = running.revision,
        )
        val envelope = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.ApprovalRequested(approval))
        val reduction = AgentReducer.reduce(running, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.WAITING_APPROVAL, newSession.state)
        assertEquals(approval, newSession.pendingApproval)
    }

    @Test
    fun approvalGrantMovesWaitingApprovalToRunning() {
        val (waiting, approval) = createWaitingApprovalSession()
        val decision = ApprovalDecision(approval.requestId, ApprovalDecisionStatus.GRANTED, approval.expectedSessionRevision)
        val envelope = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.ApprovalResolved(decision))
        val reduction = AgentReducer.reduce(waiting, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.pendingApproval)
    }

    @Test
    fun approvalDenialMovesWaitingApprovalToRunning() {
        val (waiting, approval) = createWaitingApprovalSession()
        val decision = ApprovalDecision(approval.requestId, ApprovalDecisionStatus.DENIED, approval.expectedSessionRevision)
        val envelope = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.ApprovalResolved(decision))
        val reduction = AgentReducer.reduce(waiting, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.pendingApproval)
    }

    @Test
    fun staleApprovalMovesWaitingApprovalToRunningWithoutAuthorization() {
        val (waiting, approval) = createWaitingApprovalSession()
        val decision = ApprovalDecision(approval.requestId, ApprovalDecisionStatus.STALE, approval.expectedSessionRevision)
        val envelope = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.ApprovalResolved(decision))
        val reduction = AgentReducer.reduce(waiting, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.pendingApproval)
    }

    @Test
    fun verificationStartedMovesRunningToVerifyingInProgress() {
        val running = createRunningSession()
        val runId = VerificationRunId("vrun-1")
        val envelope = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.VerificationStarted(runId, initialTransaction))
        val reduction = AgentReducer.reduce(running, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.VERIFYING, newSession.state)
        assertNotNull(newSession.verificationAttempt)
        assertEquals(VerificationPhase.IN_PROGRESS, newSession.verificationAttempt?.phase)
        assertEquals(runId, newSession.verificationAttempt?.verificationRunId)
        assertEquals(initialTransaction, newSession.verificationAttempt?.frozenDigest)
    }

    @Test
    fun verificationSuccessRemainsVerifyingAndAwaitsFinal() {
        val (verifying, runId) = createVerifyingSession()
        val verifiedDigest = VerifiedTransactionDigest(initialTransaction, runId)
        val envelope = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verifiedDigest))
        val reduction = AgentReducer.reduce(verifying, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.VERIFYING, newSession.state)
        assertEquals(VerificationPhase.PASSED_AWAITING_FINAL, newSession.verificationAttempt!!.phase)
        assertEquals(verifiedDigest, newSession.verifiedTransaction)
    }

    @Test
    fun verifiedFinalMovesVerifyingToCompleted() {
        val (passedSession, verifiedDigest) = createPassedAwaitingFinalSession()
        val envelope = AgentStateEventEnvelope(sessionId, passedSession.revision, AgentStateEvent.SessionCompleted("Task complete"))
        val reduction = AgentReducer.reduce(passedSession, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.COMPLETED, newSession.state)
        assertTrue(newSession.terminalReason is TerminalReason.Completed)
        assertEquals("Task complete", (newSession.terminalReason as TerminalReason.Completed).summary)
    }

    @Test
    fun workAfterVerificationMovesVerifyingToRunning() {
        val (passedSession, _) = createPassedAwaitingFinalSession()
        val envelope = AgentStateEventEnvelope(sessionId, passedSession.revision, AgentStateEvent.WorkResumed)
        val reduction = AgentReducer.reduce(passedSession, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.verificationAttempt)
        assertNotNull(newSession.verifiedTransaction)
    }

    @Test
    fun currentVerifiedFinalMovesRunningToCompleted() {
        val (passedSession, _) = createPassedAwaitingFinalSession()
        // Resume work to RUNNING with verifiedTransaction present
        val resumeEnvelope = AgentStateEventEnvelope(sessionId, passedSession.revision, AgentStateEvent.WorkResumed)
        val runningSession = (AgentReducer.reduce(passedSession, resumeEnvelope) as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, runningSession.state)
        assertNotNull(runningSession.verifiedTransaction)

        val completeEnvelope = AgentStateEventEnvelope(sessionId, runningSession.revision, AgentStateEvent.SessionCompleted("Task complete"))
        val reduction = AgentReducer.reduce(runningSession, completeEnvelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.COMPLETED, newSession.state)
    }

    @Test
    fun verificationFailureMovesVerifyingToRunningWhenRepairAvailable() {
        val (verifying, runId) = createVerifyingSession()
        val error = OperationError(ErrorCategory.BUILD, ErrorCode.BUILD_FAILED, "Build failed")
        val envelope = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationFailed(runId, error))
        val reduction = AgentReducer.reduce(verifying, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.verificationAttempt)
        assertEquals(1, newSession.counters.repairCyclesUsed)
    }

    @Test
    fun sixthVerificationFailureMovesVerifyingToFailed() {
        var (verifying, runId) = createVerifyingSession()
        val error = OperationError(ErrorCategory.BUILD, ErrorCode.BUILD_FAILED, "Build failed")

        // Exhaust 5 repair cycles
        for (i in 1..5) {
            val envFail = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationFailed(runId, error))
            val running = (AgentReducer.reduce(verifying, envFail) as Reduction.Accepted).session
            assertEquals(AgentState.RUNNING, running.state)

            val newRunId = VerificationRunId("vrun-${i + 1}")
            val envStart = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.VerificationStarted(newRunId, initialTransaction))
            verifying = (AgentReducer.reduce(running, envStart) as Reduction.Accepted).session
            runId = newRunId
        }

        // 6th verification failure
        val envFail6 = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationFailed(runId, error))
        val reduction = AgentReducer.reduce(verifying, envFail6)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.FAILED, newSession.state)
        assertTrue(newSession.terminalReason is TerminalReason.Failed)
    }

    @Test
    fun verificationAbortMovesVerifyingToRunningWithoutRepairUse() {
        val (verifying, runId) = createVerifyingSession()
        val envelope = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationAborted(runId))
        val reduction = AgentReducer.reduce(verifying, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.verificationAttempt)
        assertEquals(0, newSession.counters.repairCyclesUsed)
    }

    @Test
    fun explicitFailureMovesRunningToFailed() {
        val running = createRunningSession()
        val error = OperationError(ErrorCategory.TRANSPORT, ErrorCode.TRANSPORT_UNAVAILABLE, "Transport dead")
        val envelope = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.SessionFailed(error))
        val reduction = AgentReducer.reduce(running, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.FAILED, newSession.state)
        assertEquals(TerminalReason.Failed(error), newSession.terminalReason)
    }

    @Test
    fun explicitFailureMovesVerifyingToFailed() {
        val (verifying, _) = createVerifyingSession()
        val error = OperationError(ErrorCategory.TRANSPORT, ErrorCode.TRANSPORT_UNAVAILABLE, "Transport dead")
        val envelope = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.SessionFailed(error))
        val reduction = AgentReducer.reduce(verifying, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.FAILED, newSession.state)
        assertEquals(TerminalReason.Failed(error), newSession.terminalReason)
    }

    @Test
    fun cancelMovesEveryActiveStateToCancelled() {
        val running = createRunningSession()
        val (waiting, _) = createWaitingApprovalSession()
        val (verifying, _) = createVerifyingSession()

        val activeSessions = listOf(running, waiting, verifying)
        for (sess in activeSessions) {
            val env = AgentStateEventEnvelope(sessionId, sess.revision, AgentStateEvent.SessionCancelled(CancellationReason.USER_REQUESTED))
            val red = AgentReducer.reduce(sess, env)
            assertTrue(red is Reduction.Accepted)
            val newSession = (red as Reduction.Accepted).session
            assertEquals(AgentState.CANCELLED, newSession.state)
            assertEquals(TerminalReason.Cancelled(CancellationReason.USER_REQUESTED), newSession.terminalReason)
        }
    }

    @Test
    fun transactionChangeMovesWaitingApprovalToRunningAndStalesApproval() {
        val (waiting, _) = createWaitingApprovalSession()
        val envelope = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.TransactionRevisionChanged(nextTransaction))
        val reduction = AgentReducer.reduce(waiting, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.pendingApproval)
        assertEquals(nextTransaction, newSession.transaction)
    }

    @Test
    fun transactionChangeMovesVerifyingToRunningAndAbortsVerification() {
        val (verifying, _) = createVerifyingSession()
        val envelope = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.TransactionRevisionChanged(nextTransaction))
        val reduction = AgentReducer.reduce(verifying, envelope)
        assertTrue(reduction is Reduction.Accepted)
        val newSession = (reduction as Reduction.Accepted).session
        assertEquals(AgentState.RUNNING, newSession.state)
        assertNull(newSession.verificationAttempt)
        assertNull(newSession.verifiedTransaction)
        assertEquals(nextTransaction, newSession.transaction)
    }

    @Test
    fun allUnlistedStateEventPairsAreRejected() {
        val running = createRunningSession()
        // SessionStarted is invalid from RUNNING
        val env = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.SessionStarted(2000L))
        val red = AgentReducer.reduce(running, env)
        assertTrue(red is Reduction.Rejected)
        assertTrue((red as Reduction.Rejected).error is InvalidTransitionError)
    }

    @Test
    fun everyTerminalStateRejectsEveryStateEvent() {
        val running = createRunningSession()
        val cancelEnv = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.SessionCancelled(CancellationReason.USER_REQUESTED))
        val cancelled = (AgentReducer.reduce(running, cancelEnv) as Reduction.Accepted).session
        assertEquals(AgentState.CANCELLED, cancelled.state)

        val anyEnv = AgentStateEventEnvelope(sessionId, cancelled.revision, AgentStateEvent.SessionStarted(3000L))
        val red = AgentReducer.reduce(cancelled, anyEnv)
        assertTrue(red is Reduction.Rejected)
        assertTrue((red as Reduction.Rejected).error is InvalidTransitionError)
    }

    @Test
    fun idleRejectsFailureAndCancellation() {
        val idle = createInitialSession()

        val failEnv = AgentStateEventEnvelope(sessionId, idle.revision, AgentStateEvent.SessionFailed(OperationError(ErrorCategory.STATE, ErrorCode.INVALID_TRANSITION, "fail")))
        val redFail = AgentReducer.reduce(idle, failEnv)
        assertTrue(redFail is Reduction.Rejected)

        val cancelEnv = AgentStateEventEnvelope(sessionId, idle.revision, AgentStateEvent.SessionCancelled(CancellationReason.USER_REQUESTED))
        val redCancel = AgentReducer.reduce(idle, cancelEnv)
        assertTrue(redCancel is Reduction.Rejected)
    }

    @Test
    fun rejectionReturnsTheExactUnchangedSession() {
        val running = createRunningSession()
        val invalidEnv = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.SessionStarted(2000L))
        val reduction = AgentReducer.reduce(running, invalidEnv) as Reduction.Rejected
        assertEquals(running, reduction.unchangedSession)
    }

    @Test
    fun acceptedEventIncrementsSessionRevisionExactlyOnce() {
        val running = createRunningSession()
        val initialRev = running.revision
        val env = AgentStateEventEnvelope(sessionId, initialRev, AgentStateEvent.ModelTurnStarted(ModelRequestId("req-1")))
        val reduction = AgentReducer.reduce(running, env) as Reduction.Accepted
        assertEquals(SessionRevision(initialRev.value + 1L), reduction.session.revision)
    }

    @Test
    fun foreignSessionEventIsRejected() {
        val running = createRunningSession()
        val foreignSessionId = SessionId("foreign-sess")
        val env = AgentStateEventEnvelope(foreignSessionId, running.revision, AgentStateEvent.ModelTurnStarted(ModelRequestId("req-1")))
        val reduction = AgentReducer.reduce(running, env) as Reduction.Rejected
        assertEquals(ErrorCode.SESSION_ID_MISMATCH, reduction.error.code)
    }

    @Test
    fun staleExpectedRevisionIsRejected() {
        val running = createRunningSession()
        val staleRev = SessionRevision(99L)
        val env = AgentStateEventEnvelope(sessionId, staleRev, AgentStateEvent.ModelTurnStarted(ModelRequestId("req-1")))
        val reduction = AgentReducer.reduce(running, env) as Reduction.Rejected
        assertEquals(ErrorCode.STALE_SESSION_EVENT, reduction.error.code)
    }

    @Test
    fun approvalCorrelationMismatchIsRejected() {
        val (waiting, _) = createWaitingApprovalSession()
        val foreignDecision = ApprovalDecision(ApprovalRequestId("wrong-app-id"), ApprovalDecisionStatus.GRANTED, waiting.revision)
        val env = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.ApprovalResolved(foreignDecision))
        val reduction = AgentReducer.reduce(waiting, env) as Reduction.Rejected
        assertEquals(ErrorCode.APPROVAL_ID_MISMATCH, reduction.error.code)
    }

    @Test
    fun approvalWithStaleRevisionGuardIsRejectedWithoutChangingSession() {
        val (waiting, approval) = createWaitingApprovalSession()
        val staleDecision = ApprovalDecision(approval.requestId, ApprovalDecisionStatus.GRANTED, SessionRevision(0L))
        val env = AgentStateEventEnvelope(sessionId, waiting.revision, AgentStateEvent.ApprovalResolved(staleDecision))
        val reduction = AgentReducer.reduce(waiting, env) as Reduction.Rejected
        assertEquals(ErrorCode.APPROVAL_STALE, reduction.error.code)
        assertEquals(waiting, reduction.unchangedSession)
    }

    @Test
    fun processTimersAreIndependentAndSessionTimerCountsWhileWaitingForApproval() {
        val running = createRunningSession().copy(limits = AgentLimits(
            processTimeout = ProcessTimeoutBudget(100L), sessionTime = SessionTimeBudget(200L),
        ))
        val first = AgentReducer.reduce(running, AgentStateEventEnvelope(
            sessionId, running.revision, AgentStateEvent.ProcessTimingObserved(ProcessInvocationId("process-a"), 99L),
        )) as Reduction.Accepted
        val second = AgentReducer.reduce(first.session, AgentStateEventEnvelope(
            sessionId, first.session.revision, AgentStateEvent.ProcessTimingObserved(ProcessInvocationId("process-b"), 0L),
        )) as Reduction.Accepted
        assertEquals(99L, second.session.counters.processElapsedMillis[ProcessInvocationId("process-a")])
        assertEquals(0L, second.session.counters.processElapsedMillis[ProcessInvocationId("process-b")])

        val approval = PendingApproval(ApprovalRequestId("time-app"), effect = ToolEffect.MUTATING,
            actionSummary = "replace", targetSummary = "file", policyReasonCode = "APPROVAL",
            expectedSessionRevision = second.session.revision)
        val waiting = (AgentReducer.reduce(second.session, AgentStateEventEnvelope(
            sessionId, second.session.revision, AgentStateEvent.ApprovalRequested(approval),
        )) as Reduction.Accepted).session
        val timedOut = AgentReducer.reduce(waiting, AgentStateEventEnvelope(
            sessionId, waiting.revision, AgentStateEvent.SessionTimingObserved(200L),
        )) as Reduction.Accepted
        assertEquals(AgentState.CANCELLED, timedOut.session.state)
        assertEquals(CancellationReason.SESSION_TIMEOUT_WHILE_WAITING,
            (timedOut.session.terminalReason as TerminalReason.Cancelled).cancellationReason)
    }

    @Test
    fun reducerRejectsDuplicateMutationReservationAndUnknownCompletion() {
        val running = createRunningSession()
        val callId = ToolCallId("mutation-1")
        val fingerprint = ToolInvocationFingerprint("a".repeat(64))
        val reserved = (AgentReducer.reduce(running, AgentStateEventEnvelope(
            sessionId, running.revision, AgentStateEvent.MutatingCallReserved(callId, fingerprint),
        )) as Reduction.Accepted).session
        val duplicate = AgentReducer.reduce(reserved, AgentStateEventEnvelope(
            sessionId, reserved.revision, AgentStateEvent.MutatingCallReserved(callId, fingerprint),
        )) as Reduction.Rejected
        assertEquals(reserved, duplicate.unchangedSession)
        val result = com.euhedral.gemini.core.tools.ToolResult(callId, ToolName("create_file"),
            com.euhedral.gemini.core.tools.ToolOutcome.Success(com.euhedral.gemini.core.tools.ToolValue.StringValue("ok")),
            com.euhedral.gemini.core.tools.BoundedOutputMetadata(false, 1, returnedCharacters = 2))
        val unknown = AgentReducer.reduce(running, AgentStateEventEnvelope(
            sessionId, running.revision, AgentStateEvent.MutatingCallCompleted(callId, result, null),
        )) as Reduction.Rejected
        assertEquals(ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN, unknown.error.code)
    }

    @Test
    fun transactionRevisionCannotChangeTransactionIdentity() {
        val running = createRunningSession()
        val foreign = nextTransaction.copy(transactionId = TransactionId("tx-foreign"))
        val reduction = AgentReducer.reduce(running, AgentStateEventEnvelope(
            sessionId, running.revision, AgentStateEvent.TransactionRevisionChanged(foreign),
        )) as Reduction.Rejected
        assertEquals(ErrorCode.TRANSACTION_REVISION_MISMATCH, reduction.error.code)
    }

    @Test
    fun finalModelResponseCanBeRecordedAfterVerificationPasses() {
        val (passed, _) = createPassedAwaitingFinalSession()
        val turn = (AgentReducer.reduce(passed, AgentStateEventEnvelope(
            sessionId, passed.revision, AgentStateEvent.ModelTurnStarted(ModelRequestId("final-turn")),
        )) as Reduction.Accepted).session
        val recorded = AgentReducer.reduce(turn, AgentStateEventEnvelope(
            sessionId, turn.revision, AgentStateEvent.InteractionRecorded(ModelRequestId("final-turn"), InteractionId("final-interaction")),
        )) as Reduction.Accepted
        assertEquals(AgentState.VERIFYING, recorded.session.state)
    }

    @Test
    fun verificationRunMismatchIsRejected() {
        val (verifying, _) = createVerifyingSession()
        val wrongRunId = VerificationRunId("wrong-run-id")
        val verifiedDigest = VerifiedTransactionDigest(initialTransaction, wrongRunId)
        val env = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(wrongRunId, verifiedDigest))
        val reduction = AgentReducer.reduce(verifying, env) as Reduction.Rejected
        assertEquals(ErrorCode.VERIFICATION_RUN_MISMATCH, reduction.error.code)
    }

    @Test
    fun terminalReasonExistsIfAndOnlyIfTerminal() {
        val idle = createInitialSession()
        val running = createRunningSession()
        val (waiting, _) = createWaitingApprovalSession()
        val (verifying, _) = createVerifyingSession()

        assertNull(idle.terminalReason)
        assertNull(running.terminalReason)
        assertNull(waiting.terminalReason)
        assertNull(verifying.terminalReason)

        val cancelEnv = AgentStateEventEnvelope(sessionId, running.revision, AgentStateEvent.SessionCancelled(CancellationReason.USER_REQUESTED))
        val cancelled = (AgentReducer.reduce(running, cancelEnv) as Reduction.Accepted).session
        assertNotNull(cancelled.terminalReason)
    }
}
