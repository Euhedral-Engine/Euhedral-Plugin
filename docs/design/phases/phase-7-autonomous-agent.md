# Phase 7: Autonomous Agent Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 6: Gemini Transport](phase-6-gemini-transport.md)

Next phase: [Phase 8: Agent UI](phase-8-agent-ui.md)

## 1. Objective

Compose the already-tested ports into a generic model-directed agent loop while keeping policy, transactions, budgets, verification, and completion under plugin control.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- All approved Phase 1 through Phase 6 implementation plans.
- The implemented domain, tools, policy, transaction, verification, and Gemini transport contracts.
- Headless IntelliJ and Gradle fixtures created by earlier phases.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- The engine does not encode a fixed inspect, edit, test, and repair script; Gemini selects investigation steps.
- One mutating agent session runs per project; inline completion may overlap independently later.
- The project service scope owns the full model/tool/process child-job tree.
- Only batches containing independent READ_ONLY tools run concurrently; all others execute in response order.
- Every function call is schema-validated and policy-checked before execution.
- Mutating call IDs are at-most-once across retries and continuations.
- `complete_task(summary)` is the only route to successful completion and must be the sole control call in its batch.
- `complete_task` runs hard validation, targeted verification, and full verification; failure returns diagnostics and consumes one repair cycle.
- A final model response before verified completion returns a control error and continues or fails by budget.
- `request_commit(message)` is available only after a current verified digest and opens a later human-controlled application flow; it is not a Git tool.
- Cancellation and project close propagate through model, tools, approvals, and processes.
- Events are the only observable execution history consumed by later UI.

## 4. Deliverables

- Agent session service, execution engine, effect loop, and deterministic event ordering.
- Tool registry and typed dispatcher with batch scheduling.
- Model argument validation and bounded results.
- At-most-once mutating call result cache.
- Independent retry/budget enforcement and terminal reasons.
- Approval suspension/resumption and cancellation.
- `complete_task` verification/repair boundary and premature-final handling.
- Recovery metadata sufficient for Phase 8 recovery controls.
- Headless scenario harness and implementation plan at `docs/implementation/phase-7-autonomous-engine-plan.md`.

## 5. Out of scope

- Swing UI or action tree.
- Inline completion.
- OAuth.
- New tool capabilities not defined by prior phases.
- Direct Git mutation or unrestricted shell access.

## 6. Required tests and evidence

- Inspect/edit/test/repair/verify success with deterministic event order.
- Read-only concurrent batching and ordered effectful batching.
- Malformed names, schemas, arguments, and result correlation.
- Duplicate mutating calls across transport retries and continuations.
- Approval grant, denial, cancellation, and resume.
- Independent indexing, transport, tool, and repair retries.
- Premature final response and `complete_task` mixed-batch rejection.
- Exhausted turns, tools, time, processes, and repair cycles.
- Cancellation during model request, PSI, edit boundary, approval, process, and verification.
- Project close and plugin unload.
- Post-verification mutation invalidates completion and commit eligibility.

## 7. Completion gate

A headless fixture deterministically completes a multi-step edit, test, repair, and full-verification task with no UI dependency.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 7A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, all approved Phase 1 through Phase 6 plans, and their implemented contracts. Produce `docs/implementation/phase-7-autonomous-engine-plan.md`.

Plan session ownership, the effect-driven execution loop, continuation, registry, dispatcher, batching, schema validation, at-most-once mutation, budgets, event ordering, policy, approval suspension, `complete_task`, targeted and full verification, repair accounting, premature finals, cancellation, project close, and recovery metadata. Demonstrate that UI, transport, dispatcher, policy, and adapters cannot bypass each other's boundaries. Include deterministic headless scenarios.

Do not implement or add tools. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 7B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-7-autonomous-engine-plan.md`. Implement only Phase 7. Keep the reducer pure and orchestration effect-driven. Concurrently execute only independent all-read-only batches. Serialize every other batch, deduplicate mutating call IDs, and make `complete_task` the sole verified completion path.

Add all headless success, failure, budget, retry, approval, cancellation, close, duplicate-call, and post-verification tests. Run the complete suite and Plugin Verifier. Update the plan with evidence, inspect the diff, then commit and fast-forward push Phase 7 to `main`. Do not add UI, inline completion, OAuth, or new tools.
```
