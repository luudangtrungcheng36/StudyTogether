# ADR-001: Browser Authentication — Rotating Refresh Cookie

## Status

**Proposed working draft — 2026-10-22. Not implemented.** The user authorized a revisable design template; review this decision when implementing authentication.

This revision replaces the course template's **Accepted: Cookie (SPA) + Body (Mobile)** design. The current client is the React website. A native mobile token contract is deferred. Changing this document does not implement refresh, logout, OAuth login, or security configuration.

## Context

Study Together identifies users internally, regardless of whether login uses Discord, Google, or future local credentials. Application access JWTs are separate from provider tokens. Website login lifecycles are also separate from Discord/WEB study sessions.

The draft must support page reload, short-lived API access, refresh-token rotation, and immediate logout of the current login. It must agree with [API_SPEC.md](../API_SPEC.md), [DATABASE.md](../DATABASE.md), and [PROJECT-RULES.md](../PROJECT-RULES.md).

Current source still has email/password login at `/auth/login`, HS256 JWTs with email subjects, a 24-hour configured lifetime, and global CSRF disabling. No refresh/logout lifecycle has been found. These are implementation gaps, not behavior approved by this ADR.

## Options Considered

| Option | Tradeoff | Outcome |
|---|---|---|
| Refresh token in browser JSON/storage | JavaScript manages a long-lived credential | Not selected for this browser contract |
| Cookie for web plus JSON body for mobile | Multiple credential sources and client contracts | Previous template; mobile deferred |
| Opaque rotating refresh cookie plus access JWT in memory | Requires cookie/CSRF handling and persisted login lifecycle | Selected provisionally |
| Cookie-only application authentication or backend-for-frontend | Another viable architecture, but changes the current JWT API contract | Deferred |

## Decision

Use an opaque rotating refresh token in a host-only HttpOnly cookie for the browser. Return only the application access JWT in authentication JSON; keep it in frontend memory. Do not accept refresh tokens from JSON, query parameters, or an alternative token header.

### Token and lifecycle defaults

| Item | Draft value |
|---|---|
| Access JWT | 15 minutes; validate signature, expiration, issuer, and audience |
| JWT identity | `sub` = internal user UUID, `sid` = login lifecycle UUID |
| JWT signing | HS512 and environment-provided secret per PROJECT-RULES.md |
| Browser login lifecycle | 3 days absolute from creation; rotation does not extend it |
| Refresh token | Cryptographically random opaque value, at least 32 random bytes, not a JWT |
| Server refresh storage | Unique SHA-256 digest only; never raw token |
| Browser access storage | Memory; reload obtains a new access JWT through refresh |
| Logout scope | Current login lifecycle; other devices remain independent |

The 15-minute/3-day values are configurable product defaults. JWT validation also checks that the `sid` lifecycle is unexpired, unrevoked, owned by `sub`, and belongs to an active user. A valid signature alone does not satisfy this draft's immediate-logout requirement.

### Cookie

```http
Set-Cookie: refresh_token=<opaque-value>; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth; Max-Age=259200
```

Omit Domain to keep the cookie host-only. The initial Max-Age shown is 3 days; later rotations use only the remaining lifecycle lifetime. Clear with the same name/path/domain scope and `Max-Age=0`.

Production assumes HTTPS and same-site frontend/backend; same-origin proxying can simplify deployment. Cross-origin same-site requests need an explicit frontend origin allowlist and credentialed CORS. Do not combine credentialed requests with a wildcard origin. Cross-site deployment requires revisiting SameSite/cookie policy. Local HTTP may use an explicit development-only cookie setting.

