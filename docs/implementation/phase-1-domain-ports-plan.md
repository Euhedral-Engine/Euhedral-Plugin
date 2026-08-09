# Phase 1 Domain and Ports Implementation Plan

Status: Implemented

Plan date: 2026-08-09

Source blueprint: `docs/design/phases/phase-1-domain-ports.md`

Prerequisite: `docs/implementation/phase-0-platform-plan.md`

## 1. Outcome and scope

Phase 1 will replace the empty `core` and `ports` package markers with pure,
adapter-neutral contracts. The phase freezes the state machine, events, limits,
tool protocol, errors, revisions, mutation de-duplication, policy values, and
the eight application-facing ports needed by later phases.

This plan does not authorize an execution engine or any concrete adapter. Phase
1 production code must not load IntelliJ, start coroutines, call Gemini, inspect
a project, edit files, run Gradle, access Git, read credentials, write a
checkpoint, or display approval UI. It defines values and interfaces only.

The current repository baseline is Phase 0 at commit `318924a`. `core`,
`application`, and `ports` contain markers only. The existing architecture test
allows `core` to depend only on `core`, Java, and Kotlin, and allows `ports` to
depend only on `ports`, `core`, Java, and Kotlin. Those restrictions remain in
force. No Gradle dependency or plugin descriptor change is needed.

The following prompt ambiguities are resolved and locked for this plan:

- Successful verification remains in top-level `VERIFYING` with subphase
  `PASSED_AWAITING_FINAL`; only the next eligible final model response completes
  the session.
- `SessionRevision` is a monotonic state version. Cryptographic identity is
  represented by `TransactionRevisionDigest` and
  `VerifiedTransactionDigest`; no `SessionRevisionDigest` is introduced.
- Streaming ports use suspendable runtime-local sinks, not `Flow`, preserving
  the Phase 0 `ports` dependency allowlist.
- Five repair cycles are available. The sixth failed mandatory verification is
  terminal. Read-only duplicate IDs execute again across interactions, while
  duplicate IDs inside one response batch are rejected before execution.

## 2. Value-boundary notation

Every type named below is marked with one of these labels in source KDoc and in
the contract inventory:

- `[S]`: immutable, structurally serializable value. Its complete object graph
  contains only primitives, strings, enums, immutable lists or maps, and other
  `[S]` or `[SP]` values.
- `[SP]`: structurally serializable but source-bearing or model-bearing. It may
  cross a required port boundary but must not be persisted in ordinary session
  metadata, logged, or included in telemetry.
- `[L]`: live, secret, callback, or adapter-local value. It is never serialized,
  compared as session state, emitted as an event, or placed in an error.

Add `SerializableValue`, `SourceBearingValue`, and `AdapterLocalValue` marker
annotations in `core.serialization`. These are contract markers, not
`java.io.Serializable`, and they add no codec dependency. Phase 1 does not pick
JSON, CBOR, Java serialization, or a persistence library.

All times crossing a boundary use non-negative epoch or elapsed milliseconds.
All model-visible paths use validated project-relative slash-separated strings.
No portable value contains `Throwable`, `File`, `Path`, `URI`, a platform
object, a mutable collection, a function, or an arbitrary `Any` value.

## 3. Planned file inventory

Replace `CorePackage.kt` and `PortsPackage.kt` when their real types are added.
Keep `ApplicationPackage.kt`; Phase 1 adds no orchestration.

Production files:

```text
src/main/kotlin/com/euhedral/gemini/core/serialization/ValueBoundary.kt
src/main/kotlin/com/euhedral/gemini/core/agent/AgentIdentifiers.kt
src/main/kotlin/com/euhedral/gemini/core/agent/AgentState.kt
src/main/kotlin/com/euhedral/gemini/core/agent/AgentEvent.kt
src/main/kotlin/com/euhedral/gemini/core/agent/AgentBudget.kt
src/main/kotlin/com/euhedral/gemini/core/agent/AgentReducer.kt
src/main/kotlin/com/euhedral/gemini/core/agent/MutatingCallLedger.kt
src/main/kotlin/com/euhedral/gemini/core/error/AgentError.kt
src/main/kotlin/com/euhedral/gemini/core/revision/Revision.kt
src/main/kotlin/com/euhedral/gemini/core/tools/ToolEffect.kt
src/main/kotlin/com/euhedral/gemini/core/tools/ToolCall.kt
src/main/kotlin/com/euhedral/gemini/core/tools/ToolResult.kt
src/main/kotlin/com/euhedral/gemini/core/tools/ToolDescriptor.kt
src/main/kotlin/com/euhedral/gemini/core/tools/ToolBatchPlanner.kt
src/main/kotlin/com/euhedral/gemini/core/policy/PolicyDecision.kt
src/main/kotlin/com/euhedral/gemini/core/policy/ValidationFinding.kt
src/main/kotlin/com/euhedral/gemini/ports/PortResult.kt
src/main/kotlin/com/euhedral/gemini/ports/GeminiTransport.kt
src/main/kotlin/com/euhedral/gemini/ports/GeminiModels.kt
src/main/kotlin/com/euhedral/gemini/ports/WorkspaceInspectionPort.kt
src/main/kotlin/com/euhedral/gemini/ports/InspectionModels.kt
src/main/kotlin/com/euhedral/gemini/ports/WorkspaceEditPort.kt
src/main/kotlin/com/euhedral/gemini/ports/EditModels.kt
src/main/kotlin/com/euhedral/gemini/ports/BuildSystemPort.kt
src/main/kotlin/com/euhedral/gemini/ports/BuildModels.kt
src/main/kotlin/com/euhedral/gemini/ports/GitReadPort.kt
src/main/kotlin/com/euhedral/gemini/ports/GitModels.kt
src/main/kotlin/com/euhedral/gemini/ports/CredentialPort.kt
src/main/kotlin/com/euhedral/gemini/ports/CredentialModels.kt
src/main/kotlin/com/euhedral/gemini/ports/CheckpointStore.kt
src/main/kotlin/com/euhedral/gemini/ports/CheckpointModels.kt
src/main/kotlin/com/euhedral/gemini/ports/ApprovalPort.kt
src/main/kotlin/com/euhedral/gemini/ports/ApprovalModels.kt
```

Test files:

```text
src/test/kotlin/com/euhedral/gemini/core/agent/AgentReducerTest.kt
src/test/kotlin/com/euhedral/gemini/core/agent/AgentBudgetTest.kt
src/test/kotlin/com/euhedral/gemini/core/agent/MutatingCallLedgerTest.kt
src/test/kotlin/com/euhedral/gemini/core/revision/VerificationDigestTest.kt
src/test/kotlin/com/euhedral/gemini/core/tools/ToolContractTest.kt
src/test/kotlin/com/euhedral/gemini/core/error/AgentErrorTest.kt
src/test/kotlin/com/euhedral/gemini/ports/ApplicationPortContractTest.kt
src/test/kotlin/com/euhedral/gemini/ports/SerializationBoundaryTest.kt
src/test/kotlin/com/euhedral/gemini/architecture/fixtures/ForbiddenPortTypeFixture.kt
```

Update the existing `PackageArchitectureTest.kt`. Do not add files under
`adapters`, `application`, `bootstrap`, `ui`, `settings`, or `completion`.

## 4. Identifiers, revisions, and digests

### 4.1 Portable identifiers

Define validated `[S]` value classes for:

```text
SessionId
SessionRevision
EventSequence
ModelRequestId
InteractionId
TransactionId
TransactionRevision
VerificationRunId
ToolCallId
ToolName
ToolInvocationFingerprint
ApprovalRequestId
ProcessInvocationId
CheckpointBlobId
ProjectFingerprint
ProjectPath
ContentHash
Sha256Digest
ContinuationToken
```

IDs are non-blank and length-bounded. `SessionRevision`, `EventSequence`, and
`TransactionRevision` are non-negative `Long` values. Increment operations
check overflow and return a typed error instead of wrapping.

`Sha256Digest` and `ContentHash` accept exactly 64 lowercase ASCII hexadecimal
characters. A digest value never accepts an algorithm name, uppercase text, or
an unvalidated arbitrary string.

