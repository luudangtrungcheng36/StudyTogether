# Study Together — AI Working Instructions

Shared guidance for Codex and Antigravity. Read [PROJECT-OVERVIEW.md](./docs/PROJECT-OVERVIEW.md), [PROJECT-STATUS.md](./docs/PROJECT-STATUS.md), and the relevant parts of [PROJECT-RULES.md](./docs/PROJECT-RULES.md) before work.

## Working together

- This project prepares the user for professional Java/Spring work. Use idiomatic implementation and proportionate design; explain consequential choices in Vietnamese without turning application code into a step-by-step tutorial.
- Follow PROJECT-RULES.md for coding defaults: use Lombok for boilerplate, constructor injection, and records for DTOs. Business services consistently use a [Feature]Service interface in service/ and a [Feature]ServiceImpl class in service/impl/, even with one implementation; consumers inject the interface. Keep @Service, @RequiredArgsConstructor, and transaction annotations on the implementation. This user-confirmed convention supersedes the former concrete-service default; other abstractions still require an actual need.
- Use the agreed layer-based packages: entity, repository, service, service.impl, enums, and discord. Add controller/dto packages when needed; business contexts live under docs/contexts/<domain>/CONTEXT.md.
- Handwritten database CHECK constraints are deferred by the user's 2026-10-31 direction. Keep intended validation rules documented; do not automatically add CheckConstraint annotations or SQL CHECK clauses. Unique constraints, foreign keys, required columns, and indexes remain in scope as agreed.
- SQL owns the schema. Current phase: refine docs/sql/schema.sql as a draft, keep Flyway disabled, Hibernate ddl-auto:none, and automatic SQL initialization off. Create/enable V1 only after the user explicitly finalizes the main SQL; use migrations and validate afterward. Entities remain ORM mappings without schema indexes/uniques/FK names/actions/type definitions. Do not use Hibernate schema generation/update. See ADR-003.
- Use practical Java/Spring code for the current requirement. Defer new optional safeguards, restrictions, patterns, and layers by default. Before adding one, explain in Vietnamese what it solves, whether it is required, what happens if omitted now, its tradeoffs, and whether it can wait; obtain the user's agreement first. Existing agreed conventions and routine work within the authorized scope do not need repeated approval. See PROJECT-RULES.md section 0.
- Current priority (2026-10-31): implement normal flows to reach a working demo. Record advanced suggestions in docs/DEFERRED-IMPROVEMENTS.md and revisit after the demo with the user's agreement; do not automatically add retry/concurrency recovery, speculative optimizations, or extra layers. Keep routine demo work moving without repeated optional approval questions.
- Codex owns the technical assessment and sequencing: decide what the current flow needs, identify what can wait, and proactively recommend the next issue to study. The learner is not expected to classify mechanisms or discover problems first. After a runnable flow/demo checkpoint, take one relevant issue at a time: show a concrete scenario or reproduction, explain the cause and what happens without a fix, compare proportionate alternatives, and recommend one with reasons. Save the proposal and obtain agreement before expanding behavior; routine implementation choices remain autonomous.
- Architecture, database, API drafts, and Proposed ADRs are starting points. Compare them with actual code; distinguish draft choices, agreed plans, implementation, and verified results.
- Follow the user's latest direction. Resolve routine details within the requested scope; ask focused questions only for material changes to behavior, data, permissions, or architecture.
- After agreement, automatically update affected documentation and current module context. Preserve significant decision history; never mark unbuilt or unchecked work complete.
- The usual flow is discussion with Codex, an agreed prompt for Antigravity, then Codex review of code/diff. The user can assign either tool another role. Planning or review alone does not authorize application-code changes.
- Work directly from the request or agreed prompt. Report what changed, checks actually run, and remaining issues briefly in chat; use files only when they help the task.
- Review actual source/diffs where available. Include new files when preparing a requested patch; distinguish observed defects from conditional risks and optional improvements.
- Continue authorized work without asking again for each class/file. Keep unrelated changes out of scope, and update progress after meaningful work.

## Project references

- [PROJECT-RULES.md](./docs/PROJECT-RULES.md), especially section 17: conventions and automatic documentation updates.
- [API_SPEC.md](./docs/API_SPEC.md), [DATABASE.md](./docs/DATABASE.md), [ARCHITECTURE.md](./docs/ARCHITECTURE.md), and [decisions/](./docs/decisions/): read only what the task affects.
- Business context: read [user CONTEXT.md](./docs/contexts/user/CONTEXT.md) or [study CONTEXT.md](./docs/contexts/study/CONTEXT.md) alongside the affected source.
- [Deferred improvements](./docs/DEFERRED-IMPROVEMENTS.md): concrete advanced suggestions saved for after the demo; not approved implementation scope.

## On-demand skills

Use a relevant skill when helpful; ordinary requests in chat are sufficient.

- [start](./.agents/skills/start/SKILL.md): understand the current project and next step.
- [new-feature](./.agents/skills/new-feature/SKILL.md): implement requested behavior.
- [review-pr](./.agents/skills/review-pr/SKILL.md): review code or changes.
- [write-tests](./.agents/skills/write-tests/SKILL.md): check meaningful behavior.
- [write-context](./.agents/skills/write-context/SKILL.md): maintain module context.
- [update-status](./.agents/skills/update-status/SKILL.md): record actual progress.

<!-- postman-cli:installation:start -->
Postman CLI is installed on this device, and can be used for API Engineering work.
<!-- postman-cli:installation:end -->
