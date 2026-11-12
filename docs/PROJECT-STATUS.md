# Project Status — Study Together

> Last updated: 2026-11-12 | By: Codex | Session: #16
> Stage: Discord listener now calls transactional study services through command records; selective visual segments verified. SQL remains Draft; Flyway off/Hibernate none until user finalization.
> Evidence: targeted Maven package passed 17 tests (12 study/listener, 3 identity, 1 SQL, 1 metadata; no skips/JAR) on isolated PostgreSQL, 2026-11-12. Live Discord and full application/default-database startup remain unverified.
> Keep this file under 80 lines. Mark a feature complete only after implementation and relevant verification.

## Completed

- **[2026-10-22]** Planning baseline and corrected source inventory documented in PROJECT-RULES.md, ARCHITECTURE.md, DATABASE.md, and this status; contracts remain revisable targets, not inherited Company/Permission/MySQL template claims.
- **[2026-10-22]** API_SPEC.md covers profile, lifetime dashboard, history, login/linking, web study, tracked rooms, and leaderboard drafts; extension defaults are cross-referenced in architecture/database addenda.
- **[2026-10-22]** ADR-001 records the Proposed browser-only rotating-refresh design; ADR-002 preserves the Deferred historical upload proposal. Neither establishes implementation.
- **[2026-10-22]** Shared AI guidance simplified into AGENTS.md and six on-demand skills with automatic affected-document updates.
- **[2026-10-22]** Database username/password configuration now references environment variables; the bot token already references DISCORD_BOT_TOKEN.
- **[2026-10-24]** Prepared manual SQL and five JPA entities/BaseEntity/enums/contexts; offline mapping bootstrap passed, SQL was not applied. Original ADR-003 deferred V1 and allowed Hibernate scaffolding; superseded by the 2026-10-31 SQL direction, with history retained in the ADR. Recovery remains unimplemented.
- **[2026-10-30]** Aligned guidance/skills for practical Java/Spring: defer new optional mechanisms, explain their purpose and consequences of omission in Vietnamese, and obtain agreement before adding them. Continue routine authorized work; decision history is in PROJECT-RULES.md.
- **[2026-10-30]** Per subsequent user direction, simplified all five entities and BaseEntity to public Lombok no-args/all-args constructors and general accessors. Removed optional access/null-check defenses and custom constructors; schema mappings/field defaults remain. History is in module contexts.
- **[2026-10-31]** At the user's request, replaced feature packages with central entity/repository/service/service.impl/enums and top-level discord. Updated imports/scanning/tests/guidance; contexts moved to docs/contexts/user and study. Earlier feature-owned enum layout is superseded; business behavior/SQL unchanged. Six tests/JAR packaging passed.
- **[2026-10-31]** Simplified identity constraints, then deferred all handwritten CHECK constraints in entities/manual SQL at the user's request. Unique constraints/simple FKs/indexes remain; intended validation is documented for future implementation. No live schema update applied; history is in DATABASE.md and module contexts.
- **[2026-10-31]** Kept entity DDL cleanup but withdrew premature V1 activation after the user clarified the main SQL is unfinished. Restored docs/sql/schema.sql as Draft, Flyway off/Hibernate none/SQL init never, and deferred migration activation until explicit finalization. ADR-003 preserves prior checks/history; no existing database changed.
- **[2026-10-31]** Applied demo-first direction: identity service uses Transactional/RequiredArgsConstructor for normal creation/reuse/rollback; concurrency recovery/test deferred. Four targeted tests/JAR passed on isolated PostgreSQL. Clarified learning workflow: Codex assesses current needs and proactively recommends one concrete investigation/alternative comparison after a runnable increment; guidance/backlog updated without further code changes.
- **[2026-11-02]** Added DiscordVoiceUpdateCommand/DiscordMediaUpdateCommand records; wired voice/video/stream listener callbacks to StudySessionService with bot exclusion.
- **[2026-11-11]** Switched study_session_segments to selective visual persistence (camera_on OR screen_share_on); voice-only creates 0 segments. 17 tests/JAR passed; reverified and corrected the demo guide's segment expectations on 2026-11-12.

## Current Implementation — Source Reviewed

- Java 25 / Spring Boot 4.1.1 / Maven skeleton with JPA, validation, Web MVC, PostgreSQL, Flyway, Lombok, and JDA 6.7.0 dependencies.
- DiscordBotConfig creates the JDA client with GUILD_VOICE_STATES and VOICE_STATE cache, registers the listener, and shuts down the client on bean destruction.
- DiscordVoiceListener injects StudySessionService, maps voice/media commands under dto/study, excludes bots in every callback and ignores media outside voice. Voice processing logs at INFO after service completion; media details use DEBUG.
- application.yaml points to local PostgreSQL study_community and requires DB_USERNAME, DB_PASSWORD, and DISCORD_BOT_TOKEN from the environment. On 2026-11-05, created an ignored local .env for the existing VS Code envFile configuration, with postgres as the username and blank password/token; fill credentials before F5. Full startup remains unverified.
- Five entities live under entity, repositories under repository, service contract under service, implementation under service.impl, and enums under enums; BaseEntity remains under persistence. Identity resolution uses ordinary Transactional with rollback and no automatic retry. StudySession.source has no entity/SQL default and must be assigned explicitly. Draft SQL owns schema; CHECKs remain deferred and there is no active V1.

