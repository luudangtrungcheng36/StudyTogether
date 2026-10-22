---
name: write-context
description: "Create or update a module CONTEXT.md with source-based behavior, decisions, limitations, and dated change history. Use for module implementation context, not general session status."
---

# Maintain Module Context

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Create or update CONTEXT.md for the requested module using source evidence and PROJECT-RULES.md section 17.

## Purpose

Describe how the module currently works and why, while preserving a dated history of meaningful decisions. A module context is an evolving current description with a history, not a permanently frozen snapshot.

- Read code, relevant tests/results, existing context, and affected agreed/draft designs first.
- For an unbuilt feature, label the description Planned/Draft; do not describe proposed classes or tests as present.
- If a changed description would settle a new material requirement, discuss that decision before representing it as agreed. Correct factual descriptions within the authorized documentation task without redundant approvals.

## Location and Template

Use `src/main/java/<base-package>/feature/<module>/CONTEXT.md` for an existing module. Do not create empty modules solely to hold this file.

```markdown
# {Module} — Context

> Updated: {date} | By: {author} | State: {Draft / Planned / Implemented / Template}
> Verification: {actual checks/results, or not verified}

## Business Context
Current purpose and scope; distinguish proposed additions.

## Current Behavior and Dependencies
Facts checked against source/configuration, including important boundaries.

## Decisions and Alternatives
Current decision, reason, status, and relevant ADR references.

## Known Limitations and Open Questions
Intentional tradeoffs, unfinished work, and unresolved expectations.

## Refactor Log
### {date} | {author}
- Previous decision/behavior, agreed change, reason, and validation.
```

## Maintenance Rules

- Write concise English module context; explain decisions to the user in plain Vietnamese.
- Update current sections when behavior changes or descriptions are inaccurate. Keep old decision reasons in dated log entries; do not leave obsolete current information in place.
- Preserve existing history, attribution, and significant decisions. Mark obsolete decisions superseded rather than silently erasing their rationale.
- Do not invent rejected alternatives, authors, checks, or implementation. Template-only context stays clearly labelled while the module remains template code.
- Update other affected documents using the section 17 mapping; context alone does not update an API/schema contract.