### 4.2 Session revision

`SessionRevision` is the optimistic-concurrency version of `AgentSession`.
Every accepted state event increments it exactly once. Rejected events leave it
unchanged. An event envelope supplies `sessionId` and `expectedRevision`; the
reducer rejects foreign, late, or replayed state events explicitly.

Event timeline sequence and session revision are separate. Timeline deltas may
be emitted without mutating session state, and therefore do not silently change
`SessionRevision`.

### 4.3 Transaction and verified digests

Define:

```kotlin
@SerializableValue
internal data class TransactionRevisionDigest(
    val transactionId: TransactionId,
    val revision: TransactionRevision,
    val digest: Sha256Digest,
)

@SerializableValue
internal data class VerifiedTransactionDigest(
    val transaction: TransactionRevisionDigest,
    val verificationRunId: VerificationRunId,
)
```

The workspace adapter will calculate a transaction digest from this versioned
canonical sequence:

```text
transaction-v1
transaction ID
transaction revision
for every transaction-touched path, sorted by normalized ProjectPath:
  path
  existence marker
  current content hash, or a fixed absent marker
```

Each field is UTF-8 length-prefixed before SHA-256 hashing. Including the
transaction ID and revision means a mutate-then-restore operation has a new
identity even if its final file bytes equal an older state. Core never reads
files or computes content hashes; it validates and compares supplied digests.

Every committed create, replace, delete, move, rollback, or externally observed
transaction change must advance revision by exactly one. A failed pre-effect
request and a genuine no-op do not advance it.

## 5. Agent session and state machine

### 5.1 State values

`AgentState` is the exact `[S]` enum:

```text
IDLE
RUNNING
WAITING_APPROVAL
VERIFYING
COMPLETED
FAILED
CANCELLED
```

`VerificationPhase` is the exact `[S]` enum:

```text
IN_PROGRESS
PASSED_AWAITING_FINAL
```

`AgentSession` is an immutable `[SP]` value because its in-memory mutation
ledger may contain an exact source-bearing result. Its secret-free
`SessionRecoveryMetadata` projection is `[SP]`: it excludes model text and
credential material but retains the bounded terminal tool result required to
replay a completed mutating call exactly after restoration. `AgentSession` contains:

```text
id: SessionId
revision: SessionRevision
state: AgentState
projectFingerprint: ProjectFingerprint
transaction: TransactionRevisionDigest
verifiedTransaction: VerifiedTransactionDigest?
verificationAttempt: VerificationAttempt?
pendingApproval: PendingApproval?
remoteInteractionIds: List<InteractionId>
limits: AgentLimits
counters: AgentCounters
mutatingCalls: MutatingCallLedger
terminalReason: TerminalReason?
```

The caller supplies IDs, initial transaction digest, limits, and start time.
The reducer never reads a clock, generates an ID, computes a file hash, logs,
launches work, or invokes a port.

State invariants are constructor-checked:

- `IDLE` has no pending approval, verification attempt, verified digest, remote
  interaction, or terminal reason.
- `WAITING_APPROVAL` has exactly one pending approval. No other state does.
- `VERIFYING` has exactly one verification attempt. No other state does.
- `PASSED_AWAITING_FINAL` has a verified digest equal to both the attempt's
  frozen digest and the current transaction digest.
- `COMPLETED` has `TerminalReason.Completed` and an exactly current verified
  digest.
- `FAILED` has `TerminalReason.Failed`.
- `CANCELLED` has `TerminalReason.Cancelled`.
- Non-terminal states have no terminal reason.
- Terminal states are absorbing.

`TerminalReason` is a sealed `[S]` value:

```text
Completed(summary, verifiedTransaction)
Failed(error)
Cancelled(cancellationReason)
```

`CancellationReason` contains only these stable values:

```text
USER_REQUESTED
PROJECT_CLOSED
PLUGIN_UNLOADED
APPROVAL_CANCELLED
SESSION_TIMEOUT_WHILE_WAITING
SUPERSEDED
```

### 5.2 Reducer contract

```kotlin
internal object AgentReducer {
    fun reduce(
        session: AgentSession,
        envelope: AgentStateEventEnvelope,
    ): Reduction
}

internal sealed interface Reduction {
    data class Accepted(val session: AgentSession) : Reduction
    data class Rejected(
        val unchangedSession: AgentSession,
        val error: AgentError,
    ) : Reduction
}
```

`AgentStateEventEnvelope` is `[S]` and contains `sessionId`,
`expectedRevision`, and an `AgentStateEvent`. It contains no timestamp because
time observations are explicit event data. Accepted reduction increments the
revision exactly once. Rejection returns the original session instance and no
effects.

Validation order is stable so tests can assert exact errors:

1. Session ID match.
2. Expected session revision match.
3. Source-state and verification-subphase eligibility.
4. Correlation ID match for approvals, requests, calls, and verification runs.
5. Budget availability.
6. Transaction revision and digest preconditions.
7. Event-specific state invariants.

### 5.3 Reduced state events

`AgentStateEvent` is a sealed `[S]` hierarchy with these final variants:

```text
SessionStarted
ModelTurnStarted
InteractionRecorded
ToolCallsAccepted
RetryConsumed
RetryScopeReset
ProcessTimingObserved
SessionTimingObserved
MutatingCallReserved
MutatingCallCompleted
TransactionRevisionChanged
ApprovalRequested
ExternalModificationDetected
ApprovalResolved
VerificationStarted
VerificationSucceeded
VerificationFailed
VerificationAborted
WorkResumed
SessionCompleted
SessionFailed
SessionCancelled
```

`SessionStarted` carries the elapsed-time origin. `ModelTurnStarted`,
`ToolCallsAccepted`, retry events, and `ProcessTimingObserved` update only their
own budgets. `SessionTimingObserved` reports elapsed session wall-clock time
independently of any process invocation, including approval and backoff waits.
`InteractionRecorded` appends one new remote interaction ID and
rejects a duplicate or out-of-order continuation.

`MutatingCallReserved` and `MutatingCallCompleted` update only the ledger.
`TransactionRevisionChanged` is the sole reducer event that advances the
transaction and it always clears `verifiedTransaction`. It also clears or
invalidates a pending approval and aborts an active verification as described
below.

### 5.4 Exact transition matrix

The valid top-level transitions are:

| Source | Event | Target | Additional guard |
| --- | --- | --- | --- |
| IDLE | SessionStarted | RUNNING | Revision is zero and initial transaction is revision zero. |
| RUNNING | ApprovalRequested | WAITING_APPROVAL | No pending approval. |
| RUNNING | ExternalModificationDetected | WAITING_APPROVAL | Carries a correlated conflict approval. |
| RUNNING | VerificationStarted | VERIFYING | Frozen digest equals current and current is not already verified. |
| RUNNING | SessionCompleted | COMPLETED | Current digest exactly equals verified digest. |
| RUNNING | SessionFailed | FAILED | Contains a model-safe terminal error. |
| RUNNING | SessionCancelled | CANCELLED | Contains a cancellation reason. |
| WAITING_APPROVAL | ApprovalResolved(GRANTED) | RUNNING | Request and revision guards match. |
| WAITING_APPROVAL | ApprovalResolved(DENIED) | RUNNING | Denial becomes a tool/control result. |
| WAITING_APPROVAL | ApprovalResolved(STALE) | RUNNING | Pending approval is discarded. |
| WAITING_APPROVAL | TransactionRevisionChanged | RUNNING | Pending approval is stale and verification is cleared. |
| WAITING_APPROVAL | SessionCancelled | CANCELLED | Includes user, lifecycle, approval, or wait-timeout reason. |
| VERIFYING | VerificationFailed | RUNNING | Repair budget can fund another repair cycle. |
| VERIFYING | VerificationFailed | FAILED | A sixth repair would be required. |
| VERIFYING | VerificationAborted | RUNNING | Does not consume repair. |
| VERIFYING | WorkResumed | RUNNING | Allowed only from PASSED_AWAITING_FINAL. |
| VERIFYING | TransactionRevisionChanged | RUNNING | Attempt is aborted and verification is cleared. |
| VERIFYING | SessionCompleted | COMPLETED | Subphase is PASSED_AWAITING_FINAL and digest is current. |
| VERIFYING | SessionFailed | FAILED | Contains a model-safe terminal error. |
| VERIFYING | SessionCancelled | CANCELLED | Contains a cancellation reason. |

