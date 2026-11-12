# User — Context

> Updated: 2026-11-02 | By: Codex | State: Implemented entity mappings, repositories, and identity service interface/implementation; Discord listener now uses identity resolution through StudySessionService
> Verification: scripts/Test-DiscordStudy.ps1 passed 2026-11-02 (16 tests, no skips/JAR), including 3 identity tests and 11 study/listener cases on isolated PostgreSQL. Normal creation/reuse, profile preservation, owner access, rollback and identity creation through constructed JDA events passed. Cluster stopped; no default database/full application/live Discord startup. SQL remains Draft. Earlier concurrency/SQL verification below is historical.

## Business Context

An internal user owns study history independently of the Discord account. UserIdentityService finds or creates the Discord identity and its internal user before website login. DiscordVoiceListener now reaches this service through StudySessionService when starting tracked sessions; OAuth login remains planned. See the study context for session/segment behavior.

Confirmed scope, 2026-10-30 (Planned): users can log in through multiple methods, study directly on the website without Discord, and link Discord to an account created through another method. Linked history belongs to the same internal user. Provider selection and linking/merge details remain draft; the current DISCORD-only enum and manual SQL baseline do not implement this full scope.

Clarified account flows (Planned): someone already tracked through Discord signs into the website with the same Discord account and reuses the existing internal user/history, without a separate Discord-link operation. Explicit Discord linking is for a user signed in through another method whose current account has no Discord identity. Both flows support WEB study. This does not decide merging when the identity already belongs to another internal user.

## Current Behavior and Dependencies

- User and UserIdentity now live in entity/, repositories in repository/, UserIdentityService in service/, and UserIdentityServiceImpl in service/impl/. This context spans those layers. User maps users; UserIdentity maps user_identities, including a lazy, required user association. No cascade operations or reverse collections are configured.
- BaseEntity supplies Hibernate-generated UUIDs and managed Instant audit timestamps, with ordinary Lombok getters/setters and public no-args/all-args constructors.
- UserStatus and IdentityProvider live in enums/, with one enum per file. UserStatus persists ACTIVE/DISABLED as strings; IdentityProvider currently permits only DISCORD. Enum package changes do not change stored values.
- Draft docs/sql/schema.sql defines intended uniqueness, required references, types/defaults, and schema structures. Entities map names/types/relationships and runtime persistence without duplicating DDL. The SQL is not finalized or versioned.
- SQL CHECK constraints remain deferred. Enums remain unchanged. Flyway is disabled, application Hibernate uses none, and SQL initialization is off. Google/local authentication and service validation remain unimplemented; the default database was not inspected or altered.
- Draft SQL defines simple user/identity FKs without the additional (user_id, id) key; ownership consistency remains a future service responsibility.
- Entities use class-level NoArgsConstructor, AllArgsConstructor, Getter, and Setter. No setter exclusions, NonNull annotations, or handwritten null-check constructors/setters remain. UserRepository extends JpaRepository<User, UUID> with inherited operations only; no custom queries or locking yet. UserIdentityRepository returns Optional<UserIdentity> from provider/subject lookup and fetches its owner via EntityGraph so the returned user is usable after the transaction ends. Controllers, login flows, and authorization remain unimplemented.
- UserIdentityService is an interface with UserIdentityServiceImpl as its @Service implementation. The user subsequently standardized this Service/ServiceImpl structure for all business services on 2026-10-31; it is no longer an identity-only exception. findOrCreateDiscordIdentity accepts a Discord user ID, reuses an existing identity or creates an ACTIVE user and DISCORD identity atomically. It does not set login timestamps or overwrite existing profile/authentication data.
- Identity resolution uses method-level Transactional with default REQUIRED propagation and Lombok RequiredArgsConstructor for the two repositories. It creates both rows atomically and joins a caller transaction when present, so a caller rollback also rolls back new account creation. Persistence failures propagate; there is no automatic concurrent-winner reread or independent commit.
- No-args construction retains ACTIVE/DISCORD field defaults; all-args constructors assign every declared field and exclude inherited BaseEntity fields. Primary keys, required columns, user references, and provider/subject uniqueness remain.

## Decisions and Alternatives

- Keep profile fields and verified-login timestamps nullable, as documented in docs/DATABASE.md.
- Audit timestamps are managed by Hibernate for ORM writes; manual SQL must maintain updated_at itself.
- Follow clarified ADR-003: finish the main SQL before creating/enabling V1. Current configuration is Flyway off/Hibernate none; later migration/validate activation requires the user's explicit finalization. SqlSchemaTest can validate an isolated draft without approving the baseline.
- Per the user's 2026-10-31 demo-first direction, ordinary Transactional handles normal creation/reuse/rollback. TransactionTemplate, REQUIRES_NEW, and concurrent-creation recovery are deferred in docs/DEFERRED-IMPROVEMENTS.md; required SQL uniqueness remains. This supersedes the earlier independent-transaction choice recorded below.
- The user's 2026-10-30 simplification supersedes earlier optional access/null-check defenses. Introduce extra mechanisms only for an actual feature requirement or observed issue, with a concrete explanation.

## Known Limitations and Open Questions

