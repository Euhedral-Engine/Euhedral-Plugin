package com.euhedral.gemini.core.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentErrorTest {

    @Test
    fun everyStableErrorCodeHasOneCategory() {
        for (code in ErrorCode.values()) {
            val category = code.defaultCategory
            assertNotNull(category)
        }
    }

    @Test
    fun everyErrorCodeHasExactlyOneRetryClass() {
        for (code in ErrorCode.values()) {
            val retryClass = code.defaultRetryClass
            assertNotNull(retryClass)
        }
    }

    @Test
    fun httpCodesNeverUseToolOrRepairRetry() {
        val httpCodes = listOf(ErrorCode.RATE_LIMITED, ErrorCode.REMOTE_SERVER_ERROR, ErrorCode.TRANSPORT_UNAVAILABLE, ErrorCode.TRANSPORT_TIMEOUT)
        for (code in httpCodes) {
            assertEquals(RetryClass.HTTP, code.defaultRetryClass)
        }
    }

    @Test
    fun transientToolCodesNeverUseHttpOrRepairRetry() {
        val toolCodes = listOf(ErrorCode.INDEX_UNAVAILABLE, ErrorCode.EDIT_FAILED, ErrorCode.PROCESS_START_FAILED)
        for (code in toolCodes) {
            assertEquals(RetryClass.TRANSIENT_TOOL, code.defaultRetryClass)
        }
    }

    @Test
    fun repairClassificationIsMandatoryVerificationOnly() {
        assertEquals(RetryClass.REPAIR_CYCLE, ErrorCode.REPAIR_CYCLE_EXHAUSTED.defaultRetryClass)
        for (code in ErrorCode.values()) {
            if (code != ErrorCode.REPAIR_CYCLE_EXHAUSTED) {
                assertTrue(code.defaultRetryClass != RetryClass.REPAIR_CYCLE)
            }
        }
    }

    @Test
    fun errorsCannotRetainThrowable() {
        val error = OperationError(ErrorCategory.STATE, ErrorCode.INVALID_TRANSITION, "Failed")
        // AgentError fields do not contain Throwable
        val fields = AgentError::class.java.declaredFields.map { it.type }
        assertTrue(fields.none { Throwable::class.java.isAssignableFrom(it) })
    }

    @Test(expected = IllegalArgumentException::class)
    fun errorsRejectUnsafeDetailKeysAndOversizedValues() {
        OperationError(
            category = ErrorCategory.STATE,
            code = ErrorCode.INVALID_TRANSITION,
            safeMessage = "Failed",
            details = mapOf("bad key!" to "value"),
        )
    }

    @Test
    fun cancellationIsNotConvertedToPortFailure() {
        val cancellationError = OperationError(
            category = ErrorCategory.CANCELLATION,
            code = ErrorCode.OPERATION_CANCELLED,
            safeMessage = "Cancelled by user",
        )
        assertEquals(ErrorCategory.CANCELLATION, cancellationError.category)
        assertEquals(ErrorCode.OPERATION_CANCELLED, cancellationError.code)
    }
}
