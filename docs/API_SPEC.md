# Study Together — API Specification

> Status: working draft, 2026-10-22. Target contracts, not a list of implemented endpoints.
> Revise each section alongside its feature. Defaults below are provisional choices, not permanent product decisions.

## 1. Scope and working defaults

The workbook [Study-Together-Theo-Doi-Du-An.xlsx](./Study-Together-Theo-Doi-Du-An.xlsx) defines the original Discord scope. Follow-up discussion adds draft Google/local login, verified account linking, and web study. All learning history belongs to an internal user, independently of login provider.

| Stage | Features | References |
|---|---|---|
| Core | Discord login, identity resolution, profile, lifetime dashboard, history, tracked rooms, Discord events, leaderboard | AUTH-01/02, IDENTITY-01, PROFILE-01, ROOM-01/02, STUDY-01 through STUDY-06, RANK-01 |
| Extension draft | Google login, local email/password, linking, web study | IDEA-01 through IDEA-04 and subsequent discussion |

Company management, generic User/Role/Permission CRUD, uploads, achievements, chat, and games are outside this contract. Administrative actions still require authorization. Existing template endpoints do not establish implementation of these target APIs.

| Topic | Provisional default |
|---|---|
| Dashboard | Lifetime totals from completed sessions with `needs_review = false`; no hours/rank column on `users`. |
| Active study | Show current session separately; exclude it from aggregate totals and rankings. |
| Concurrent sources | One open credited session per internal user across WEB and DISCORD. Existing session keeps priority; no automatic preemption. |
| Web presence | Heartbeat every 30 seconds; expire after 120 seconds without a valid heartbeat, closing at the last confirmed observation. Configurable values. |
| Web visual | Zero initially. Camera/share credit needs a separate observation design. |
| Room disabled | Close active Discord sessions at the disable boundary. Re-enable/add starts only from fresh confirmed presence. |
| Ranking | Completed total seconds descending; competition rank (1, 2, 2, 4). Exclude disabled and zero-time users. |
| Login lifecycle | Access JWT: 15 minutes; browser refresh/login lifecycle: 3 days absolute. Logout affects the current login only. |
| Identity conflict | No automatic merge. An identity owned by another user remains a conflict until a verified merge workflow exists. |

This draft is a starting point for implementation. Update DTOs, migrations, tests, and affected documents together when a default changes. Schema dependencies are recorded in [DATABASE.md](./DATABASE.md); feature ownership is in [ARCHITECTURE.md](./ARCHITECTURE.md).

## 2. Shared conventions

- JSON API prefix: `/api/v1`; local example: `http://localhost:8080/api/v1`. Deployment origins are configured.
- OAuth browser redirects use section 5 paths and do not return JSON envelopes.
- Protected requests use `Authorization: Bearer <application-access-token>`. Resolve `/me` ownership from authentication, never a client-supplied user ID.
- Internal IDs are UUID strings. Discord IDs/provider subjects are strings. Login provider and study source are separate: a Discord login can start a WEB study session.
- UTC ISO timestamps, e.g. `2026-10-22T02:00:00Z`; calendar reporting uses `Asia/Ho_Chi_Minh`.
- Durations are whole seconds. Sum exact durations before flooring; frontend formats hours/minutes. Visual is part of total, not extra time.
- Windows are `[from, to)`. Clip intervals at reporting boundaries. Never return passwords, token hashes, secrets, or private provider tokens in profile/history data.
- Bodies are JSON unless identified as browser redirects. Reject malformed IDs, invalid enums/ranges, and attempts to set server-owned fields.

### Response envelope

```json
{
  "statusCode": 200,
  "data": {},
  "message": "Success",
  "timestamp": "2026-10-22T08:00:00Z"
}
```

Examples below show `data` only unless stated otherwise. HTTP status must match `statusCode`. Creation returns 201. Successful empty operations return 200 with null data. Redirects do not use this envelope.

Lists use `page` (default 1), `size` (default 20, range 1–100), and deterministic ordering. Invalid values return 400. Arbitrary sort expressions are not accepted in this draft.

```json
{
  "meta": { "page": 1, "pageSize": 20, "pages": 0, "total": 0 },
  "result": []
}
```

