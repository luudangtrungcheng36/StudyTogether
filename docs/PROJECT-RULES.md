# Project Rules — Study Together

> Shared coding conventions for developers and AI tools.
> Learning project: design documents and Proposed ADRs are evolving starting points. Use section 17 to compare evidence, settle material changes with the user, and keep documentation current as features are built.
---

## 0. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Backend | Spring Boot 4.x / Spring Framework 7 |
| Security | Spring Security 7, Discord OAuth2 login, JWT via OAuth2 Resource Server |
| Database | PostgreSQL + Spring Data JPA |
| Frontend | React |
| Discord Bot | Library and runtime to be decided |
| Build | Maven |
| Test | JUnit 6 + Mockito + MockMvc |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Validation | Jakarta Bean Validation |

---

## 1. Package Structure

Structure under `src/main/java/<base-package>/`:

```text
├── StudyTogetherApplication.java
├── config/                     # Security, JWT, CORS, OpenAPI configuration
├── security/                   # Current user and authentication helpers
├── exception/                  # Custom exceptions and global handler
├── dto/                        # Shared API response and pagination DTOs
├── feature/
│   ├── auth/                   # Discord login, token refresh, logout
│   ├── user/                   # Discord identity and user profiles
│   ├── discord/                # Bot lifecycle, listeners, Discord clients
│   ├── study/                  # Study rooms, sessions, history, and statistics
│   └── leaderboard/            # Rankings
└── util/                       # Shared stateless utilities
```

Each feature may contain:

```text
feature/<name>/
├── <Name>Controller.java
├── <Name>Service.java
├── <Name>ServiceImpl.java
├── <Name>Repository.java
├── <Entity>.java
├── dto/
└── CONTEXT.md
```

Configuration files belong in `src/main/resources/`:

```text
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── application-test.yml
```

### Package Rules

- Group code by business module under `feature/`, not by individual API or use case.
- Keep feature DTOs inside the feature's `dto/` package.
- Shared `dto/` contains only DTOs used across modules.
- Keep shared configuration and security components at the top level.
- The Discord bot runs within the Java backend under `feature/discord/`.
- Discord listeners delegate business logic to feature services.
- Create classes and packages only when needed; not every module requires all classes shown above.
- Tests mirror the Java package structure under `src/test/java/`.

---

## 2. Naming Convention

### Classes

| Type | Pattern | Example |
|------|---------|---------|
| Entity | `[Name]` | `User`, `StudyRoom`, `StudySession` |
| Controller | `[Feature]Controller` | `StudyRoomController` |
| Service interface | `[Feature]Service` | `StudySessionService` |
| Service implementation | `[Feature]ServiceImpl` | `StudySessionServiceImpl` |
| Repository | `[Entity]Repository` | `StudySessionRepository` |
| Request DTO | `[Action]Request` | `CreateStudyRoomRequest` |
| Response DTO | `[Name]Response` | `UserResponse`, `StudySessionResponse` |
| Exception | `[Name]Exception` | `ResourceNotFoundException` |
| Configuration | `[Name]Config` | `SecurityConfig`, `DiscordConfig` |
| Discord listener | `[Event]Listener` | `DiscordVoiceStateListener` |

### Methods

| Layer | Convention | Example |
|-------|------------|---------|
| Controller | Resource action | `getProfile()`, `createStudyRoom()` |
| Service | Business action | `startSession()`, `endSession()`, `calculateRanking()` |
| Repository | Spring Data convention | `findByDiscordId()`, `existsByChannelId()` |

### Variables and Packages

- Use `camelCase` for fields, variables, and methods.
- Use `UPPER_SNAKE_CASE` for constants.
- Use lowercase package names: `studysession`, `leaderboard`.
- Use meaningful names: `studySessionRepository`, not `ssRepo`.
- Boolean methods use `is`, `has`, or `can`: `isActive()`, `hasPermission()`.
- Boolean fields use clear names such as `active` or `cameraEnabled`.
- Use English names consistently throughout the code.

---

## 3. Service Layer — Interface + Implementation

### Interface

```java
public interface StudySessionService {
    void startSession(String discordId, String channelId);
    void endSession(String discordId);
}
```

### Implementation

```java
@Service
public class StudySessionServiceImpl implements StudySessionService {

    private final StudySessionRepository studySessionRepository;

    public StudySessionServiceImpl(
            StudySessionRepository studySessionRepository) {
        this.studySessionRepository = studySessionRepository;
    }

    @Override
    @Transactional
    public void startSession(String discordId, String channelId) {
        // Validate eligibility and create the study session.
    }

    @Override
    @Transactional
    public void endSession(String discordId) {
        // Calculate duration and close the study session.
    }
}
```

