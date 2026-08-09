package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.agent.ContinuationToken
import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.ToolName
import com.euhedral.gemini.core.error.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolContractTest {

    private val callId1 = ToolCallId("call-1")
    private val callId2 = ToolCallId("call-2")

    @Test
    fun allTwentyTwoDescriptorNamesAreUnique() {
        val descriptors = StandardToolDescriptors.ALL
        assertEquals(22, descriptors.size)
        val names = descriptors.map { it.name.value }
        assertEquals(22, names.toSet().size)
    }

    @Test
    fun everyDescriptorHasTheExactLockedEffect() {
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.WORKSPACE_CONTEXT.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.READ_FILE_RANGE.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.SEARCH_TEXT.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.FIND_SYMBOL.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.FIND_REFERENCES.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.FIND_IMPLEMENTATIONS.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.FILE_METADATA.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.GIT_STATUS.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.GIT_DIFF.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.GIT_DIFF_FILE.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.GIT_LOG.effect)
        assertEquals(ToolEffect.READ_ONLY, StandardToolDescriptors.GIT_BLAME.effect)

        assertEquals(ToolEffect.MUTATING, StandardToolDescriptors.REPLACE_TEXT.effect)
        assertEquals(ToolEffect.MUTATING, StandardToolDescriptors.CREATE_FILE.effect)
        assertEquals(ToolEffect.MUTATING, StandardToolDescriptors.DELETE_FILE.effect)
        assertEquals(ToolEffect.MUTATING, StandardToolDescriptors.MOVE_FILE.effect)

        assertEquals(ToolEffect.PROCESS, StandardToolDescriptors.BUILD_PROJECT.effect)
        assertEquals(ToolEffect.PROCESS, StandardToolDescriptors.TEST_MODULE.effect)
        assertEquals(ToolEffect.PROCESS, StandardToolDescriptors.TEST_CLASS.effect)
        assertEquals(ToolEffect.PROCESS, StandardToolDescriptors.TEST_METHOD.effect)

        assertEquals(ToolEffect.CONTROL, StandardToolDescriptors.COMPLETE_TASK.effect)
        assertEquals(ToolEffect.CONTROL, StandardToolDescriptors.REQUEST_COMMIT.effect)
    }

    @Test
    fun everyDescriptorHasTheExactRequiredAndOptionalParameters() {
        val readFileRange = StandardToolDescriptors.READ_FILE_RANGE
        val reqs = readFileRange.schema.parameters.filter { it.required }.map { it.name }
        val opts = readFileRange.schema.parameters.filter { !it.required }.map { it.name }

        assertEquals(listOf("path", "start_line", "end_line"), reqs)
        assertEquals(listOf("continuation_token"), opts)
    }

    @Test
    fun schemasAreShallowClosedAndRejectAdditionalProperties() {
        val call = ToolCall(
            id = callId1,
            name = ToolName("file_metadata"),
            arguments = ToolValue.ObjectValue.of(mapOf("path" to ToolValue.StringValue("src/Main.kt"), "extra" to ToolValue.StringValue("bad"))),
        )
        val plan = ToolBatchPlanner.planBatch(listOf(call))
        assertTrue(plan is BatchPlan.Rejected)
        assertEquals(ErrorCode.INVALID_TOOL_ARGUMENTS, (plan as BatchPlan.Rejected).error.code)
    }

    @Test
    fun toolValuesHaveStructuralCanonicalEquality() {
        val val1 = ToolValue.ObjectValue.of(mapOf("b" to ToolValue.IntegerValue(2), "a" to ToolValue.StringValue("x")))
        val val2 = ToolValue.ObjectValue.of(mapOf("a" to ToolValue.StringValue("x"), "b" to ToolValue.IntegerValue(2)))
        assertEquals(val1, val2)
        assertEquals(val1.canonicalEncoding(), val2.canonicalEncoding())
    }

    @Test
    fun canonicalEncodingCannotCollideAcrossDistinctObjectShapes() {
        val nestedDelimiter = ToolValue.ObjectValue.of(mapOf("a" to ToolValue.StringValue("x,b:string:y")))
        val splitFields = ToolValue.ObjectValue.of(mapOf("a" to ToolValue.StringValue("x"), "b" to ToolValue.StringValue("y")))
        assertFalse(nestedDelimiter.canonicalEncoding() == splitFields.canonicalEncoding())

        val first = ToolCall(callId1, ToolName("create_file"), nestedDelimiter)
        val second = ToolCall(callId1, ToolName("create_file"), splitFields)
        assertFalse(first.computeFingerprint(ToolEffect.MUTATING) == second.computeFingerprint(ToolEffect.MUTATING))
    }

    @Test
    fun toolResultRequiresExactlyOneOutcome() {
        val bounded = BoundedOutputMetadata(truncated = false, returnedItems = 1, returnedCharacters = 10)
        val resSuccess = ToolResult(callId1, ToolName("git_status"), ToolOutcome.Success(ToolValue.StringValue("clean")), bounded)
        assertTrue(resSuccess.outcome is ToolOutcome.Success)
    }

    @Test
    fun toolResultPreservesCallIdAndToolName() {
        val bounded = BoundedOutputMetadata(truncated = false, returnedItems = 1, returnedCharacters = 10)
        val toolName = ToolName("git_status")
        val res = ToolResult(callId1, toolName, ToolOutcome.Success(ToolValue.StringValue("clean")), bounded)
        assertEquals(callId1, res.id)
        assertEquals(toolName, res.toolName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundedMetadataRejectsNegativeOrImpossibleCounts() {
        BoundedOutputMetadata(truncated = false, returnedItems = 10, totalItems = 5, returnedCharacters = 10)
    }

    @Test
    fun truncatedContinuableOutputRequiresToken() {
        val meta = BoundedOutputMetadata(
            truncated = true,
            returnedItems = 10,
            totalItems = 20,
            returnedCharacters = 100,
            continuationToken = ContinuationToken("tok-1"),
        )
        assertTrue(meta.truncated)
        assertEquals(ContinuationToken("tok-1"), meta.continuationToken)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonTruncatedOutputRejectsContinuationToken() {
        BoundedOutputMetadata(
            truncated = false,
            returnedItems = 10,
            totalItems = 10,
            returnedCharacters = 100,
            continuationToken = ContinuationToken("tok-1"),
        )
    }

    @Test
    fun onlyIndependentReadOnlyBatchIsConcurrent() {
        val call1 = ToolCall(callId1, ToolName("search_text"), ToolValue.ObjectValue.of(mapOf("query" to ToolValue.StringValue("foo"))))
        val call2 = ToolCall(callId2, ToolName("git_status"), ToolValue.ObjectValue.of(emptyMap()))

        val plan = ToolBatchPlanner.planBatch(listOf(call1, call2))
        assertTrue(plan is BatchPlan.ConcurrentReadOnly)
    }

    @Test
    fun everyEffectfulBatchIsOrdered() {
        val call1 = ToolCall(callId1, ToolName("search_text"), ToolValue.ObjectValue.of(mapOf("query" to ToolValue.StringValue("foo"))))
        val call2 = ToolCall(
            callId2,
            ToolName("create_file"),
            ToolValue.ObjectValue.of(mapOf("path" to ToolValue.StringValue("src/Test.kt"), "content" to ToolValue.StringValue("content"))),
        )

        val plan = ToolBatchPlanner.planBatch(listOf(call1, call2))
        assertTrue(plan is BatchPlan.Ordered)
    }

    @Test
    fun completeTaskMixedBatchIsRejectedBeforeAnyExecution() {
        val call1 = ToolCall(callId1, ToolName("complete_task"), ToolValue.ObjectValue.of(mapOf("summary" to ToolValue.StringValue("Done"))))
        val call2 = ToolCall(callId2, ToolName("git_status"), ToolValue.ObjectValue.of(emptyMap()))

        val plan = ToolBatchPlanner.planBatch(listOf(call1, call2))
        assertTrue(plan is BatchPlan.Rejected)
        assertEquals(ErrorCode.CONTROL_BATCH_INVALID, (plan as BatchPlan.Rejected).error.code)
    }

    @Test
    fun duplicateBatchIdsAreRejectedBeforeBudgetOrExecution() {
        val call1 = ToolCall(callId1, ToolName("git_status"), ToolValue.ObjectValue.of(emptyMap()))
        val call2 = ToolCall(callId1, ToolName("search_text"), ToolValue.ObjectValue.of(mapOf("query" to ToolValue.StringValue("foo"))))

        val plan = ToolBatchPlanner.planBatch(listOf(call1, call2))
        assertTrue(plan is BatchPlan.Rejected)
        assertEquals(ErrorCode.DUPLICATE_CALL_ID_IN_BATCH, (plan as BatchPlan.Rejected).error.code)
    }

    @Test
    fun unknownToolAndInvalidArgumentsRejectBeforeExecution() {
        val unknownCall = ToolCall(callId1, ToolName("unknown_tool"), ToolValue.ObjectValue.of(emptyMap()))
        val plan = ToolBatchPlanner.planBatch(listOf(unknownCall))
        assertTrue(plan is BatchPlan.Rejected)
        assertEquals(ErrorCode.UNKNOWN_TOOL, (plan as BatchPlan.Rejected).error.code)
    }
}
