---
name: write-tests
description: "Create or adjust meaningful tests for a requested feature, regression, or untested behavior, using the project contracts and an isolated test environment."
---

# Write Tests

Repository paths in these instructions are relative to the project root containing `AGENTS.md`, not to this skill folder. Read the shared working instructions there before applying this workflow.

Write or adjust tests for the requested feature after reading source and the intended behavior. Apply the implementation/decision workflow in PROJECT-RULES.md section 17.

## Select Behavior

- Read relevant code, existing tests, module context, API/database sections, and ADR status.
- Separate agreed behavior from draft choices and inherited template behavior. Do not make a bug the expected result just because the source currently does it.
- Resolve material ambiguous expectations with a focused recommendation; do not require the entire design to be finalized or re-approve already agreed requirements.
- Cover meaningful success, failure, boundary, and regression cases for the change. Avoid tests that merely repeat implementation or check getters/setters and trivial records.
- Match the agreed demo scope: verify normal outcomes and required rollback/constraints. Do not introduce concurrency recovery or other deferred production behavior just to satisfy a speculative test; record advanced suggestions in docs/DEFERRED-IMPROVEMENTS.md for after the demo.
- Choose assertions about observable results. Verify exact repository call counts only when that interaction is part of the behavior, not to mirror the implementation.

## Choose the Appropriate Layer

- Unit tests: isolate business logic using mocks only for its direct external dependencies; no live database/provider calls.
- MVC/security tests: check actual request contracts, validation, ownership, status/envelope, and relevant cookie/CSRF behavior. Use real authentication/lifecycle tests where mocked JWTs would bypass the behavior under test.
- Database integration tests: use isolated PostgreSQL with required migrations and constraints; never development/production storage. H2 is not a substitute for PostgreSQL-specific behavior.
- During the draft phase, SqlSchemaTest applies docs/sql/schema.sql to fresh disposable PostgreSQL and explicitly validates ORM mappings there; it requires TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD and is skipped without TEST_DB_URL. This test does not finalize the SQL or activate migrations. After the user finalizes V1, switch integration setup to actual migrations. Never generate test schemas from entities. EntityMappingTest only checks metadata bootstrap without JDBC/DDL.
- Disable real Discord bot startup and mock provider/network boundaries. Use the project's actual framework dependencies/imports rather than copying an outdated test template.
- Do not run the current full database suite until its configuration is verified to target isolated storage. Test cleanup must not touch retained data.

## Feature-Specific Examples — When Applicable

- Identity: normal creation/reuse and atomic rollback now; concurrent bot/login recovery is deferred until selected. Verified linking, ownership conflict, and no automatic email/name merge apply when those features are implemented.
- Study: repeated/late events, one credited source, simultaneous starts, room moves, segment coverage, camera/share overlap, and heartbeat/recovery boundaries.
- Reporting: completed versus ongoing/review eligibility, lifetime totals, clipped periods, aggregate rounding, and ties across pages.
- Authentication: CSRF/origin checks, rotation/replay, absolute expiry, logout/refresh races, and independence of different login lifecycles.
- These are examples selected for the current scope, not a requirement to implement unbuilt features while testing another one.

## Run and Report

- Run the relevant checks when the isolated environment is ready. Broaden testing when failures or affected behavior justify it.
- Record exact commands, results, and environment limitations; label unrun/blocked checks clearly.
- Do not claim passing tests from source inspection, old reports, compiled files, or hypothetical outcomes.
- Update affected current context and PROJECT-STATUS.md with actual results. If testing reveals a requirement/design change, discuss it and update the mapped documents after agreement.
