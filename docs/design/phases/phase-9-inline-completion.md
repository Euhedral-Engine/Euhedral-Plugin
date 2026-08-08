# Phase 9: Inline Completion Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 8: Agent UI](phase-8-agent-ui.md)

Next phase: [Phase 10: OAuth](phase-10-oauth.md)

## 1. Objective

Add a low-overhead native inline completion vertical slice that is independent from autonomous sessions and optimized for cancellation and stale-result safety.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- The current plugin transport primitives and settings, without reading autonomous engine internals.
- Current official IntelliJ IDEA 2026.2 `InlineCompletionProvider` documentation and extension contract.
- Current official Gemini 3.5 Flash-Lite model, thinking, streaming, and stateless request documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Use `gemini-3.5-flash-lite`, minimal thinking, and `store=false`.
- Register the native inline completion provider; never install a global Tab handler.
- Inline completion does not call `AgentExecutionEngine` or reuse agent conversations.
- Maintain one active request per editor and cancel it immediately on new input or disposal.
- Capture imports, class declaration, nearest semantic container, up to 8 KiB before the caret, and up to 4 KiB after it.
- Use immutable editor/document revision identity and reject every stale response before rendering.
- Cache only in memory with a bounded LRU; no source or completion content is persisted.
- Use a five-second response timeout unless measured evidence changes the configured default.
- Measure local context, dispatch, first-result, cache, cancellation, and stale-result timing without source telemetry.
- Plugin overhead has a measured p95 target; remote sub-100 ms completion is not promised.
- Inline completion may overlap an autonomous agent session without sharing state.

## 4. Deliverables

- Native provider registration and provider implementation.
- Semantic bounded completion context builder.
- Per-editor request coordinator, debounce, cancellation, timeout, and disposal.
- Stateless Flash-Lite request mapping and streaming completion elements.
- Stale-revision rejection.
- Memory-only LRU cache.
- Source-free local metrics and benchmark harness.
- Implementation plan at `docs/implementation/phase-9-inline-completion-plan.md`.

## 5. Out of scope

- Changes to autonomous agent state or prompts.
- Repository-wide retrieval.
- Persistent cache or telemetry.
- Global Tab interception.
- OAuth.
- Remote latency guarantees.

## 6. Required tests and evidence

- Context windows at file start/end, imports, nested classes, methods, and oversized files.
- One active request per editor and isolation across editors.
- New keystroke, caret move, document edit, editor close, project close, and timeout cancellation.
- Stale result rejection before first and later streamed elements.
- Multi-line native rendering and custom keymap compatibility smoke check.
- LRU bounds, cache keys, cache invalidation, and no persistence.
- Concurrent inline and agent activity.
- Local overhead p50/p95 benchmark with measured results.

## 7. Completion gate

Native multi-line suggestions remain responsive and never render stale content while typing or while an autonomous session runs.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 9A: Plan

Mode: Reasoning

Reasoning level: Max

```text
Read this mini blueprint and the current plugin. Re-check the current IntelliJ IDEA 2026.2 inline completion API and Gemini 3.5 Flash-Lite contract. Produce `docs/implementation/phase-9-inline-completion-plan.md`.

Plan native registration, per-editor ownership, debounce, cancellation, semantic context, 8 KiB prefix, 4 KiB suffix, stateless minimal-thinking requests, streaming, stale revision rejection, multi-line elements, memory-only LRU cache, timeout, source-free metrics, benchmarks, and overlap with agent sessions. Resolve public versus experimental APIs.

Do not read or couple to autonomous engine internals. Do not implement or promise remote sub-100 ms latency. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 9B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-9-inline-completion-plan.md`. Implement only Phase 9 with `gemini-3.5-flash-lite`, `store=false`, and minimal thinking. Use the native provider and acceptance behavior, maintain one active request per editor, cancel on new input, and reject every stale result.

Keep context and cache independent from agent state and persist no source content. Add all context, cache, cancellation, stale-result, rendering, disposal, concurrency, and local-overhead tests. Run the complete suite and Plugin Verifier. Record measured results in the plan, inspect the diff, then commit and fast-forward push Phase 9 to `main`. Do not add OAuth.
```
