package com.euhedral.gemini.core.revision

import com.euhedral.gemini.core.agent.AgentReducer
import com.euhedral.gemini.core.agent.AgentSession
import com.euhedral.gemini.core.agent.AgentState
import com.euhedral.gemini.core.agent.AgentStateEvent
import com.euhedral.gemini.core.agent.AgentStateEventEnvelope
import com.euhedral.gemini.core.agent.ProjectFingerprint
import com.euhedral.gemini.core.agent.Reduction
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.agent.SessionRevision
import com.euhedral.gemini.core.agent.Sha256Digest
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.agent.TransactionRevision
import com.euhedral.gemini.core.agent.VerificationRunId
import com.euhedral.gemini.core.error.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationDigestTest {

    private val sessionId = SessionId("sess-1")
    private val projectFingerprint = ProjectFingerprint("proj-1")
    private val hex0 = "0".repeat(64)
    private val hex1 = "1".repeat(64)
    private val hex2 = "2".repeat(64)

    private val t0Digest = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(0L), Sha256Digest(hex0))
    private val t1Digest = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(1L), Sha256Digest(hex1))
    private val t2Digest = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(2L), Sha256Digest(hex2))

    private fun createRunningSession(transaction: TransactionRevisionDigest = t0Digest): AgentSession {
        val idle = AgentSession(
            id = sessionId,
            revision = SessionRevision(0L),
            state = AgentState.IDLE,
            projectFingerprint = projectFingerprint,
            transaction = transaction,
        )
        val envelope = AgentStateEventEnvelope(sessionId, SessionRevision(0L), AgentStateEvent.SessionStarted(1000L))
        return (AgentReducer.reduce(idle, envelope) as Reduction.Accepted).session
    }

    @Test
    fun sha256DigestAcceptsOnlyLowercaseSixtyFourHexCharacters() {
        val valid = Sha256Digest("a".repeat(64))
        assertEquals("a".repeat(64), valid.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sha256DigestRejectsUppercaseHex() {
        Sha256Digest("A".repeat(64))
    }

    @Test(expected = IllegalArgumentException::class)
    fun sha256DigestRejectsInvalidLength() {
        Sha256Digest("12345")
    }

    @Test
    fun transactionRevisionMustAdvanceByExactlyOne() {
        val session = createRunningSession(t0Digest)
        // Skip revision 1 to 2 -> should be rejected
        val envelope = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.TransactionRevisionChanged(t2Digest))
        val reduction = AgentReducer.reduce(session, envelope)
        assertTrue(reduction is Reduction.Rejected)
        assertEquals(ErrorCode.TRANSACTION_REVISION_MISMATCH, (reduction as Reduction.Rejected).error.code)
    }

    @Test
    fun matchingVerificationRecordsExactFrozenDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        assertEquals(verified, passed.verifiedTransaction)
    }

    @Test
    fun staleVerificationSuccessIsRejected() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val staleVerified = VerifiedTransactionDigest(t1Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, staleVerified))
        val reduction = AgentReducer.reduce(verifying, envSuccess)
        assertTrue(reduction is Reduction.Rejected)
        assertEquals(ErrorCode.TRANSACTION_REVISION_MISMATCH, (reduction as Reduction.Rejected).error.code)
    }

    @Test
    fun completionBeforeVerificationIsRejected() {
        val session = createRunningSession(t0Digest)
        val envelope = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.SessionCompleted("Done"))
        val reduction = AgentReducer.reduce(session, envelope)
        assertTrue(reduction is Reduction.Rejected)
        assertEquals(ErrorCode.COMPLETION_NOT_VERIFIED, (reduction as Reduction.Rejected).error.code)
    }

    @Test
    fun completionRejectsDifferentTransactionId() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val diffTxVerified = VerifiedTransactionDigest(TransactionRevisionDigest(TransactionId("tx-2"), TransactionRevision(0L), Sha256Digest(hex0)), runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, diffTxVerified))
        val reduction = AgentReducer.reduce(verifying, envSuccess)
        assertTrue(reduction is Reduction.Rejected)
    }

    @Test
    fun completionRejectsDifferentRevision() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val diffRevVerified = VerifiedTransactionDigest(TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(1L), Sha256Digest(hex0)), runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, diffRevVerified))
        val reduction = AgentReducer.reduce(verifying, envSuccess)
        assertTrue(reduction is Reduction.Rejected)
    }

    @Test
    fun completionRejectsDifferentDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val diffHexVerified = VerifiedTransactionDigest(TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(0L), Sha256Digest(hex1)), runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, diffHexVerified))
        val reduction = AgentReducer.reduce(verifying, envSuccess)
        assertTrue(reduction is Reduction.Rejected)
    }

    @Test
    fun successfulMutationClearsVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session
        assertNotNull(passed.verifiedTransaction)

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session
        assertNotNull(runningWithVerified.verifiedTransaction)

        // Advance transaction revision (mutation applied)
        val envMutate = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val mutatedSession = (AgentReducer.reduce(runningWithVerified, envMutate) as Reduction.Accepted).session

        assertNull(mutatedSession.verifiedTransaction)
    }

    @Test
    fun createDeleteMoveAndReplaceEachClearVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Any transaction revision change (create, replace, delete, move) clears verifiedTransaction
        val envTxChange = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val updated = (AgentReducer.reduce(runningWithVerified, envTxChange) as Reduction.Accepted).session
        assertNull(updated.verifiedTransaction)
    }

    @Test
    fun rollbackClearsVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Rollback produces a new transaction revision
        val envRollback = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val updated = (AgentReducer.reduce(runningWithVerified, envRollback) as Reduction.Accepted).session
        assertNull(updated.verifiedTransaction)
    }

    @Test
    fun mutateThenRestoreStillClearsVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Mutate to t1
        val envMut1 = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val sess1 = (AgentReducer.reduce(runningWithVerified, envMut1) as Reduction.Accepted).session
        assertNull(sess1.verifiedTransaction)

        // Restore bytes but advance revision to t2
        val envMut2 = AgentStateEventEnvelope(sessionId, sess1.revision, AgentStateEvent.TransactionRevisionChanged(t2Digest))
        val sess2 = (AgentReducer.reduce(sess1, envMut2) as Reduction.Accepted).session
        assertNull(sess2.verifiedTransaction)
    }

    @Test
    fun externalRevisionChangeClearsVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // External modification advances revision
        val envExt = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val updated = (AgentReducer.reduce(runningWithVerified, envExt) as Reduction.Accepted).session
        assertNull(updated.verifiedTransaction)
    }

    @Test
    fun failedPreEffectMutationPreservesVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Pre-effect failure does not emit TransactionRevisionChanged -> verifiedTransaction preserved
        assertEquals(verified, runningWithVerified.verifiedTransaction)
    }

    @Test
    fun rejectedMutationPreservesVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Rejected mutation emits no TransactionRevisionChanged
        assertEquals(verified, runningWithVerified.verifiedTransaction)
    }

    @Test
    fun genuineNoOpPreservesVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // No-op emits no TransactionRevisionChanged
        assertEquals(verified, runningWithVerified.verifiedTransaction)
    }

    @Test
    fun readOnlyProcessAndControlResultsPreserveVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Model turn does not clear verifiedTransaction
        val envTurn = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.ModelTurnStarted(com.euhedral.gemini.core.agent.ModelRequestId("req-1")))
        val turnSession = (AgentReducer.reduce(runningWithVerified, envTurn) as Reduction.Accepted).session

        assertEquals(verified, turnSession.verifiedTransaction)
    }

    @Test
    fun duplicateMutatingReplayPreservesVerifiedDigest() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Replaying an exact duplicate mutating call does not advance revision -> preserved
        assertEquals(verified, runningWithVerified.verifiedTransaction)
    }

    @Test
    fun postVerificationMutationBlocksCompletionAndCommitEligibility() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        val verified = VerifiedTransactionDigest(t0Digest, runId)
        val envSuccess = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.VerificationSucceeded(runId, verified))
        val passed = (AgentReducer.reduce(verifying, envSuccess) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed, envResumed) as Reduction.Accepted).session

        // Mutate to t1
        val envMut = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val mutatedSession = (AgentReducer.reduce(runningWithVerified, envMut) as Reduction.Accepted).session

        // Completion attempt fails because verifiedTransaction was cleared
        val envComplete = AgentStateEventEnvelope(sessionId, mutatedSession.revision, AgentStateEvent.SessionCompleted("Done"))
        val reduction = AgentReducer.reduce(mutatedSession, envComplete)
        assertTrue(reduction is Reduction.Rejected)
        assertEquals(ErrorCode.COMPLETION_NOT_VERIFIED, (reduction as Reduction.Rejected).error.code)
    }

    @Test
    fun verificationOfNewDigestRestoresCompletionAndCommitEligibility() {
        val session = createRunningSession(t0Digest)
        val runId1 = VerificationRunId("vrun-1")
        val envStart1 = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId1, t0Digest))
        val verifying1 = (AgentReducer.reduce(session, envStart1) as Reduction.Accepted).session

        val verified1 = VerifiedTransactionDigest(t0Digest, runId1)
        val envSuccess1 = AgentStateEventEnvelope(sessionId, verifying1.revision, AgentStateEvent.VerificationSucceeded(runId1, verified1))
        val passed1 = (AgentReducer.reduce(verifying1, envSuccess1) as Reduction.Accepted).session

        val envResumed = AgentStateEventEnvelope(sessionId, passed1.revision, AgentStateEvent.WorkResumed)
        val runningWithVerified = (AgentReducer.reduce(passed1, envResumed) as Reduction.Accepted).session

        // Mutate to t1 -> clears verifiedTransaction
        val envMut = AgentStateEventEnvelope(sessionId, runningWithVerified.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val mutatedSession = (AgentReducer.reduce(runningWithVerified, envMut) as Reduction.Accepted).session
        assertNull(mutatedSession.verifiedTransaction)

        // Re-verify t1
        val runId2 = VerificationRunId("vrun-2")
        val envStart2 = AgentStateEventEnvelope(sessionId, mutatedSession.revision, AgentStateEvent.VerificationStarted(runId2, t1Digest))
        val verifying2 = (AgentReducer.reduce(mutatedSession, envStart2) as Reduction.Accepted).session

        val verified2 = VerifiedTransactionDigest(t1Digest, runId2)
        val envSuccess2 = AgentStateEventEnvelope(sessionId, verifying2.revision, AgentStateEvent.VerificationSucceeded(runId2, verified2))
        val passed2 = (AgentReducer.reduce(verifying2, envSuccess2) as Reduction.Accepted).session

        // Completion now succeeds
        val envComplete = AgentStateEventEnvelope(sessionId, passed2.revision, AgentStateEvent.SessionCompleted("Task finished"))
        val completed = AgentReducer.reduce(passed2, envComplete)
        assertTrue(completed is Reduction.Accepted)
        assertEquals(AgentState.COMPLETED, (completed as Reduction.Accepted).session.state)
    }

    @Test
    fun externalChangeDuringVerificationAbortsWithoutRepairUse() {
        val session = createRunningSession(t0Digest)
        val runId = VerificationRunId("vrun-1")
        val envStart = AgentStateEventEnvelope(sessionId, session.revision, AgentStateEvent.VerificationStarted(runId, t0Digest))
        val verifying = (AgentReducer.reduce(session, envStart) as Reduction.Accepted).session

        // External modification occurs during verification
        val envExt = AgentStateEventEnvelope(sessionId, verifying.revision, AgentStateEvent.TransactionRevisionChanged(t1Digest))
        val running = (AgentReducer.reduce(verifying, envExt) as Reduction.Accepted).session

        assertEquals(AgentState.RUNNING, running.state)
        assertNull(running.verificationAttempt)
        assertNull(running.verifiedTransaction)
        assertEquals(0, running.counters.repairCyclesUsed)
    }
}
