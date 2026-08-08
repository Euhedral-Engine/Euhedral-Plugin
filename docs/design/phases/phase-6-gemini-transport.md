# Phase 6: Gemini Transport Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 5: Gradle and Verification](phase-5-gradle-verification.md)

Next phase: [Phase 7: Autonomous Agent](phase-7-autonomous-agent.md)

## 1. Objective

Implement an adapter-isolated, cancellable Gemini Interactions transport with API-key credentials, stateful agent continuations, streaming, deletion, and exact retry behavior.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-1-domain-ports-plan.md` and its `GeminiTransport` and `CredentialPort` contracts.
- The current source and settings shell.
- Current official Gemini Interactions API, function calling, thinking, model, deprecation, billing, and API-key documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- Use `java.net.http.HttpClient`, REST, and SSE directly; no Ktor or sidecar.
- Use `gemini-3.6-flash` for agent work.
- Default agent thinking to medium and allow low, medium, or high.
- Use `store=true` and ordered `previous_interaction_id` continuation by default.
- Re-send tools, system instructions, and generation configuration on every continued interaction.
- Preserve every function call ID in its matching function result.
- Own wire DTOs inside the Gemini adapter; domain and ports never import Google schemas.
- Store API keys only in IntelliJ `PasswordSafe` through `CredentialAttributes`.
- Respect `Retry-After`; transport retries never consume tool or repair budgets.
- Unknown non-critical steps are preserved safely; correctness-critical unknowns fail clearly.
- Never log credentials, authorization headers, prompts, source payloads, or raw tool results by default.
- OAuth is deferred to Phase 10.

## 4. Deliverables

- PasswordSafe API-key credential adapter and settings integration.
- HTTP client lifecycle and request/response mapping for current Interactions `steps`.
- SSE parser robust to arbitrary byte and frame boundaries.
- Unary and streaming interaction operations, function results, stateful continuation, cancellation, and interaction deletion.
- HTTP error taxonomy, retry policy, safe logging, and source-free metrics.
- Fake HTTP/SSE server and opt-in live smoke test.
- Implementation plan at `docs/implementation/phase-6-gemini-transport-plan.md`.

## 5. Out of scope

- Autonomous session orchestration or tool dispatch.
- Inline completion transport behavior.
- OAuth.
- Ktor, Node, Python, or another sidecar.
- Logging full payloads.

## 6. Required tests and evidence

- Unary and streaming success.
- Split SSE fields, frames, CRLF variants, comments, and split UTF-8 bytes.
- Malformed JSON, malformed call schema, unknown event/step, and premature EOF.
- 401 and missing key behavior before mutation.
- 429 with Retry-After and retryable 5xx accounting.
- Timeout and cancellation at connect, headers, body, and stream stages.
- Stateful function-result continuation with repeated per-turn configuration.
- Interaction-chain deletion and partial deletion failure.
- Opt-in live smoke test excluded from normal builds.

## 7. Completion gate

A scripted function-call exchange succeeds against the fake server and an optional live endpoint without leaking Gemini wire types beyond the adapter.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 6A: Plan

Mode: Reasoning

Reasoning level: Ultra

```text
Read this mini blueprint, the Phase 1 port plan, the current source, and current official Gemini documentation. Produce `docs/implementation/phase-6-gemini-transport-plan.md`.

Resolve the current Interactions request, `steps`, SSE, continuation, function call/result, deletion, model, and thinking contracts. Plan PasswordSafe API keys, `java.net.http.HttpClient`, lifecycle, DTO isolation, call IDs, required per-turn configuration, cancellation, Retry-After, retry taxonomy, unknown-step compatibility, safe logging, fake-server tests, and an opt-in live smoke test.

Do not add Ktor, a sidecar, OAuth, or agent orchestration. Do not implement. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 6B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-6-gemini-transport-plan.md`. Implement only Phase 6 with plugin-owned DTOs and a `java.net.http` adapter. Use the current Interactions `steps` schema, preserve call IDs, repeat interaction-scoped configuration, default to stateful storage, store keys only in PasswordSafe, and propagate cancellation.

Add every fake-server contract test and keep live tests opt-in. Run all offline tests and Plugin Verifier. Update the plan with the exact API contract and evidence, inspect the diff, then commit and fast-forward push Phase 6 to `main`. Do not add the agent loop, inline completion, or OAuth.
```