### Rules

- Service interfaces define business operations using method signatures only.
- Put `@Service` on implementation classes, not interfaces.
- Controllers and Discord listeners depend on service interfaces.
- Use constructor injection with `final` fields.
- Put `@Transactional` on implementation methods that require transactional writes.
- Keep related database changes within one business transaction.
- Use `@Transactional(readOnly = true)` for read operations when needed.
- Keep HTTP concerns out of business services: no `HttpServletRequest`,
  `ResponseEntity`, or `HttpStatus`.
- Convert entities to response DTOs in the service layer for API operations.
- Avoid external network calls inside database transactions.

---

## 4. Controller Layer

```java
@RestController
@RequestMapping("/api/v1/studyrooms")
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    public StudyRoomController(StudyRoomService studyRoomService) {
        this.studyRoomService = studyRoomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudyRoomResponse>> createStudyRoom(
            @RequestBody @Valid CreateStudyRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        studyRoomService.createStudyRoom(request)));
    }
}
```

### Rules

- Controllers receive requests, call services, and return responses.
- Keep business logic out of controllers.
- Use `@Valid` on request DTOs received through `@RequestBody`.
- Use constructor injection with `final` fields.
- Inject service interfaces, not implementation classes.
- Never use `@Autowired` field injection.
- Return `ResponseEntity<ApiResponse<T>>` for standard JSON APIs.
- Use appropriate responses for redirects, file downloads, and no-content endpoints.
- Keep the HTTP status consistent with the response body.
- Delegate exception handling to the global exception handler.

---

## 5. ApiResponse Wrapper

Use a consistent response envelope for standard JSON APIs.

```java
import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        int statusCode,
        T data,
        String message,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(), data, "Success", Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(), data, "Created", Instant.now());
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return new ApiResponse<>(
                statusCode, null, message, Instant.now());
    }
}
```

### Rules

- Standard JSON APIs return `ApiResponse<T>`.
- Use factory methods (`success()`, `created()`, `error()`) instead of constructing responses directly in controllers.
- Keep `statusCode` consistent with the actual HTTP status.
- Error responses use the same wrapper through exception and security handlers.
- Use `Instant` for response timestamps; display times in `Asia/Ho_Chi_Minh`.
- Redirects, file downloads, and no-content responses do not require this wrapper.
- Never expose stack traces or internal error details in responses.

---

## 6. DTO Rules — Java Records

### Request DTO

```java
public record UpdateUserRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName
) {}
```

### Response DTO

```java
public record UserResponse(
        String displayName,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }
}
```

### Rules

- Use Java records for request and response DTOs.
- Apply Jakarta Bean Validation annotations to request fields according to API constraints.
- Response DTOs provide a static `fromEntity()` method for direct entity mapping.
- Perform entity-to-DTO conversion in the service layer.
- Return DTOs, never JPA entities, from API endpoints.
- Never expose passwords, password hashes, signing keys, or internal credentials.
- Return authentication tokens only through the intended authentication responses.
- Never put `@Entity` or JPA annotations on DTOs.
- Use `Instant` for timestamp fields.

---

## 7. Entity Rules

Entities map database tables; their fields and relationships follow
`docs/DATABASE.md`.

### Rules

- Use `@Entity` and explicit `@Table(name = "...")` with snake_case table names.
- Define `@Column` constraints according to the database schema.
- Follow the agreed schema for ID types and generation strategies.
- Use `@Enumerated(EnumType.STRING)`, never `ORDINAL`.
- Use `Instant` for event timestamps and `LocalDate` for date-only fields.
- Use `LocalDateTime` only when a local date and time without a time zone is intended.
- Do not use `java.util.Date` or `java.sql.Timestamp` for entity fields.
- Provide an explicit public or protected no-argument constructor for JPA.
- Write explicit getters and only the setters needed by the application.
- Do not use Lombok `@Data` on entities.
- Keep Hibernate-managed `createdAt` and `updatedAt` fields getter-only.
- Never return entities directly from API endpoints.

---

## 8. JWT and Discord OAuth2 Login

### Approach

Use Spring Security OAuth2 Client for Discord login.
After successful login, the backend issues Study Together access JWTs.

Use `JwtEncoder` to create tokens and `JwtDecoder` with
OAuth2 Resource Server to validate incoming API requests.
No custom JWT authentication filter is needed.

