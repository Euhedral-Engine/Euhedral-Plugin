# Phase 11: Hardening and Release Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 10: OAuth](phase-10-oauth.md)

Next phase: None

## 1. Objective

Close every design invariant and known failure mode with evidence, then produce a verified private plugin artifact without adding unrelated features.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- All phase plans under `docs/implementation/`.
- The complete current source, open TODOs, test reports, benchmark reports, and packaged artifacts.
- Current official IntelliJ IDEA 2026.2 compatibility, Plugin Verifier, inspection, packaging, privacy, and dependency documentation.
- Current official Gemini model, Interactions, retention, credential, OAuth, and deprecation documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- No unmet hard invariant may be downgraded to a known limitation; it is a release blocker.
- The release target remains IntelliJ IDEA 2026.2 and Java 25.
- Core/application remain isolated from IntelliJ, Gemini, Gradle, Git, and UI adapters.
- No Git stash, model-visible Git mutation, unrestricted shell, or automatic commit exists.
- No mutation precedes durable checkpoint persistence, and rollback never overwrites unknown content.
- `complete_task` plus current targeted and full verification is the only completed state.
- API keys and OAuth tokens exist only in PasswordSafe.
- Normal metrics contain no prompts, source, diffs, credentials, environment, or raw model output.
- Version-sensitive APIs and model IDs are revalidated immediately before release.
- Only fixes required by acceptance, failure, security, compatibility, privacy, or measured regression evidence are in scope.

## 4. Deliverables

- Acceptance traceability matrix mapping every criterion below to automated evidence or a manual check.
- Failure matrix execution and recorded evidence.
- API and internal/experimental usage review.
- Dependency, license, credential, privacy, and telemetry audit.
- Checkpoint fault injection, recovery, concurrency stress, large-output, and cancellation campaigns.
- Inline and agent performance benchmarks.
- Clean install, upgrade, uninstall, settings, and recovery checks.
- Complete tests, IDE inspections, Plugin Verifier, and packaged private plugin artifact.
- ASCII release notes with exact versions, checks, known non-blocking limitations, and rollback/install guidance.
- Implementation plan at `docs/implementation/phase-11-hardening-release-plan.md`.

## 5. Out of scope

- New capabilities or redesigns not needed to satisfy a blocker.
- Additional IDE versions, Maven, other languages, marketplace publication, or unrestricted terminal access.
- Suppressing verifier, inspection, test, privacy, or security failures without a documented and approved basis.

## 6. Required tests and evidence

- Architecture: Java 25/2026.2 target, package isolation, project-scope cancellation.
- Agent: generic loop, completion gate, independent budgets, runaway termination, deterministic event stream.
- Rules: Core import barriers, module language levels, logging, encoding, hot-path advisory scope.
- Safety: no stash/Git mutation/shell, durable checkpoint first, conflict-safe rollback, path/symlink rejection, PasswordSafe-only secrets.
- Verification: direct tests when confident, module escalation, full verification, and post-verification invalidation.
- UI/completion: event-only tree, native diffs/commit handoff/provider, custom keymaps, immediate cancellation, no stale render, measured p95 overhead.
- Failure cases: 429/5xx, malformed/unknown Gemini steps, indexing, user edits, close/unload, hung Gradle, iterative failure, mandatory failure, exhausted repair, missing key, expired state, rollback conflict, checkpoint failure, and file change after verification.
- Clean package install, upgrade, uninstall, configuration, recovery, and private release artifact verification.

## 7. Completion gate

All criteria and failure cases have evidence on IntelliJ IDEA 2026.2, no release blocker remains, and the private plugin artifact plus release notes are reproducible.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 11A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, every phase plan, the complete source, open TODOs, tests, and reports. Re-check current IntelliJ IDEA 2026.2 and Gemini compatibility contracts. Produce `docs/implementation/phase-11-hardening-release-plan.md`.

Map every locked contract, required test, and listed failure case in this mini blueprint to an automated test or explicit manual check. Plan API/internal-usage review, dependency/license audit, credentials/privacy/telemetry review, checkpoint fault injection, recovery, concurrency stress, large outputs, cancellation, benchmarks, inspections, Plugin Verifier, install/upgrade/uninstall, packaging, and release notes. Mark every unmet hard invariant as a release blocker.

Do not implement or add features. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 11B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-11-hardening-release-plan.md`. Execute only the approved hardening and release work. Fix issues required by a locked contract, test, failure case, compatibility check, security/privacy review, or measured regression. Add no unrelated feature.

Run the complete automated suite, fault injection, recovery, concurrency stress, large-output tests, local benchmarks, IntelliJ inspections, and Plugin Verifier against IntelliJ IDEA 2026.2. Produce the reproducible private plugin artifact and ASCII release notes with exact evidence and non-blocking limitations. Update the plan, inspect the diff, then commit and fast-forward push the release-ready state to `main`.
```
