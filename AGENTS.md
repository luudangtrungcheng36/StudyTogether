# Study Together — AI Working Instructions

Shared guidance for Codex and Antigravity. Read [PROJECT-OVERVIEW.md](./docs/PROJECT-OVERVIEW.md), [PROJECT-STATUS.md](./docs/PROJECT-STATUS.md), and the relevant parts of [PROJECT-RULES.md](./docs/PROJECT-RULES.md) before work.

## Working together

- This is a learning project. Build one useful part at a time, explain consequential choices in plain Vietnamese, and keep the process lightweight.
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
- Module `CONTEXT.md`: read alongside source before modifying its behavior.

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
