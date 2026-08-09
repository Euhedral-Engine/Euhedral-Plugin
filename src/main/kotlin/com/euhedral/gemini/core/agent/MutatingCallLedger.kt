package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.error.DuplicateCallIdError
import com.euhedral.gemini.core.error.ErrorCategory
import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.ToolCall
import com.euhedral.gemini.core.tools.ToolEffect
import com.euhedral.gemini.core.tools.ToolResult

@SourceBearingValue
sealed interface LedgerEntry {
    @SerializableValue
    object Reserved : LedgerEntry

    @SerializableValue
    object EffectStarted : LedgerEntry

    @SourceBearingValue
    data class Completed(
        val result: ToolResult,
        val transactionDigest: TransactionRevisionDigest?,
    ) : LedgerEntry

    @SerializableValue
    object OutcomeUnknown : LedgerEntry
}

@SourceBearingValue
data class MutatingCallEntry(
    val fingerprint: ToolInvocationFingerprint,
    val state: LedgerEntry,
)

@SourceBearingValue
sealed interface LedgerClaim {
    @SourceBearingValue
    data class Execute(val nextLedger: MutatingCallLedger) : LedgerClaim

    @SourceBearingValue
    data class WaitForOriginal(val currentEntry: LedgerEntry) : LedgerClaim

    @SourceBearingValue
    data class Replay(val recordedResult: ToolResult) : LedgerClaim

    @SourceBearingValue
    data class RejectMismatch(val error: AgentError) : LedgerClaim

    @SourceBearingValue
    data class RejectOutcomeUnknown(val error: AgentError) : LedgerClaim

    @SerializableValue
    object ExecuteAgain : LedgerClaim
}

@SourceBearingValue
data class MutatingCallLedger(
    val entries: Map<ToolCallId, MutatingCallEntry> = emptyMap(),
) {
    fun claim(call: ToolCall, effect: ToolEffect): LedgerClaim {
        if (effect == ToolEffect.READ_ONLY) {
            return LedgerClaim.ExecuteAgain
        }

        val fingerprint = call.computeFingerprint(effect)
        val existing = entries[call.id]

        if (existing == null) {
            val newEntry = MutatingCallEntry(fingerprint, LedgerEntry.Reserved)
            val updatedMap = entries + (call.id to newEntry)
            return LedgerClaim.Execute(MutatingCallLedger(updatedMap))
        }

        if (existing.fingerprint != fingerprint) {
            return LedgerClaim.RejectMismatch(
                DuplicateCallIdError(
                    code = ErrorCode.DUPLICATE_CALL_ID_MISMATCH,
                    safeMessage = "Duplicate call ID '${call.id.value}' used with different name, effect, or arguments",
                )
            )
        }

        return when (val state = existing.state) {
            is LedgerEntry.Reserved, is LedgerEntry.EffectStarted -> LedgerClaim.WaitForOriginal(state)
            is LedgerEntry.Completed -> LedgerClaim.Replay(state.result)
            is LedgerEntry.OutcomeUnknown -> LedgerClaim.RejectOutcomeUnknown(
                OperationError(
                    category = ErrorCategory.TOOL_CALL,
                    code = ErrorCode.MUTATING_CALL_OUTCOME_UNKNOWN,
                    safeMessage = "Mutating call '${call.id.value}' has unknown outcome; speculative re-execution is forbidden",
                )
            )
        }
    }

    fun markEffectStarted(id: ToolCallId): MutatingCallLedger {
        val existing = requireNotNull(entries[id]) { "Call ID '${id.value}' not reserved in ledger" }
        val updated = existing.copy(state = LedgerEntry.EffectStarted)
        return copy(entries = entries + (id to updated))
    }

    fun recordCompleted(
        id: ToolCallId,
        result: ToolResult,
        digest: TransactionRevisionDigest?,
    ): MutatingCallLedger {
        val existing = requireNotNull(entries[id]) { "Call ID '${id.value}' not reserved in ledger" }
        val updated = existing.copy(state = LedgerEntry.Completed(result, digest))
        return copy(entries = entries + (id to updated))
    }

    fun markOutcomeUnknown(id: ToolCallId): MutatingCallLedger {
        val existing = requireNotNull(entries[id]) { "Call ID '${id.value}' not reserved in ledger" }
        val updated = existing.copy(state = LedgerEntry.OutcomeUnknown)
        return copy(entries = entries + (id to updated))
    }
}
