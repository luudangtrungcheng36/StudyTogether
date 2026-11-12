# Study — Context

> Updated: 2026-11-11 | By: Antigravity | State: Implemented selective visual study segments (camera_on OR screen_share_on); verified on isolated PostgreSQL
> Verification: targeted Maven package passed 2026-11-11 (17 tests, no skips/JAR) on isolated PostgreSQL: 12 study/listener cases, 3 identity cases, draft SQL and metadata checks. Verified voice-only sessions create 0 segments, join/media on/off/move/leave persist selective visual intervals, bot exclusion, room filtering and rollback. Reproduce with scripts/Test-DiscordStudy.ps1. SQL remains Draft; no default-database or live Discord startup.

## Business Context

Study rooms identify tracked Discord channels. Sessions record presence in one room; segments record intervals with stable camera/screen-sharing state. DiscordVoiceListener now delegates normal voice/media transitions to StudySessionService. Duration reporting and recovery remain planned; live Discord connectivity is unverified.

Confirmed scope, 2026-10-30 (Planned): support timed study directly on the website as well as Discord study. People already tracked through Discord log into the website with the same Discord account, reuse their existing internal user/history, and need no separate linking step. People using another website login method with no Discord identity explicitly link Discord to that account to share history across sources. Website study itself does not require Discord. Login method does not determine study source; both sources' history belongs to the same internal user. Current mappings permit only DISCORD; WEB persistence, timing, and source-coordination contracts remain draft and unimplemented.

Confirmed 2026-10-31: one active study session per internal user across linked identities/sources/devices; keep the existing session and block another start. The Discord service now checks for an open session by user, without a source filter, before starting. WEB and concurrent cross-source enforcement remain unimplemented; the SQL unique index is still Discord-only. Blocking means no second credited session, not preventing login or Discord voice entry. Notice copy and delivery remain draft/TBD.

## Current Behavior and Dependencies

- StudyRoom, StudySession, and StudySessionSegment now live in entity/ and map tables described by docs/sql/schema.sql, which remains Draft. Relations are lazy without ORM cascade/reverse collections; database FK delete behavior belongs in SQL.
- BaseEntity remains in persistence/ and supplies UUIDs and Hibernate-managed Instant audit timestamps, with ordinary public Lombok accessors/constructors; StudySource lives in enums/, permits only DISCORD, and is stored as a string. Moving its package does not change stored values.
- StudySession maps simple user/identity/room joins. Draft SQL alone defines FKs and ON DELETE RESTRICT. StudySessionService derives the session's owner from the resolved Discord identity; there is no composite ownership FK.
- No-args constructors leave source and timestamps unset. StudySessionService explicitly assigns DISCORD and startedAt/lastObservedAt, then saves an initial segment in the same transaction only when camera or screen sharing is active. There is no periodic checkpoint, disconnect recovery, or room-disable reconciliation.
- Keep last_observed_at, end_reason, and needs_review unchanged as requested. endReason remains a string because the final reason codes are not settled.
- Draft SQL contains no CHECK constraints. Normal service transitions preserve time boundaries, end reasons, and Discord references; broader historical/recovery validation remains deferred. Application Hibernate none creates no DDL; no schema validation runs automatically at startup.
- Draft SQL defines intended room/segment uniqueness, normal indexes, and both partial unique indexes. Entities contain no DDL index/unique/FK options, SQL type definitions, or column length/nullability declarations. SQL finalization and migration activation remain pending.
- All three entities use plain NoArgsConstructor, AllArgsConstructor, Getter, and Setter, without access exclusions or handwritten constructors/null guards. All-args construction assigns declared fields; it does not derive ownership/checkpoints. No entity-wide equality or relation-based toString is generated.
- StudyRoomRepository finds enabled rooms; StudySessionRepository finds open sessions by user or exact Discord identity/guild/channel; StudySessionSegmentRepository finds the open segment. Their custom lookups use native SQL; writes use inherited JPA operations and managed-entity dirty checking.
- StudySessionService is an interface in service/; StudySessionServiceImpl in service/impl/ implements it with Service, RequiredArgsConstructor, and two Transactional entry points. Consumers inject the interface. handleDiscordVoiceUpdate accepts DiscordVoiceUpdateCommand from dto/study with nullable left/joined channels, flags, and backend observation time. It closes the previous matching session and any open segment with LEFT_ROOM or MOVED_ROOM, then starts an enabled destination in the same transaction. A new segment is created on start only if camera or screen sharing is active. Optional contains only a newly started session; empty also covers leave-only, ignored, or blocked starts, not a notification contract.
- handleDiscordMediaUpdate accepts DiscordMediaUpdateCommand with both current flags, ignores unmatched rooms and older observations, and creates or splits segments only when visual media is active (`camera_on OR screen_share_on`). When both flags become false, the open segment is closed without reopening. Equal segment-start boundaries update flags in place or close. Event times are truncated to PostgreSQL microsecond precision. Flushes release partial unique keys before replacement inserts.
- DiscordVoiceListener supplies JDA identity/guild/channel IDs and Instant.now(), excludes bots in voice/video/stream callbacks, and calls the service interface. Video events use their new camera flag plus the cached stream flag; stream events use the cached camera flag plus their new stream flag. Media callbacks with no current channel return without calling the service. Voice processing logs at INFO after the service returns; media details log at DEBUG. This service is not a public authentication boundary. Rooms must already exist in SQL; no automatic registration or room-management API was added. Setup and result queries are in [Discord study demo](../../DISCORD-STUDY-DEMO.md).