Phase-preserving accepted events are:

- `VerificationSucceeded` keeps `VERIFYING`, changes its subphase from
  `IN_PROGRESS` to `PASSED_AWAITING_FINAL`, and records the verified digest.
- Operational and accounting events remain `RUNNING` when accepted there.
- Verification process timing and transient retry events remain `VERIFYING`
  only while subphase is `IN_PROGRESS`.
- `TransactionRevisionChanged` remains `RUNNING` when already running. From
  `WAITING_APPROVAL` or `VERIFYING` it returns to `RUNNING`, discards the stale
  approval or verification attempt, and clears verification.

All unlisted event/source combinations are invalid. In particular:

- `IDLE` cannot fail or cancel.
- `WAITING_APPROVAL` cannot complete, fail, mutate through an agent call, or
  begin verification.
- Model or process work cannot continue while approval is pending.
- `VerificationSucceeded` is invalid outside `VERIFYING/IN_PROGRESS`.
- `VerificationFailed` is invalid after `PASSED_AWAITING_FINAL`.
- `ApprovalDecision.Cancelled` is translated by orchestration to
  `SessionCancelled`; it is not reduced as `ApprovalResolved`.
- A final model response before current verification is rejected with
  `COMPLETION_NOT_VERIFIED` and leaves the session active.
- Every state event is rejected from `COMPLETED`, `FAILED`, and `CANCELLED`.

### 5.5 Observable event stream

`AgentEvent` is distinct from `AgentStateEvent`. The reducer consumes state
events; later orchestration emits ordered observable facts only after accepted
state changes or real operation progress. This prevents display deltas from
changing session concurrency state.

`AgentEventEnvelope` is `[S]` or `[SP]` according to its payload and contains:

```text
sessionId
eventSequence
sessionRevision
occurredAtEpochMillis
payload
```

Final payload variants and their required correlation fields are:

```text
SessionStarted(sessionId)
ModelRequestStarted(modelRequestId)
ModelOutputDelta(modelRequestId, boundedText)
InteractionRecorded(modelRequestId, interactionId)
ToolBatchAccepted(modelRequestId, callIds)
ToolStarted(callId, toolName, effect)
ToolCompleted(callId, toolName, result, executionDisposition)
RetryScheduled(retryClass, scopeId, retryNumber)
ApprovalRequested(request)
ApprovalResolved(decision)
FilesChanged(callId?, changes, transactionDigest)
ExternalModificationDetected(conflict)
ValidationFailed(callId, findings)
ProcessStarted(processInvocationId, logicalOperation)
ProcessOutput(processInvocationId, chunk)
ProcessFinished(processInvocationId, resultSummary)
VerificationStarted(verificationRunId, frozenDigest)
VerificationPassed(verificationRunId, verifiedDigest)
VerificationFailed(verificationRunId, error, repairCyclesUsed)
RepairStarted(repairCycleNumber)
SessionCompleted(reason)
SessionFailed(reason)
SessionCancelled(reason)
```

Event payloads never contain raw Google steps, PSI/VFS objects, process
handlers, exceptions, secrets, absolute paths, environment values, or unbounded
text.

## 6. Independent budgets and retry classes

### 6.1 Limits and counters

`AgentLimits` and `AgentCounters` are `[S]`. Implement seven separate immutable
budget types rather than a map keyed by enum:

| Type | Default | Scope | Exhaustion boundary |
| --- | ---: | --- | --- |
| HttpRetryBudget | 3 retries | One logical HTTP operation | Fourth retry request is rejected. |
| TransientToolRetryBudget | 2 retries | One accepted tool invocation | Third retry request is rejected. |
| RepairCycleBudget | 5 cycles | Session | Sixth failed mandatory verification is terminal. |
| ModelTurnBudget | 40 turns | Session | Forty-first model request is rejected. |
| ToolCallBudget | 100 calls | Session | Accepted occurrence 101 is rejected. |
| ProcessTimeoutBudget | 900000 ms | One process invocation | Timed out at elapsed >= limit. |
| SessionTimeBudget | 1800000 ms | Session wall clock | Timed out at elapsed >= limit. |

Each budget returns `BudgetDecision.Allowed(next)` or
`BudgetDecision.Exhausted(error, unchanged)`. It never throws, saturates, wraps,
or increments beyond its limit.

HTTP and transient retry values contain a scope ID and per-scope use count.
Starting a new request or tool call creates a fresh scope. Success or terminal
failure closes that scope. It does not reset any cumulative counter retained for
display. Process timeout is similarly fresh for every process invocation.

Repair, model-turn, tool-call, and session-time budgets reset only when a new
session is constructed. Session time includes transport backoff, approval wait,
verification, and repair. Process time cannot pause or extend session time.

### 6.2 Accounting rules

- A limit of three HTTP retries means one initial request plus at most three
  retries. Internal HTTP retries do not consume model turns.
- Consume a model turn immediately before each logical Interactions request.
- Consume a tool call for every schema-valid accepted occurrence, including an
  exact later duplicate that will replay a mutating result.
- Rejecting a malformed batch or repeated ID inside one batch consumes no tool
  calls because no occurrence was accepted.
- Internal transient attempts do not consume additional tool calls.
- Only failure of mandatory completion verification consumes a repair cycle.
  Ordinary model-requested build or test failure consumes none.
- Process start failure may use `TRANSIENT_TOOL`; a process that started and
  reached its deadline returns `PROCESS_TIMEOUT` and is not a start retry.
- HTTP, model-turn, tool-call, repair, and active session-time exhaustion ends an
  active `RUNNING` or `VERIFYING` session as `FAILED`.
- Transient-tool exhaustion returns a structured tool failure and remains
  `RUNNING`.
- A process timeout returns a structured process/tool result and remains
  `RUNNING`; when it occurs during mandatory verification, the enclosing
  verification failure separately consumes one repair cycle.
- Session timeout while `WAITING_APPROVAL` transitions to `CANCELLED` with
  `SESSION_TIMEOUT_WHILE_WAITING`, preserving the locked transition graph.

### 6.3 Retry classification

`RetryClass` is the exact `[S]` enum:

```text
NONE
HTTP
TRANSIENT_TOOL
REPAIR_CYCLE
```

The classification is exclusive. `HTTP` covers network failures, rate limiting,
and retryable server responses. `TRANSIENT_TOOL` covers indexing, temporary I/O,
and process-start failures. `REPAIR_CYCLE` is an engine recovery classification,
not an adapter retry; it applies only to failed mandatory verification. Every
other error is `NONE`.

## 7. Tool protocol

### 7.1 Calls and structured values

`ToolCall` is `[SP]` and contains `ToolCallId`, `ToolName`, and a closed
`ToolObjectValue` argument object. It never contains `Map<String, Any?>` or raw
JSON.

`ToolValue` is a sealed immutable `[S]` or `[SP]` tree:

```text
NullValue
BooleanValue
IntegerValue
DecimalValue, stored as validated canonical decimal text
StringValue
ListValue
ObjectValue, unique keys with canonical lexicographic ordering
```

Structural equality ignores object insertion order and preserves list order.
The canonical encoder rejects duplicate object keys, non-finite numbers, and
non-canonical decimals.

### 7.2 Descriptors and effects

`ToolEffect` is the exact `[S]` enum:

```text
READ_ONLY
MUTATING
PROCESS
CONTROL
```

`ToolDescriptor` `[S]` contains name, model-safe description, effect, a shallow
closed `ToolObjectSchema`, `BoundedOutputPolicy`, and
`independentlyExecutable`. It contains no executor, callback, port, or adapter
object.

`ToolParameter` supports only `STRING`, `INTEGER`, and `BOOLEAN`, with required,
description, enum values, numeric bounds, and string length or pattern metadata.
Schemas reject undeclared fields. Cross-field rules such as start line <= end
line are validated after structural schema validation.

`StandardToolDescriptors` defines exactly these 22 names and effects:

| Effect | Tool names |
| --- | --- |
| READ_ONLY | `workspace_context`, `read_file_range`, `search_text`, `find_symbol`, `find_references`, `find_implementations`, `file_metadata`, `git_status`, `git_diff`, `git_diff_file`, `git_log`, `git_blame` |
| MUTATING | `replace_text`, `create_file`, `delete_file`, `move_file` |
| PROCESS | `build_project`, `test_module`, `test_class`, `test_method` |
| CONTROL | `complete_task`, `request_commit` |

Required parameter schemas are:

| Tool | Required parameters | Optional parameters |
| --- | --- | --- |
| workspace_context | none | none |
| read_file_range | path, start_line, end_line | continuation_token |
| search_text | query | path, file_glob, continuation_token |
| find_symbol | name | kind, scope, continuation_token |
| find_references | symbol_id | continuation_token |
| find_implementations | symbol_id | continuation_token |
| file_metadata | path | none |
| replace_text | path, old_text, new_text, expected_hash | none |
| create_file | path, content | none |
| delete_file | path, expected_hash | none |
| move_file | source, destination, expected_hash | none |
| build_project | none | none |
| test_module | module | none |
| test_class | module, class_name | none |
| test_method | module, class_name, method_name | none |
| git_status | none | none |
| git_diff | none | continuation_token |
| git_diff_file | path | continuation_token |
| git_log | limit | continuation_token |
| git_blame | path, line | none |
| complete_task | summary | none |
| request_commit | message | none |

All current `READ_ONLY` descriptors set `independentlyExecutable=true`; all
others set it false.

### 7.3 Results and bounded output

`ToolResult` is `[SP]` and preserves call ID and tool name exactly. It contains
exactly one of `ToolOutcome.Success(value)` or
`ToolOutcome.Failure(AgentError)`, plus `BoundedOutputMetadata` and optional
transaction metadata. Constructor invariants reject both-or-neither outcomes.

`BoundedOutputMetadata` `[S]` contains:

```text
truncated: Boolean
returnedItems: Int
totalItems: Int?
returnedCharacters: Int
totalCharacters: Int?
continuationToken: ContinuationToken?
```

All counts are non-negative; returned values cannot exceed known totals. A
truncated continuable result requires a token. A non-truncated result has no
token. `BoundedText` and `BoundedList` pair content with this metadata.

### 7.4 Batch planning

`ToolBatchPlanner` is pure and returns `ConcurrentReadOnly`,
`Ordered`, or `Rejected(error)`.

- A non-empty batch is concurrent only when every descriptor is `READ_ONLY` and
  independently executable.
- Every batch containing `MUTATING`, `PROCESS`, or `CONTROL` is ordered in model
  response order.
- `complete_task` must be the only call in its batch.
- Every call ID in one response batch must be unique. A duplicate rejects the
  whole batch before budgets, policy, checkpoints, or effects.
- Unknown tools and schema failures reject before any call executes.

## 8. Mutating call de-duplication

`MutatingCallLedger` is immutable, `[SP]`, session-scoped, and keyed by
`ToolCallId`. A record contains the structurally canonical call fingerprint and
one of:

```text
Reserved
EffectStarted
Completed(exact ToolResult, resulting TransactionRevisionDigest?)
OutcomeUnknown
```

`ToolInvocationFingerprint` is SHA-256 of the version tag `tool-call-v1`, tool
name, effect, and canonical structural argument encoding. The call ID is the
ledger key and is not included in the fingerprint.

The pure claim operation returns exactly one disposition:

```text
Execute, after inserting Reserved for the first mutating occurrence
WaitForOriginal, for an exact duplicate whose original is still active
Replay, with the exact recorded result for a completed exact duplicate
RejectMismatch, for the same ID with a different name, effect, or arguments
RejectOutcomeUnknown, when recovery cannot prove the first effect's outcome
ExecuteAgain, for a read-only call, which is never entered in the ledger
```

The contract is at-most-once, not best-effort:

1. Validate the whole response batch and consume its call budget.
2. For a mutating call, reserve its ID before checkpointing or executing.
3. Persist the pending mutation and reservation before the adapter effect.
4. Mark `EffectStarted` immediately before crossing the mutation boundary.
5. Record either the final success or final failure result.
6. Replay that exact result for every later exact duplicate without policy,
   approval, checkpoint, adapter, validator, revision, or verification work.

Both successful and failed terminal results are replayed. Internal transient
retries belong to the original execution. A recovered reservation whose effect
cannot be proven absent or complete becomes `OutcomeUnknown`; it is never
speculatively executed.

The same ID is independent in a newly constructed session. Across interactions,
a duplicate `READ_ONLY` ID executes again. `PROCESS` and `CONTROL` calls are not
covered by the mutation ledger and retain their own orchestration rules.

An exact mutating replay consumes one accepted tool-call occurrence, but it does
not emit `TransactionRevisionChanged` and therefore cannot invalidate a current
verification.

## 9. Verification invalidation and completion

`VerificationStarted` freezes the exact current
`TransactionRevisionDigest`. It is rejected when that digest is already current
and verified, preventing a pointless re-verification from discarding evidence.

`VerificationSucceeded` is accepted only when its run ID and digest equal the
active attempt and the frozen digest still equals the current transaction. It
records `VerifiedTransactionDigest` and changes the subphase to
`PASSED_AWAITING_FINAL` without leaving `VERIFYING`.

The next model response has two choices:

- A final response dispatches `SessionCompleted` and transitions directly from
  `VERIFYING` to `COMPLETED`.
- More work dispatches `WorkResumed` and returns to `RUNNING`. Read-only,
  process, and non-mutating control work preserve the verified digest. A later
  actual transaction revision change clears it.

Any accepted `TransactionRevisionChanged` clears the verified digest before any
completion or commit eligibility check. This includes successful edit, create,
delete, move, rollback, mutate-then-restore, and external revision observation.
A failed pre-effect request, rejected mutation, no-op, read-only call, process
call, or duplicate mutating replay does not clear it.

`complete_task` is the only way to start mandatory verification. Completion and
future `request_commit` eligibility require exact equality of transaction ID,
revision, and digest between current and verified values. A mismatch returns
`COMPLETION_NOT_VERIFIED` and leaves the session active.

An external revision during verification is represented by
`TransactionRevisionChanged`, which aborts the attempt, returns to `RUNNING`,
and clears verification without consuming repair. If conflict approval is then
needed, a following `ExternalModificationDetected` enters
`WAITING_APPROVAL`.

## 10. Error taxonomy

All expected errors are immutable `[S]` data. Port adapters translate native
failures at their boundary. No error retains an exception, raw HTTP body,
credential, prompt, source text, environment value, command line, absolute path,
or unbounded output. Coroutine cancellation remains control flow and is never
converted into a normal `PortResult.Failure`.

`AgentError` is sealed and exposes `category`, `code`, `safeMessage`,
`retryClass`, and safe `Map<String, String>` details. Required concrete types
are `InvalidTransitionError`, `BudgetExhaustedError`,
`DuplicateCallIdError`, and `OperationError`.

`ErrorCategory` values:

```text
STATE
BUDGET
TOOL_CALL
TRANSPORT
CREDENTIAL
INSPECTION
EDIT
CONFLICT
POLICY
CHECKPOINT
PROCESS
BUILD
GIT_READ
APPROVAL
VERIFICATION
CANCELLATION
```

Required stable `ErrorCode` values:

```text
INVALID_TRANSITION
SESSION_ID_MISMATCH
STALE_SESSION_EVENT
APPROVAL_ID_MISMATCH
VERIFICATION_RUN_MISMATCH
TRANSACTION_REVISION_MISMATCH
COMPLETION_NOT_VERIFIED
VERIFIED_DIGEST_STALE
HTTP_RETRY_EXHAUSTED
TRANSIENT_TOOL_RETRY_EXHAUSTED
REPAIR_CYCLE_EXHAUSTED
MODEL_TURN_LIMIT_EXCEEDED
TOOL_CALL_LIMIT_EXCEEDED
PROCESS_TIMEOUT
SESSION_TIMEOUT
UNKNOWN_TOOL
INVALID_TOOL_ARGUMENTS
DUPLICATE_CALL_ID_IN_BATCH
DUPLICATE_CALL_ID_MISMATCH
MUTATING_CALL_OUTCOME_UNKNOWN
CONTROL_BATCH_INVALID
TRANSPORT_UNAVAILABLE
RATE_LIMITED
REMOTE_SERVER_ERROR
TRANSPORT_TIMEOUT
MALFORMED_RESPONSE
UNKNOWN_REQUIRED_STEP
INTERACTION_EXPIRED
CREDENTIAL_MISSING
CREDENTIAL_REJECTED
INVALID_PATH
INVALID_RANGE
INDEX_UNAVAILABLE
SYMBOL_NOT_FOUND
SYMBOL_AMBIGUOUS
SYMBOL_STALE
STALE_CONTENT
EXTERNAL_MODIFICATION
READ_ONLY_FILE
REPLACEMENT_NOT_FOUND
REPLACEMENT_AMBIGUOUS
CHECKPOINT_WRITE_FAILED
EDIT_FAILED
ROLLBACK_CONFLICT
POLICY_DENIED
APPROVAL_DENIED
APPROVAL_CANCELLED
APPROVAL_STALE
VALIDATION_FAILED
PROCESS_START_FAILED
PROCESS_FAILED
BUILD_CONFIGURATION_MISSING
BUILD_FAILED
TEST_FAILED
GIT_UNAVAILABLE
GIT_READ_FAILED
OPERATION_CANCELLED
```

Retry mapping is exact:

- `RATE_LIMITED`, retryable `REMOTE_SERVER_ERROR`,
  `TRANSPORT_UNAVAILABLE`, and retryable `TRANSPORT_TIMEOUT` use `HTTP`.
- `INDEX_UNAVAILABLE`, temporary I/O represented as `EDIT_FAILED`, and
  `PROCESS_START_FAILED` use `TRANSIENT_TOOL` only when the concrete error is
  marked transient.
- Failed mandatory verification uses `REPAIR_CYCLE` at the engine boundary.
- All other codes use `NONE`.

A compiler or test assertion failure is normally a successful port invocation
whose `BuildResult.succeeded` is false, not a retryable port error.

## 11. Neutral policy values

Phase 1 defines values only, not a policy engine.

`PolicyDecision` `[S]` has `ALLOW`, `DENY`, and `REQUIRE_APPROVAL`, a stable
reason code, model-safe explanation, and zero or more findings.

`ValidationFinding` `[S]` contains stable code, `FindingSeverity` (`ERROR`,
`WARNING`, or `ADVISORY`), optional project-relative path and source range,
model-safe explanation, and suggested recovery. It never contains PSI,
inspection objects, or exceptions.

## 12. Common port result contract

```kotlin
internal sealed interface PortResult<out T> {
    data class Success<T>(val value: T) : PortResult<T>
    data class Failure(val error: AgentError) : PortResult<Nothing>
}
```

`PortResult<T>` is `[S]` or `[SP]` only when `T` is. Expected operational
failures return `Failure`; cancellation is rethrown. Ports do not return null to
mean failure and do not expose adapter exception types.

## 13. Application ports

There are exactly eight ports. No separate clock, process, serializer, hash,
policy, Git mutation, or generic command port is added in Phase 1.

### 13.1 GeminiTransport

```kotlin
internal interface GeminiTransport {
    suspend fun interact(
        request: InteractionRequest,
    ): PortResult<InteractionResponse>

    suspend fun stream(
        request: InteractionRequest,
        sink: InteractionStreamSink,
    ): PortResult<InteractionResponse>

    suspend fun deleteInteraction(
        interactionId: InteractionId,
    ): PortResult<DeleteInteractionResult>
}
```

`InteractionRequest` `[SP]` contains session and model request IDs, model name,
optional previous interaction ID, store flag, system instruction, typed inputs,
all tool descriptors, generation configuration, and remaining HTTP retry
allowance. Continued requests must repeat configuration and preserve call IDs.

`InteractionInput` `[SP]` variants are `UserText` and `FunctionResults`.
`InteractionResponse` `[SP]` contains interaction ID, ordered plugin-owned
`InteractionStep` values, optional token usage, and HTTP retries used. Steps are
`ModelText`, `FunctionCall`, and `SafeUnknownStep`; correctness-critical unknown
steps return an error rather than raw wire data.

`InteractionStreamEvent` `[SP]` variants are `Created`, `TextDelta`,
`FunctionCallReady`, `SafeUnknown`, and `Completed`. `InteractionStreamSink`
is `[L]` with one suspending `emit` method. It carries no Google, HTTP, JSON, or
SSE type. The stream has exactly one returned terminal `PortResult`; sink or
caller cancellation cancels transport work.

`HttpClient`, requests, responses, SSE parser state, JSON nodes, authorization
headers, and Google DTOs are `[L]` adapter values.

### 13.2 WorkspaceInspectionPort

```kotlin
internal interface WorkspaceInspectionPort {
    suspend fun workspaceContext(request: WorkspaceContextRequest): PortResult<WorkspaceContext>
    suspend fun readFileRange(request: ReadFileRangeRequest): PortResult<FileRangeResult>
    suspend fun searchText(request: SearchTextRequest): PortResult<TextSearchResult>
    suspend fun findSymbol(request: FindSymbolRequest): PortResult<SymbolSearchResult>
    suspend fun findReferences(request: FindReferencesRequest): PortResult<SymbolLocationsResult>
    suspend fun findImplementations(request: FindImplementationsRequest): PortResult<SymbolLocationsResult>
    suspend fun fileMetadata(request: FileMetadataRequest): PortResult<FileMetadataResult>
}
```

All request/result metadata is `[S]`; snippets and file text are `[SP]`.
Bounded queries contain maximum items/characters and optional continuation
token. All returned locations are project-relative. File reads include content
hash and actual line bounds. Symbol results return opaque session-scoped
`SymbolId` values, never PSI.

Workspace context contains logical project identity, Gradle identity, module
IDs, roles and confidence, source sets, SDKs, source language levels, active
file metadata, and a compact rules summary. Exact Phase 2 discovery behavior is
out of scope, but the value boundary is fixed here.

IntelliJ `Project`, `Module`, `VirtualFile`, `Document`, PSI elements, smart
pointers, indexes, read actions, and absolute roots are `[L]`.

### 13.3 WorkspaceEditPort

```kotlin
internal interface WorkspaceEditPort {
    suspend fun openTransaction(request: OpenTransactionRequest): PortResult<TransactionSnapshot>
    suspend fun inspectTransaction(request: InspectTransactionRequest): PortResult<TransactionSnapshot>
    suspend fun replaceText(request: ReplaceTextRequest): PortResult<WorkspaceMutationResult>
    suspend fun createFile(request: CreateFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun deleteFile(request: DeleteFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun moveFile(request: MoveFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun rollbackOperation(request: RollbackOperationRequest): PortResult<RollbackResult>
    suspend fun rollbackTransaction(request: RollbackTransactionRequest): PortResult<RollbackResult>
    suspend fun saveTransactionDocuments(request: SaveTransactionDocumentsRequest): PortResult<SaveDocumentsResult>
}
```

Each mutation request `[SP]` contains session, transaction, and call IDs,
expected transaction revision, target paths, and `ExpectedFileRevision`.
`ExpectedFileRevision` is `Absent` for create or `Present(ContentHash)` for
replace, delete, and move. The model never supplies a document modification
stamp; the adapter tracks it locally as an additional conflict guard.

`WorkspaceMutationResult` `[S]` contains the call ID, prior and resulting
transaction digests, changed paths and ranges, prior/result content hashes, and
neutral findings. Rollback results include restored paths, new transaction
digest, and explicit conflicts. Unknown current content is never overwritten.

Every implementation must coordinate with `CheckpointStore` so durable
`persistBeforeMutation` succeeds before any document or filesystem effect. A
checkpoint failure returns `CHECKPOINT_WRITE_FAILED` and proves no mutation.

Documents, virtual files, write commands/actions, undo objects, writable-status
objects, and modification stamps are `[L]`.

### 13.4 BuildSystemPort

