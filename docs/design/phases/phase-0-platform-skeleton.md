# Phase 0: Platform Skeleton Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: None

Next phase: [Phase 1: Domain and Ports](phase-1-domain-ports.md)

## 1. Objective

Create the smallest verifiable IntelliJ plugin foundation and freeze platform, lifecycle, package, and build boundaries before domain code is added.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- The current repository tree.
- Current official IntelliJ IDEA 2026.2 plugin documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Target IntelliJ IDEA 2026.2 only.
- Use Java 25, Kotlin, IntelliJ Platform Gradle Plugin 2.x, and platform-bundled coroutines.
- Use one Gradle module initially, with architecture tests enforcing package boundaries.
- A project-level service with an injected CoroutineScope owns project-lifetime jobs.
- UI registration proves wiring only; it contains no agent behavior.

## 4. Deliverables

- Gradle wrapper and plugin build configured for Java 25 and IntelliJ IDEA 2026.2.
- `plugin.xml` with the project service, empty tool window, settings shell, and required module/plugin dependencies.
- Project service cancellation and disposal probe.
- Package skeleton for `core`, `application`, `ports`, `adapters`, `policy`, `completion`, `ui`, `settings`, and `bootstrap`.
- Architecture tests, platform smoke tests, `runIde`, and Plugin Verifier configuration.
- Implementation plan at `docs/implementation/phase-0-platform-plan.md`.

## 5. Out of scope

- Agent domain types or ports.
- PSI project intelligence.
- Editing, policy, Gradle execution, Gemini transport, OAuth, or real UI workflows.
- Internal or experimental IntelliJ APIs unless a documented blocker is approved.

## 6. Required tests and evidence

- Plugin loads and unloads without leaked jobs.
- Project close cancels the injected project scope.
- Tool window and settings extension registrations resolve.
- Architecture tests reject forbidden package dependencies.
- Build fails if the Java target drifts from 25.
- Plugin Verifier passes against the selected 2026.2 build.

## 7. Completion gate

The plugin loads, unloads, verifies, and exposes only inert UI shells with no scope leaks.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 0A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, the current Euhedral-Plugin repository, and the latest official IntelliJ IDEA 2026.2 plugin documentation. Produce `docs/implementation/phase-0-platform-plan.md`.

Resolve exact Gradle, Kotlin, Java 25, IntelliJ Platform Gradle Plugin 2.x, platform dependency, test framework, Plugin Verifier, and `runIde` configuration. Specify `plugin.xml`, the injected project CoroutineScope service, empty tool-window and settings shells, package boundaries, architecture tests, public versus internal APIs, file paths, commands, risks, and exit evidence.

Do not implement production code. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 0B: Implement

Mode: Coding

Reasoning level: Medium

```text
Read this mini blueprint and `docs/implementation/phase-0-platform-plan.md`. Implement only Phase 0. Establish the package boundaries even when packages contain only minimal marker types required by tests.

Add the smallest tool-window and settings shells needed to prove registration and project-scope cancellation. Run every Phase 0 test, `runIde` smoke check, and Plugin Verifier against the selected IntelliJ IDEA 2026.2 target. Update the phase plan with actual versions, commands, results, and deviations. Inspect the diff, then commit and fast-forward push Phase 0 to `main`. Do not start Phase 1.
```
