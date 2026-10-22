---
name: review-pr
description: "Review requested code changes, files, or a pull request for concrete defects, requirement mismatches, and verification gaps. Use when a code review is requested; a review does not authorize unrelated fixes."
---

# Review Changes

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Review the requested changes using source evidence and the shared workflow in PROJECT-RULES.md section 17. If no Git diff is available, review the specified files and state the scope; do not invent a PR or revision.

## Establish the Intended Behavior

- Compare actual code/diff and available check results with the user's request or agreed prompt. State the reviewed scope and any missing context.
- Report findings in chat. If the user wants another tool to implement fixes, summarize the agreed corrections as a copyable prompt.
- Read relevant source, contracts, ADR status, module context, and check evidence.
- Distinguish Draft, Agreed/Planned, Implemented, Verified, and inherited template behavior.
- Report a material mismatch as document expectation, observed behavior, impact, recommended resolution, and an acceptance check.
- Do not call an optional draft choice or code-size target a blocker merely because code differs. Do flag actual defects, lost history, ownership failures, or violated agreed behavior.
- A review-only request does not authorize unrelated fixes. Within an already requested fix, do not add redundant confirmation steps.

## Review Applicable Areas

- Controllers/listeners delegate business work to services; dependencies use the agreed constructor/interface conventions.
- Standard JSON returns safe DTOs and the correct response envelope/status; legitimate authentication responses may contain intended access tokens.
- Validation, custom error mapping, security-filter errors, redirects, and background failures are handled at the right boundaries.
- Ownership, public/admin permissions, CSRF for cookie routes, token lifecycle/replay behavior, and secret handling follow the current agreed contract.
- Database constraints, migration strategy, transactional writes, query ordering/pagination, and fetch/query behavior fit the affected use case.
- Study changes preserve non-overlapping credited history, correct segments/visual union, period boundaries, idempotency, and recovery semantics where applicable.
- Tests exercise meaningful behavior on isolated storage; do not infer a pass from test-file existence.
- Current context/API/database/architecture/ADR/status descriptions match the agreed change; history explains significant revised decisions.
- Naming, logging, and code responsibilities follow current conventions. Size targets are prompts to review clarity, not mandatory failures.

## Output

1. Concrete findings, most consequential first, with file/line evidence where available.
2. Open decisions or assumptions that need user input, separately from confirmed defects.
3. Verification actually performed and its limits. If no defects were found, say so without claiming unrun tests passed.

Assess risks relevant to the change and distinguish observed defects from conditional concerns and optional improvements. Recheck affected behavior after fixes. Keep feedback actionable and proportional to the work; update affected documentation after agreed changes.

Explain consequential findings in plain Vietnamese; recommend the simplest adequate correction for the current learning stage.
