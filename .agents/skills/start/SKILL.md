---
name: start
description: "Orient a development session by reading project guidance, progress, and source evidence. Use when the user asks to start or resume work, understand repository context, or select the next task."
---

# Start a Work Session

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Start a project work session by understanding the current task and evidence.

## Read

- Read `AGENTS.md`, `docs/PROJECT-OVERVIEW.md`, and `docs/PROJECT-STATUS.md`.
- Read PROJECT-RULES.md section 17 for the shared decision/documentation workflow; read the remaining relevant conventions before modifying code.
- Read only the architecture, API, database, ADR, and module context sections affected by the task.
- Inspect relevant source/configuration and available check results. Distinguish inherited template code, draft targets, and implemented behavior.

## Report Briefly

1. What is documented, implemented, and actually verified for this task.
2. Relevant differences, open decisions, or known environment constraints.
3. The next small, useful step and any assumption it relies on.

## Continue Within Scope

- If the user already requested implementation or a documentation change, proceed within that scope; do not ask for the same authorization again.
- If this skill is only a session orientation with no task, provide the status/next-step suggestion and wait for the user to choose work.
- Ask focused questions for new material requirements or design conflicts, not every class or routine detail. Continue independent work while that decision is pending.
- Once agreement changes a requirement, update affected documentation automatically and label unbuilt behavior Planned. Do not require the whole project design to be finalized before starting a small feature.