`total` counts matching records before pagination. Pages beyond the results are empty. Validation errors put a field-to-message map in `data`; other errors use null. Clients rely on HTTP status, not message wording.

| Status | Meaning |
|---|---|
| 400 | Invalid request, malformed UUID, invalid filter/OAuth operation |
| 401 | Missing/invalid/expired authentication, bad login or refresh credentials |
| 403 | Forbidden action, disabled account, or failed CSRF check |
| 404 | Resource absent; conceal another user's study session as 404 |
| 409 | Duplicate room, identity ownership conflict, active-session conflict, or invalid presence state |
| 429 | Configured login/registration or other rate limit exceeded |
| 502 / 503 | Provider failure / required dependency unavailable |
| 500 | Unexpected failure; generic client message, private diagnostic log |

## 3. Profile and lifetime dashboard

### GET /me

Requires an active authenticated account. Returns the application profile, not all provider identity data.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "An",
  "avatarUrl": null,
  "createdAt": "2026-10-01T02:00:00Z",
  "firstWebLoginAt": "2026-10-20T02:00:00Z"
}
```

Profile editing, other users' public profiles, and uploads are deferred. Initialize new name/avatar from available verified provider data. Later provider refreshes do not automatically overwrite the application profile in this draft.

### GET /me/study-stats

Requires login. Default `period=ALL` returns lifetime totals across the caller's linked WEB/DISCORD history. Other periods: `DAY`, `WEEK`, `MONTH`, `CUSTOM`.

- DAY/WEEK/MONTH accept optional `date=YYYY-MM-DD`, default today in Vietnam. Week starts Monday; month means calendar month.
- CUSTOM requires UTC ISO `from` and `to`, with `from < to`. Other periods reject custom boundaries. ALL rejects `date`.
- ALL has null boundaries and counts completed eligible history as of the request snapshot. Bounded periods clip intervals to their window.

```json
{
  "period": "ALL",
  "timezone": "Asia/Ho_Chi_Minh",
  "from": null,
  "to": null,
  "asOf": "2026-10-22T08:00:00Z",
  "includesOngoing": false,
  "totalStudySeconds": 198000,
  "visualSeconds": 72000,
  "bySource": { "WEB": 72000, "DISCORD": 126000 },
  "sessionCount": 42
}
```

`totalStudySeconds = bySource.WEB + bySource.DISCORD`; 198000 seconds is 55 hours. `sessionCount` counts eligible completed sessions with positive intersection. Empty history returns zeros. Aggregate sessions and visual segments separately to avoid multiplying session duration through joins.

The current timer comes from `/me/study-sessions/current`; a frontend estimate is not a persisted/ranked total. Add rebuildable aggregates/caches only if measured load requires them.

## 4. Personal study history

| Method | Path | Result |
|---|---|---|
| GET | `/me/study-sessions` | Paginated owned session history |
| GET | `/me/study-sessions/current` | Current session, or null data |
| GET | `/me/study-sessions/{id}` | One owned session; otherwise 404 |

All require login. List filters: `source=WEB|DISCORD`, `state=ALL|OPEN|CLOSED` (default ALL), pagination, optional UTC `from`/`to` pair. Require both boundaries or neither, with `from < to`. Order `startedAt DESC, id DESC`.

Filter by interval intersection, not just start date. Open intervals use `lastObservedAt` as the known end. History includes uncertain sessions with a visible flag, although totals exclude them. List/detail durations describe the full observed session rather than clipping to the filter window.

Session representation:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "source": "DISCORD",
  "room": {
    "id": "550e8400-e29b-41d4-a716-446655440020",
    "displayName": "Hoc Java"
  },
  "startedAt": "2026-10-22T02:00:00Z",
  "endedAt": "2026-10-22T03:00:00Z",
  "lastObservedAt": "2026-10-22T03:00:00Z",
  "totalStudySeconds": 3600,
  "visualSeconds": 2400,
  "endReason": "LEFT_ROOM",
  "needsReview": false
}
```

WEB has null `room`. Open sessions have null `endedAt`/`endReason`; their durations end at `lastObservedAt`, not an unobserved future instant. These are provisional values, excluded from aggregate totals. Reconcile an expired web lease before returning it as current.