## In Progress

- StudyRoomRepository, StudySessionRepository, StudySessionSegmentRepository and transactional StudySessionService/Impl now receive listener calls. Isolated persistence is verified; room setup and live join/leave/media are next. Login/full startup remain pending.
- **[Partially implemented, 2026-10-31]** Discord starts keep an existing user session, including across linked identities. Multiple login methods/WEB and concurrent cross-source enforcement remain Planned. Conflict messages/Discord delivery remain draft; identity linking remains as recorded in API/database/context.
- Listener-to-service-to-database persistence is verified with constructed JDA events and mocked member/voice state; no Gateway connection was opened. Live connectivity and actual Discord events remain unverified.

## Deferred Issues

- **[P1]** Main SQL is not final: keep Flyway disabled/Hibernate none until explicit finalization. Linking/merge, study recovery/event concurrency, and source coordination still need feature-level work; CHECK validation remains deferred.
- **[P2]** Proposals are saved in [DEFERRED-IMPROVEMENTS.md](./DEFERRED-IMPROVEMENTS.md); Codex recommends investigations after a runnable flow. Source priority is Planned (existing session wins); notifications/handoff, totals, heartbeat/expiry, room-disable closure, and ranking details remain draft.
- **[P3]** Uploads, achievements/rewards, habits, chat, games, and advanced aggregation remain outside the current API scope.

## Warnings

- application.yaml disables Flyway, sets Hibernate none, and SQL init never. There is no automatic schema creation/migration/validation at startup. SQL draft remains unfinished; existing database state is unverified.
- contextLoads still uses default database/Discord configuration; isolate those boundaries before running it. Current configuration performs no startup migrations, and the SQL integration test avoids Spring entirely.
- There is no implemented discord.enabled condition. Discord credentials are required whenever the normal Spring context starts.
- Only pre-registered, enabled study_rooms start sessions. Room administration, current-presence synchronization and recovery remain pending; rejoin after bot startup for the current demo. See DISCORD-STUDY-DEMO.md.
- Earlier local credentials were present in the initial commit; this session removes literals from current configuration without rewriting existing history.
- Main DDL lives in docs/sql/schema.sql as Draft; no V1 migration is packaged. create-database.sql is external bootstrap. Optional auth/hardening/query scripts and ERD images remain absent. ADR-001 is Proposed and ADR-002 Deferred/historical.
- Draft SQL partial uniqueness is checked on isolated PostgreSQL; this does not finalize it. Service tests verify normal segment coverage and atomic rollback; concurrent events, replay, periodic checkpoints, and recovery remain deferred.

## Next Tasks

1. **[P1] Foundation:** refine the main SQL and wait for explicit user finalization before creating/enabling V1 and Hibernate validate. Then plan existing-database adoption without data loss. Bot suppression/full startup remain separate work; never use Hibernate schema updates.
2. **[P1] Shared API/security:** implement the target response/errors and authentication infrastructure as needed by the selected API slice.
3. **[P1] Accounts/auth:** integrate the identity service with Discord/OAuth consumers; implement Discord OAuth2, profile, and the reviewed login/refresh/logout contract. Bot observations must not be treated as authenticated login.
4. **[P1] Discord study:** configure a fresh demo database, credentials/token and actual guild/channel IDs; verify live join/leave/media with the connected listener. Room administration, concurrency, checkpoints, and recovery are subsequent slices.
5. **[P2] Read APIs:** build lifetime dashboard, history, and leaderboard with duration/window/tie checks.
6. **[P2] Extensions/frontend:** review Google/local linking and WEB coordination when selected; build React views against implemented APIs.

## Milestones

- [x] Initial Study Together design/API drafts and source inventory; remaining alignment is separate.
- [ ] Phase 0: Main SQL finalized by user, V1/adoption activated, full startup and shared API/security foundation verified. SQL is currently Draft; earlier candidate tests do not complete this milestone.
- [ ] Phase 1: UUID users/identities, Discord login, profile, and login lifecycle implemented and tested.
- [ ] Phase 2: Managed Discord rooms, persisted tracking, segments, concurrency, and recovery implemented and tested.
- [ ] Phase 3: Dashboard, history, and leaderboard implemented and tested.
- [ ] Phase 4: Google/local login, linking/merge, WEB study, and cross-source safeguards tested.
- [ ] Phase 5: React integration, room administration, and end-to-end acceptance verified.

## Validation

- **[2026-11-12]** scripts/Test-DiscordStudy.ps1 passed again: 17 tests, no failures/errors/skips; JAR packaged. Includes voice-only session (0 segments), join/media on/off/move/leave with selective visual coverage, bot exclusion, room filtering and rollback/SQL checks. Reports in target/surefire-reports; temporary PostgreSQL stopped. No default DB or live Discord used.
- **[2026-10-31, historical]** SQL/source-default checks passed 2 tests/JAR; identity simplification passed 4 tests/JAR; Service/Impl split passed 12 tests/JAR (8 study, 3 identity, 1 metadata), including transaction rollback. Temporary clusters stopped; no default database changes or V1 activation. Decisions and older evidence remain in module contexts and ADR-003.
- **[2026-10-24]** Manual SQL checked without execution. Full app/default-database/Discord startup was not run.
- Future acceptance must cover ownership, token lifecycle, duplicate/reordered events, cross-source overlap, recovery, and time/ranking boundaries.
