# Phase 4: Policy and Validators Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 3: Durable Editing](phase-3-durable-editing.md)

Next phase: [Phase 5: Gradle and Verification](phase-5-gradle-verification.md)

## 1. Objective

Put authorization and mechanical post-edit validation between every effectful tool request and its adapter so architectural rules cannot be bypassed by prompting.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-2-project-intelligence-plan.md` and `phase-3-durable-editing-plan.md`.
- The implemented rules, classification, edit transaction, and rollback contracts.
- Current official IntelliJ 2026.2 path, VFS, PSI, and approval-related APIs used by the design.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Every MUTATING, PROCESS, or CONTROL effect is evaluated by `PolicyEngine` before adapter execution.
- Policy results are ALLOW, DENY, or REQUIRE_APPROVAL with stable reason codes.
- Workspace boundaries use canonical paths and reject symlink escapes.
- Hard validators enforce explicit rules; heuristics and performance preferences produce advisory findings only.
- Hard validators include workspace boundary, forbidden import, language level, logging, encoding, and sensitive path checks.
- Core roles reject configured Spring and Reactor imports.
- `System.out` and configured raw print calls are rejected.
- Deletion, move, rules-file changes, protected paths, and any command escape hatch require explicit approval as configured.
- A rejected mutation rolls back only that operation and preserves the session transaction.

## 4. Deliverables

- Policy engine, decision flow, stable reason codes, and model-safe findings.
- Canonical workspace, symlink, protected-path, and secret-like-path authorization.
- Forbidden import, source language level, SLF4J/System.out, encoding, and sensitive-path validators.
- Advisory hot-path allocation and concurrency findings limited to configured paths.
- Approval suspension/resumption contracts integrated with the existing approval port.
- Policy matrix and implementation plan at `docs/implementation/phase-4-policy-plan.md`.

## 5. Out of scope

- Gradle execution or affected-test analysis.
- Gemini tool parsing or agent loop.
- UI approval cards.
- Automatic Git operations.
- Hard denial based only on heuristic module classification.

## 6. Required tests and evidence

- Canonical and lexical traversal, nested symlink, destination-parent symlink, and race-resistant path checks.
- Protected and secret-like paths, including rules files.
- Explicit versus heuristic module roles.
- Forbidden Spring/Reactor imports in Core and allowed imports in adapters/tests.
- Java syntax above module language level.
- System.out, logger construction, encoding preservation, and advisory hot-path findings.
- ALLOW, DENY, approval pause, approval denial, approval resume, and cancellation.
- Operation-only rollback after validator rejection.

## 7. Completion gate

Invalid or unapproved effects are rejected before builds or external processes start, with deterministic findings suitable for both the model and UI.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 4A: Plan

Mode: Reasoning

Reasoning level: Max

```text
Read this mini blueprint, the Phase 2 and Phase 3 plans, and the implemented rule and editing contracts. Produce `docs/implementation/phase-4-policy-plan.md`.

Plan ALLOW, DENY, and REQUIRE_APPROVAL flow; canonical workspace and symlink defenses; protected and secret-like paths; hard versus advisory validators; forbidden imports; language level; SLF4J and System.out; encoding; configured hot paths; deletion, move, rules-file, and command approvals; suspension/resumption; stable reason codes; and model-safe findings. Include a complete policy matrix and adversarial path tests.

Do not implement, and never turn heuristic classification into a hard denial. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 4B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-4-policy-plan.md`. Implement only Phase 4. Route every effectful tool through `PolicyEngine` before its adapter. Implement hard validators and advisory performance findings with stable codes. Reject hard architecture changes only from explicit or unambiguous roles.

Add every required policy, path, approval, validation, and rollback test. Run the complete suite and Plugin Verifier. Update the plan with evidence and deviations, inspect the diff, then commit and fast-forward push Phase 4 to `main`. Do not add Gradle, Gemini, or UI.
```
