# Project Overview — Study Together

Study Together tracks study time through Discord and a planned React website. It is built to practice professional Java/Spring development, using Java 25, Spring Boot 4, PostgreSQL, and application JWTs as the initial technical direction. As confirmed on 2026-10-30, the product must support multiple website login methods, timed study directly on the website without requiring Discord, and linking Discord to an existing account created through another login method. Linked study history belongs to the same internal user. Provider selection and detailed contracts remain draft; these capabilities are Planned, not implemented.

Planned account flows: a person already tracked through Discord signs into the website with that same Discord account and reuses the existing internal user/history, with no separate linking step. A person using another website login method who has not linked Discord uses an explicit Discord-link operation to associate Discord with the current internal user. Both can study on the website; using both study sources does not itself require another account or another link.

The repository contains a Spring Boot skeleton, five ORM entities and their repositories, UserIdentityService/UserIdentityServiceImpl for Discord identity resolution, and StudySessionService/StudySessionServiceImpl for transactional join/leave/move/media persistence. DiscordVoiceListener now creates command records and calls the service interface for voice, camera, and screen-sharing events, excluding bots at every callback. Custom study lookups use native SQL. Concurrent-event recovery remains deferred; live Discord connectivity is still unverified. Inspect [PROJECT-STATUS.md](./PROJECT-STATUS.md) for check evidence and [Discord study demo](./DISCORD-STUDY-DEMO.md) for isolated checks and live setup. The main SQL in docs/sql/schema.sql is still Draft. Flyway is disabled and application Hibernate uses none until the user finalizes the SQL. The earlier V1 activation was premature and has been withdrawn; passing isolated tests does not finalize the baseline.

## Working Model

Build useful increments with idiomatic code and design suited to actual requirements. Prefer established framework features and Lombok over handwritten boilerplate; use Service interfaces with ServiceImpl implementations consistently, including single-implementation business services. Introduce other patterns and additional layers when they solve a concrete problem. Explain tradeoffs in Vietnamese while keeping implementation suitable for professional work. Readability remains a maintenance concern; tutorial-style expansion is not a coding requirement.

Current priority, confirmed 2026-10-31: complete normal flows for a working demo. Save concrete advanced proposals in [DEFERRED-IMPROVEMENTS.md](./DEFERRED-IMPROVEMENTS.md) for later discussion instead of adding them automatically or interrupting normal work with hypothetical scenarios.

The user is learning through working software. Codex evaluates what is necessary now and proactively recommends subsequent investigations; the user need not classify complexity first. After each runnable flow/demo checkpoint, study one relevant issue through a concrete scenario or reproduction, its cause, alternatives, and a reasoned recommendation. Build understanding before expanding the implementation; do not wait for the entire application to be finished before beginning this cycle.

Design documents and Proposed ADRs are revisable starting points. Discuss consequential changes when the affected feature is built, automatically update affected documentation after agreement, and distinguish planned behavior from implemented and verified work. The current entities use plain Lombok constructors/accessors and retain their database mappings. Defer new optional mechanisms by default; before adding one, explain its purpose, consequences of omission, tradeoffs, and ability to wait in Vietnamese, and obtain the user's agreement. Existing agreed conventions and routine authorized work continue without repeated approval.

Shared AI instructions live in the root [AGENTS.md](../AGENTS.md). The detailed decision/documentation workflow is in [PROJECT-RULES.md, section 17](./PROJECT-RULES.md#17-documentation-requirements).

Code is grouped by layer under entity, repository, service/service.impl, and enums; Discord integration lives under discord and BaseEntity remains under persistence. Command records for Discord study events live under dto/study; controllers remain planned. The user requested the layer-based layout on 2026-10-31.

As directed on 2026-10-31, handwritten database CHECK constraints are deferred. Draft SQL omits them; intended validation rules remain documented. Unique constraints, foreign keys, required columns, types, and indexes belong in SQL, not entity DDL annotations. Entities retain mapping and runtime persistence behavior. After the user finalizes the main SQL, create V1, enable migrations, and use Hibernate validate. Existing databases have not been changed; [ADR-003](./decisions/003-schema-baseline-workflow.md) records the clarified sequence.

## Document Map

- [PROJECT-RULES.md](./PROJECT-RULES.md): current coding conventions and collaboration workflow.
- [PROJECT-STATUS.md](./PROJECT-STATUS.md): inspected implementation, actual progress, checks, and next tasks.
- [DEFERRED-IMPROVEMENTS.md](./DEFERRED-IMPROVEMENTS.md): advanced suggestions to revisit after the demo, with current consequences and triggers.
- [ARCHITECTURE.md](./ARCHITECTURE.md): module responsibilities and request/event flows.
- [API_SPEC.md](./API_SPEC.md): draft API contracts and provisional defaults.
- [DATABASE.md](./DATABASE.md): target schema, constraints, and extension dependencies.
- [decisions/](./decisions/): significant decisions, their status, rationale, and revision history.
- Business contexts: [user](./contexts/user/CONTEXT.md) and [study](./contexts/study/CONTEXT.md), with current behavior and dated decision/refactor history spanning the layer-based source.

## Reusing These Files

Keep common collaboration guidance in AGENTS.md and reusable skills in `.agents/skills/`. When copying the structure to another project, adapt this overview, technology/domain rules, and design documents to that project. Rebuild its status from its actual source/checks; never copy completed or verified claims as facts about a new codebase.
