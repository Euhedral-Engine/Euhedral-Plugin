# Phase 5: Gradle and Verification Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 4: Policy and Validators](phase-4-policy-validators.md)

Next phase: [Phase 6: Gemini Transport](phase-6-gemini-transport.md)

## 1. Objective

Provide safe Gradle execution, structured diagnostics, affected-test escalation, and the engine-owned verification boundary used to define done.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-1-domain-ports-plan.md`, `phase-2-project-intelligence-plan.md`, `phase-3-durable-editing-plan.md`, and `phase-4-policy-plan.md`.
- The implemented build port, project model, transactions, and policy contracts.
- The real Euhedral Gradle wrapper and linked Gradle model when available.
- Current official IntelliJ IDEA 2026.2 process execution and threading documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Only the Gradle adapter constructs Gradle commands; the application layer calls `BuildSystemPort`.
- Use the repository wrapper, `GeneralCommandLine`, `OSProcessHandler`, and argument arrays without a shell.
- Do not force `clean`, cache flags, daemon flags, or repository settings.
- Save transaction-owned documents before builds without saving unrelated user documents.
- No process wait occurs on EDT or while holding a PSI read lock.
- Stream bounded, redacted output and return structured compiler and test diagnostics.
- Targeted verification escalates from direct tests to module tests based on affected symbols and uncertainty, then always runs full configured verification.
- Interface, build-script, broad signature, and unknown-impact changes escalate conservatively.
- Successful verification records a transaction revision digest; later mutation invalidates it.
- Only failed mandatory verification consumes a repair cycle. Ordinary iterative test failures do not.

## 4. Deliverables

- IntelliJ process runner with streamed events, timeout, cancellation, and process-tree termination.
- Gradle wrapper/root/JVM discovery and `BuildSystemPort` adapter.
- `build_project`, `test_module`, `test_class`, and `test_method` behavior.
- Bounded diagnostic parsers for compiler and JUnit/Gradle failures.
- Affected-code analyzer and verification plan escalation.
- Verification service and verified-digest recording.
- Implementation plan at `docs/implementation/phase-5-gradle-verification-plan.md`.

## 5. Out of scope

- Maven support.
- Shell execution.
- Gemini transport or autonomous orchestration.
- UI process console.
- Automatic clean builds or cache policy changes.

## 6. Required tests and evidence

- Single-root and multi-project wrapper discovery and exact argument arrays.
- Gradle JVM selection and missing-wrapper errors.
- Streaming, output caps, redaction, timeout, cancellation, and child-process termination.
- Compiler and test diagnostic parsing, including malformed and large output.
- Direct body change, method signature, interface, build script, no-known-test, and broad-impact escalation.
- Transaction document save isolation.
- No EDT block and no process wait under PSI read lock.
- Full verification, digest recording, failure, cancellation, and post-verification invalidation.

## 7. Completion gate

A manually initiated transaction can be built, targeted-tested, fully verified, cancelled, diagnosed, and rolled back without Gemini.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 5A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, its named prerequisite plans, and the implemented contracts. Produce `docs/implementation/phase-5-gradle-verification-plan.md`.

Plan the Gradle-only `BuildSystemPort`: linked root and wrapper discovery, Gradle JVM, exact argument arrays, `GeneralCommandLine`, `OSProcessHandler`, streaming, output bounds and redaction, process-tree cancellation, timeout, compiler/test diagnostics, transaction document saving, affected-code analysis, escalation, mandatory verification, repair accounting, and verified transaction digests. Include multi-project fixtures and hung-process tests.

Do not use a shell or force clean/cache/daemon flags. Do not implement. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 5B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-5-gradle-verification-plan.md`. Implement only Phase 5 through `BuildSystemPort`. Keep command construction in the Gradle adapter, stream process events, bound and redact outputs, and return structured diagnostics. Implement targeted tests, escalation, full verification, cancellation, timeouts, process-tree termination, and verified-digest invalidation.

Add every required fixture and concurrency test, including proof that no process wait holds EDT or a PSI read lock. Run the complete suite and Plugin Verifier. Update the plan, inspect the diff, then commit and fast-forward push Phase 5 to `main`. Do not add Gemini or UI.
```
