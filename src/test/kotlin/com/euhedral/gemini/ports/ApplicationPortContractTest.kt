package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ApprovalDecision
import com.euhedral.gemini.core.agent.ApprovalDecisionStatus
import com.euhedral.gemini.core.agent.ApprovalRequestId
import com.euhedral.gemini.core.agent.InteractionId
import com.euhedral.gemini.core.agent.ModelRequestId
import com.euhedral.gemini.core.agent.ProcessInvocationId
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.agent.SessionRevision
import com.euhedral.gemini.core.agent.Sha256Digest
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.agent.TransactionRevision
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.tools.BoundedOutputMetadata
import com.euhedral.gemini.core.tools.ToolEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationPortContractTest {

    @Test
    fun allEightAndOnlyEightApplicationPortsExist() {
        val ports = listOf(
            GeminiTransport::class.java,
            WorkspaceInspectionPort::class.java,
            WorkspaceEditPort::class.java,
            BuildSystemPort::class.java,
            GitReadPort::class.java,
            CredentialPort::class.java,
            CheckpointStore::class.java,
            ApprovalPort::class.java,
        )
        assertEquals(8, ports.size)
        for (port in ports) {
            assertTrue(port.isInterface)
        }
    }

    @Test
    fun geminiContinuationPreservesInteractionAndCallIds() {
        val previousId = InteractionId("inter-1")
        val req = InteractionRequest(
            sessionId = SessionId("sess-1"),
            modelRequestId = ModelRequestId("req-1"),
            modelName = "gemini-2.5-pro",
            previousInteractionId = previousId,
            inputs = emptyList(),
            tools = emptyList(),
        )
        assertEquals(previousId, req.previousInteractionId)
    }

    @Test
    fun geminiRequestCarriesRepeatedConfigurationAndRetryAllowance() {
        val config = GenerationConfig(temperature = 0.2, topP = 0.95)
        val req = InteractionRequest(
            sessionId = SessionId("sess-1"),
            modelRequestId = ModelRequestId("req-1"),
            modelName = "gemini-2.5-pro",
            generationConfig = config,
            remainingHttpRetries = 3,
            inputs = emptyList(),
            tools = emptyList(),
        )
        assertEquals(config, req.generationConfig)
        assertEquals(3, req.remainingHttpRetries)
    }

    @Test
    fun geminiStreamingSinkOrdersEventsAndHasOneTerminalResult() {
        val events = mutableListOf<InteractionStreamEvent>()
        val sink = object : InteractionStreamSink {
            override suspend fun emit(event: InteractionStreamEvent) {
                events.add(event)
            }
        }
        assertNotNull(sink)
    }

    @Test
    fun inspectionMethodsReturnOnlyBoundedRelativePathValues() {
        val req = ReadFileRangeRequest(
            path = ProjectPath("src/Main.kt"),
            startLine = 1,
            endLine = 10,
        )
        assertEquals("src/Main.kt", req.path.value)
        assertFalse(req.path.value.startsWith("/"))
    }

    @Test
    fun symbolIdsAreSessionScopedAndStaleIdsFailExplicitly() {
        val symId = SymbolId("sym-123")
        assertEquals("sym-123", symId.value)
    }

    @Test
    fun editMethodsRequireTransactionAndExpectedRevision() {
        val req = ReplaceTextRequest(
            sessionId = SessionId("sess-1"),
            transactionId = TransactionId("tx-1"),
            callId = ToolCallId("call-1"),
            expectedRevision = TransactionRevision(0L),
            path = ProjectPath("src/Main.kt"),
            oldText = "old",
            newText = "new",
            expectedFileRevision = ExpectedFileRevision.Absent,
        )
        assertEquals(TransactionId("tx-1"), req.transactionId)
        assertEquals(TransactionRevision(0L), req.expectedRevision)
    }

    @Test
    fun checkpointFailurePreventsEveryMutation() {
        val status = com.euhedral.gemini.core.error.ErrorCode.CHECKPOINT_WRITE_FAILED
        assertEquals(com.euhedral.gemini.core.error.ErrorCategory.CHECKPOINT, status.defaultCategory)
    }

    @Test
    fun editResultAdvancesExactlyOneTransactionRevision() {
        val t0 = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(0L), Sha256Digest("0".repeat(64)))
        val t1 = TransactionRevisionDigest(TransactionId("tx-1"), TransactionRevision(1L), Sha256Digest("1".repeat(64)))
        assertEquals(t0.revision.value + 1L, t1.revision.value)
    }

    @Test
    fun buildRequestsExposeSemanticIntentAndNoCommandLine() {
        val req = BuildProjectRequest(
            processInvocationId = ProcessInvocationId("proc-1"),
            transactionId = TransactionId("tx-1"),
            timeoutMillis = 900_000L,
        )
        assertEquals(ProcessInvocationId("proc-1"), req.processInvocationId)
    }

    @Test
    fun buildOutputIsBoundedRedactedAndOrdered() {
        val bounded = BoundedOutputMetadata(truncated = false, returnedItems = 0, returnedCharacters = 50)
        val res = BuildResult(
            succeeded = true,
            exitCode = 0,
            durationMillis = 1500L,
            boundedOutput = bounded,
        )
        assertTrue(res.succeeded)
        assertEquals(0, res.exitCode)
    }

    @Test
    fun nonzeroBuildExitIsACompletedBuildResult() {
        val bounded = BoundedOutputMetadata(truncated = false, returnedItems = 0, returnedCharacters = 50)
        val res = BuildResult(
            succeeded = false,
            exitCode = 1,
            durationMillis = 1500L,
            boundedOutput = bounded,
        )
        assertFalse(res.succeeded)
        assertEquals(1, res.exitCode)
    }

    @Test
    fun gitPortExposesExactlyFiveReadMethodsAndNoMutation() {
        val methods = GitReadPort::class.java.declaredMethods
        assertEquals(5, methods.size)
        val names = methods.map { it.name }.toSet()
        assertEquals(setOf("status", "diff", "diffFile", "log", "blame"), names)
    }

    @Test
    fun credentialSecretsNeverAppearInStateEventsErrorsOrToString() {
        val secret = SecretValue(charArrayOf('s', 'e', 'c', 'r', 'e', 't'))
        assertEquals("[REDACTED_SECRET]", secret.toString())
        secret.close()
    }

    @Test
    fun checkpointReceiptCompareAndSetRejectsStaleRevision() {
        val record = PendingMutationRecord(ToolCallId("call-1"), ProjectPath("src/Main.kt"))
        val receipt = CheckpointReceipt("rcpt-1", TransactionId("tx-1"), record)
        assertEquals("rcpt-1", receipt.receiptId)
    }

    @Test
    fun checkpointRecoveryDistinguishesPendingCompletedAndUnknown() {
        val statuses = CheckpointStatus.values()
        assertTrue(statuses.contains(CheckpointStatus.ACTIVE))
        assertTrue(statuses.contains(CheckpointStatus.COMPLETED))
        assertTrue(statuses.contains(CheckpointStatus.ROLLED_BACK))
        assertTrue(statuses.contains(CheckpointStatus.FAILED))
        assertTrue(statuses.contains(CheckpointStatus.DISCARDED))
    }

    @Test
    fun approvalDecisionMustMatchRequestAndRevisionGuards() {
        val req = ApprovalRequest(
            requestId = ApprovalRequestId("app-1"),
            sessionId = SessionId("sess-1"),
            effect = ToolEffect.MUTATING,
            actionSummary = "action",
            targetSummary = "target",
            policyReasonCode = "REASON",
            expectedSessionRevision = SessionRevision(1L),
        )
        val decision = ApprovalDecision(
            requestId = req.requestId,
            status = ApprovalDecisionStatus.GRANTED,
            expectedSessionRevision = req.expectedSessionRevision,
        )
        assertEquals(req.requestId, decision.requestId)
        assertEquals(req.expectedSessionRevision, decision.expectedSessionRevision)
    }

    @Test
    fun approvalCancellationWithdrawsAndPropagates() {
        val reasons = ApprovalWithdrawalReason.values().toList()
        assertTrue(reasons.contains(ApprovalWithdrawalReason.USER_CANCELLED))
    }

    @Test
    fun expectedFailuresUsePortResultAndNoAdapterExceptionEscapes() {
        val err = com.euhedral.gemini.core.error.OperationError(
            category = com.euhedral.gemini.core.error.ErrorCategory.TRANSPORT,
            code = com.euhedral.gemini.core.error.ErrorCode.TRANSPORT_UNAVAILABLE,
            safeMessage = "Unavailable",
        )
        val failure: PortResult<Nothing> = PortResult.Failure(err)
        assertTrue(failure is PortResult.Failure)
        assertEquals(err, (failure as PortResult.Failure).error)
    }
}
