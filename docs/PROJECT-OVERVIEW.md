# Project Overview — Study Together

Study Together is a learning project for tracking study time through Discord and a planned React website. The initial technical direction is Java 25, Spring Boot 4, PostgreSQL, and application JWTs. Google/local login, verified account linking, and web study have draft extension contracts.

The repository currently contains a Spring Boot application skeleton and a JDA bot that logs Discord voice, camera, and screen-sharing events. Study persistence, authentication, and the React frontend remain planned. Design documents describe target behavior; inspect actual source and [PROJECT-STATUS.md](./PROJECT-STATUS.md) for implementation and verification evidence.

## Working Model

The user learns while building one useful increment at a time. Design documents and Proposed ADRs are revisable starting points. Discuss consequential changes when the affected feature is built, automatically update affected documentation after agreement, and distinguish planned behavior from implemented and verified work.

Shared AI instructions live in the root [AGENTS.md](../AGENTS.md). The detailed decision/documentation workflow is in [PROJECT-RULES.md, section 17](./PROJECT-RULES.md#17-documentation-requirements).

## Document Map

- [PROJECT-RULES.md](./PROJECT-RULES.md): current coding conventions and collaboration workflow.
- [PROJECT-STATUS.md](./PROJECT-STATUS.md): inspected implementation, actual progress, checks, and next tasks.
- [ARCHITECTURE.md](./ARCHITECTURE.md): module responsibilities and request/event flows.
- [API_SPEC.md](./API_SPEC.md): draft API contracts and provisional defaults.
- [DATABASE.md](./DATABASE.md): target schema, constraints, and extension dependencies.
- [decisions/](./decisions/): significant decisions, their status, rationale, and revision history.
- Module `CONTEXT.md`: current implementation context and dated decision/refactor history; read it with source before modifying that module.

## Reusing These Files

Keep common collaboration guidance in AGENTS.md and reusable skills in `.agents/skills/`. When copying the structure to another project, adapt this overview, technology/domain rules, and design documents to that project. Rebuild its status from its actual source/checks; never copy completed or verified claims as facts about a new codebase.
