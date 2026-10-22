# Project Status — Study Together

> Last updated: 2026-10-22 | By: Codex | Session: #7
> Stage: planning baseline and initial Discord event logging; study persistence remains planned.
> Evidence: current source inspection and Maven packaging with tests skipped; no live runtime verified.
> Keep this file under 80 lines. Mark a feature complete only after implementation and relevant verification.

## Completed

- **[2026-10-22]** Planning baseline documented in PROJECT-RULES.md, ARCHITECTURE.md, and DATABASE.md; contracts remain revisable design targets.
- **[2026-10-22]** API_SPEC.md covers profile, lifetime dashboard, history, login/linking, web study, tracked rooms, and leaderboard drafts; extension defaults are cross-referenced in architecture/database addenda.
- **[2026-10-22]** ADR-001 records the Proposed browser-only rotating-refresh design; ADR-002 preserves the Deferred historical upload proposal. Neither establishes implementation.
- **[2026-10-22]** Shared AI guidance simplified into AGENTS.md and six on-demand skills with automatic affected-document updates.
- **[2026-10-22]** Source inventory corrected against this workspace, replacing obsolete Company/Permission/User CRUD and MySQL claims from an earlier template inventory.
- **[2026-10-22]** Database username/password configuration now references environment variables; the bot token already references DISCORD_BOT_TOKEN.

## Current Implementation — Source Reviewed

- Java 25 / Spring Boot 4.1.1 / Maven skeleton with JPA, validation, Web MVC, PostgreSQL, Flyway, Lombok, and JDA 6.7.0 dependencies.
- DiscordBotConfig creates the JDA client with GUILD_VOICE_STATES and VOICE_STATE cache, registers the listener, and shuts down the client on bean destruction.
- DiscordVoiceListener logs room joins/leaves/moves and camera/screen-sharing changes. Join/leave handling excludes bots; camera/share callbacks do not yet apply that exclusion.
- application.yaml points to local PostgreSQL study_community and requires DB_USERNAME, DB_PASSWORD, and DISCORD_BOT_TOKEN from the environment.
- Only a contextLoads Spring Boot test exists; no isolated test profile, bot-disable switch, migration scripts, entities, repositories, controllers, auth/security implementation, or frontend exists in the current source.

## In Progress

- Documentation, AI guidance, and initial Discord integration are grouped separately for version control; the learning roadmap remains planned.
- Bot event logging is implemented; live connectivity and persisted study behavior remain unverified.

## Deferred Issues

- **[P1]** Identity resolution, credentials, verified linking/merge, recovery, event concurrency, and source coordination need feature-level design before implementation.
- **[P2]** API defaults remain provisional: completed-only lifetime totals, existing-source priority, heartbeat 30s/expiry 120s, room-disable closure, and competition ranking.
- **[P3]** Uploads, achievements/rewards, habits, chat, games, and advanced aggregation remain outside the current API scope.

## Warnings

- flyway.enabled is nested under spring.datasource rather than spring.flyway; dependencies alone do not establish a verified migration setup.
- contextLoads uses the default application configuration and can connect to the local database and real Discord. Isolate PostgreSQL and disable/mock bot startup before running it.
- There is no implemented discord.enabled condition. Discord credentials are required whenever the normal Spring context starts.
- Bot callbacks currently log events at INFO and do not persist study sessions or segments; tracked-room filtering and recovery remain unbuilt.
- Earlier local credentials were present in the initial commit; this session removes literals from current configuration without rewriting existing history.
- DATABASE.md references SQL/ERD artifacts absent from the workspace. ADR-001 remains Proposed and ADR-002 Deferred/historical.

## Next Tasks

1. **[P1] Foundation:** isolate PostgreSQL tests, support disabling the bot, correct Flyway configuration, introduce migrations, and verify startup without contacting Discord during tests.
2. **[P1] Shared API/security:** implement the target response/errors and authentication infrastructure as needed by the selected API slice.
3. **[P1] Accounts/auth:** implement UUID users/identities, concurrent identity resolution, Discord OAuth2, profile, and the reviewed login/refresh/logout contract.
4. **[P1] Discord study:** implement tracked rooms, room-management authorization, persisted sessions/segments, event concurrency, checkpoints, and recovery.
5. **[P2] Read APIs:** build lifetime dashboard, history, and leaderboard with duration/window/tie checks.
6. **[P2] Extensions/frontend:** review Google/local linking and WEB coordination when selected; build React views against implemented APIs.

## Milestones

- [x] Initial Study Together design/API drafts and source inventory; remaining alignment is separate.
- [ ] Phase 0: PostgreSQL, isolated tests, migrations, and shared API/security foundation verified.
- [ ] Phase 1: UUID users/identities, Discord login, profile, and login lifecycle implemented and tested.
- [ ] Phase 2: Managed Discord rooms, persisted tracking, segments, concurrency, and recovery implemented and tested.
- [ ] Phase 3: Dashboard, history, and leaderboard implemented and tested.
- [ ] Phase 4: Google/local login, linking/merge, WEB study, and cross-source safeguards tested.
- [ ] Phase 5: React integration, room administration, and end-to-end acceptance verified.

## Validation

- **[2026-10-22]** mvn -B -DskipTests package succeeded: production and test sources compiled and the executable JAR was packaged; tests were explicitly skipped.
- **[2026-10-22]** Node checked 15 Markdown files, 37 local links (none missing), six skill frontmatters, and the status-file limit (69 lines); the project workbook ZIP/XML was also inspected.
- The full Spring test and live database/Discord startup were not run because test isolation and bot suppression are not implemented.
- Future acceptance must cover ownership, token lifecycle, duplicate/reordered events, cross-source overlap, recovery, and time/ranking boundaries.
