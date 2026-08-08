# Phase 10: OAuth Mini Blueprint

Status: Approved implementation slice

Parent: [Master design blueprint](../gemini-intellij-plugin-master-blueprint.md)

Previous phase: [Phase 9: Inline Completion](phase-9-inline-completion.md)

Next phase: [Phase 11: Hardening and Release](phase-11-hardening-release.md)

## 1. Objective

Add optional desktop OAuth without disturbing the working API-key credential and transport contracts.

## 2. Context boundary

A fresh agent working on this phase needs only:

- This mini blueprint.
- `docs/implementation/phase-6-gemini-transport-plan.md` and its implemented credential/transport contracts.
- The current settings UI.
- Current official Google Gemini OAuth, installed application, loopback redirect, token refresh, revocation, billing, and Cloud-project documentation.

The full master blueprint is not required for normal execution of this phase. If this file conflicts with the master, stop and resolve the conflict instead of guessing.

Common repository rules:

- Read repository agent instructions before acting.
- Work directly on `main` only with fast-forward pushes.
- Preserve unrelated changes and stage only phase-owned paths.
- Keep plans, documentation, commit messages, and generated text ASCII-only.
- Do not amend, rebase, force-push, or combine this phase with another phase.
- Re-check version-sensitive official documentation only for APIs first introduced here.

## 3. Locked contracts

- API-key authentication remains the default and must continue passing all transport tests.
- OAuth requires a Google Cloud project and Generative Language API access; consumer Gemini subscriptions are not API billing.
- Keep provider behavior behind `CredentialPort` and transport-neutral credential values.
- Use the current Google desktop application contract and PKCE only where the current official contract supports or requires it.
- Bind the callback server to loopback only, use a random available port, verify state, and enforce timeout and cancellation.
- Store refresh and access tokens only in PasswordSafe; never persist authorization codes in settings or files.
- Never log codes, tokens, client secrets, redirect query strings, or authorization headers.
- Refresh is serialized per account/provider to avoid token storms.
- Logout clears local credentials; revoke attempts remote invalidation and reports partial failure.
- Project close and plugin unload stop callback and refresh work.

## 4. Deliverables

- Authentication provider selection and settings guidance.
- Desktop OAuth authorization launcher and loopback callback service.
- State and PKCE handling matching the current official contract.
- Token exchange, refresh, expiry, revoke, logout, and cancellation.
- PasswordSafe token storage and migration-safe credential keys.
- Provider-neutral integration with the existing transport.
- Fake authorization/token/revoke server tests.
- Implementation plan at `docs/implementation/phase-10-oauth-plan.md`.

## 5. Out of scope

- Replacing API-key auth.
- Assuming consumer subscription entitlements.
- Embedding a client secret that must remain confidential in a desktop binary.
- Changing Gemini wire schemas or agent behavior.
- A general-purpose local web server.

## 6. Required tests and evidence

- Authorization success and browser-launch request construction.
- User denial, malformed callback, state mismatch, duplicate callback, and callback timeout.
- Occupied or lost port and loopback-only binding.
- PKCE verifier/challenge behavior when required by current contract.
- Token exchange, proactive refresh, concurrent refresh collapse, revoked refresh token, and clock skew.
- PasswordSafe persistence and no secret leakage in logs/errors.
- Logout, revoke success, revoke partial failure, project close, and plugin unload.
- API-key and OAuth providers pass the same transport credential contract suite.

## 7. Completion gate

API-key and OAuth modes satisfy the same transport contract, while API-key remains the stable default and no token leaves PasswordSafe except for authorized requests.

The phase plan must record final type names, file paths, verified commands, test results, API choices, deviations, and remaining risks.

## 8. Prompt set

### Prompt 10A: Plan

Mode: Reasoning

Reasoning level: Max

```text
Read this mini blueprint, `docs/implementation/phase-6-gemini-transport-plan.md`, the implemented credential code, settings, and current official Google OAuth documentation. Produce `docs/implementation/phase-10-oauth-plan.md`.

Resolve the exact desktop flow, Cloud-project prerequisites, loopback callback, state, PKCE applicability, browser launch, timeout, token exchange, refresh, revoke, logout, PasswordSafe keys, provider selection, cancellation, collisions, concurrency, and user guidance. Preserve API-key defaults and the existing transport contract.

Do not assume consumer subscription billing and do not implement. Review the ASCII-only plan, then commit and fast-forward push only that plan to `main`.
```

### Prompt 10B: Implement

Mode: Coding

Reasoning level: High

```text
Read this mini blueprint and `docs/implementation/phase-10-oauth-plan.md`. Implement only Phase 10 against the current official Google contract. Keep provider logic behind `CredentialPort`, bind callbacks to loopback, verify state, apply PKCE as required, serialize refresh, store tokens only in PasswordSafe, and log no secrets.

Add all fake-server, callback, timeout, collision, refresh, revoke, logout, lifecycle, and shared credential-contract tests. Run all offline tests and Plugin Verifier. Update the plan with evidence, inspect the diff, then commit and fast-forward push Phase 10 to `main`. Do not change agent or completion behavior.
```