### Maven Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

### Rules

- Discord tokens and Study Together tokens serve different purposes;
  never use Discord tokens as application access JWTs.
- Use HS512 with a cryptographically random secret of at least 64 bytes.
- Load JWT secrets and Discord credentials from environment variables.
- Use the internal user ID, represented as a string, as the JWT subject.
- Do not put sensitive information in JWT claims.
- Validate the signature, expiration, and expected issuer and audience.
- Keep token lifetimes configurable; the default access-token lifetime is 15 minutes.
- Keep refresh-token storage, expiration, and revocation consistent with
  the agreed strategy in `docs/decisions/`.
- API authentication uses JWTs; temporary OAuth2 login state is handled separately.
- Do not disable CSRF globally merely because JWTs are used.
- Protect cookie-authenticated endpoints against CSRF.
- Explicitly configure public endpoints; require authentication for other endpoints.
- Return API authentication and authorization errors using `ApiResponse`.
- Discord is the only login provider in the initial phase.
- Keep provider-specific login logic separate from application JWT issuance.
- Users have an internal identity independent of their login provider.
- Additional login methods may be added later without changing API authentication.
- Account-linking rules will be defined when additional login methods are introduced.

---

## 9. Exception Handling

### Approach

Services throw specific application exceptions.
`GlobalExceptionHandler` uses `@RestControllerAdvice` to translate
MVC exceptions into consistent API error responses.

Security filters and background event processing have separate
error handlers.

### Rules

- All custom application exceptions extend `AppException`.
- Services throw specific exceptions without constructing HTTP responses.
- Controllers do not catch exceptions merely to build error responses.
- Standard JSON error responses use `ApiResponse`.
- Keep the response body's `statusCode` consistent with the HTTP status.
- Field validation errors return a field-to-message map in `data`.
- Preserve appropriate HTTP statuses for known Spring MVC request errors.
- Return 404 for missing resources and 400 for invalid requests.
- Handle API authentication failures through `AuthenticationEntryPoint`
  and authorization failures through `AccessDeniedHandler`.
- Security handlers return the same API error format.
- Handle Discord listener and background-task failures at their entry points;
  they are not handled by MVC exception advice.
- Log unexpected exceptions with their stack traces and return a generic
  500 message to the client.
- Never expose stack traces, SQL errors, credentials, or internal details
  in client responses.
---

## 10. Repository Layer

### Example Methods

```java
// Inside UserRepository
Optional<User> findByDiscordId(String discordId);

boolean existsByDiscordId(String discordId);
```

### Rules

- Repository interfaces extend Spring Data repositories with the correct entity and ID types.
- Prefer derived query methods for simple queries.
- Use JPQL for queries that cannot be expressed clearly through method names.
- Use native SQL when PostgreSQL-specific features or query requirements justify it.
- Return `Optional<T>` for single-entity lookups where the result may be absent.
- Keep business logic in services, not repositories.
- Use pagination for potentially large result sets.

---

## 11. API Versioning

Use a version prefix in application API URLs.

### Example

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    // User API endpoints
}
```

### Rules

- Use `/api/v1/...` for the initial application APIs.
- Keep versioning consistent across controllers and API documentation.
- Do not create additional versions until a breaking API change requires them.
- Adding features does not automatically require a new API version.
- Separate DTOs by version only when their contracts differ.
- Coordinate breaking changes with affected clients.
- Deprecate versions still used by clients before removing them.

---

## 12. Spring Boot 4 Conventions

### Null Safety

Use JSpecify annotations when explicit nullability improves clarity.

```java
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public @NonNull String displayNameOrDefault(@Nullable String displayName) {
    return displayName != null ? displayName : "Unknown user";
}
```

- Nullability annotations describe contracts; they do not replace runtime validation.
- Use `Optional<T>` for repository lookups that may have no result.
- Respect nullability contracts when implementing or overriding methods.

### HTTP Service Clients

Use HTTP Service interfaces backed by `RestClient` for custom synchronous
REST integrations when appropriate.

```java
// Illustrative external API contract.
@HttpExchange
public interface ExternalApiClient {

    @GetExchange("/status")
    String getStatus();
}
```

- Register interfaces as HTTP Service client beans before injecting them.
- Configure base URLs and timeouts in client configuration.
- Use Spring Security OAuth2 Client for supported login flows.
- Use the Discord library for operations it already supports.
- Keep external API calls out of database transactions.

### Record-based Configuration

Group related settings using `@ConfigurationProperties` records.

```java
@ConfigurationProperties("discord")
public record DiscordProperties(
        String botToken
) {}
```

```yaml
discord:
  bot-token: ${DISCORD_BOT_TOKEN}
