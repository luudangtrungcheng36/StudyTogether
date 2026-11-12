# ADR-003: Finish the SQL draft before activating migrations

## Status

**Accepted / Draft-SQL phase — clarified 2026-10-31.** SQL owns schema, with mapping-only entities. Flyway is disabled, application Hibernate uses none, and automatic SQL initialization is off until the user explicitly finalizes the main SQL. V1/validate activation is Planned. Earlier isolated migration checks are historical evidence, not approval of a baseline; no existing database was changed.

## Context

The initial database convention required versioned migrations for schema changes. While learning and building the initial foundation, the user prefers to refine the current schema without keeping a migration for every intermediate step. Future features are not all known yet.

## Current Decision — 2026-10-31

- Refine docs/sql/schema.sql as the main draft; it is not a finalized schema or versioned migration. Keep one maintained SQL source.
- During this phase, set Flyway enabled:false, Hibernate ddl-auto:none, and SQL init mode:never. Do not automatically run SQL, apply migrations, or validate an unfinished schema at application startup.
- Keep ORM mapping cleanup and SQL ownership. Do not restore Hibernate DDL annotations or schema generation/update.
- Only after the user explicitly finalizes the main SQL, create V1, enable migrations, and switch Hibernate to validate. A passing test does not constitute finalization.
- After that checkpoint, preserve applied/shared migrations and add V2/V3 changes. Existing non-empty databases still require an explicit adoption plan without data loss.
- CHECK constraints remain deferred. SqlSchemaTest may apply draft SQL and validate ORM behavior on fresh isolated PostgreSQL only; it does not activate application migrations.

## Superseded immediate-activation interpretation — 2026-10-31

- Use hand-authored versioned SQL migrations under `src/main/resources/db/migration` as the schema authority from now on. V1 captures the existing five-table core; no new product features are implied.
- Flyway applies migrations at startup, before JPA initialization. Hibernate uses `ddl-auto: validate`; automatic update/create/create-drop and separate Spring schema.sql initialization are disabled.
- Entities retain table/column/join names, relationships, enum encoding, Java defaults, ID generation, and ORM audit timestamps. SQL defines database types/lengths, NOT NULL, defaults, indexes, uniqueness, and named FK actions. Do not duplicate those DDL declarations in entities.
- Keep the user's CHECK deferral. SQL migrations contain no CHECK clauses, and validate-only Hibernate does not create inferred enum checks.
- Once V1 has been applied/shared, leave it unchanged and add V2/V3 migrations for subsequent schema changes. Database creation is still an external bootstrap step.
- Do not auto-baseline a non-empty database. Inspect and reconcile its actual schema/data with V1 before an explicit adoption decision. Do not drop retained databases or run V1 blindly over existing tables.
- Database-mutating tests apply the actual migrations to fresh isolated PostgreSQL, then validate ORM mappings and database behavior. The integration test uses explicit TEST_DB_* settings and never the application's default datasource.

## Historical consequences of the activation attempt

The schema has one maintained SQL source, including PostgreSQL partial unique indexes. JPA remains the ORM; choosing SQL schema ownership does not require replacing repositories or ORM value generation. Hibernate validation catches mapping/type mismatches but does not prove every index, default, or constraint, so migration tests also exercise SQL behavior.

The agent initially moved docs/sql/schema.sql into V1 and verified that candidate on isolated storage. The user then clarified that the main SQL was unfinished, so the SQL was moved back to docs/sql/schema.sql, migration activation was withdrawn, and application Hibernate was set to none. The earlier checks remain historical evidence only; existing-database adoption and full application startup remain unverified.

## Superseded Decision — 2026-10-24

- Add tables and fields only when the selected feature needs them; the proposed database design does not require creating every table now.
- During initial scaffolding, allow Hibernate `ddl-auto: update` on a disposable local development database and disable Flyway in that development profile. Resetting this database is allowed only when it has no data that needs preserving; this decision does not authorize deleting any existing database.
- Once the user agrees that the initial foundation is stable, capture the current schema in `V1__initial_schema.sql` and verify that it creates a fresh PostgreSQL database correctly.
- After that checkpoint, use new versioned migrations for schema changes and Hibernate validation rather than automatic schema updates. Keep applied/shared migrations unchanged.
- A separate automated-test database is deferred while development is manual and local data is disposable. Before database-mutating automated tests are introduced, isolate their storage from development and retained data.

## Superseded Consequences

Early schema changes remain visible in Git through entity/configuration/documentation changes, without a migration for each iteration. Automatic schema updates do not reliably handle every rename, removal, or PostgreSQL-specific constraint; inspect the resulting schema and use explicit SQL where needed.

The baseline checkpoint need not wait for all future product features. If existing data must be preserved at that point, agree on its transition separately before applying the baseline.

## History

- **2026-10-24:** User agreed to build the initial foundation first and track subsequent schema changes after a consolidated baseline, revising the earlier migration-from-the-start convention.
- **2026-10-24:** User deferred a separate test database during initial manual development and requested SQL for manual database setup; preparation of that script does not establish the final V1 checkpoint.
- **2026-10-31:** User explicitly requested SQL/migration schema ownership and removal of entity DDL metadata. This supersedes the delayed V1 checkpoint and permission to use Hibernate updates. Added V1/Flyway configuration, mapping-only entities, and isolated PostgreSQL migration validation; no retained database was changed.
- **2026-10-31, subsequent clarification:** the user did not authorize treating the SQL as finished. Corrected the agent's immediate-activation interpretation: restored the draft SQL phase with Flyway off/Hibernate none, retaining SQL ownership and mapping-only entities. Explicit finalization is required before V1.

References: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Flyway versioned migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html), [Flyway baseline-on-migrate](https://documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html).