## 5. Login and logout

### Browser OAuth login

Draft routes use Spring Security OAuth login conventions outside `/api/v1`:

| Method | Full path | Purpose |
|---|---|---|
| GET | `/oauth2/authorization/discord` | Start Discord login |
| GET | `/oauth2/authorization/google` | Start Google login; extension draft |
| GET | `/login/oauth2/code/{provider}` | Callback; provider is `discord` or `google` |

Use backend-managed state; validate the provider response and resolve `(provider, provider_subject)`. Google identity uses verified `sub`; Discord uses its verified user ID. Never merge by email/name. Reuse bot-created Discord users; create user/identity atomically with concurrent-creation handling.

Success creates a login lifecycle, sets the refresh cookie, and redirects to an allowlisted frontend completion page. The frontend obtains CSRF context, then calls `/auth/refresh` for its access JWT. Never put access/refresh tokens in redirect URLs. Cancellation/invalid callbacks redirect to a fixed error page without creating a login. Temporary OAuth state is separate from study sessions.

### Browser authentication defaults — provisional

The proposed decision and rotation/revocation details are recorded in [ADR-001](./decisions/001-refresh-token-strategy.md). Authentication and CSRF responses use `Cache-Control: no-store`; the lifecycle remains unimplemented.

- `GET /auth/csrf`: no bearer required; returns `{ "headerName": "X-XSRF-TOKEN", "token": "<csrf-token>" }` plus backend-managed CSRF context. Fetch again after login/logout rotates context.
- Cookie-authenticated mutations require that token in the returned header, allowed-origin validation, and browser credentials. Do not disable CSRF globally.
- Refresh cookie: `HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth`. Production assumes same-site frontend/backend; cross-site deployment needs cookie/CORS review. Local HTTP needs explicit development configuration.
- Access JWT is returned only by authentication responses and held in frontend memory. `sub` is internal user UUID; `sid` identifies a login lifecycle. Check lifecycle validity for immediate logout.
- Refresh tokens are opaque random values, stored as digests. Rotate atomically; reject expired/revoked/used tokens. Replay revokes the associated login. Client serializes refresh calls; test concurrent refresh behavior before release.
- Login lifecycle is 3 days absolute; rotation does not extend it. Cookie lifetime follows remaining login lifetime. Prevent caching of authentication responses.

### POST /auth/refresh

Refresh cookie plus CSRF; no access JWT or body required. Returns 200:

```json
{ "accessToken": "<application-access-jwt>", "tokenType": "Bearer", "expiresIn": 900 }
```

Set the rotated cookie; do not return browser refresh tokens in JSON. Invalid credentials return 401 and clear invalid cookies where applicable. Mobile delivery is deferred.

### POST /auth/logout

CSRF-protected browser operation. Resolve the current login from a valid refresh cookie or access JWT; if both valid credentials identify different logins, return 400 without revoking either. Otherwise revoke the identified lifecycle and its refresh tokens, then clear the cookie. Already absent/expired login returns 200 with null data after CSRF validation. Database failure returns 503, not successful logout. Other devices, study sessions, linked identities, and history remain independent.

### Local account extension

| Method | Path | Request | Success |
|---|---|---|---|
| POST | `/auth/register` | `displayName`, `email`, `password` | 201 profile as `/me`; no automatic login |
| POST | `/auth/login` | `email`, `password` | 200 access-token data as `/auth/refresh`, plus refresh cookie |

Both require browser CSRF context and rate limits. Draft validation: display name 1–100 characters after trimming, valid email at most 254 characters, password 8–128 characters without silent trimming. Use consistently normalized unique login email and a password hash in separate credentials storage. Bad login returns generic 401; duplicate registration 409. Disabled users cannot log in.

Credentials migration, normalization, email verification/recovery, and abuse controls require feature design before enabling local accounts. Matching a provider email does not automatically link accounts.

## 6. Linked identities — extension draft

### GET /me/identities

Requires login. Returns linked identities with internal `id`, `provider`, `providerDisplayName`, `providerAvatarUrl`, `linkedAt` (from `created_at`), and `lastAuthenticatedAt`. No raw provider subjects/tokens. Local credentials are separate.