```kotlin
internal interface BuildSystemPort {
    suspend fun buildProject(request: BuildProjectRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testModule(request: TestModuleRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testClass(request: TestClassRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testMethod(request: TestMethodRequest, sink: ProcessOutputSink): PortResult<BuildResult>
}
```

Requests `[S]` contain process invocation ID, transaction ID, semantic target,
timeout milliseconds, and output bounds. They contain no executable, wrapper
path, Gradle task array, environment, JVM object, shell text, or IDE process
type. Only the adapter maps semantic intent to Gradle.

`ProcessOutputSink` is `[L]`; it receives ordered `[SP]`
`ProcessOutputChunk` values identified as stdout or stderr with bounded/redacted
metadata. `BuildResult` `[SP]` contains success, exit code, duration, bounded
stdout/stderr, logical tasks, structured compiler diagnostics, structured test
failures, report paths, and timeout status.

Cancellation must terminate the process tree and propagate. A normal nonzero
build/test exit is a `PortResult.Success(BuildResult(succeeded=false))`.
`GeneralCommandLine`, process handlers, `Process`, environment maps, Gradle
models, and wrapper paths are `[L]`.

### 13.5 GitReadPort

```kotlin
internal interface GitReadPort {
    suspend fun status(request: GitStatusRequest): PortResult<GitStatusResult>
    suspend fun diff(request: GitDiffRequest): PortResult<GitDiffResult>
    suspend fun diffFile(request: GitDiffFileRequest): PortResult<GitDiffResult>
    suspend fun log(request: GitLogRequest): PortResult<GitLogResult>
    suspend fun blame(request: GitBlameRequest): PortResult<GitBlameResult>
}
```

Requests/results are bounded and use project-relative paths. Diffs and snippets
are `[SP]`; metadata is `[S]`. There is no generic command, stage, stash,
commit, reset, checkout, clean, branch, merge, fetch, pull, push, or other Git
mutation method.

Git integration objects, repositories, revisions, roots, command handlers, and
processes are `[L]`.

### 13.6 CredentialPort

```kotlin
internal interface CredentialPort {
    suspend fun status(reference: CredentialReference): PortResult<CredentialStatus>
    suspend fun acquire(request: CredentialRequest): PortResult<CredentialLease>
    suspend fun store(request: StoreCredentialRequest): PortResult<CredentialStatus>
    suspend fun remove(request: RemoveCredentialRequest): PortResult<CredentialStatus>
    suspend fun revoke(request: RevokeCredentialRequest): PortResult<CredentialRevokeResult>
}
```

Credential reference, provider, account label, availability, expiry metadata,
and remove/revoke outcome are `[S]` and secret-free. Providers are `API_KEY` and
`OAUTH`; Phase 1 implements neither.

`CredentialLease`, `SecretValue`, and `StoreCredentialRequest` are `[L]`.
Secret holders use defensive character buffers, redact `toString`, have no data
class `copy`, and clear memory on close. They never appear in agent state,
events, errors, tool results, checkpoints, equality snapshots, or settings
state. Refresh and provider behavior remain adapter responsibilities.

PasswordSafe, credential attributes, API keys, access/refresh tokens, OAuth
codes, and authorization headers are `[L]`.

### 13.7 CheckpointStore

```kotlin
internal interface CheckpointStore {
    suspend fun initialize(request: InitializeCheckpointRequest): PortResult<CheckpointManifest>
    suspend fun persistBeforeMutation(request: PersistPendingMutationRequest): PortResult<CheckpointReceipt>
    suspend fun recordMutation(request: RecordAppliedMutationRequest): PortResult<CheckpointManifest>
    suspend fun load(transactionId: TransactionId): PortResult<CheckpointManifest?>
    suspend fun readBlob(blobId: CheckpointBlobId): PortResult<CheckpointBlob>
    suspend fun scanRecoverable(projectFingerprint: ProjectFingerprint): PortResult<List<RecoverableCheckpoint>>
    suspend fun setStatus(request: SetCheckpointStatusRequest): PortResult<CheckpointManifest>
    suspend fun discard(transactionId: TransactionId): PortResult<Unit>
    suspend fun prune(request: PruneCheckpointsRequest): PortResult<PruneCheckpointsResult>
}
```

Manifest, receipt, mutation records, recovery summaries, statuses, and blob IDs
are `[S]` local-persistence values. `CheckpointBlob` is defensive-copy `[SP]`
source content that may be persisted only by the checkpoint adapter and is never
model-visible by default.

`persistBeforeMutation` is the durability boundary. It must atomically persist
referenced content plus the pending record before returning success.
`recordMutation` compare-and-sets the receipt/manifest revision after the
effect. Recovery can therefore distinguish no-effect, pending/in-doubt, and
completed operations without violating at-most-once mutation.

Filesystem paths, channels, locks, permission objects, temporary files, and I/O
exceptions are `[L]`.

### 13.8 ApprovalPort

```kotlin
internal interface ApprovalPort {
    suspend fun awaitDecision(request: ApprovalRequest): PortResult<ApprovalDecision>
    suspend fun withdraw(requestId: ApprovalRequestId, reason: ApprovalWithdrawalReason): PortResult<Unit>
}
```

Approval requests and decisions are `[S]`. A request contains request, session,
and call IDs; `ToolEffect`; exact model-safe action and target summaries; stable
policy reason; consequences; expected `SessionRevision`; and optional expected
transaction digest.

Decisions are `Granted`, `Denied`, `Cancelled`, or `Stale`. They echo the
request ID and revision guards. The engine must revalidate them before resuming;
a stale grant authorizes nothing. Cancellation withdraws the request in a
`finally` path and propagates.

Swing components, callbacks, deferred objects, UI cards, and disposal handles
are `[L]`.

## 14. Exact test plan

All new core and port tests use ordinary JUnit 4 and pure fakes. They do not
extend an IntelliJ fixture class and do not load a Gemini wire schema.

### 14.1 AgentReducerTest

Add these exact tests:

```text
sessionStartedMovesIdleToRunning
approvalRequestMovesRunningToWaitingApproval
approvalGrantMovesWaitingApprovalToRunning
approvalDenialMovesWaitingApprovalToRunning
staleApprovalMovesWaitingApprovalToRunningWithoutAuthorization
verificationStartedMovesRunningToVerifyingInProgress
verificationSuccessRemainsVerifyingAndAwaitsFinal
verifiedFinalMovesVerifyingToCompleted
workAfterVerificationMovesVerifyingToRunning
currentVerifiedFinalMovesRunningToCompleted
verificationFailureMovesVerifyingToRunningWhenRepairAvailable
sixthVerificationFailureMovesVerifyingToFailed
verificationAbortMovesVerifyingToRunningWithoutRepairUse
explicitFailureMovesRunningToFailed
explicitFailureMovesVerifyingToFailed
cancelMovesEveryActiveStateToCancelled
transactionChangeMovesWaitingApprovalToRunningAndStalesApproval
transactionChangeMovesVerifyingToRunningAndAbortsVerification
allUnlistedStateEventPairsAreRejected
everyTerminalStateRejectsEveryStateEvent
idleRejectsFailureAndCancellation
rejectionReturnsTheExactUnchangedSession
acceptedEventIncrementsSessionRevisionExactlyOnce
foreignSessionEventIsRejected
staleExpectedRevisionIsRejected
approvalCorrelationMismatchIsRejected
verificationRunMismatchIsRejected
terminalReasonExistsIfAndOnlyIfTerminal
```

The transition test is parameterized from an explicit table of every valid row
in section 5.4, then generates the Cartesian product of all seven source states
and every transition event. Every combination not in the table must return
`InvalidTransitionError`. Separate fixtures cover both verification subphases.

### 14.2 AgentBudgetTest