```

- Register configuration properties through `@ConfigurationPropertiesScan`
  or `@EnableConfigurationProperties`.
- Keep configuration out of business code.
- Load secrets from environment variables.
- Never log configuration objects containing secrets.

---

## 13. Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should reject lookup when the user does not exist")
    void getUserByDiscordId_notFound_throwsException() {
        String discordId = "123456789012345678";

        when(userRepository.findByDiscordId(discordId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByDiscordId(discordId));
    }
}
```

### MVC Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 401 when requesting a protected profile without JWT")
    void getProfile_withoutToken_returnsUnauthorized() throws Exception {
        // Replace with the implemented profile endpoint.
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401));
    }
}
```

### application-test.yml

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL}
    username: ${TEST_DB_USERNAME}
    password: ${TEST_DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: create-drop  # Disposable test database only

jwt:
  secret-key: ${TEST_JWT_SECRET_KEY}

discord:
  enabled: false
  bot-token: unused-test-token
```

The bot startup implementation must honor `discord.enabled`.
Setting the property alone does not disable the bot.

### Rules

- Name tests using `[method]_[scenario]_[expected]`.
- Use descriptive test names and `@DisplayName` to explain behavior.
- Unit tests use JUnit and Mockito without starting Spring or a database.
- Choose focused test slices or full-context tests according to what is being verified.
- Use `@AutoConfigureMockMvc` when full-context tests require MockMvc.
- Spring-context tests use the `test` profile.
- Database integration tests use isolated PostgreSQL, not H2 or development/production databases.
- Use `create-drop` only for disposable test databases.
- Disable real Discord bot startup and mock external network boundaries.
- Test important business outcomes and relevant failure cases.
- Prioritize study duration, camera/screen-sharing intervals, repeated events,
  Vietnam reporting boundaries, and ranking ties.
- Use controllable time in time-sensitive tests; avoid waiting with `Thread.sleep()`.
- Field injection is allowed in Spring test classes; production code uses constructor injection.
- Example methods and routes must be adapted to the implemented API contracts.

### Spring Boot 4 Imports

- MockMvc configuration:
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
- Jackson 3 ObjectMapper:
  `tools.jackson.databind.ObjectMapper`
---

## 14. Logging

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log =
        LoggerFactory.getLogger(StudySessionServiceImpl.class);
```

### Usage Examples

```java
log.info("Study session closed: sessionId={}, durationSeconds={}",
        sessionId, durationSeconds);

log.warn("Discord connection interrupted");

log.error("Failed to process study session: sessionId={}",
        sessionId, exception);
