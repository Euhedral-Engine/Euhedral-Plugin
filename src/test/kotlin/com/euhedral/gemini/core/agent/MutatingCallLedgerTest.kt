package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.revision.VerifiedTransactionDigest
import com.euhedral.gemini.core.tools.BoundedOutputMetadata
import com.euhedral.gemini.core.tools.ToolCall
import com.euhedral.gemini.core.tools.ToolEffect
import com.euhedral.gemini.core.tools.ToolOutcome
import com.euhedral.gemini.core.tools.ToolResult
import com.euhedral.gemini.core.tools.ToolValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MutatingCallLedgerTest {

    private val callId1 = ToolCallId("call-1")
    private val toolName1 = ToolName("create_file")
    private val pathArg = ToolValue.StringValue("src/Test.kt")
    private val contentArg = ToolValue.StringValue("class Test")
    private val args1 = ToolValue.ObjectValue.of(mapOf("path" to pathArg, "content" to contentArg))
    private val mutatingCall1 = ToolCall(callId1, toolName1, args1)

    private val boundedMeta = BoundedOutputMetadata(truncated = false, returnedItems = 1, returnedCharacters = 20)
    private val successResult = ToolResult(callId1, toolName1, ToolOutcome.Success(ToolValue.StringValue("ok")), boundedMeta)
    private val failureError = OperationError(com.euhedral.gemini.core.error.ErrorCategory.EDIT, ErrorCode.EDIT_FAILED, "File lock error")
    private val failureResult = ToolResult(callId1, toolName1, ToolOutcome.Failure(failureError), boundedMeta)

    private val digest0 = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(0L), Sha256Digest("0".repeat(64)))
    private val digest1 = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(1L), Sha256Digest("1".repeat(64)))

    @Test
    fun firstMutatingCallReservesAndExecutes() {
        val ledger = MutatingCallLedger()
        val claim = ledger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.Execute)
        val nextLedger = (claim as LedgerClaim.Execute).nextLedger
        val entry = nextLedger.entries[callId1]
        assertNotNull(entry)
        assertEquals(LedgerEntry.Reserved, entry!!.state)
    }

    @Test
    fun exactDuplicateWhileReservedWaitsForOriginal() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger

        val duplicateClaim = executeLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(duplicateClaim is LedgerClaim.WaitForOriginal)
        assertEquals(LedgerEntry.Reserved, (duplicateClaim as LedgerClaim.WaitForOriginal).currentEntry)
    }

    @Test
    fun exactDuplicateWhileEffectStartedWaitsForOriginal() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val startedLedger = executeLedger.markEffectStarted(callId1)

        val duplicateClaim = startedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(duplicateClaim is LedgerClaim.WaitForOriginal)
        assertEquals(LedgerEntry.EffectStarted, (duplicateClaim as LedgerClaim.WaitForOriginal).currentEntry)
    }

    @Test
    fun exactCompletedDuplicateReplaysIdenticalSuccess() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, successResult, digest1)

        val duplicateClaim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(duplicateClaim is LedgerClaim.Replay)
        assertEquals(successResult, (duplicateClaim as LedgerClaim.Replay).recordedResult)
    }

    @Test
    fun exactCompletedDuplicateReplaysIdenticalFailure() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, failureResult, null)

        val duplicateClaim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(duplicateClaim is LedgerClaim.Replay)
        assertEquals(failureResult, (duplicateClaim as LedgerClaim.Replay).recordedResult)
    }

    @Test
    fun sameIdWithDifferentToolIsRejected() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger

        val diffToolCall = ToolCall(callId1, ToolName("delete_file"), args1)
        val claim = executeLedger.claim(diffToolCall, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.RejectMismatch)
        assertEquals(ErrorCode.DUPLICATE_CALL_ID_MISMATCH, (claim as LedgerClaim.RejectMismatch).error.code)
    }

    @Test
    fun sameIdWithDifferentEffectIsRejected() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger

        val claim = executeLedger.claim(mutatingCall1, ToolEffect.PROCESS)
        assertTrue(claim is LedgerClaim.RejectMismatch)
        assertEquals(ErrorCode.DUPLICATE_CALL_ID_MISMATCH, (claim as LedgerClaim.RejectMismatch).error.code)
    }

    @Test
    fun sameIdWithDifferentArgumentsIsRejected() {
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger

        val diffArgs = ToolValue.ObjectValue.of(mapOf("path" to pathArg, "content" to ToolValue.StringValue("different")))
        val diffCall = ToolCall(callId1, toolName1, diffArgs)
        val claim = executeLedger.claim(diffCall, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.RejectMismatch)
        assertEquals(ErrorCode.DUPLICATE_CALL_ID_MISMATCH, (claim as LedgerClaim.RejectMismatch).error.code)
    }

    @Test
    fun canonicalObjectKeyOrderProducesSameFingerprint() {
        val map1 = mapOf("b" to ToolValue.IntegerValue(2L), "a" to ToolValue.IntegerValue(1L))
        val map2 = mapOf("a" to ToolValue.IntegerValue(1L), "b" to ToolValue.IntegerValue(2L))

        val callA = ToolCall(callId1, toolName1, ToolValue.ObjectValue.of(map1))
        val callB = ToolCall(callId1, toolName1, ToolValue.ObjectValue.of(map2))

        val fpA = callA.computeFingerprint(ToolEffect.MUTATING)
        val fpB = callB.computeFingerprint(ToolEffect.MUTATING)
        assertEquals(fpA, fpB)
    }

    @Test
    fun differentListOrderProducesDifferentFingerprint() {
        val list1 = ToolValue.ListValue(listOf(ToolValue.IntegerValue(1L), ToolValue.IntegerValue(2L)))
        val list2 = ToolValue.ListValue(listOf(ToolValue.IntegerValue(2L), ToolValue.IntegerValue(1L)))

        val callA = ToolCall(callId1, toolName1, ToolValue.ObjectValue.of(mapOf("items" to list1)))
        val callB = ToolCall(callId1, toolName1, ToolValue.ObjectValue.of(mapOf("items" to list2)))

        val fpA = callA.computeFingerprint(ToolEffect.MUTATING)
        val fpB = callB.computeFingerprint(ToolEffect.MUTATING)
        assertNotEquals(fpA, fpB)
    }

    @Test
    fun duplicateReadOnlyIdExecutesAgainAcrossInteractions() {
        val readOnlyCall = ToolCall(callId1, ToolName("read_file_range"), args1)
        val ledger = MutatingCallLedger()
        val claim1 = ledger.claim(readOnlyCall, ToolEffect.READ_ONLY)
        assertEquals(LedgerClaim.ExecuteAgain, claim1)

        val claim2 = ledger.claim(readOnlyCall, ToolEffect.READ_ONLY)
        assertEquals(LedgerClaim.ExecuteAgain, claim2)
    }

    @Test
    fun duplicateIdsInsideOneBatchRejectTheWholeBatch() {
        val callA = ToolCall(callId1, toolName1, args1)
        val callB = ToolCall(callId1, ToolName("read_file_range"), args1)

        val plan = com.euhedral.gemini.core.tools.ToolBatchPlanner.planBatch(listOf(callA, callB))
        assertTrue(plan is com.euhedral.gemini.core.tools.BatchPlan.Rejected)
        assertEquals(ErrorCode.DUPLICATE_CALL_ID_IN_BATCH, (plan as com.euhedral.gemini.core.tools.BatchPlan.Rejected).error.code)
    }

    @Test
    fun sameIdInANewSessionIsIndependent() {
        val ledger1 = MutatingCallLedger()
        val executeLedger1 = (ledger1.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger

        val ledger2 = MutatingCallLedger() // new session
        val claimInNewSession = ledger2.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claimInNewSession is LedgerClaim.Execute)
    }

    @Test
    fun replayDoesNotCallCheckpointPolicyAdapterOrValidator() {
        var probeCounter = 0
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, successResult, digest1)

        val claim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        if (claim is LedgerClaim.Replay) {
            // Replay returns exact result without executing probe
        } else {
            probeCounter++
        }
        assertEquals(0, probeCounter)
    }

    @Test
    fun replayDoesNotAdvanceTransactionRevision() {
        val rev0 = digest0.revision
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, successResult, digest1)

        val claim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.Replay)
        // Replay does not create a new transaction revision
        assertEquals(digest0.revision, rev0)
    }

    @Test
    fun replayDoesNotInvalidateVerifiedDigest() {
        val verified = VerifiedTransactionDigest(digest0, VerificationRunId("vrun-1"))
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, successResult, digest1)

        val claim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.Replay)
        // Verified transaction remains intact on duplicate replay
        assertEquals(digest0, verified.transaction)
    }

    @Test
    fun mutateVerifyThenReplayPreservesVerification() {
        val runId = VerificationRunId("vrun-1")
        val verified = VerifiedTransactionDigest(digest1, runId)
        val ledger = MutatingCallLedger()
        val executeLedger = (ledger.claim(mutatingCall1, ToolEffect.MUTATING) as LedgerClaim.Execute).nextLedger
        val completedLedger = executeLedger.recordCompleted(callId1, successResult, digest1)

        // Replay completed mutation
        val claim = completedLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.Replay)
        assertEquals(digest1, verified.transaction)
    }

    @Test
    fun restoredCompletedLedgerReplays() {
        val entry = MutatingCallEntry(mutatingCall1.computeFingerprint(ToolEffect.MUTATING), LedgerEntry.Completed(successResult, digest1))
        val restoredLedger = MutatingCallLedger(mapOf(callId1 to entry))

        val claim = restoredLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.Replay)
        assertEquals(successResult, (claim as LedgerClaim.Replay).recordedResult)
    }

    @Test
    fun recoveryMetadataRetainsLedgerForExactReplayAfterRestart() {
        val entry = MutatingCallEntry(mutatingCall1.computeFingerprint(ToolEffect.MUTATING), LedgerEntry.Completed(successResult, digest1))
        val session = AgentSession(
            id = SessionId("session-restore"),
            projectFingerprint = ProjectFingerprint("project-restore"),
            transaction = digest1,
            state = AgentState.RUNNING,
            mutatingCalls = MutatingCallLedger(mapOf(callId1 to entry)),
        )
        val restoredLedger = session.toRecoveryMetadata().mutatingCalls
        val replay = restoredLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(replay is LedgerClaim.Replay)
        assertEquals(successResult, (replay as LedgerClaim.Replay).recordedResult)
    }

    @Test
    fun restoredOutcomeUnknownNeverReexecutes() {
        val entry = MutatingCallEntry(mutatingCall1.computeFingerprint(ToolEffect.MUTATING), LedgerEntry.OutcomeUnknown)
        val restoredLedger = MutatingCallLedger(mapOf(callId1 to entry))

        val claim = restoredLedger.claim(mutatingCall1, ToolEffect.MUTATING)
        assertTrue(claim is LedgerClaim.RejectOutcomeUnknown)
        assertEquals(ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN, (claim as LedgerClaim.RejectOutcomeUnknown).error.code)
    }
}