- Linking, profile synchronization, input validation, and disabled-account eligibility remain future feature work. Finding an identity does not authenticate its owner or grant study eligibility. The bot listener now reaches this service through StudySessionService; live Discord acceptance is unverified.
- No automatic persistence cascade or API serialization contract is provided by these entities.
- Concurrent first creation may fail for one caller instead of automatically returning the winner. Save advanced proposals in docs/DEFERRED-IMPROVEMENTS.md and revisit after the demo when selected by the user.

## Refactor Log

### 2026-11-02 | Codex

- Connected the existing Discord listener to StudySessionService, which already resolves identity through UserIdentityService. Identity implementation, schema and login timestamps are unchanged. The isolated suite passed 16 tests/JAR, including listener-driven account creation, bot exclusion and rollback; no live Discord credentials were available in the agent process.

### 2026-10-24 | Codex

- Added the initial User/UserIdentity mappings and supporting enums against the manually prepared SQL; offline metadata bootstrapping passes. No database or Discord connection was opened.

### 2026-10-30 | Codex

- Replaced handwritten accessors and empty JPA constructors with Lombok under the revised project conventions. Kept constructor defaults, null checks, ORM mappings, and the public/protected API unchanged; focused mapping test and packaging passed.
- After the user's style clarification, moved setters to class level with explicit exclusions for identity/ownership keys; the existing validating setStatus method remains authoritative.
- Replaced UserIdentity's assignment/null-check constructor with RequiredArgsConstructor and NonNull fields, preserving its two-argument signature and DISCORD default. Other custom construction paths retain their selected arguments/defaults and derived data.
- Subsequently, at the user's explicit request, removed protected access, setter exclusions, NonNull/RequiredArgsConstructor, and handwritten constructors/setStatus. Both entities now use public no-args/all-args constructors and general setters; the earlier API-preserving restriction is superseded. Database mappings are unchanged.
- Recorded the user's confirmed multi-login/web-study/Discord-linking scope as Planned. Existing candidate contracts remain draft; no application code or schema was changed for this clarification.
- Clarified that Discord login reuses Discord-origin accounts/history without a separate link; explicit Discord linking serves accounts using another method with no Discord identity. This refines the earlier scope wording; code/schema and the unresolved distinct-account merge policy are unchanged.

### 2026-10-31 | Codex

- Subsequent user clarification: withdrew premature V1 activation, returned SQL to docs/sql/schema.sql as Draft, and set application Flyway off/Hibernate none. Mapping-only entities remain; the main SQL must be finalized before migration activation. Renamed the integration test to SqlSchemaTest for manual draft application; current clean build passed with that test skipped. Earlier migration evidence below is historical.
- SQL migration revision: moved schema authority to Flyway V1 and removed entity unique/FK-DDL/type/length/nullability declarations while retaining mapping and runtime ID/audit behavior. Verified migration, validate-only ORM round trips, and SQL constraints on isolated PostgreSQL. Prior manual/generated-DDL evidence below describes earlier states; no retained database changed.
- Moved UserStatus and IdentityProvider into the feature-owned enums package and updated entity imports. Names, enum constants, field defaults, and ORM mappings are unchanged. Offline mapping test, clean packaging, and JAR package inspection passed.
- Subsequently applied the user's agreed constraint simplification: removed unique (user_id, id), synchronized removal of the named provider check in manual SQL, and retained provider/subject uniqueness. Ownership pairing moved to service responsibilities; no service or live schema update was added. Updated offline mapping/manual-SQL verification passed.
- Subsequently deferred all remaining handwritten CHECK constraints at the user's request, including User's status check. Updated rules and mapping tests record the distinction between documented validation, handwritten constraints, and framework-inferred enum checks. Offline test/packaging passed; no live schema update was performed.
- Added UserRepository alongside User, extending JpaRepository<User, UUID> without custom queries. mvn -B -DskipTests compile passed; no database/bot startup or repository persistence test. UserIdentityRepository is the next planned file; automatic user creation and study tracking remain unimplemented.
- Added UserIdentityRepository alongside UserIdentity with an Optional lookup by IdentityProvider and providerSubject. mvn -B -DskipTests compile passed; no Spring/database/bot startup or query execution. UserIdentityService is next planned; automatic user creation and study tracking remain unimplemented.
- Per user direction, added UserIdentityService/UserIdentityServiceImpl and owner fetching in UserIdentityRepository. Implemented atomic find/create and rollback/reread for duplicate creation. Four service integration tests plus EntityMappingTest/SqlSchemaTest passed on temporary PostgreSQL (6 total, no skips), including two synchronized insert attempts and rollback of unrelated failures. Cluster stopped; application settings/SQL draft unchanged and listener integration remains planned.
- Subsequently moved user entities to entity, repositories to repository, service contract to service, implementation to service.impl, and enums to enums at the user's request. Moved this context from the former feature/user package to docs/contexts/user. Updated test package/imports and entity scanning; clean compilation and 6 tests/JAR packaging passed on isolated PostgreSQL. Behavior/schema values are unchanged; this supersedes the earlier feature-owned enum layout.
- Demo-first revision: user explicitly deferred advanced handling until after a working demo. Replaced TransactionTemplate/REQUIRES_NEW and rollback/reread with ordinary method-level Transactional and RequiredArgsConstructor. Removed the concurrent-winner test because that behavior is deferred; retained creation/reuse/rollback tests. Four targeted tests and packaging passed on isolated PostgreSQL; no SQL/configuration change. Unlike the earlier implementation, account creation now participates in a caller transaction and its rollback.
