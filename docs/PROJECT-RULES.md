# Project Rules — Study Together

> Shared coding conventions for developers and AI tools.
> Professional Java/Spring practice: prefer idiomatic code, framework capabilities, and design proportionate to actual requirements. Explain choices separately from implementation. Design documents and Proposed ADRs remain evolving starting points; use section 17 to track agreement and evidence.
---

## 0. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Backend | Spring Boot 4.x / Spring Framework 7 |
| Security | Spring Security 7, Discord OAuth2 login, JWT via OAuth2 Resource Server |
| Database | PostgreSQL + Spring Data JPA |
| Frontend | React |
| Discord Bot | JDA, running within the Spring Boot backend |
| Build | Maven |
| Test | JUnit 6 + Mockito + MockMvc |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |

### Design and Implementation Defaults

- These are this project's conventions, not a claim that every professional team uses the same style. Apply them consistently and adapt when requirements justify a different design.
- Use established Java/Spring/library features before writing equivalent infrastructure. Prefer Lombok for mechanical accessors, constructors, and loggers; retain explicit code when it carries behavior or configuration.
- Apply KISS and YAGNI: implement current requirements with sufficient design for correctness and change. Avoid speculative factories, generic base services/controllers, extra layers, microservices, or CQRS without a concrete need.
- Apply DRY to repeated knowledge and behavior. Extract cohesive abstractions when repetition warrants them; similar-looking code alone does not require a shared framework.
- Use SOLID to assess responsibilities and dependency boundaries. Business services follow the agreed Service/ServiceImpl convention in section 3; other classes do not require interfaces or patterns by default. Prefer composition; use inheritance for a real shared contract or framework mapping need.
- Keep validation, ownership, transaction boundaries, database constraints, and failure handling deliberate. Reducing boilerplate must preserve these behaviors.
- Use streams, Optional, builders, and modern language features when they fit the operation. Avoid compressed expressions, redundant comments, and wrapper methods that add no useful responsibility.
- Put learning explanations in chat or focused documentation. Code comments should capture non-obvious intent, constraints, and tradeoffs rather than narrate Java syntax.
- Use the layer-based package layout agreed on 2026-10-31: entity, repository, service, service.impl, enums, and discord. Evolve business contracts with actual use cases; package organization alone does not authorize unrelated behavior or architecture changes.

### Optional Additions and Explanation

- Write practical, idiomatic code for the selected feature. Distinguish framework requirements and agreed functional/data rules from optional design preferences; do not call an optional mechanism mandatory or justify it solely as "professional practice".
- Defer new optional safeguards, access restrictions, null-check boilerplate, abstractions, patterns, and layers by default. A possible future mistake alone is not a reason to add them now.
- Demo priority (2026-10-31): finish normal feature flows first. Save concrete advanced suggestions in [DEFERRED-IMPROVEMENTS.md](./DEFERRED-IMPROVEMENTS.md), including their problem, omission consequence, revisit trigger, and tradeoff. Continue demo work without adding those mechanisms or repeatedly asking to implement them; revisit after the demo when the user selects them. Preserve required atomic writes, data constraints, and permissions for the current feature.
- Codex is responsible for judging current necessity and recommending the learning sequence; do not ask the learner to know which mechanisms are basic or advanced. Classify by the selected flow's actual needs and evidence, not by unfamiliar names or code length. Build and verify a runnable normal flow first, then proactively recommend one relevant issue to investigate at a time. Explain a concrete scenario/reproduction, the root cause, the consequence of doing nothing, alternatives and their tradeoffs, and why the recommended approach fits now. This can follow each runnable increment; it need not wait for the entire product to be finished. Record proposals and agree material behavior/scope changes before implementing them.
- If proposing an optional addition, explain before implementation in plain Vietnamese: the concrete problem it solves; why it is optional or required; what happens if it is omitted in the current code; the extra code/complexity or behavior it introduces; and whether it can wait. Separate observed problems from hypothetical future risks.
- Wait for the user's agreement before implementing that new optional addition. While waiting, continue independent work already authorized. An explanation after adding it is not prior agreement.
- Do not ask again for existing agreed conventions or routine implementation details within the requested scope. Required behavior and data constraints remain part of the current feature; if a requirement is unclear, discuss the affected choice rather than silently dropping it or adding a speculative mechanism.
- Example: JPA requires a public or protected no-args constructor; choosing protected access is optional. Explain that it limits calls from outside the package, while public access remains valid, before proposing that restriction. The current agreed default is public Lombok constructors.