```

### Rules

- Use SLF4J with an explicitly declared logger.
- Use parameterized `{}` placeholders, not string concatenation.
- Never use `System.out.println()` or `printStackTrace()` for application logging.
- Never log passwords, JWTs, Discord bot tokens, OAuth credentials,
  authorization codes, or authentication headers/cookies.
- Avoid unnecessary personal data; use internal identifiers when appropriate.
- Use ERROR for failures, WARN for notable abnormal conditions,
  INFO for important operational/business events, and DEBUG for diagnostic details.
- Keep frequent Discord event details at DEBUG level.
- Keep DEBUG disabled in normal production operation.
- Avoid duplicating HTTP access logs in controllers.
- Log unexpected exceptions with their stack traces at the handling boundary.
- Avoid logging the same exception repeatedly across layers.
- Logs support diagnostics; persisted study records remain in the database.
---

## 15. Code Size Guidelines

| Metric | Target | Review action |
|--------|--------|---------------|
| Java source file | < 300 lines | Check whether the class has too many responsibilities |
| Method | < 50 lines | Consider extracting coherent steps |
| Method parameters | < 5 | Consider a meaningful parameter object |
| Constructor dependencies | < 7 | Review dependency count and class responsibilities |
| Nested blocks | < 3 levels | Consider early returns or extracted methods |

### Rules

- Treat these targets as review guidelines, not automatic failures.
- Refactor to improve clarity and cohesion, not merely to reduce line counts.
- Do not create unnecessary classes or parameter objects just to meet a target.
- These targets do not apply to documentation or generated code.

---

## 16. Commit Checklist

- [ ] Production code uses constructor injection with final fields.
- [ ] Controllers and Discord listeners delegate business logic to services.
- [ ] Services follow the interface + implementation convention.
- [ ] API endpoints return DTOs, not entities.
- [ ] Request DTO validation is applied where appropriate.
- [ ] Standard JSON responses use ApiResponse with matching HTTP status codes.
- [ ] Application exceptions and security errors follow the agreed handling conventions.
- [ ] Configuration is externalized; secrets are not committed or logged.
- [ ] Important behavior changes have appropriate test coverage.
- [ ] Relevant tests and checks pass.
- [ ] Code size and responsibilities have been reviewed.
- [ ] Relevant documentation is updated when contracts, schema, or important logic change.

---

## 17. Documentation Requirements

| When | Action |
|------|--------|
| End of a coding session | Update docs/PROJECT-STATUS.md with actual progress, remaining work, and blockers |
| New module with non-obvious logic | Create CONTEXT.md inside the feature package |
| Important module behavior changes | Update that module's CONTEXT.md |
| Architecture or significant technical decision | Create or update the relevant document in docs/decisions/ |
| API contract changes | Update docs/API_SPEC.md |
| Database schema changes | Update docs/DATABASE.md |
| Coding convention changes | Update docs/PROJECT-RULES.md |

### Rules

- Document implemented behavior and distinguish it from proposals or future ideas.
- Update only documents affected by the change.
- Keep documentation concise and avoid duplicating source code.
- Do not mark unfinished or unverified work as completed.

### Design and Implementation States

| State | Meaning |
|---|---|
| Draft / Proposed | Initial idea, example, assumption, or working default; open to revision during feature work |
| Agreed / Planned | Agreed behavior for the relevant scope, not yet implemented |
| Implemented | Code exists; identify verification still missing |
| Verified | A named check was actually run with recorded results and limits; evidence applies to the checked state |
| Deferred | Outside the current implementation scope |

An Accepted ADR records a decision, not implementation or verification. A decision can later be superseded with a dated explanation. Do not require all project designs to be finalized before building a useful increment.

### Learning and Decision Workflow

1. **Read and compare:** before the affected work, inspect relevant documentation, source/configuration, and available check evidence. Separate inherited template code, current facts, agreed expectations, and draft targets. Code is evidence of existing behavior, not proof that it is the intended behavior.
2. **Explain the next slice:** summarize what the requested feature should do and a few concrete acceptance examples in plain Vietnamese. Explain consequential choices at the user's learning level; avoid unnecessary terminology or a complete up-front project design.
3. **Handle routine details:** continue work already requested and resolve ordinary implementation details within scope. Reuse previously agreed decisions; state reversible draft assumptions where needed. Do not ask for authorization again after every class, layer, file, or standard check.
4. **Discuss material differences:** when a new choice affects behavior, data/history, API compatibility, permissions, or architecture, state the document expectation, observed code/result, recommended change, tradeoff, affected documents, and acceptance check. Ask one or two focused questions when needed. Wait for agreement only on the dependent change, while continuing independent authorized work.
5. **Record agreement automatically:** once the user agrees, update only the affected sections using the mapping above, without another request for each Markdown file. Mark agreed but unbuilt behavior Planned, unresolved choices Draft/TBD, and implementation/check evidence separately. The latest explicit user direction can revise an earlier draft; do not silently turn an AI assumption into a user decision.
6. **Implement within authorization:** a request to implement a feature authorizes its necessary routine work. A discussion or documentation-only request does not authorize new application code or unrelated features. If a material conflict appears midway, return to step 4 for that part rather than redefining the requirement from the current code.
7. **Verify and synchronize:** run checks appropriate to the change and environment, update actual progress, and report what changed, why, checks/results, and remaining decisions. Never claim a test pass from file existence or stale reports.

### Maintain Current Descriptions and History

- Keep the current API/schema/architecture/context sections accurate after an agreed change; avoid indefinitely stacking addenda that contradict the main description.
- Preserve prior significant decisions, authorship, and reasons in ADR history or dated module Refactor Log entries. Updating current module context is allowed; leaving known-obsolete information as current is not the goal of history preservation.
- Use the current conventions consistently. Propose a justified revision when learning or evidence shows a better fit; do not redesign unrelated code merely because the conventions are revisable.
- PROJECT-STATUS.md tracks actual progress and evidence, not hypothetical completion. Do not rewrite facts simply to match a plan.
- Keep AGENTS.md, docs/PROJECT-OVERVIEW.md, and relevant shared skills aligned with this workflow. Prefer brief chat explanations and existing documents over extra process files.
