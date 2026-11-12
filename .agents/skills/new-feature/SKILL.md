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
- Apply PROJECT-RULES.md design defaults: use idiomatic Java/Spring and Lombok for boilerplate; explain concepts separately rather than writing tutorial-style implementation.
- Prioritize normal demo flows. Save concrete advanced suggestions in docs/DEFERRED-IMPROVEMENTS.md with their consequence and revisit trigger; continue the selected feature without speculative retries, concurrency recovery, optimizations, extra layers, or repeated optional approval questions. Implement deferred items only when the user selects them.
- Own the technical assessment: the learner is not expected to classify complexity or discover problems. After a runnable increment, proactively recommend one concrete issue to study, illustrate/reproduce its cause, compare proportionate alternatives, and explain why the proposed next step is suitable. A saved proposal must include guidance rather than an unexplained choice for the user; agree material scope changes before implementation.
- Follow PROJECT-RULES.md section 0: defer new optional mechanisms by default. When proposing safeguards, access/null-check restrictions, patterns, or extra layers, explain in Vietnamese what they solve, whether they are required, what happens if omitted now, their cost, and whether they can wait. Obtain the user's agreement before adding them; continue independent authorized work and do not re-ask for existing conventions or routine details.
- Business services use a [Feature]Service interface in service/ and a [Feature]ServiceImpl implementation in service/impl/, even with one implementation. Put @Service, @RequiredArgsConstructor, and @Transactional on the implementation; controllers, Discord listeners, and other consumers inject the interface. This is the agreed project convention, not a new optional layer requiring repeated approval. Other patterns, mappers, and extra layers still require an actual need.
- Use request/response records for applicable JSON APIs; validate inputs, enforce ownership/authorization, and keep HTTP concerns at HTTP boundaries.
- Use the agreed response envelope for standard JSON APIs. OAuth redirects and no-content/file responses follow their own contract.
- Follow the SQL-owned phases in `docs/DATABASE.md` and ADR-003: currently refine docs/sql/schema.sql without declaring it final, keep Flyway disabled and Hibernate ddl-auto:none. Do not create/enable V1 until the user explicitly finalizes the main SQL. After that checkpoint, use versioned migrations and Hibernate validate. Keep entity mapping separate from DDL and preserve retained data; never use Hibernate schema generation/update.
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