Technical references: [Lombok accessors](https://projectlombok.org/features/GetterSetter), [constructors](https://projectlombok.org/features/constructor), [logging](https://projectlombok.org/features/log), [Spring constructor injection](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html), and [transaction advice](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).

---

## 1. Package Structure

Structure under `src/main/java/<base-package>/`:

```text
├── StudyCommunityApplication.java
├── entity/                     # JPA entity mappings
├── repository/                 # Spring Data repositories
├── service/                    # Business service interfaces
│   └── impl/                   # Implementations of service interfaces
├── controller/                 # REST controllers, when implemented
├── dto/                        # Request/response records, when implemented
│   ├── user/                   # User DTOs, when needed
│   └── study/                  # Study DTOs, when needed
├── enums/                      # One enum per file
├── discord/                    # Bot integration
│   ├── config/
│   └── listener/
├── persistence/                # Shared ORM base types
├── config/                     # Security, JWT, CORS, OpenAPI configuration
├── security/                   # Current user and authentication helpers
├── exception/                  # Custom exceptions and global handler
└── util/                       # Shared stateless utilities
```

Create packages only when they contain implementation. The tree includes planned locations, not a claim that controllers, DTOs, security, and other future components exist. Business contexts span layers and live in `docs/contexts/<domain>/CONTEXT.md`.

Configuration files belong in `src/main/resources/`:

```text
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── application-test.yml
```

### Package Rules

- Group application code by layer: entity, repository, service, and controller. Service interface implementations live under service.impl.
- Keep request/response records under dto; use dto/user and dto/study when those types are introduced. Shared response/pagination records may live directly under dto.
- Keep enums under enums, one enum per file, with meaningful names such as UserStatus, IdentityProvider, and StudySource. Package moves do not change enum values stored in SQL.
- Keep shared configuration and security components at the top level.
- The Discord bot runs within the Java backend under discord/, with its configuration and listener subpackages.
- Discord listeners delegate business logic to services.
- Create classes and packages only when needed; do not create empty directories for planned layers.
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
| Repository | Spring Data convention | `findByProviderAndProviderSubject()`, `existsByChannelId()` |

### Variables and Packages

- Use `camelCase` for fields, variables, and methods.
- Use `UPPER_SNAKE_CASE` for constants.
- Use lowercase package names: `studysession`, `leaderboard`.
- Use meaningful names: `studySessionRepository`, not `ssRepo`.
- Boolean methods use `is`, `has`, or `can`: `isActive()`, `hasPermission()`.
- Boolean fields use clear names such as `active` or `cameraEnabled`.
- Use English names consistently throughout the code.

---

## 3. Service Layer

### Convention: Service Interface and Implementation

Examples illustrate coding conventions, not implemented APIs or complete business logic.

```java
import lombok.RequiredArgsConstructor;

// service/StudySessionService.java
public interface StudySessionService {
    void startSession(String discordId, String channelId);
    void endSession(String discordId);
}

// service/impl/StudySessionServiceImpl.java
@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl implements StudySessionService {

    private final StudySessionRepository studySessionRepository;

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

- User-confirmed convention (2026-10-31): every business service uses a `[Feature]Service` interface in `service/` and a `[Feature]ServiceImpl` class in `service/impl/`, even with one implementation. Apply consistently to existing and new services; this supersedes the earlier concrete-service default.
- Put public business operations on the interface and implementation details in the Impl. Both UserIdentityService and StudySessionService follow this structure. It is a project organization choice, not a Spring requirement or a reason to create a service for every entity.
- Put `@Service`, `@RequiredArgsConstructor`, and transaction annotations on the implementation; mark interface implementations with `@Override`.
- Controllers, Discord listeners, and collaborating services inject the interface. Integration tests register the implementation bean and resolve/call the interface.
- Use constructor injection with `private final` dependencies; prefer `@RequiredArgsConstructor` for simple assignment. Write the constructor when parameter qualifiers, custom initialization, or other behavior requires it; do not assume Lombok copies injection annotations to parameters.
- Put `@Transactional` on concrete service methods that require transactional writes. Calls must go through Spring's proxy for proxy-based transaction advice; a method calling another method on the same instance does not start a new advised transaction.
- Use ordinary service-level `@Transactional` for the current identity find/create flow, with Lombok `@RequiredArgsConstructor` for repository injection. Automatic concurrent-creation recovery and independent transactions are deferred in DEFERRED-IMPROVEMENTS.md. Do not add TransactionTemplate, REQUIRES_NEW, retry, or extra transaction services for a hypothetical race without agreement; never continue database work in a failed transaction.
- Keep related database changes within one business transaction.
- Use `@Transactional(readOnly = true)` for read operations when needed.
- Keep HTTP concerns out of business services: no `HttpServletRequest`,
  `ResponseEntity`, or `HttpStatus`.
- Convert entities to response DTOs in the service layer for API operations.
- Avoid external network calls inside database transactions.

---

## 4. Controller Layer

```java
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/studyrooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

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
- Use constructor injection with `private final` dependencies; prefer `@RequiredArgsConstructor` for simple assignment.
- Inject the service interface; the corresponding Impl is the Spring-managed implementation.
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
- Use `fromEntity()` for small direct mappings; use a dedicated mapper when mapping complexity or reuse warrants it. A separate mapper or mapping library is not mandatory for every DTO.
- Perform entity-to-DTO conversion within the service's use case, directly or through a mapper; do not leave lazy entity access to API serialization.
- Use builders when many optional fields or construction variants justify them; do not add builders to every record or entity.
- Return DTOs, never JPA entities, from API endpoints.
- Never expose passwords, password hashes, signing keys, or internal credentials.
- Return authentication tokens only through the intended authentication responses.
- Never put `@Entity` or JPA annotations on DTOs.
- Use `Instant` for timestamp fields.

---

## 7. Entity Rules

Entities map database tables; their fields and relationships follow
`docs/DATABASE.md`.

Schema evolution follows ADR-003's latest clarification: finish the main SQL first. Current source is `docs/sql/schema.sql` (Draft), with Flyway disabled, Hibernate `ddl-auto: none`, and automatic SQL initialization disabled. Do not create or enable V1 until the user explicitly finalizes that SQL. After the checkpoint, use versioned SQL migrations and Hibernate validate. Database-mutating tests use fresh isolated PostgreSQL and the draft SQL (later, finalized migrations); never generate schema from entities.

### Rules

- Use `@Entity` and explicit `@Table(name = "...")` with snake_case table names.
- Use `@Column(name = "...")` and `@JoinColumn(name = "...")` for mapping. Define SQL types/lengths, NOT NULL, defaults, uniqueness, index definitions, FK names/actions, and any later checks in migrations, without duplicating DDL metadata in entities. Keep ORM behavior such as `updatable = false`, fetch type, enum encoding, required relationships, ID generation, and audit value generation where needed.
- Identity/session constraint decision (2026-10-31): keep unique `(provider, provider_subject)` and simple user/identity FKs; omit the extra `(user_id, id)` key, composite ownership FK, and explicitly maintained Discord-only provider check. Future services must preserve session/identity ownership. Do not restore the removed defenses without a newly agreed need; other schema constraints are unaffected.
- Subsequent CHECK deferral (2026-10-31): omit handwritten `@CheckConstraint` / Hibernate `@Check` declarations and SQL `CHECK` clauses for now, including status/source, Discord references, interval/checkpoint bounds, and end-reason consistency. Keep intended rules documented as Planned/Deferred; do not claim unbuilt services enforce them. PKs, required columns, unique constraints, FKs, and indexes remain in SQL. Hibernate validate does not generate enum checks or any other DDL.
- Follow the agreed schema for ID types and generation strategies.
- Use `@Enumerated(EnumType.STRING)`, never `ORDINAL`.
- Use `Instant` for event timestamps and `LocalDate` for date-only fields.
- Use `LocalDateTime` only when a local date and time without a time zone is intended.
- Do not use `java.util.Date` or `java.sql.Timestamp` for entity fields.
- Default to plain class-level `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, and `@Setter` on the current entities and mapped superclass. Keep database mappings and field defaults explicit.
- Do not add protected constructors, setter exclusions, `@NonNull`, handwritten null guards, or custom creation methods solely to prevent hypothetical coding mistakes. For any new optional mechanism, explain its purpose and the consequence of omitting it, then obtain agreement before implementation as described in section 0. This user-directed simplification supersedes the earlier accessor/constructor restrictions on 2026-10-30.
- Add handwritten construction or state-transition behavior when the selected feature needs it; generated all-args constructors only assign fields. They do not derive ownership or checkpoint values automatically.
- Use the four explicit Lombok annotations above; add entity equality, diagnostic output, builders, or other generated behavior only when needed.
- Prefer lazy associations; choose fetch joins, projections, or entity graphs for the query's actual needs and check for N+1 queries. Configure cascade and orphan removal only for the intended ownership lifecycle.
- Hibernate manages `createdAt` and `updatedAt` on ORM writes. The current simple entities also expose setters; additional lifecycle restrictions are deferred until needed.
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
- The initial implemented entity baseline permits only DISCORD; this is an incremental starting point. The confirmed product scope (2026-10-30, Planned) includes multiple website login methods, timed web study without Discord, and linking Discord to an existing account. Provider selection and detailed contracts remain draft. Keep login provider separate from study source and linked history under the internal user.
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
// Inside UserIdentityRepository
Optional<UserIdentity> findByProviderAndProviderSubject(
        IdentityProvider provider, String providerSubject);

boolean existsByProviderAndProviderSubject(
        IdentityProvider provider, String providerSubject);
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should reject lookup when the user does not exist")
    void getUserById_notFound_throwsException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(userId));
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
      ddl-auto: none  # Draft phase; SqlSchemaTest explicitly validates its isolated schema

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
- During the draft phase, apply reviewed draft SQL only to isolated test storage and validate mappings explicitly there. Once V1 is finalized, apply migrations instead. Never use Hibernate create-drop/update to generate a test schema.
- Disable real Discord bot startup and mock external network boundaries.
- Test important business outcomes and relevant failure cases.
- Avoid tests of Lombok-generated accessors, trivial records, or repository call sequences that merely mirror implementation. Use integration tests for persistence and framework behavior that mocks cannot establish.
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl implements StudySessionService {
    private final StudySessionRepository studySessionRepository;
    // Business methods use log.debug(...), log.info(...), etc.
}
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

- Use SLF4J through Lombok `@Slf4j` on classes that log. Declare the logger manually only when special configuration requires it.
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
- [ ] Boilerplate uses the agreed Lombok/record defaults; explicit code preserves necessary behavior.
- [ ] Controllers and Discord listeners delegate business logic to services.
- [ ] Business services follow Service/ServiceImpl with interface injection; other patterns and layers serve a concrete requirement.
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
| New business area with non-obvious logic | Create docs/contexts/<domain>/CONTEXT.md |
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

### Implementation and Decision Workflow

1. **Read and compare:** before the affected work, inspect relevant documentation, source/configuration, and available check evidence. Separate inherited template code, current facts, agreed expectations, and draft targets. Code is evidence of existing behavior, not proof that it is the intended behavior.
2. **Explain the next slice:** summarize the intended behavior, important tradeoffs, and relevant acceptance examples in Vietnamese. Use practical implementation conventions and distinguish required behavior from optional additions. Explain unfamiliar concepts separately; do not introduce extra mechanisms for teaching or anticipated future needs. Avoid a complete up-front project design.
3. **Handle routine details:** continue work already requested and resolve ordinary implementation details within scope. Reuse previously agreed decisions; state reversible draft assumptions where needed. Do not ask for authorization again after every class, layer, file, or standard check.
4. **Discuss material differences and optional additions:** when a new choice affects behavior, data/history, API compatibility, permissions, or architecture, explain the expectation, observed code/result, recommended change, tradeoff, affected documents, and acceptance check. Apply section 0 to new optional mechanisms: explain what omission means now and obtain agreement before adding them. Ask focused questions and wait only on the dependent change, while continuing independent authorized work.
5. **Record agreement automatically:** once the user agrees, update only the affected sections using the mapping above, without another request for each Markdown file. Mark agreed but unbuilt behavior Planned, unresolved choices Draft/TBD, and implementation/check evidence separately. The latest explicit user direction can revise an earlier draft; do not silently turn an AI assumption into a user decision.
6. **Implement within authorization:** a request to implement a feature authorizes its necessary routine work. A discussion or documentation-only request does not authorize new application code or unrelated features. If a material conflict appears midway, return to step 4 for that part rather than redefining the requirement from the current code.
7. **Verify and synchronize:** run checks appropriate to the change and environment, update actual progress, and report what changed, why, checks/results, and remaining decisions. Never claim a test pass from file existence or stale reports.

### Maintain Current Descriptions and History

- Keep the current API/schema/architecture/context sections accurate after an agreed change; avoid indefinitely stacking addenda that contradict the main description.
- Preserve prior significant decisions, authorship, and reasons in ADR history or dated module Refactor Log entries. Updating current module context is allowed; leaving known-obsolete information as current is not the goal of history preservation.
- Use the current conventions consistently. Propose a justified revision when learning or evidence shows a better fit; do not redesign unrelated code merely because the conventions are revisable.
- PROJECT-STATUS.md tracks actual progress and evidence, not hypothetical completion. Do not rewrite facts simply to match a plan.
- Keep AGENTS.md, docs/PROJECT-OVERVIEW.md, and relevant shared skills aligned with this workflow. Prefer brief chat explanations and existing documents over extra process files.

### Convention History

- **2026-10-31 — Demo-first scope:** user requested normal flows first and advanced proposals saved for after the demo. Replaced identity TransactionTemplate/independent retry transactions with ordinary Transactional and RequiredArgsConstructor. Required atomicity and SQL uniqueness remain; automatic recovery from concurrent creation is deferred in DEFERRED-IMPROVEMENTS.md.
- **2026-10-31 — Service consistency correction:** the user standardized business services as [Feature]Service interfaces in service/ with [Feature]ServiceImpl implementations in service/impl/, including a single implementation. Consumers inject interfaces; annotations and transaction boundaries stay on implementations. Split StudySessionService accordingly without changing its business logic. Updated AGENTS, creation/review guidance, architecture, examples, and contexts. This supersedes the concrete-service default from 2026-10-30; UserIdentityService is no longer a special exception.
- **2026-10-31 — Learning sequence clarification:** Codex, not the learner, evaluates what the selected flow needs and proactively recommends staged improvements. First reach a runnable flow, then study one concrete problem, reproduce or illustrate it, compare solutions, and explain the recommendation. Saving a backlog must not transfer technical diagnosis or prioritization to the user; runtime verification and implementation remain distinct from proposals.
- **2026-10-31 — Layer-based layout:** at the user's request, replaced feature packages with central entity/repository/service/service.impl/enums packages and a top-level discord integration package. Moved business contexts to docs/contexts/user and docs/contexts/study; tests mirror the new packages. This supersedes the earlier feature-owned enum organization below without changing business behavior or SQL mappings.
- **2026-10-31 — User clarification: finish SQL before V1:** SQL ownership does not mean immediate migration activation. Moved the prematurely versioned SQL back to docs/sql/schema.sql, disabled Flyway, and set application Hibernate to none. Explicit finalization of the main SQL is required before V1/validate activation; entity mapping cleanup remains. Earlier immediate-activation guidance below is superseded.
- **2026-10-31 — SQL migration authority:** user explicitly replaced the earlier Hibernate scaffolding phase with SQL/Flyway migrations. V1 owns schema/types/constraints/indexes, entity DDL declarations are removed, and Hibernate only validates mappings. Isolated PostgreSQL migration/ORM tests replace generated-DDL assertions. ADR-003 preserves the superseded workflow.
- **2026-10-31 — Subsequent CHECK deferral:** the user requested removing all remaining handwritten database checks and revisiting them later. Removed the user-status, five session, and segment-time checks from entities/manual SQL; retained their intended rules in DATABASE.md without claiming runtime enforcement. This supersedes earlier statements that time/state CHECK constraints remain. Uniqueness, simple FKs, required columns, and indexes are unchanged.
- **2026-10-31 — Identity constraints:** applied the previously discussed simplification at the user's request: external-account uniqueness remains, identity/user references are simple FKs, and ownership pairing is delegated to service logic. Removed the extra composite key/FK and named provider check; existing live schemas were not altered. Details and implications are in DATABASE.md.
- **2026-10-31 — Package organization:** retained the single backend and feature-oriented layout, with enums grouped in each feature's `enums` package. This is source organization, not a change to persistence values, business rules, or an adoption of Clean/Hexagonal architecture. Create other components/packages only when needed.
- **2026-10-30 — User-directed revision:** prioritize professional Java/Spring practice over tutorial-style code. Replaced mandatory handwritten entity accessors/JPA no-arg constructors and loggers with Lombok defaults; replaced mandatory service interface/implementation pairs with contracts chosen for actual needs. Kept records, constructor injection, validation, transaction/security boundaries, and meaningful verification. These conventions apply to new or affected work; this documentation change does not refactor existing source.
- **2026-10-30 — Subsequent user clarification:** remove optional entity access/null-check defenses and use plain public Lombok constructors and class-level accessors. Defer extra defensive mechanisms until a concrete need can be demonstrated. Earlier constructor/setter restrictions are superseded; the agreed database schema and required data constraints are unchanged.
- **2026-10-30 — Explanation before optional additions:** user explicitly requested discussion before adding non-required mechanisms, including their purpose and what happens without them. Defer by default; explain current need, omission consequences, and tradeoffs in Vietnamese, then wait for agreement. Continue routine work already authorized without repeated permission requests.