## Decisions and Alternatives

- Define partial uniqueness directly in versioned SQL; do not replace it with global user/session uniqueness, which would block completed history. Hibernate-generated DDL is no longer used.
- Follow clarified ADR-003: keep Flyway off/Hibernate none while refining the main SQL. Create/enable V1 and validate only after explicit user finalization. Service tests use isolated draft SQL; no test finalizes it.
- Keep segment orchestration inside StudySessionServiceImpl so session and segment writes share one transaction; no separate segment service is needed. The user subsequently standardized Service/ServiceImpl for all business services, superseding the initial concrete-service choice.
- Optional defensive access/null-check mechanisms are deferred under the user's 2026-10-30 clarification. On 2026-10-31, the user replaced composite database ownership enforcement with service responsibility, then deferred all remaining handwritten CHECK constraints. Simple references, required columns, unique constraints, and indexes remain.

## Known Limitations and Open Questions

- Live Discord startup/persistence is unverified. Per-user serialization, complete replay/reorder handling, historical overlap validation, reporting/ranking, and recovery are not implemented. Ordinary transactions do not serialize concurrent callbacks; advanced work is recorded in DEFERRED-IMPROVEMENTS.md.
- A checkpoint is a persisted boundary based on the event-derived state, not a fresh per-user Discord acknowledgement. Its schedule and recovery policy remain unimplemented.
- Basic stale-time and exact-room checks protect the currently matched session; they do not establish global event ordering or prevent replay after an old session has closed. Conflict notifications are not implemented.

## Refactor Log

### 2026-11-11 | Antigravity

- At the user's request, updated StudySessionServiceImpl and study_session_segments to selective visual persistence: only intervals with active visual media (camera_on OR screen_share_on = true) create segment records. Voice-only intervals (both off) do not create segment rows; total study time continues to be measured directly by study_sessions.
- In StudySessionServiceImpl, startSession creates a segment only if camera or screen sharing is active; handleDiscordMediaUpdate creates a segment when visual media turns on, closes without reopening when visual media turns off, and updates in place when multiple updates share the same timestamp. closeSession safely closes an open segment if present.
- Updated StudySessionServiceIntegrationTest (including new voice-only test) and verified with scripts/Test-DiscordStudy.ps1: 17 tests passed (no skips/JAR) on isolated PostgreSQL.

### 2026-11-02 | Codex

- At the user's request, replaced scalar service arguments with DiscordVoiceUpdateCommand and DiscordMediaUpdateCommand records and connected the existing JDA listener to the service interface. Kept session rules, reason codes, schema and application initialization settings unchanged; bot filtering now covers camera/stream as well as voice updates.
- Added isolated listener-to-PostgreSQL acceptance cases for join/media on/off/move/leave, complete segment coverage, bot exclusion, untracked rooms and disconnected media. Added scripts/Test-DiscordStudy.ps1 for disposable PostgreSQL checks and docs/DISCORD-STUDY-DEMO.md for live setup/result queries. Check evidence is recorded above after execution; no live token/DB credentials were available in the agent process.

