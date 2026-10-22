---
name: new-feature
description: "Implement a requested feature or extend application behavior in a small, reviewable increment using the project conventions. Use for implementation requests, not brainstorming or documentation-only discussion."
---

# Build a Feature

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Build the requested project feature in a small, reviewable increment. Apply PROJECT-RULES.md section 17; design documents are revisable starting points.

## 1. Establish the Feature

- Work from the user's request or agreed prompt. If asked only to plan, prepare the implementation prompt without starting application code.
- Read `AGENTS.md`, `docs/PROJECT-OVERVIEW.md`, current status, and relevant API/database/architecture/ADR/context sections.
- Inspect actual source and configuration. Identify existing reusable code, template behavior, and missing implementation.
- State the intended behavior, relevant draft assumptions, and a few acceptance examples in plain language.
- A request to implement the feature authorizes routine work within that scope. Do not stop for confirmation of every class or layer.
- If a new choice materially changes behavior, persisted data, API compatibility, authorization, or architecture, explain the difference and a recommendation. Agree that affected choice before implementing it; continue independent work.

## 2. Implement a Useful Slice

- Reuse the existing business module or add one only when needed. Do not create an entity, controller, repository, or full CRUD set merely because a template shows them.
- Controllers and Discord listeners call feature services; follow the current interface/implementation and constructor-injection conventions.
- Use request/response records for applicable JSON APIs; validate inputs, enforce ownership/authorization, and keep HTTP concerns at HTTP boundaries.
- Use the agreed response envelope for standard JSON APIs. OAuth redirects and no-content/file responses follow their own contract.
- Add schema changes through versioned migrations when persistence changes. Preserve retained history and coordinate related writes.
- Adapt the implementation order to the feature: an event handler, query, or authentication flow need not start with a new entity.
- For study mutations, consider duplicate/late events, source conflicts, transaction boundaries, segments, and recovery according to the current agreed design.

## 3. Check the Behavior

- Write meaningful tests for the behavior introduced or changed; use the sibling `write-tests` skill's guidance when selecting tests and isolating the environment.
- Verify acceptance examples and affected existing behavior with the checks appropriate to the change.
- Do not run database tests against development/retained data or contact real providers by default.
- If a contract is still a draft and a material expectation is unclear, resolve that point rather than encoding an accidental implementation as the requirement.

## 4. Keep Documentation Current

- After agreement, update affected API/database/architecture/ADR sections automatically; use Planned until implemented and Verified only with recorded check evidence.
- Create/update module CONTEXT.md for non-obvious behavior. Keep its current description accurate and append a dated Refactor Log entry for significant changes.
- Update PROJECT-STATUS.md with actual progress, remaining work, and checks. Do not mark a whole feature complete after only scaffolding or documentation.
- Treat code-size targets as review guidance, not automatic blockers. Report changed files, checks/results, and remaining issues briefly in chat. Provide a complete diff/patch when requested; review can also use the shared workspace directly.
