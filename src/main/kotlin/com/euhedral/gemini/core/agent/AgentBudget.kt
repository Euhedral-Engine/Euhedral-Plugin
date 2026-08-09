package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.error.BudgetExhaustedError
import com.euhedral.gemini.core.error.ErrorCategory
import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.error.RetryClass
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
sealed interface BudgetDecision<out T> {
    data class Allowed<T>(val nextBudget: T) : BudgetDecision<T>
    data class Exhausted<T>(val error: AgentError, val unchangedBudget: T) : BudgetDecision<T>
}

@SerializableValue
data class HttpRetryBudget(
    val maxRetries: Int = 3,
    val currentScopeId: String? = null,
    val currentScopeRetries: Int = 0,
    val totalRetriesUsed: Int = 0,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(currentScopeRetries >= 0) { "currentScopeRetries must be non-negative" }
        require(totalRetriesUsed >= 0) { "totalRetriesUsed must be non-negative" }
    }

    fun consumeRetry(scopeId: String): BudgetDecision<HttpRetryBudget> {
        val retriesInScope = if (currentScopeId == scopeId) currentScopeRetries else 0
        return if (retriesInScope < maxRetries && totalRetriesUsed < Int.MAX_VALUE) {
            BudgetDecision.Allowed(
                copy(
                    currentScopeId = scopeId,
                    currentScopeRetries = retriesInScope + 1,
                    totalRetriesUsed = totalRetriesUsed + 1,
                )
            )
        } else {
            BudgetDecision.Exhausted(
                BudgetExhaustedError(
                    code = ErrorCode.HTTP_RETRY_EXHAUSTED,
                    safeMessage = "HTTP retry budget exhausted for scope '$scopeId' ($maxRetries retries max)",
                    retryClass = RetryClass.HTTP,
                ),
                this,
            )
        }
    }

    fun closeScope(scopeId: String): HttpRetryBudget {
        return if (currentScopeId == scopeId) {
            copy(currentScopeId = null, currentScopeRetries = 0)
        } else {
            this
        }
    }
}

@SerializableValue
data class TransientToolRetryBudget(
    val maxRetries: Int = 2,
    val currentScopeId: String? = null,
    val currentScopeRetries: Int = 0,
    val totalRetriesUsed: Int = 0,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(currentScopeRetries >= 0) { "currentScopeRetries must be non-negative" }
        require(totalRetriesUsed >= 0) { "totalRetriesUsed must be non-negative" }
    }

    fun consumeRetry(scopeId: String): BudgetDecision<TransientToolRetryBudget> {
        val retriesInScope = if (currentScopeId == scopeId) currentScopeRetries else 0
        return if (retriesInScope < maxRetries && totalRetriesUsed < Int.MAX_VALUE) {
            BudgetDecision.Allowed(
                copy(
                    currentScopeId = scopeId,
                    currentScopeRetries = retriesInScope + 1,
                    totalRetriesUsed = totalRetriesUsed + 1,
                )
            )
        } else {
            BudgetDecision.Exhausted(
                BudgetExhaustedError(
                    code = ErrorCode.TRANSIENT_TOOL_RETRY_EXHAUSTED,
                    safeMessage = "Transient tool retry budget exhausted for scope '$scopeId' ($maxRetries retries max)",
                    retryClass = RetryClass.TRANSIENT_TOOL,
                ),
                this,
            )
        }
    }

    fun closeScope(scopeId: String): TransientToolRetryBudget {
        return if (currentScopeId == scopeId) {
            copy(currentScopeId = null, currentScopeRetries = 0)
        } else {
            this
        }
    }
}

@SerializableValue
data class RepairCycleBudget(
    val maxCycles: Int = 5,
    val cyclesUsed: Int = 0,
) {
    init {
        require(maxCycles >= 0) { "maxCycles must be non-negative" }
        require(cyclesUsed >= 0) { "cyclesUsed must be non-negative" }
    }

    fun consumeCycle(): BudgetDecision<RepairCycleBudget> {
        return if (cyclesUsed < maxCycles) {
            BudgetDecision.Allowed(copy(cyclesUsed = cyclesUsed + 1))
        } else {
            BudgetDecision.Exhausted(
                BudgetExhaustedError(
                    code = ErrorCode.REPAIR_CYCLE_EXHAUSTED,
                    safeMessage = "Repair cycle budget exhausted ($maxCycles cycles max)",
                    retryClass = RetryClass.REPAIR_CYCLE,
                ),
                this,
            )
        }
    }
}

@SerializableValue
data class ModelTurnBudget(
    val maxTurns: Int = 40,
    val turnsUsed: Int = 0,
) {
    init {
        require(maxTurns >= 0) { "maxTurns must be non-negative" }
        require(turnsUsed >= 0) { "turnsUsed must be non-negative" }
    }

    fun consumeTurn(): BudgetDecision<ModelTurnBudget> {
        return if (turnsUsed < maxTurns) {
            BudgetDecision.Allowed(copy(turnsUsed = turnsUsed + 1))
        } else {
            BudgetDecision.Exhausted(
                BudgetExhaustedError(
                    code = ErrorCode.MODEL_TURN_LIMIT_EXCEEDED,
                    safeMessage = "Model turn budget exceeded ($maxTurns turns max)",
                    retryClass = RetryClass.NONE,
                ),
                this,
            )
        }
    }
}