### 2026-10-24 | Codex

- Added the three initial study mappings and StudySource. Kept the existing recovery-related fields and manual partial indexes; offline mapping test passes. No database or Discord connection was opened.

### 2026-10-30 | Codex

- Applied Lombok to mechanical accessors and empty JPA constructors in the three entities and shared BaseEntity. Preserved SQL mapping, recovery fields, explicit validation, and public/protected signatures; the focused mapping test and packaging passed without database/Discord connections.
- After the user's style clarification, moved setters to class level and explicitly excluded immutable keys/start boundaries; field mappings and supported mutations remain unchanged.
- Subsequently removed optional protected access, setter exclusions, and custom constructors/null-check setters at the user's request. Public Lombok constructors/accessors replace the earlier restricted APIs; future services will initialize owner/checkpoint values. The SQL schema and field defaults are unchanged.
- Recorded independent WEB/DISCORD study and common user ownership as Planned scope. No source/check constraints, session behavior, or schema changed for this clarification.
- Refined the earlier "Discord study after linking" wording: Discord-origin users already have the identity and reuse it through Discord login on the website. A separate link is only needed for another-method account without Discord. No timing/concurrency or implementation behavior changed.

### 2026-10-31 | Codex

- Service consistency correction: extracted StudySessionService as the public interface and moved existing logic unchanged into service/impl/StudySessionServiceImpl with Override and the existing service/transaction annotations. Integration tests register the Impl and call through the interface. This supersedes the initial concrete-service choice below and aligns with the user-confirmed project-wide convention.

- Added StudyRoomRepository, StudySessionRepository, StudySessionSegmentRepository, and concrete StudySessionService at the user's request. Implemented native lookups and transactional join/leave/move/media changes with explicit source/ownership, first-session priority, and continuous segments. Listener/SQL configuration unchanged; advanced concurrency and recovery remain deferred. Added isolated PostgreSQL acceptance checks; results recorded above after execution.

- Subsequent user clarification: the main SQL is unfinished. Returned it to docs/sql/schema.sql, disabled Flyway and application schema validation, and retained mapping-only entities. SqlSchemaTest now applies draft SQL only when explicitly configured for isolated storage; current clean build passed with that integration test skipped. Earlier V1 verification below did not approve a baseline.
- SQL migration revision: removed entity index/unique/FK-DDL/type/length/nullability declarations and moved all schema structures into Flyway V1. Verified migration, validate-only ORM round trips, FK restriction, partial active uniqueness, and retained completed history on isolated PostgreSQL. Prior manual/generated-DDL evidence below describes earlier states; no retained database changed.
- Moved StudySource into the feature-owned enums package and updated StudySession's import. Enum constants, field defaults, and ORM mappings are unchanged. Offline mapping test, clean packaging, and JAR package inspection passed.
- Subsequently replaced the composite ownership FK with the agreed simple identity FK and identity_id index in entities/manual SQL. Updated mapping verification checks this contract while retaining provider uniqueness and completed-history support. Earlier composite-ownership verification describes the superseded mapping; no live database was changed.
- Subsequently removed the five session checks and the segment-time check at the user's request. Kept intended rules as Planned validation in DATABASE.md; retained FKs/uniqueness/indexes and updated offline mapping tests. No service validation, custom enum mapping, or live database update was added.
- Subsequently moved study entities to entity and StudySource to enums under the user's layer-based layout. Moved this context to docs/contexts/study and updated test imports; clean compilation and 6 tests/JAR packaging passed on isolated PostgreSQL. ORM/SQL values and study behavior are unchanged; the former feature-owned enum package is superseded.
- Removed the implicit DISCORD study source at the user's request, both in StudySession and draft SQL. Session creation must explicitly assign source; the SQL column remains NOT NULL and StudySource still only contains DISCORD. Updated the existing SQL test fixture and omission check; no existing database was changed or WEB flow implemented.
- Confirmed one active session per user across identities/sources, with the first session retaining priority. Recorded it as Planned in API/architecture/database/context, with draft conflict messaging and unresolved Discord notification delivery. No session blocking, notification, schema, or application behavior was implemented by this discussion.
