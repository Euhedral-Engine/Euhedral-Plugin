package com.euhedral.gemini.core.error

import com.euhedral.gemini.core.agent.AgentState
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
enum class RetryClass {
    NONE,
    HTTP,
    TRANSIENT_TOOL,
    REPAIR_CYCLE,
}

@SerializableValue
enum class ErrorCategory {
    STATE,
    BUDGET,
    TOOL_CALL,
    TRANSPORT,
    CREDENTIAL,
    INSPECTION,
    EDIT,
    CONFLICT,
    POLICY,
    CHECKPOINT,
    PROCESS,
    BUILD,
    GIT_READ,
    APPROVAL,
    VERIFICATION,
    CANCELLATION,
}

@SerializableValue
enum class ErrorCode {
    INVALID_TRANSITION,
    SESSION_ID_MISMATCH,
    STALE_SESSION_EVENT,
    APPROVAL_ID_MISMATCH,
    VERIFICATION_RUN_MISMATCH,
    TRANSACTION_REVISION_MISMATCH,
    COMPLETION_NOT_VERIFIED,
    VERIFIED_DIGEST_STALE,
    HTTP_RETRY_EXHAUSTED,
    TRANSIENT_TOOL_RETRY_EXHAUSTED,
    REPAIR_CYCLE_EXHAUSTED,
    MODEL_TURN_LIMIT_EXCEEDED,
    TOOL_CALL_LIMIT_EXCEEDED,
    PROCESS_TIMEOUT,
    SESSION_TIMEOUT,
    UNKNOWN_TOOL,
    INVALID_TOOL_ARGUMENTS,
    DUPLICATE_CALL_ID_IN_BATCH,
    DUPLICATE_CALL_ID_MISMATCH,
    MUTATING_CALL_OUTCOME_UNKNOWN,
    CONTROL_BATCH_INVALID,
    TRANSPORT_UNAVAILABLE,
    RATE_LIMITED,
    REMOTE_SERVER_ERROR,
    TRANSPORT_TIMEOUT,
    MALFORMED_RESPONSE,
    UNKNOWN_REQUIRED_STEP,
    INTERACTION_EXPIRED,
    CREDENTIAL_MISSING,
    CREDENTIAL_REJECTED,
    INVALID_PATH,
    INVALID_RANGE,
    INDEX_UNAVAILABLE,
    SYMBOL_NOT_FOUND,
    SYMBOL_AMBIGUOUS,
    SYMBOL_STALE,
    STALE_CONTENT,
    EXTERNAL_MODIFICATION,
    READ_ONLY_FILE,
    REPLACEMENT_NOT_FOUND,
    REPLACEMENT_AMBIGUOUS,
    CHECKPOINT_WRITE_FAILED,
    EDIT_FAILED,
    ROLLBACK_CONFLICT,
    POLICY_DENIED,
    APPROVAL_DENIED,
    APPROVAL_CANCELLED,
    APPROVAL_STALE,
    VALIDATION_FAILED,
    PROCESS_START_FAILED,
    PROCESS_FAILED,
    BUILD_CONFIGURATION_MISSING,
    BUILD_FAILED,
    TEST_FAILED,
    GIT_UNAVAILABLE,
    GIT_READ_FAILED,
    OPERATION_CANCELLED;

