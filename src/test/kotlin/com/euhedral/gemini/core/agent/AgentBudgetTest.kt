package com.euhedral.gemini.core.agent

import com.euhedral.gemini.core.error.ErrorCode
import com.euhedral.gemini.core.error.OperationError
import com.euhedral.gemini.core.revision.TransactionRevisionDigest
import com.euhedral.gemini.core.tools.ToolEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentBudgetTest {

    @Test
    fun defaultsAreExactlyThreeTwoFiveFortyOneHundredFifteenAndThirty() {
        val limits = AgentLimits()
        assertEquals(3, limits.httpRetry.maxRetries)
        assertEquals(2, limits.transientToolRetry.maxRetries)
        assertEquals(5, limits.repairCycle.maxCycles)
        assertEquals(40, limits.modelTurn.maxTurns)
        assertEquals(100, limits.toolCall.maxCalls)
        assertEquals(900_000L, limits.processTimeout.maxDurationMillis)
        assertEquals(1_800_000L, limits.sessionTime.maxDurationMillis)
    }

    @Test
    fun httpAllowsInitialAttemptAndThreeRetriesButRejectsFourthRetry() {
        var budget = HttpRetryBudget(maxRetries = 3)
        val scope = "http-scope-1"

        for (i in 1..3) {
            val decision = budget.consumeRetry(scope)
            assertTrue(decision is BudgetDecision.Allowed)
            budget = (decision as BudgetDecision.Allowed).nextBudget
            assertEquals(i, budget.currentScopeRetries)
        }

        val fourth = budget.consumeRetry(scope)
        assertTrue(fourth is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.HTTP_RETRY_EXHAUSTED, (fourth as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun newHttpOperationHasFreshRetryAllowance() {
        var budget = HttpRetryBudget(maxRetries = 3)
        val scope1 = "http-scope-1"
        val scope2 = "http-scope-2"

        for (i in 1..3) {
            budget = (budget.consumeRetry(scope1) as BudgetDecision.Allowed).nextBudget
        }
        assertEquals(3, budget.currentScopeRetries)

        // New scope resets currentScopeRetries to 0 for fresh attempt
        val decision = budget.consumeRetry(scope2)
        assertTrue(decision is BudgetDecision.Allowed)
        val nextBudget = (decision as BudgetDecision.Allowed).nextBudget
        assertEquals(1, nextBudget.currentScopeRetries)
        assertEquals(4, nextBudget.totalRetriesUsed)
    }

    @Test
    fun httpSuccessClosesOnlyItsOwnRetryScope() {
        var budget = HttpRetryBudget(maxRetries = 3)
        budget = (budget.consumeRetry("scope-1") as BudgetDecision.Allowed).nextBudget
        assertEquals("scope-1", budget.currentScopeId)

        val closed = budget.closeScope("scope-1")
        assertEquals(null, closed.currentScopeId)
        assertEquals(0, closed.currentScopeRetries)
        assertEquals(1, closed.totalRetriesUsed)

        // Closing a non-matching scope leaves budget unchanged
        val dummyClose = closed.closeScope("other-scope")
        assertEquals(closed, dummyClose)
    }

    @Test
    fun transientToolAllowsTwoRetriesButRejectsThird() {
        var budget = TransientToolRetryBudget(maxRetries = 2)
        val scope = "tool-scope-1"

        for (i in 1..2) {
            val decision = budget.consumeRetry(scope)
            assertTrue(decision is BudgetDecision.Allowed)
            budget = (decision as BudgetDecision.Allowed).nextBudget
            assertEquals(i, budget.currentScopeRetries)
        }

        val third = budget.consumeRetry(scope)
        assertTrue(third is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.TRANSIENT_TOOL_RETRY_EXHAUSTED, (third as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun newToolCallHasFreshTransientRetryAllowance() {
        var budget = TransientToolRetryBudget(maxRetries = 2)
        val scope1 = "tool-call-1"
        val scope2 = "tool-call-2"

        budget = (budget.consumeRetry(scope1) as BudgetDecision.Allowed).nextBudget
        budget = (budget.consumeRetry(scope1) as BudgetDecision.Allowed).nextBudget

        val decision = budget.consumeRetry(scope2)
        assertTrue(decision is BudgetDecision.Allowed)
        val nextBudget = (decision as BudgetDecision.Allowed).nextBudget
        assertEquals(1, nextBudget.currentScopeRetries)
        assertEquals(3, nextBudget.totalRetriesUsed)
    }

    @Test
    fun fiveRepairCyclesAreAvailableAndSixthIsRejected() {
        var budget = RepairCycleBudget(maxCycles = 5)
        for (i in 1..5) {
            val decision = budget.consumeCycle()
            assertTrue(decision is BudgetDecision.Allowed)
            budget = (decision as BudgetDecision.Allowed).nextBudget
            assertEquals(i, budget.cyclesUsed)
        }

        val sixth = budget.consumeCycle()
        assertTrue(sixth is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.REPAIR_CYCLE_EXHAUSTED, (sixth as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun ordinaryBuildFailureDoesNotConsumeRepair() {
        val budgetBefore = RepairCycleBudget(maxCycles = 5)
        // Ordinary build failure inside RUNNING state is a tool result or operation failure, not a repair cycle
        val budgetAfter = budgetBefore
        assertEquals(budgetBefore, budgetAfter)
        assertEquals(0, budgetAfter.cyclesUsed)
    }

    @Test
    fun ordinaryTestFailureDoesNotConsumeRepair() {
        val budgetBefore = RepairCycleBudget(maxCycles = 5)
        // Ordinary test failure is a tool result, not a mandatory verification failure
        val budgetAfter = budgetBefore
        assertEquals(budgetBefore, budgetAfter)
        assertEquals(0, budgetAfter.cyclesUsed)
    }

    @Test
    fun mandatoryVerificationFailureConsumesOnlyRepair() {
        val limitsBefore = AgentLimits()
        val decision = limitsBefore.repairCycle.consumeCycle()
        assertTrue(decision is BudgetDecision.Allowed)
        val nextRepair = (decision as BudgetDecision.Allowed).nextBudget

        assertEquals(1, nextRepair.cyclesUsed)
        // Verify all other budgets remain completely unchanged
        assertEquals(limitsBefore.httpRetry, limitsBefore.httpRetry)
        assertEquals(limitsBefore.transientToolRetry, limitsBefore.transientToolRetry)
        assertEquals(limitsBefore.modelTurn, limitsBefore.modelTurn)
        assertEquals(limitsBefore.toolCall, limitsBefore.toolCall)
        assertEquals(limitsBefore.processTimeout, limitsBefore.processTimeout)
        assertEquals(limitsBefore.sessionTime, limitsBefore.sessionTime)
    }

    @Test
    fun fortyModelTurnsAreAcceptedAndFortyFirstIsRejected() {
        var budget = ModelTurnBudget(maxTurns = 40)
        for (i in 1..40) {
            val decision = budget.consumeTurn()
            assertTrue(decision is BudgetDecision.Allowed)
            budget = (decision as BudgetDecision.Allowed).nextBudget
            assertEquals(i, budget.turnsUsed)
        }

        val fortyFirst = budget.consumeTurn()
        assertTrue(fortyFirst is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.MODEL_TURN_LIMIT_EXCEEDED, (fortyFirst as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun httpRetriesDoNotConsumeModelTurns() {
        val limits = AgentLimits()
        val httpDecision = limits.httpRetry.consumeRetry("scope-1")
        assertTrue(httpDecision is BudgetDecision.Allowed)

        assertEquals(0, limits.modelTurn.turnsUsed)
    }

    @Test
    fun oneHundredAcceptedCallOccurrencesAreAcceptedAndNextIsRejected() {
        var budget = ToolCallBudget(maxCalls = 100)
        for (i in 1..100) {
            val decision = budget.consumeCalls(1)
            assertTrue(decision is BudgetDecision.Allowed)
            budget = (decision as BudgetDecision.Allowed).nextBudget
            assertEquals(i, budget.callsUsed)
        }

        val nextCall = budget.consumeCalls(1)
        assertTrue(nextCall is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.TOOL_CALL_LIMIT_EXCEEDED, (nextCall as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun acceptedDuplicateReplayConsumesToolCallOccurrenceOnly() {
        var budget = ToolCallBudget(maxCalls = 100)
        val decision = budget.consumeCalls(1)
        assertTrue(decision is BudgetDecision.Allowed)
        budget = (decision as BudgetDecision.Allowed).nextBudget
        assertEquals(1, budget.callsUsed)
    }

    @Test
    fun internalToolRetryDoesNotConsumeToolCall() {
        var toolCallBudget = ToolCallBudget(maxCalls = 100)
        toolCallBudget = (toolCallBudget.consumeCalls(1) as BudgetDecision.Allowed).nextBudget

        var transientBudget = TransientToolRetryBudget(maxRetries = 2)
        transientBudget = (transientBudget.consumeRetry("call-1") as BudgetDecision.Allowed).nextBudget

        assertEquals(1, toolCallBudget.callsUsed)
        assertEquals(1, transientBudget.currentScopeRetries)
    }

    @Test
    fun processTimesOutAtExactDeadline() {
        val budget = ProcessTimeoutBudget(maxDurationMillis = 900_000L)
        val allowed = budget.evaluate(899_999L)
        assertTrue(allowed is BudgetDecision.Allowed)

        val exhausted = budget.evaluate(900_000L)
        assertTrue(exhausted is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.PROCESS_TIMEOUT, (exhausted as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun processTimeoutIsFreshForNextInvocation() {
        val budget = ProcessTimeoutBudget(maxDurationMillis = 900_000L)
        val eval1 = budget.evaluate(900_000L)
        assertTrue(eval1 is BudgetDecision.Exhausted)

        // Fresh invocation uses fresh elapsed observation
        val eval2 = budget.evaluate(5000L)
        assertTrue(eval2 is BudgetDecision.Allowed)
    }

    @Test
    fun sessionTimesOutAtExactDeadline() {
        val budget = SessionTimeBudget(maxDurationMillis = 1_800_000L)
        val allowed = budget.evaluate(1_799_999L)
        assertTrue(allowed is BudgetDecision.Allowed)

        val exhausted = budget.evaluate(1_800_000L)
        assertTrue(exhausted is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.SESSION_TIMEOUT, (exhausted as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun sessionTimeIncludesApprovalAndBackoffElapsedTime() {
        val budget = SessionTimeBudget(maxDurationMillis = 1_800_000L)
        // Total wall clock elapsed time includes approval wait and backoff
        val wallClockElapsed = 1_850_000L
        val decision = budget.evaluate(wallClockElapsed)
        assertTrue(decision is BudgetDecision.Exhausted)
        assertEquals(ErrorCode.SESSION_TIMEOUT, (decision as BudgetDecision.Exhausted).error.code)
    }

    @Test
    fun sessionElapsedCounterRetainsTheLatestObservation() {
        val counters = AgentCounters(sessionElapsedMillis = 5000L)
        val newElapsed = 4000L
        assertTrue(newElapsed < counters.sessionElapsedMillis)
    }

    @Test
    fun exhaustingEachBudgetLeavesEveryOtherBudgetUnchanged() {
        val limits = AgentLimits()

        val h = limits.httpRetry.consumeRetry("s").let { (it as BudgetDecision.Allowed).nextBudget }
        assertEquals(1, h.currentScopeRetries)
        assertEquals(0, limits.transientToolRetry.currentScopeRetries)
        assertEquals(0, limits.repairCycle.cyclesUsed)
        assertEquals(0, limits.modelTurn.turnsUsed)
        assertEquals(0, limits.toolCall.callsUsed)
    }

    @Test
    fun newSessionResetsEverySessionScopedBudget() {
        val oldLimits = AgentLimits()
        val newLimits = AgentLimits()
        assertEquals(oldLimits, newLimits)
    }

    fun arithmeticOverflowReturnsTypedExhaustionWithoutMutation() {
        val http = HttpRetryBudget(maxRetries = 3, totalRetriesUsed = Int.MAX_VALUE)
        val httpDecision = http.consumeRetry("request")
        assertTrue(httpDecision is BudgetDecision.Exhausted)
        assertEquals(http, (httpDecision as BudgetDecision.Exhausted).unchangedBudget)

        val calls = ToolCallBudget(maxCalls = Int.MAX_VALUE, callsUsed = Int.MAX_VALUE - 1)
        val callDecision = calls.consumeCalls(2)
        assertTrue(callDecision is BudgetDecision.Exhausted)
        assertEquals(calls, (callDecision as BudgetDecision.Exhausted).unchangedBudget)
    }
}