@SerializableValue
data class ToolCallBudget(
    val maxCalls: Int = 100,
    val callsUsed: Int = 0,
) {
    init {
        require(maxCalls >= 0) { "maxCalls must be non-negative" }
        require(callsUsed >= 0) { "callsUsed must be non-negative" }
    }

    fun consumeCalls(count: Int = 1): BudgetDecision<ToolCallBudget> {
        require(count >= 0) { "count must be non-negative" }
        return if (count <= maxCalls - callsUsed) {
            BudgetDecision.Allowed(copy(callsUsed = callsUsed + count))
        } else {
            BudgetDecision.Exhausted(
                BudgetExhaustedError(
                    code = ErrorCode.TOOL_CALL_LIMIT_EXCEEDED,
                    safeMessage = "Tool call budget exceeded ($maxCalls calls max, requested $count, current $callsUsed)",
                    retryClass = RetryClass.NONE,
                ),
                this,
            )
        }
    }
}

@SerializableValue
data class ProcessTimeoutBudget(
    val maxDurationMillis: Long = 900_000L,
) {
    init {
        require(maxDurationMillis >= 0L) { "maxDurationMillis must be non-negative" }
    }

    fun evaluate(elapsedMillis: Long): BudgetDecision<ProcessTimeoutBudget> {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative" }
        return if (elapsedMillis < maxDurationMillis) {
            BudgetDecision.Allowed(this)
        } else {
            BudgetDecision.Exhausted(
                OperationError(
                    category = ErrorCategory.BUDGET,
                    code = ErrorCode.PROCESS_TIMEOUT,
                    safeMessage = "Process execution timed out at ${elapsedMillis}ms (limit ${maxDurationMillis}ms)",
                    retryClass = RetryClass.NONE,
                ),
                this,
            )
        }
    }
}

@SerializableValue
data class SessionTimeBudget(
    val maxDurationMillis: Long = 1_800_000L,
) {
    init {
        require(maxDurationMillis >= 0L) { "maxDurationMillis must be non-negative" }
    }

    fun evaluate(elapsedMillis: Long): BudgetDecision<SessionTimeBudget> {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative" }
        return if (elapsedMillis < maxDurationMillis) {
            BudgetDecision.Allowed(this)
        } else {
            BudgetDecision.Exhausted(
                OperationError(
                    category = ErrorCategory.BUDGET,
                    code = ErrorCode.SESSION_TIMEOUT,
                    safeMessage = "Session time budget exhausted at ${elapsedMillis}ms (limit ${maxDurationMillis}ms)",
                    retryClass = RetryClass.NONE,
                ),
                this,
            )
        }
    }
}

@SerializableValue
data class AgentLimits(
    val httpRetry: HttpRetryBudget = HttpRetryBudget(),
    val transientToolRetry: TransientToolRetryBudget = TransientToolRetryBudget(),
    val repairCycle: RepairCycleBudget = RepairCycleBudget(),
    val modelTurn: ModelTurnBudget = ModelTurnBudget(),
    val toolCall: ToolCallBudget = ToolCallBudget(),
    val processTimeout: ProcessTimeoutBudget = ProcessTimeoutBudget(),
    val sessionTime: SessionTimeBudget = SessionTimeBudget(),
)

@SerializableValue
data class AgentCounters(
    val httpRetriesUsed: Int = 0,
    val transientToolRetriesUsed: Int = 0,
    val repairCyclesUsed: Int = 0,
    val modelTurnsUsed: Int = 0,
    val toolCallsUsed: Int = 0,
    val startEpochMillis: Long = 0L,
    /** Latest elapsed time for each independent process invocation. */
    val processElapsedMillis: Map<ProcessInvocationId, Long> = emptyMap(),
    /** Latest elapsed time observed for the whole session. */
    val sessionElapsedMillis: Long = 0L,
) {
    init {
        require(httpRetriesUsed >= 0) { "httpRetriesUsed must be non-negative" }
        require(transientToolRetriesUsed >= 0) { "transientToolRetriesUsed must be non-negative" }
        require(repairCyclesUsed >= 0) { "repairCyclesUsed must be non-negative" }
        require(modelTurnsUsed >= 0) { "modelTurnsUsed must be non-negative" }
        require(toolCallsUsed >= 0) { "toolCallsUsed must be non-negative" }
        require(startEpochMillis >= 0L) { "startEpochMillis must be non-negative" }
        require(processElapsedMillis.values.all { it >= 0L }) { "processElapsedMillis values must be non-negative" }
        require(sessionElapsedMillis >= 0L) { "sessionElapsedMillis must be non-negative" }
    }
}