    val defaultCategory: ErrorCategory
        get() = when (this) {
            INVALID_TRANSITION, SESSION_ID_MISMATCH, STALE_SESSION_EVENT, APPROVAL_ID_MISMATCH,
            VERIFICATION_RUN_MISMATCH, TRANSACTION_REVISION_MISMATCH, COMPLETION_NOT_VERIFIED, VERIFIED_DIGEST_STALE -> ErrorCategory.STATE
            HTTP_RETRY_EXHAUSTED, TRANSIENT_TOOL_RETRY_EXHAUSTED, REPAIR_CYCLE_EXHAUSTED, MODEL_TURN_LIMIT_EXCEEDED,
            TOOL_CALL_LIMIT_EXCEEDED, PROCESS_TIMEOUT, SESSION_TIMEOUT -> ErrorCategory.BUDGET
            UNKNOWN_TOOL, INVALID_TOOL_ARGUMENTS, DUPLICATE_CALL_ID_IN_BATCH, DUPLICATE_CALL_ID_MISMATCH,
            MUTATING_CALL_OUTCOME_UNKNOWN, CONTROL_BATCH_INVALID -> ErrorCategory.TOOL_CALL
            TRANSPORT_UNAVAILABLE, RATE_LIMITED, REMOTE_SERVER_ERROR, TRANSPORT_TIMEOUT, MALFORMED_RESPONSE,
            UNKNOWN_REQUIRED_STEP, INTERACTION_EXPIRED -> ErrorCategory.TRANSPORT
            CREDENTIAL_MISSING, CREDENTIAL_REJECTED -> ErrorCategory.CREDENTIAL
            INVALID_PATH, INVALID_RANGE, INDEX_UNAVAILABLE, SYMBOL_NOT_FOUND, SYMBOL_AMBIGUOUS, SYMBOL_STALE -> ErrorCategory.INSPECTION
            STALE_CONTENT, EXTERNAL_MODIFICATION, READ_ONLY_FILE, REPLACEMENT_NOT_FOUND, REPLACEMENT_AMBIGUOUS, EDIT_FAILED, ROLLBACK_CONFLICT -> ErrorCategory.EDIT
            POLICY_DENIED, VALIDATION_FAILED -> ErrorCategory.POLICY
            CHECKPOINT_WRITE_FAILED -> ErrorCategory.CHECKPOINT
            PROCESS_START_FAILED, PROCESS_FAILED -> ErrorCategory.PROCESS
            BUILD_CONFIGURATION_MISSING, BUILD_FAILED, TEST_FAILED -> ErrorCategory.BUILD
            GIT_UNAVAILABLE, GIT_READ_FAILED -> ErrorCategory.GIT_READ
            APPROVAL_DENIED, APPROVAL_CANCELLED, APPROVAL_STALE -> ErrorCategory.APPROVAL
            OPERATION_CANCELLED -> ErrorCategory.CANCELLATION
        }

    val defaultRetryClass: RetryClass
        get() = when (this) {
            RATE_LIMITED, REMOTE_SERVER_ERROR, TRANSPORT_UNAVAILABLE, TRANSPORT_TIMEOUT -> RetryClass.HTTP
            INDEX_UNAVAILABLE, EDIT_FAILED, PROCESS_START_FAILED -> RetryClass.TRANSIENT_TOOL
            REPAIR_CYCLE_EXHAUSTED -> RetryClass.REPAIR_CYCLE
            else -> RetryClass.NONE
        }
}

private val SAFE_DETAIL_KEY_REGEX = Regex("^[a-zA-Z0-9_]{1,64}$")

@SerializableValue
sealed class AgentError {
    abstract val category: ErrorCategory
    abstract val code: ErrorCode
    abstract val safeMessage: String
    abstract val retryClass: RetryClass
    abstract val details: Map<String, String>

    protected fun validateDetails() {
        require(safeMessage.isNotBlank()) { "Error message cannot be blank" }
        require(safeMessage.length <= 2048) { "Error message exceeds maximum length of 2048 characters" }
        details.forEach { (key, value) ->
            require(SAFE_DETAIL_KEY_REGEX.matches(key)) { "Invalid detail key '$key': must be alphanumeric/underscore (1-64 chars)" }
            require(value.length <= 1024) { "Detail value for '$key' exceeds maximum length of 1024 characters" }
        }
    }
}

@SerializableValue
data class InvalidTransitionError(
    val fromState: AgentState,
    val eventName: String,
    override val safeMessage: String = "Invalid transition from $fromState on event $eventName",
    override val details: Map<String, String> = emptyMap(),
) : AgentError() {
    override val category: ErrorCategory get() = ErrorCategory.STATE
    override val code: ErrorCode get() = ErrorCode.INVALID_TRANSITION
    override val retryClass: RetryClass get() = RetryClass.NONE

    init {
        validateDetails()
    }
}

@SerializableValue
data class BudgetExhaustedError(
    override val code: ErrorCode,
    override val safeMessage: String,
    override val retryClass: RetryClass = code.defaultRetryClass,
    override val details: Map<String, String> = emptyMap(),
) : AgentError() {
    override val category: ErrorCategory get() = ErrorCategory.BUDGET

    init {
        validateDetails()
    }
}

@SerializableValue
data class DuplicateCallIdError(
    override val code: ErrorCode,
    override val safeMessage: String,
    override val details: Map<String, String> = emptyMap(),
) : AgentError() {
    override val category: ErrorCategory get() = ErrorCategory.TOOL_CALL
    override val retryClass: RetryClass get() = RetryClass.NONE

    init {
        validateDetails()
    }
}

@SerializableValue
data class OperationError(
    override val category: ErrorCategory,
    override val code: ErrorCode,
    override val safeMessage: String,
    override val retryClass: RetryClass = code.defaultRetryClass,
    override val details: Map<String, String> = emptyMap(),
) : AgentError() {
    init {
        validateDetails()
    }
}
