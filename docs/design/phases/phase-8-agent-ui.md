# Phase 8: Agent UI Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 7: Autonomous Agent](phase-7-autonomous-agent.md)

Next phase: [Phase 9: Inline Completion](phase-9-inline-completion.md)

## 1. Objective

Expose the complete agent workflow through a native IntelliJ tool window whose state is reduced exclusively from engine events.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-7-autonomous-engine-plan.md`.
- The implemented `AgentEvent` types, application use cases, recovery metadata, and settings shell.
- Current official IntelliJ IDEA 2026.2 tool window, Swing, Kotlin UI DSL 2, DiffManager, commit workflow, threading, accessibility, and disposal documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- The UI observes `AgentEvent` streams and invokes application use cases only.
- UI code never calls Gemini, PSI, build, Git, checkpoint, or policy adapters directly.
- View state is immutable and produced by a deterministic reducer.
- All Swing mutations occur on EDT; reduction and non-UI work stay off EDT.
- Every subscription, editor reference, process stream, and child component is bound to project/plugin disposal.
- Changed-file links open native diffs from checkpoint content to current content.
- `request_commit` opens IntelliJ's native human-controlled Commit UI without staging, committing, or pushing.
- Approval controls show the exact effect, target, reason, and consequences.
- Cancel, rollback, recovery, and conflict inspection remain available after failure where safe.
- The action tree is a projection of events, not a probe into engine internals.

## 4. Deliverables

- Tool-window chat transcript and immutable view-model reducer.
- Expandable action tree for model, tool, validation, verification, repair, approval, and terminal events.
- Streamed model and process output with bounded retention.
- Clickable diagnostics and changed-file list.
- Native DiffManager integration.
- Approval, conflict, cancellation, rollback, and recovery controls.
- Budget and session-state display.
- Settings integration and native Commit UI handoff.
- Implementation plan at `docs/implementation/phase-8-agent-ui-plan.md`.

## 5. Out of scope

- Agent orchestration changes.
- Direct adapter access from UI.
- Inline completion.
- OAuth.
- Automatic Git staging, commit, or push.
- Custom diff or commit implementations where native IntelliJ UI exists.

## 6. Required tests and evidence

- Pure view-model reduction for every event and invalid event order.
- EDT confinement and background event delivery.
- Subscription disposal on project close and plugin unload.
- Action-tree grouping and streamed-output bounds.
- Diagnostic and file-link navigation.
- Checkpoint-to-current diff request construction.
- Approval grant/deny/cancel and stale approval cards.
- Conflict, rollback, and recovery availability rules.
- Native commit handoff does not stage or commit.
- Accessibility names, keyboard navigation, and basic runIde workflow checklist.

## 7. Completion gate

A user can run, inspect, approve, cancel, diff, roll back, recover, and hand off a verified task to native Commit UI without reading logs.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 8A: Plan

Mode: Reasoning

Reasoning level: Max

```text
Read this mini blueprint, `docs/implementation/phase-7-autonomous-engine-plan.md`, and the implemented events and application use cases. Produce `docs/implementation/phase-8-agent-ui-plan.md`.

Plan the immutable view reducer, transcript, action tree, streaming, diagnostics, process output, changed files, DiffManager, approvals, conflicts, cancellation, rollback, recovery, budgets, settings, and native Commit UI handoff. Specify EDT boundaries, disposal, accessibility, and UI fixtures. Prove the UI needs only events and application use cases.

Do not implement or change engine contracts unless a documented blocker is escalated. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 8B: Implement

Mode: Coding

Reasoning level: Medium

```text
Read this mini blueprint and `docs/implementation/phase-8-agent-ui-plan.md`. Implement only Phase 8 with public IntelliJ IDEA 2026.2 APIs and Kotlin UI DSL 2 where appropriate. Reduce events into immutable state, mutate Swing only on EDT, and bind every subscription to disposal.

Implement native checkpoint-to-current diffs and native Commit UI handoff without staging or committing. Add all reducer, UI, disposal, accessibility, and smoke tests. Run the complete suite and Plugin Verifier. Update the plan, inspect the diff, then commit and fast-forward push Phase 8 to `main`. Do not add inline completion or OAuth.
```
