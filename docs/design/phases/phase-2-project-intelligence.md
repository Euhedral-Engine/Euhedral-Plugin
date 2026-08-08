# Phase 2: Project Rules and Intelligence Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 1: Domain and Ports](phase-1-domain-ports.md)

Next phase: [Phase 3: Durable Editing](phase-3-durable-editing.md)

## 1. Objective

Build model-neutral, cancellable IntelliJ project intelligence and a versioned Euhedral rules configuration before any file can be changed.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-1-domain-ports-plan.md` and its implemented port contracts.
- The current source.
- The real Euhedral workspace when available; do not invent module paths when it is absent.
- Current official IntelliJ 2026.2 PSI, project-model, Gradle-model, search, and threading documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Explicit `.euhedral-agent.yaml` configuration wins over path, module-name, dependency, and import heuristics.
- Heuristic classification may report confidence but cannot create later hard policy denials.
- Distinguish plugin runtime JDK, module SDK, and Java source language level.
- Distinguish main and test source sets and dependency scopes.
- Use IntelliJ indexes and `JavaPsiFacade`, `ReferencesSearch`, `ClassInheritorsSearch`, and `OverridingMethodsSearch`; do not recursively recreate index behavior.
- Project-wide PSI work is cancellable and smart-mode aware.
- Model-visible paths are project-relative; results are bounded and truncation is explicit.
- Opaque symbol IDs are session-scoped and backed by invalidation-aware pointers.

## 4. Deliverables

- Versioned `.euhedral-agent.yaml` schema, parser, validation, precedence, and rule summary.
- Project context service and Gradle-linked module mapping.
- Module classifier with role, confidence, SDK, language level, and source-set data.
- PSI context extractor and symbol resolver.
- Inspection tools: `workspace_context`, `read_file_range`, `search_text`, `find_symbol`, `find_references`, `find_implementations`, and `file_metadata`.
- Model-neutral bounded result types.
- Implementation plan at `docs/implementation/phase-2-project-intelligence-plan.md`.

## 5. Out of scope

- Workspace mutation or checkpoints.
- Hard policy enforcement.
- Gemini schemas or calls.
- Build/test process execution.
- Affected-test selection.

## 6. Required tests and evidence

- Java 11, 17, and 21 module language-level extraction.
- Cross-module port-to-adapter references, inheritors, and overrides.
- Test-only or transitive framework dependencies do not misclassify production modules.
- Explicit rule precedence over every heuristic.
- Indexing wait, cancellation, pointer invalidation, and project close.
- Bounded output, truncation, invalid ranges, ambiguous symbols, and project-relative paths.

## 7. Completion gate

Every inspection tool can be invoked from tests or a temporary developer action without Gemini and returns stable model-neutral data.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 2A: Plan

Mode: Reasoning

Reasoning level: Max

```text
Read this mini blueprint, `docs/implementation/phase-1-domain-ports-plan.md`, its implemented contracts, and the current source. Inspect the real Euhedral workspace only if available. Produce `docs/implementation/phase-2-project-intelligence-plan.md`.

Plan the versioned rules schema and precedence, project context, IntelliJ module and linked Gradle mapping, SDK versus source language level, source sets, smart read actions, symbol IDs and pointers, `JavaPsiFacade`, native reference/inheritor/override searches, indexing behavior, bounded results, and every inspection tool. Include fixture projects and exact tests for Ports and Adapters lookup and test-only dependency misclassification.

Do not implement or invent repository module paths. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 2B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-2-project-intelligence-plan.md`. Implement only Phase 2 using public IntelliJ IDEA 2026.2 APIs. Add the rules service, classifier, context extractor, symbol resolver, inspection adapter, and model-neutral bounded results. Keep project-wide PSI work cancellable and smart-mode aware.

Add all required fixtures and tests. Run the complete suite and Plugin Verifier, update the plan with final decisions and evidence, inspect the diff, then commit and fast-forward push Phase 2 to `main`. Do not add Gemini, editing, policy enforcement, or build execution.
```