### POST /me/identities/{provider}/link

Requires login; provider is `discord` or `google`. Create a short-lived, one-use operation bound to the authenticated user, browser context, and provider. Returns 201:

```json
{
  "authorizationUrl": "<backend-generated-provider-authorization-url>",
  "expiresAt": "2026-10-22T08:05:00Z"
}
```

Frontend navigates to that URL. `GET /api/v1/auth/link/callback/{provider}` handles the callback without a bearer header: verify identity, state, operation expiry, initiating context, and that the initiating login remains active. Consume the operation, link transactionally, and redirect to a fixed frontend result page. Browser-supplied user IDs/provider subjects are never proof.

Linking must be explicitly distinguished from login; normal OAuth login cannot implicitly attach an identity to an open account. Same identity/same user is a successful no-op. Another user's identity is a conflict (409 for JSON operations, fixed conflict result for browser callbacks). Draft limit: one identity per provider per user. Replacement/unlinking is deferred.

If the bot already created another user with Discord history, preserve both accounts/history and report conflict. A verified merge workflow must reconcile overlapping history and active sessions; moving identity ownership alone is insufficient. No merge API is specified yet.

## 7. Web study — extension draft

All operations require login/active account and affect WEB sessions only. `clientSessionId` is a UUID generated for one browser study attempt. It identifies an attempt, not authentication; persist its binding to the session and authenticated user. Server observations determine time. Clients never submit credited seconds or timestamps.

| Method | Path | Request | Success |
|---|---|---|---|
| POST | `/me/study-sessions` | `{ "clientSessionId": "<uuid>" }` | 201 new WEB session |
| POST | `/me/study-sessions/{id}/heartbeat` | `{ "clientSessionId": "<uuid>" }` | 200 updated session |
| POST | `/me/study-sessions/{id}/end` | `{ "clientSessionId": "<uuid>" }` | 200 closed session |

Responses use section 4's session representation.

- Start fixes source to WEB, Discord identity/room null, and both initial visual flags false. Web presence does not depend on which provider authenticated the user.
- Repeated start for the same user/attempt returns the existing session with 200, even if closed. New attempts need new IDs. Another active WEB attempt or DISCORD session returns 409.
- Heartbeat updates the owning WEB attempt's observation. Expire a stale lease before processing a late heartbeat; return 409 and require a new attempt. Never extend the missing interval retroactively.
- End closes at server observation while the lease is healthy, with proposed `USER_STOPPED`. If already timed out, return the existing closed result. Repeated end is idempotent. Other user's session: 404; wrong source/attempt: 409.
- Missing heartbeat for 120 seconds closes at last confirmed observation with proposed `HEARTBEAT_TIMEOUT`. No credit for the unobserved gap. Backend expiry/reconciliation runs even when tab closure delivers no request.
- Heartbeat proves application presence, not attention or camera use. Evaluate browser presence/visual trust before adding rewards or stronger claims.

### Coordination with Discord

Serialize start/end, expiry, Discord events, and reconciliation on the same internal user. Enforce one open credited session across WEB/DISCORD in database and services. Completed credited intervals must not overlap across sources. This strengthens the Discord-only baseline; migrations are required before web study.

If WEB is active, Discord entry creates no second credited session. `/current` stays WEB. When WEB ends/expires, reconciliation may start DISCORD after confirming current presence in an enabled room, from the fresh observation without backfill. If DISCORD is active, WEB start returns 409. Channel moves only transition the matching active Discord session; never close WEB or a newer room's session.

## 8. Tracked Discord rooms

Require room-management authority; ordinary users receive 403. Exact role/permission representation is deferred. Validate guild/channel existence, voice-channel type, and bot access before registration, outside the database write transaction.