```text
defaultsAreExactlyThreeTwoFiveFortyOneHundredFifteenAndThirty
httpAllowsInitialAttemptAndThreeRetriesButRejectsFourthRetry
newHttpOperationHasFreshRetryAllowance
httpSuccessClosesOnlyItsOwnRetryScope
transientToolAllowsTwoRetriesButRejectsThird
newToolCallHasFreshTransientRetryAllowance
fiveRepairCyclesAreAvailableAndSixthIsRejected
ordinaryBuildFailureDoesNotConsumeRepair
ordinaryTestFailureDoesNotConsumeRepair
mandatoryVerificationFailureConsumesOnlyRepair
fortyModelTurnsAreAcceptedAndFortyFirstIsRejected
httpRetriesDoNotConsumeModelTurns
oneHundredAcceptedCallOccurrencesAreAcceptedAndNextIsRejected
acceptedDuplicateReplayConsumesToolCallOccurrenceOnly
internalToolRetryDoesNotConsumeToolCall
processTimesOutAtExactDeadline
processTimeoutIsFreshForNextInvocation
sessionTimesOutAtExactDeadline
sessionTimeIncludesApprovalAndBackoffElapsedTime
elapsedTimeRegressionIsRejected
exhaustingEachBudgetLeavesEveryOtherBudgetUnchanged
newSessionResetsEverySessionScopedBudget
counterOverflowIsRejectedWithoutMutation
```

For every budget, assert initial state, values immediately below and at the
limit, the first rejected use, the exact error code, no over-increment, and
equality of all six independent budget values.

### 14.3 MutatingCallLedgerTest

```text
firstMutatingCallReservesAndExecutes
exactDuplicateWhileReservedWaitsForOriginal
exactDuplicateWhileEffectStartedWaitsForOriginal
exactCompletedDuplicateReplaysIdenticalSuccess
exactCompletedDuplicateReplaysIdenticalFailure
sameIdWithDifferentToolIsRejected
sameIdWithDifferentEffectIsRejected
sameIdWithDifferentArgumentsIsRejected
canonicalObjectKeyOrderProducesSameFingerprint
differentListOrderProducesDifferentFingerprint
duplicateReadOnlyIdExecutesAgainAcrossInteractions
duplicateIdsInsideOneBatchRejectTheWholeBatch
sameIdInANewSessionIsIndependent
replayDoesNotCallCheckpointPolicyAdapterOrValidator
replayDoesNotAdvanceTransactionRevision
replayDoesNotInvalidateVerifiedDigest
mutateVerifyThenReplayPreservesVerification
restoredCompletedLedgerReplays
restoredOutcomeUnknownNeverReexecutes
```

Use counting fake effect, checkpoint, policy, approval, and validation probes to
prove zero secondary calls on replay. The production ledger itself remains pure.

### 14.4 VerificationDigestTest

```text
sha256DigestAcceptsOnlyLowercaseSixtyFourHexCharacters
transactionRevisionMustAdvanceByExactlyOne
matchingVerificationRecordsExactFrozenDigest
staleVerificationSuccessIsRejected
completionBeforeVerificationIsRejected
completionRejectsDifferentTransactionId
completionRejectsDifferentRevision
completionRejectsDifferentDigest
successfulMutationClearsVerifiedDigest
createDeleteMoveAndReplaceEachClearVerifiedDigest
rollbackClearsVerifiedDigest
mutateThenRestoreStillClearsVerifiedDigest
externalRevisionChangeClearsVerifiedDigest
failedPreEffectMutationPreservesVerifiedDigest
rejectedMutationPreservesVerifiedDigest
genuineNoOpPreservesVerifiedDigest
readOnlyProcessAndControlResultsPreserveVerifiedDigest
duplicateMutatingReplayPreservesVerifiedDigest
postVerificationMutationBlocksCompletionAndCommitEligibility
verificationOfNewDigestRestoresCompletionAndCommitEligibility
externalChangeDuringVerificationAbortsWithoutRepairUse
```

### 14.5 ToolContractTest

```text
allTwentyTwoDescriptorNamesAreUnique
everyDescriptorHasTheExactLockedEffect
everyDescriptorHasTheExactRequiredAndOptionalParameters
schemasAreShallowClosedAndRejectAdditionalProperties
toolValuesHaveStructuralCanonicalEquality
toolResultRequiresExactlyOneOutcome
toolResultPreservesCallIdAndToolName
boundedMetadataRejectsNegativeOrImpossibleCounts
truncatedContinuableOutputRequiresToken
nonTruncatedOutputRejectsContinuationToken
onlyIndependentReadOnlyBatchIsConcurrent
everyEffectfulBatchIsOrdered
completeTaskMixedBatchIsRejectedBeforeAnyExecution
duplicateBatchIdsAreRejectedBeforeBudgetOrExecution
unknownToolAndInvalidArgumentsRejectBeforeExecution
```

### 14.6 AgentErrorTest

```text
everyStableErrorCodeHasOneCategory
everyErrorCodeHasExactlyOneRetryClass
httpCodesNeverUseToolOrRepairRetry
transientToolCodesNeverUseHttpOrRepairRetry
repairClassificationIsMandatoryVerificationOnly
errorsCannotRetainThrowable
errorsRejectUnsafeDetailKeysAndOversizedValues
cancellationIsNotConvertedToPortFailure
```

### 14.7 ApplicationPortContractTest

```text
allEightAndOnlyEightApplicationPortsExist
geminiContinuationPreservesInteractionAndCallIds
geminiRequestCarriesRepeatedConfigurationAndRetryAllowance
geminiStreamingSinkOrdersEventsAndHasOneTerminalResult
inspectionMethodsReturnOnlyBoundedRelativePathValues
symbolIdsAreSessionScopedAndStaleIdsFailExplicitly
editMethodsRequireTransactionAndExpectedRevision
checkpointFailurePreventsEveryMutation
editResultAdvancesExactlyOneTransactionRevision
buildRequestsExposeSemanticIntentAndNoCommandLine
buildOutputIsBoundedRedactedAndOrdered
nonzeroBuildExitIsACompletedBuildResult
gitPortExposesExactlyFiveReadMethodsAndNoMutation
credentialSecretsNeverAppearInStateEventsErrorsOrToString
checkpointReceiptCompareAndSetRejectsStaleRevision
checkpointRecoveryDistinguishesPendingCompletedAndUnknown
approvalDecisionMustMatchRequestAndRevisionGuards
approvalCancellationWithdrawsAndPropagates
expectedFailuresUsePortResultAndNoAdapterExceptionEscapes
```

### 14.8 SerializationBoundaryTest

Maintain explicit roots for every `[S]`, `[SP]`, and `[L]` type, then recurse
through constructor properties and generic element types.

```text
everyCoreAndPortValueHasExactlyOneBoundaryClassification
serializableGraphsContainOnlyAllowedStructuralTypes
sourceBearingGraphsRemainStructurallySerializable
serializableGraphsNeverReachAdapterLocalValues
agentSessionContainsNoSecretOrRuntimeHandle
agentEventsContainNoSecretThrowableOrNativeType
toolDescriptorsContainNoExecutorOrPort
portRequestsAndResultsContainNoPlatformWireOrFilesystemType
credentialLeaseAndSinksAreAdapterLocal
checkpointContentIsSourceBearingAndAbsentFromSessionMetadata
```

The test explicitly rejects IntelliJ, Google, Gradle, Git, HTTP, Swing/AWT,
`Throwable`, `File`, `Path`, `URI`, `Process`, mutable collections, and function
types from portable graphs.

### 14.9 Architecture tests

Retain every Phase 0 architecture rule and negative fixture. Extend the domain
import guard to forbid these packages from `core`, `ports`, `application`, and
`policy`:

```text
com.intellij..
org.jetbrains..
com.google..
org.gradle..
git4idea..
org.eclipse.jgit..
java.net.http..
javax.swing..
java.awt..
com.euhedral.gemini.adapters..
```

Keep `core` limited to `core`, `java`, and `kotlin`, and `ports` limited to
`ports`, `core`, `java`, and `kotlin`. No `kotlinx.coroutines.Flow` exception is
needed because stream sinks use ordinary suspend functions.

Add a forbidden test fixture whose port signature leaks representative native
types and assert the rule fails. Architecture and reflection tests together
must catch both direct imports and native types nested inside portable DTOs.

## 15. Implementation sequence

Implement Phase 1 in this order:

1. Add boundary markers, identifiers, error values, and revision values.
2. Add independent budgets and their tests.
3. Add tool values, descriptors, results, batching, and tests.
4. Add the session, reduced events, reducer, and exhaustive transition tests.
5. Add mutation ledger and verification invalidation tests.
6. Add neutral policy values.
7. Add common port results, eight port interfaces, and all portable/local DTOs.
8. Add serialization and port signature tests.
9. Strengthen architecture rules and prove their negative fixture.
10. Remove only the superseded `core` and `ports` markers.
11. Run the complete suite and verification commands.
12. Update this plan with actual final names, commands, test counts, deviations,
    risks, and implementation commit before publishing Phase 1 code.

Do not add an execution engine, dispatcher implementation, policy engine,
adapter, settings state, UI, descriptor registration, Gradle dependency, or
wire codec during this sequence.

## 16. Verification commands and evidence requirements

Run from the repository root under the repository's JDK 25 environment:

```text
mise exec -- ./gradlew --version
mise exec -- ./gradlew clean verifyJava25 test
mise exec -- ./gradlew verifyPluginProjectConfiguration verifyPluginStructure
mise exec -- ./gradlew buildPlugin verifyPlugin
git diff --check
```

The Phase 1 implementation record must include:

- Exact final production and test file lists and type names.
- Gradle, Java, Kotlin, IntelliJ Platform plugin, and IDEA target versions.
- Test count and duration, with each test class above represented.
- Proof that core/port tests use no IntelliJ fixture or Gemini wire schema.
- Proof of all valid and invalid transitions.
- Independent limit, reset, and exhaustion evidence for all seven budgets.
- Counting-fake evidence that duplicate mutation IDs execute once.
- Verification invalidation and re-verification evidence.
- Architecture negative-guard and serialization-boundary evidence.
- Plugin structure, packaging, and verifier results.
- Deviations and remaining risks. Any contract change requires a plan update
  before implementation continues.

## 17. Risks and controls

| Risk | Control |
| --- | --- |
| `VERIFYING` ambiguously means active work and completed verification | Use the explicit `IN_PROGRESS` and `PASSED_AWAITING_FINAL` subphases and test every allowed event in both. |
| Session and transaction versions are conflated | Use monotonic `SessionRevision` only for session concurrency and the full transaction ID/revision/SHA-256 tuple for workspace verification. |
| One retry path silently consumes another limit | Use seven concrete budget types and assert the other six are unchanged on every operation. |
| Retry counts have off-by-one behavior | Define retries as additional attempts and test the exact first rejected use. |
| A repeated mutating call executes after a transport continuation | Reserve before effect, persist the reservation, and replay the exact recorded terminal result. |
| Crash recovery guesses whether a mutation occurred | Record `OutcomeUnknown` and refuse speculative re-execution. |
| Duplicate IDs cannot be correlated within one batch | Reject the entire batch before budget consumption or execution. |
| A verification remains eligible after content changes | Make transaction revision change the sole advance event and unconditionally clear the verified digest. |
| Adapter objects leak through convenient DTO fields | Mark every value, reflect its complete graph, and retain strict architecture imports. |
| Source or secrets are mistaken for ordinary serializable metadata | Distinguish `[SP]` from `[S]`; keep secrets and live sinks `[L]`; test reachability from state and events. |
| Generic shell or Git mutation enters through a broad port | Expose semantic build methods and five Git reads only. |
| Phase 1 accidentally adds runtime behavior | Keep `application` as a marker and add only immutable contracts, pure functions, interfaces, and pure tests. |

## 18. Plan review checklist

- [x] The state machine has exactly seven locked top-level states.
- [x] Every valid transition and the invalid-transition matrix are specified.
- [x] Verification success, final response, continued work, and subphases are
  unambiguous.
- [x] All seven budgets have exact defaults, scopes, reset rules, and terminal
  behavior.
- [x] Retry classes are exclusive and repair is identified as engine recovery.
- [x] Tool calls, values, results, descriptors, effects, schemas, bounds, and
  batching are specified.
- [x] Duplicate batch IDs, read-only repeats, exact mutating replays,
  mismatches, in-flight duplicates, failures, and recovery are specified.
- [x] Session revision, transaction revision digest, verified digest, and every
  verification invalidation case are specified.
- [x] Error values have stable categories/codes and contain no native failure.
- [x] All eight application ports and their method surfaces are specified.
- [x] Every portable, source-bearing, and adapter-local boundary is marked.
- [x] This planning task adds no production implementation, and the future
  implementation adds no adapter dependency.
- [x] The plan text is ASCII-only.

## 19. Plan publication record

Planning review completed on 2026-08-09:

```text
iconv -f US-ASCII -t US-ASCII docs/implementation/phase-1-domain-ports-plan.md
git diff --check -- docs/implementation/phase-1-domain-ports-plan.md
mise exec -- ./gradlew test
```

Results:

- ASCII conversion check: passed.
- Git whitespace check: passed.
- Baseline suite: passed, 16 tests executed, 0 failed, 0 skipped.
- Scope review: only
  `docs/implementation/phase-1-domain-ports-plan.md` is added.
- New external API choices: none. The plan deliberately keeps the Phase 0
  Java/Kotlin-only domain boundary and uses suspendable sinks instead of adding
  a streaming dependency.
- Deviations: none from the Phase 1 mini blueprint. The four prompt ambiguities
  recorded in section 1 were resolved by the user before drafting.
- Remaining risk: exact adapter API choices remain intentionally deferred to
  their owning phases; no adapter behavior can be validated in this pure
  contract phase.

## 20. Implementation publication record

Implementation completed on 2026-08-09:

```text
mise exec -- ./gradlew --version
mise exec -- ./gradlew clean verifyJava25 test
mise exec -- ./gradlew verifyPluginProjectConfiguration verifyPluginStructure
mise exec -- ./gradlew buildPlugin verifyPlugin
iconv -f US-ASCII -t US-ASCII docs/implementation/phase-1-domain-ports-plan.md
git diff --check
```

Results and evidence:

- Final environment: Gradle 9.5.0, Java 25.0.2 (Oracle Corporation), Kotlin 2.4.0, IntelliJ Platform Plugin 2.18.1, Target IDE IU-262.8665.337.
- Complete suite execution: 168 total tests executed (163 Phase 1 tests across 9 test classes + 5 Phase 0 platform tests), 0 failed, 0 skipped.
  - AgentReducerTest: 33 tests passing (valid and invalid transitions, approval guards, independent timers, de-dup reducer guards, subphase eligibility, revision incrementing, terminal reason invariants).
  - AgentBudgetTest: 22 tests passing (7 independent budgets, scope resets, exhaustion rules, process/session deadlines, counter overflow guard).
  - MutatingCallLedgerTest: 20 tests passing (at-most-once mutation claim, duplicate wait/replay, fingerprint matching, recovery replay, outcome unknown refusal).
  - VerificationDigestTest: 23 tests passing (SHA-256 validation, transaction revision advancement, verification frozen digest recording, post-mutation invalidation).
  - ToolContractTest: 16 tests passing (22 standard tool descriptors, collision-safe canonical fingerprints, parameter schemas, bounded metadata, batch planner concurrency/ordering).
  - AgentErrorTest: 8 tests passing (stable categories, error codes, retry mapping, details safety).
  - ApplicationPortContractTest: 19 tests passing (8 application ports, Gemini transport, workspace inspection/edit, build system, git read, credentials, checkpoint store, approval port).
  - SerializationBoundaryTest: 10 tests passing ([S], [SP], [L] boundary classifications, reachability rules).
  - PackageArchitectureTest: 12 tests passing (pure domain/port import guards, negative fixtures).
- Pure contract verification: No IntelliJ fixture, Gemini wire DTO, HTTP client, or Gradle process is loaded by core or port tests.
- Plugin structure and packaging: `buildPlugin` and `verifyPlugin` completed with exit code 0. Plugin structure is verified compatible with target IDE IU-262.8665.337, marked dynamically reloadable.
- ASCII conversion check: passed.
- Git whitespace check: passed.
- Deviations: none from Phase 1 blueprint and plan.
- Remaining risks: concrete IntelliJ adapters, Gemini HTTP behavior, Gradle command execution, and Swing UI components are intentionally deferred to Phase 2 and beyond.
