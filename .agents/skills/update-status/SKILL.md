---
name: update-status
description: "Update docs/PROJECT-STATUS.md from actual session work, decisions, and check evidence. Use when recording relevant completed work or when a progress-file update is requested, not for a brief conversational status question."
---

# Update Project Status

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Update docs/PROJECT-STATUS.md from the actual work and evidence in this session. Apply PROJECT-RULES.md section 17.

1. Read the current status and relevant changed docs/source/check output before editing.
2. Distinguish Draft, Agreed/Planned, Implemented, Verified, Deferred, and inherited template code. Documentation/scaffolding completion is not feature completion.
3. Record what actually changed with dates. Keep partially done work in In Progress; label agreed unbuilt work Planned.
4. Record meaningful open decisions and discovered issues with practical priority. Draft defaults remain provisional until the affected feature is reviewed.
5. Update relevant warnings and Next Tasks; remove resolved warnings and obsolete tasks rather than leaving contradictory progress.
6. Check milestones only when their stated scope and verification are satisfied. Never infer passing tests from files, stale reports, or earlier unsupported claims.
7. State checks actually run and results; explicitly identify checks not run and any known environment limitations.
8. When agreement changes requirements, automatically update affected API/database/architecture/ADR/context sections as appropriate; changing status alone does not synchronize a contract.
9. Update date/author and increment the session number once per work session, not on every minor edit in the same session.
10. Keep under 80 lines; consolidate or archive old completed entries with a usable reference when necessary. Preserve important decisions in their owning documents.
11. Continue authorized work without asking permission for each documentation file. Ask only focused material decisions that remain unresolved; updating status does not authorize new implementation.
12. Finish with a brief report of updated documents, actual checks, and the next unresolved task.
