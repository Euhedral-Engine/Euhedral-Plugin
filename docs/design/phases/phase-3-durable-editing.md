# Phase 3: Durable Editing Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 2: Project Rules and Intelligence](phase-2-project-intelligence.md)

Next phase: [Phase 4: Policy and Validators](phase-4-policy-validators.md)

## 1. Objective

Make every model-originated file mutation durable, conflict-aware, undoable, and recoverable without using Git as a transaction manager.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-1-domain-ports-plan.md` and `phase-2-project-intelligence-plan.md`.
- The implemented workspace inspection and edit port contracts.
- Current official IntelliJ 2026.2 document, command, write-action, VFS, and coroutine documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- No mutation occurs until its checkpoint manifest and original content are durably persisted.
- Checkpoints use a transaction manifest plus content-addressed blobs, atomic persistence, and restrictive permissions.
- Git stashes and all Git mutations are forbidden.
- Document changes occur through minimal IntelliJ commands and write actions so native undo participates.
- Every edit requires an expected content hash; open-document modification stamps add conflict detection.
- Rollback writes only when the current revision is known to be agent-owned.
- A hard operation failure rolls back that operation; session rollback restores the complete recorded lineage.
- Canonical workspace and symlink enforcement is strengthened in Phase 4, but Phase 3 must preserve enough path identity to support it.

## 4. Deliverables

- Edit transaction and operation lineage model.
- Durable checkpoint store, manifest, blob storage, atomic update, retention, and recovery scan.
- Exact `replace_text`, `create_file`, `delete_file`, and `move_file` adapters.
- Operation rollback and full-session rollback.
- External-change detection, safe refusal, and conflict result types.
- Open-document save coordination required before later builds.
- Implementation plan at `docs/implementation/phase-3-durable-editing-plan.md`.

## 5. Out of scope

- Policy decisions and architecture validators.
- Gradle or external process execution.
- Gemini orchestration.
- Git stash, staging, commit, checkout, reset, or clean.
- Arbitrary unified-diff application.

## 6. Required tests and evidence

- Checkpoint-write failure proves no edit occurred.
- Crash injection before and after each journal boundary.
- Exact replacement zero-match and multiple-match rejection.
- Stale hash and modification-stamp conflict.
- Open unsaved document behavior.
- Read-only files and failed VFS operations.
- Create, delete, move, and multi-file rollback lineage.
- Rollback refusal when current content has an unknown hash.
- Symlinked parent fixtures preserve detectable canonical identity.

## 7. Completion gate

Multi-file rollback is deterministic, incomplete sessions are recoverable, and external user changes are never overwritten.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 3A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, the Phase 1 and Phase 2 plans, and the implemented contracts. Produce `docs/implementation/phase-3-durable-editing-plan.md`.

Resolve current public IntelliJ IDEA 2026.2 command and suspending write-action APIs. Design the edit transaction, manifest and content-addressed blob store, atomic persistence, permissions, document save coordination, exact replacement, create/delete/move lineage, operation and session rollback, crash recovery, retention, hashes, modification stamps, external conflicts, and safe rollback refusal. Include failure-injection and multi-file recovery tests.

No Git mutation is allowed. Do not implement. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 3B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-3-durable-editing-plan.md`. Implement only Phase 3. Confirm durable checkpoint persistence before every mutation. Apply document changes through minimal IntelliJ commands and write actions. Enforce expected hashes and refuse to overwrite external conflicts.

Implement exact replace, create, delete, move, rollback, journal recovery, and retention with all failure-injection tests. Run the complete suite and Plugin Verifier. Update the plan with final design and evidence, inspect the diff, then commit and fast-forward push Phase 3 to `main`. Do not add policy, Gradle, Gemini, or Git mutation.
```