HttpOnly prevents JavaScript from reading this cookie, but injected script can still make authenticated requests. It is not complete XSS protection. SameSite=Lax is an additional browser restriction, not this application's entire CSRF defense. These distinctions follow [OWASP's CSRF guidance](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## Browser Flows

All JSON routes below include the full `/api/v1` prefix. Return the shared ApiResponse envelope, with HTTP status matching `statusCode`, and prevent caching of authentication/CSRF responses using `Cache-Control: no-store`. Never log raw credentials or put access/refresh tokens in redirect URLs.

### 1. Establish CSRF context

`GET /api/v1/auth/csrf` requires no access JWT. It returns the current backend-validated CSRF token and establishes the associated browser context:

```json
{ "headerName": "X-XSRF-TOKEN", "token": "<csrf-token>" }
```

This is the envelope's `data`, not the complete response. Use Spring Security's CSRF facilities with a context-bound token; choose/configure the repository during implementation. The CSRF token is not the refresh token. OAuth login state, CSRF context, and application login lifecycle have distinct purposes.

Send the returned token in `X-XSRF-TOKEN` for cookie-authenticated mutations, including local login/register, refresh, and logout, with browser credentials enabled. Verify the token and allowed origin; define a checked Referer fallback when Origin is absent. Missing/untrusted origin evidence on these browser mutations is rejected. Do not disable CSRF globally.

Fetch a new CSRF token when login/logout resets that context. OAuth browser callbacks use their separately validated state/operation binding, not a custom browser CSRF header. Bearer-only API handling must remain separate from cookie-authenticated routes. Test the SPA token/header handling and context reset behavior against the selected framework version; see [Spring Security CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

### 2. Successful login

- OAuth login: verify the provider response, resolve the internal identity, create the login lifecycle and initial refresh-token digest, and set the cookie. Redirect to a fixed/allowlisted frontend completion page. The frontend gets CSRF context, then calls refresh for its access JWT.
- Local login extension: validate credentials and browser CSRF context, create the same lifecycle/refresh records, set the cookie, and return the access-token response below.
- Registering a local account does not automatically log in in the API draft. Provider credentials are never application refresh tokens.

Authentication response `data`:

```json
{ "accessToken": "<application-access-jwt>", "tokenType": "Bearer", "expiresIn": 900 }
```

`expiresIn` is seconds, not milliseconds. No `refreshToken` property is returned. Account/provider details are obtained through the authenticated profile/identity APIs.

### 3. Refresh

`POST /api/v1/auth/refresh` uses the refresh cookie plus CSRF header. No body or access JWT is required; a submitted JSON refresh-token credential is unsupported.

1. Validate browser CSRF/origin context before changing credentials.
2. Hash the supplied cookie and locate its token/login record. Acquire the login lifecycle lock using a consistent order.
3. Recheck account status, ownership, absolute lifecycle expiry/revocation, and token expiry/use/revocation under the lock.
4. Mark the old token used and insert its replacement digest in one transaction. Replacement expiry cannot exceed the original lifecycle expiry.
5. After successful persistence, set the new cookie and return the access-token response. Never issue a usable replacement when the transaction failed.

Retain used-token digests and their login association at least until that lifecycle expires so reuse can be recognized. Design cleanup/retention before enabling the feature. Rotation and reuse handling are informed by [RFC 9700 section 4.14](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14); the values and browser contract here are application-specific choices.

If a recognized used token is presented while its lifecycle is active, revoke that entire login lifecycle and its refresh tokens, clear the cookie, and return generic 401. Commit revocation even when returning an authentication failure; throwing an exception must not accidentally roll it back. Other login lifecycles are unaffected.

Serialize refresh across requests/tabs for this browser context. The provisional policy has no grace period: a duplicate request, network retry after an already committed rotation, or lost successful response can force login again. Do not silently resend an old token or create a second credential source; revisit recovery/concurrent-refresh behavior during implementation.

### 4. Access APIs and reload

Send the in-memory access JWT in the Authorization Bearer header. Validate JWT claims plus login lifecycle state. Return 401 for expired/revoked authentication. On token expiration, coordinate one refresh attempt, then retry the original request once. Do not create infinite refresh/retry loops.

After reload, obtain CSRF context and refresh from the cookie. A failed refresh clears local access state and returns the website to logged-out state. The browser never needs to read the refresh cookie.

Immediate revocation requires server-side lifecycle checks even though access tokens are JWTs. Start with authoritative database checks; adding a cache later must preserve revocation guarantees or explicitly revise that guarantee.

### 5. Logout

`POST /api/v1/auth/logout` is CSRF-protected. Identify the current login using a valid refresh cookie or access JWT. If both valid credentials identify different logins, return 400 without revoking either; do not guess which account to revoke.

Revoke the identified lifecycle and its refresh tokens under the same lock used by refresh, clear the cookie, and return 200 with null data. Repeating logout, or having no active login left, returns 200 after CSRF validation. Clear frontend access state and renew CSRF context as needed.

Once revocation commits, newly validated requests using that login's JWTs fail; an already-authorized in-flight request is not retroactively undone. Logout does not unlink accounts, erase learning history, or stop Discord/WEB study tracking. WEB study still expires through its presence rules if heartbeats cease.

### Failure responses

| Case | Response |
|---|---|
| Invalid CSRF/origin context | 403; do not rotate/revoke based on an untrusted request |
| Missing/unknown/expired/revoked refresh credential | 401; clear the invalid refresh cookie where applicable |
| Recognized used-token replay | Revoke that login, clear cookie, generic 401 |
| Disabled account | 403, revoke the relevant login and clear cookie after trusted identification |
| Conflicting valid logout credentials | 400; revoke neither |
| Logout without active credentials after valid CSRF | 200, null data, clear cookie |
| Database unavailable | 503; do not treat it as successful login/rotation/logout |

## Persistence and Planned Implementation

Proposed tables in DATABASE.md:

| Table | Fields needed by this decision |
|---|---|
| `auth_sessions` | `id`, `user_id`, `created_at`, `expires_at`, `revoked_at` |
| `refresh_tokens` | `id`, `auth_session_id`, `token_hash`, `issued_at`, `expires_at`, `used_at`, `revoked_at` |

One login lifecycle owns all its token rotations. Store neither access JWTs nor raw refresh/provider tokens in these tables. UUID ownership constraints, unique digests, expiry checks, indexes, and cleanup need versioned migrations; the referenced auth SQL artifact is not present yet.

Controllers/security handlers own HTTP cookies, headers, redirects, and status mapping. Business services receive credential/context values and perform lifecycle operations without HttpServletRequest/ResponseEntity concerns. Repositories handle persistence; `auth_sessions` is unrelated to `study_sessions`.

Planned work: auth controller/service and DTOs, lifecycle entities/repositories, security claim/lifecycle validation and CSRF/CORS handlers, environment-based configuration, PostgreSQL migrations, and frontend refresh coordination. Existing classes are not evidence these changes are implemented.

## Consequences and Verification

Cookie-only browser refresh avoids two accepted refresh-credential sources. Rotation can detect reuse; persisted lifecycle checks support current-login revocation. Costs include database access, refresh concurrency handling, browser deployment constraints, and possible reauthentication after an ambiguous network failure.

Before marking implementation complete, verify cookie flags/deletion, no raw credential exposure, CSRF/origin rejection, login/reload/refresh, atomic rotation and replay revocation, cross-tab races, absolute expiry, disabled users, logout-versus-refresh races, two-device isolation, database failure behavior, and claim/lifecycle ownership checks.

No runtime tests are claimed by this document. Update this ADR, the API contract, affected database/architecture notes, and PROJECT-STATUS.md when the design or verified implementation changes.

## Decision History

- Original course template: marked dual-source cookie/body refresh Accepted, with rotation deferred and insufficient CSRF assumptions. Preserved here as historical context, not current implementation guidance.
- **2026-10-22:** replaced with this Proposed browser-only working draft; explicit CSRF, opaque rotation, login lifecycle revocation, and configurable lifetimes. Native mobile delivery and future alternative authentication architectures remain out of scope.
