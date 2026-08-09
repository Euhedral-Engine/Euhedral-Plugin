package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.agent.ToolCallId
import com.euhedral.gemini.core.agent.ToolName
import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.error.DuplicateCallIdError
import com.euhedral.gemini.core.error.ErrorCategory
import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue

@SourceBearingValue
sealed interface BatchPlan {
    @SourceBearingValue
    data class ConcurrentReadOnly(val calls: List<ToolCall>) : BatchPlan

    @SourceBearingValue
    data class Ordered(val calls: List<ToolCall>) : BatchPlan

    @SourceBearingValue
    data class Rejected(val error: AgentError) : BatchPlan
}

object ToolBatchPlanner {
    fun planBatch(
        calls: List<ToolCall>,
        descriptors: Map<ToolName, ToolDescriptor> = StandardToolDescriptors.BY_NAME,
    ): BatchPlan {
        if (calls.isEmpty()) {
            return BatchPlan.Rejected(
                OperationError(
                    category = ErrorCategory.TOOL_CALL,
                    code = ErrorCode.CONTROL_BATCH_INVALID,
                    safeMessage = "Tool call batch cannot be empty",
                )
            )
        }

        val seenIds = mutableSetOf<ToolCallId>()
        for (call in calls) {
            if (!seenIds.add(call.id)) {
                return BatchPlan.Rejected(
                    DuplicateCallIdError(
                        code = ErrorCode.DUPLICATE_CALL_ID_IN_BATCH,
                        safeMessage = "Duplicate call ID '${call.id.value}' detected within single response batch",
                    )
                )
            }
        }

        for (call in calls) {
            val descriptor = descriptors[call.name]
                ?: return BatchPlan.Rejected(
                    OperationError(
                        category = ErrorCategory.TOOL_CALL,
                        code = ErrorCode.UNKNOWN_TOOL,
                        safeMessage = "Unknown tool name '${call.name.value}'",
                    )
                )

            val validationError = validateArguments(call, descriptor)
            if (validationError != null) {
                return BatchPlan.Rejected(validationError)
            }
        }

        val hasCompleteTask = calls.any { it.name.value == "complete_task" }
        if (hasCompleteTask && calls.size > 1) {
            return BatchPlan.Rejected(
                OperationError(
                    category = ErrorCategory.TOOL_CALL,
                    code = ErrorCode.CONTROL_BATCH_INVALID,
                    safeMessage = "'complete_task' must be the sole tool call in its batch",
                )
            )
        }

        val allConcurrentReadOnly = calls.all { call ->
            val desc = descriptors.getValue(call.name)
            desc.effect == ToolEffect.READ_ONLY && desc.independentlyExecutable
        }

        return if (allConcurrentReadOnly) {
            BatchPlan.ConcurrentReadOnly(calls)
        } else {
            BatchPlan.Ordered(calls)
        }
    }

    private fun validateArguments(call: ToolCall, descriptor: ToolDescriptor): AgentError? {
        val argsMap = call.arguments.entries
        for (param in descriptor.schema.parameters) {
            if (param.required && !argsMap.containsKey(param.name)) {
                return OperationError(
                    category = ErrorCategory.TOOL_CALL,
                    code = ErrorCode.INVALID_TOOL_ARGUMENTS,
                    safeMessage = "Missing required argument '${param.name}' for tool '${call.name.value}'",
                )
            }
            val argVal = argsMap[param.name] ?: continue
            when (param.type) {
                ToolParameterType.STRING -> if (argVal !is ToolValue.StringValue) {
                    return OperationError(
                        category = ErrorCategory.TOOL_CALL,
                        code = ErrorCode.INVALID_TOOL_ARGUMENTS,
                        safeMessage = "Argument '${param.name}' for tool '${call.name.value}' must be String",
                    )
                }
                ToolParameterType.INTEGER -> if (argVal !is ToolValue.IntegerValue) {
                    return OperationError(
                        category = ErrorCategory.TOOL_CALL,
                        code = ErrorCode.INVALID_TOOL_ARGUMENTS,
                        safeMessage = "Argument '${param.name}' for tool '${call.name.value}' must be Integer",
                    )
                }
                ToolParameterType.BOOLEAN -> if (argVal !is ToolValue.BooleanValue) {
                    return OperationError(
                        category = ErrorCategory.TOOL_CALL,
                        code = ErrorCode.INVALID_TOOL_ARGUMENTS,
                        safeMessage = "Argument '${param.name}' for tool '${call.name.value}' must be Boolean",
                    )
                }
            }
        }

        for (argName in argsMap.keys) {
            if (descriptor.schema.parameters.none { it.name == argName }) {
                return OperationError(
                    category = ErrorCategory.TOOL_CALL,
                    code = ErrorCode.INVALID_TOOL_ARGUMENTS,
                    safeMessage = "Undeclared argument '$argName' for tool '${call.name.value}'",
                )
            }
        }

        return null
    }
}
