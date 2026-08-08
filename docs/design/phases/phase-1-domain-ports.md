# Phase 1: Domain and Ports Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 0: Platform Skeleton](phase-0-platform-skeleton.md)

Next phase: [Phase 2: Project Rules and Intelligence](phase-2-project-intelligence.md)

## 1. Objective

Define the pure, adapter-neutral contracts that constrain every later subsystem: state, events, effects, budgets, errors, revisions, and ports.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-0-platform-plan.md`.
- The current Phase 0 source and architecture tests.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- `core` and `ports` import no IntelliJ, Google, Gradle, Git, HTTP, Swing, or adapter types.
- `AgentReducer` is pure; orchestration owns side effects.
- Top-level states are IDLE, RUNNING, WAITING_APPROVAL, VERIFYING, COMPLETED, FAILED, and CANCELLED.
- Tool effects are READ_ONLY, MUTATING, PROCESS, and CONTROL.
- HTTP retry, transient-tool retry, repair-cycle, model-turn, tool-call, process-timeout, and session-time budgets are independent.
- A duplicate mutating function call ID returns the recorded result and never repeats the effect.
- Any mutation invalidates a previously verified transaction digest.

## 4. Deliverables

- Agent session state, reducer, events, terminal reasons, and invalid-transition errors.
- Tool calls, results, descriptors, effects, bounded-output metadata, and error taxonomy.
- Independent budget and retry accounting.
- Transaction revision and verified-digest value types.
- Transport-neutral ports for Gemini, inspection, editing, build, Git read, credentials, checkpoints, and approvals.
- Exhaustive pure unit tests.
- Implementation plan at `docs/implementation/phase-1-domain-ports-plan.md`.

## 5. Out of scope

- Concrete IntelliJ adapters.
- Gemini wire DTOs or HTTP behavior.
- Gradle command construction.
- UI state or Swing components.
- Policy implementation beyond neutral decision/finding values.

## 6. Required tests and evidence

- Every valid and invalid state transition.
- Independent exhaustion and reset behavior for every budget.
- Duplicate read-only and mutating call semantics.
- Verified-digest invalidation after mutation.
- Serialization boundaries for values that cross adapters.
- Architecture tests for all core and port imports.

## 7. Completion gate

Pure tests fully define engine contracts without loading IntelliJ or understanding Gemini's wire schema.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 1A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, `docs/implementation/phase-0-platform-plan.md`, and the current source. Produce `docs/implementation/phase-1-domain-ports-plan.md`.

Specify the pure state machine, reducer, events, budgets, retry classes, tool calls/results/descriptors/effects, error taxonomy, session and transaction revision digests, mutating call de-duplication contract, and every application port. Mark serializable values and adapter-local values. Include exact tests for all transitions, independent counters, duplicate call IDs, and verification invalidation.

Do not implement production code or introduce adapter dependencies. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 1B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-1-domain-ports-plan.md`. Implement only the pure domain and ports. Keep the reducer side-effect-free, make invalid transitions explicit, keep budgets independent, deduplicate mutating call IDs, and invalidate verified revisions after mutation.

Run the complete current suite and architecture tests. Update the phase plan with final names, commands, evidence, and deviations. Inspect the diff, then commit and fast-forward push Phase 1 to `main`. Do not add IntelliJ adapters, Gemini DTOs, Gradle logic, or UI.
```