| Method | Path | Request / query | Success |
|---|---|---|---|
| GET | `/admin/study-rooms` | Pagination; optional `trackingEnabled`, `discordGuildId` | 200 paginated rooms; `createdAt DESC, id DESC` |
| POST | `/admin/study-rooms` | `discordGuildId`, `discordChannelId` | 201 room; tracking enabled by default |
| PATCH | `/admin/study-rooms/{id}` | `{ "trackingEnabled": false }` or true | 200 room |

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440020",
  "discordGuildId": "123456789012345678",
  "discordChannelId": "234567890123456789",
  "displayName": "Hoc Java",
  "trackingEnabled": true,
  "createdAt": "2026-10-01T02:00:00Z",
  "updatedAt": "2026-10-22T08:00:00Z"
}
```

Discord IDs: digit strings, 1–20 characters. Names come from Discord, not identity keys. Duplicate guild/channel: 409; invalid/inaccessible channel: 400; missing internal room: 404. Dependency failures: 502/503. Repeated tracking state is a no-op. No delete endpoint; preserve history by disabling.

Disable coordinates configuration and active Discord session closure at one boundary using `ROOM_DISABLED`, serialized against joins/moves. Add/re-enable triggers fresh presence reconciliation respecting source priority. Validate this transition under concurrency before release.

## 9. Leaderboard

### GET /leaderboard

Requires login in this draft; guest access is deferred. Pagination and period/date/from/to match `/me/study-stats`, default ALL.

Compute completed eligible seconds using the same rules as dashboard statistics. Rank globally before pagination; equal seconds receive equal rank across pages. Deterministic ordering: `rank ASC, userId ASC`. Exclude disabled/zero-time users.

Response data contains `period`, `timezone`, `from`, `to`, `asOf`, `includesOngoing: false`, pagination `meta`, and `result`. Each result contains `rank`, `userId`, `displayName`, `avatarUrl`, `totalStudySeconds`, `visualSeconds`. Visual is supplementary, never a bonus/tie-breaker. Empty data returns empty `result` and zero pagination totals. Calculate on request initially; shared snapshots/caching across requests can be designed when needed.

## 10. Discord events — internal contracts

No public Discord study write endpoint. Browsers cannot submit Discord IDs/durations to fabricate history. Verified bot listeners delegate to services.

| Operation | Trigger | Behavior |
|---|---|---|
| `resolveDiscordUser` | Verified bot observation or OAuth login | Resolve/create same internal user and Discord identity |
| `startStudySession` | Human joins enabled tracked room | Open session + segment only if no credited source active |
| `updateVisualState` | Camera/share changes | Transition segments; camera OR share counts once |
| `endStudySession` | Leave tracked room | Close matching Discord session + open segment atomically |
| `transitionStudyRoom` | Channel changes | Close old/open new at same observation when eligible |
| `reconcilePresence` | Startup/reconnect, expiry, tracking change | Confirm actual state; no unobserved-gap credit |

Exclude bot accounts/untracked rooms. Camera not required for total time; mute/deafen does not stop it. Duplicate events are harmless; delayed old-room events cannot alter newer sessions. Persist checkpoints. Draft uncertain recovery closes at last observation with `needs_review = true` and `RECOVERY_UNCERTAIN`, excluded from totals until resolved. Verify SDK ordering/resume details during integration.

## 11. Follow-up and references

- Keep contracts marked draft until controllers, security paths, DTOs, migrations, services, and meaningful tests exist. Route grouping does not mandate one module per route.
- Apply the planning addendum in DATABASE.md before implementing extension features; no credentials, web-attempt binding, login lifecycle, or cross-source constraint is implied to exist today.
- [ADR-001](./decisions/001-refresh-token-strategy.md) now records the proposed browser-only rotating-cookie lifecycle, including replay, concurrency, CSRF, and logout behavior. Review it alongside this contract during implementation. [ADR-002](./decisions/002-file-upload-strategy.md) does not add uploads to this scope.
- Revisit defaults during feature work: ongoing totals, source priority, account merge/unlink, local-account verification/recovery, presence trust, room recovery, guest access, ranking style, and report limits.
- Validate source concurrency, duplicate/reordered events, link conflicts, session ownership, refresh/logout, and reporting boundaries against the intended database. Documentation changes do not establish test results.

Technical references: [Spring Security OAuth login routes](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html), [Google OpenID Connect identity](https://developers.google.com/identity/openid-connect/openid-connect), and [PostgreSQL partial indexes](https://www.postgresql.org/docs/current/indexes-partial.html).
