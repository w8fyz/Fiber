---
name: fiber-framework
description: >-
  Work with the Fiber Java web framework: create controllers, endpoints, authentication,
  sessions, validation, security, middleware, file uploads, and email. Use when the user
  works with FiberServer, @Controller, @RequestMapping, ResponseEntity, AuthenticationService,
  SessionService, or any sh.fyz.fiber class.
---

# Fiber Framework

Fiber is a Java 21+ RESTful API framework built on Jetty 11 with annotation-driven routing, built-in JWT authentication, OAuth2, server-side sessions, CORS, CSRF, rate limiting, validation, file uploads, and email templating.

## Project Structure

```
src/main/java/sh/fyz/fiber/
├── FiberServer.java                            # Entry point, server lifecycle, service registry
├── annotations/
│   ├── auth/
│   │   ├── IdentifierField.java                # Marks login identifier fields on user entity
│   │   └── PasswordField.java                  # Marks BCrypt password field on user entity
│   ├── dto/
│   │   └── IgnoreDTO.java                      # Exclude field from DTO serialization
│   ├── params/
│   │   ├── AuthenticatedUser.java              # Inject current authenticated user
│   │   ├── CurrentSession.java                 # Inject current FiberSession
│   │   ├── FileUpload.java                     # File upload with size/MIME constraints
│   │   ├── Param.java                          # Query string parameter
│   │   ├── PathVariable.java                   # URL path variable
│   │   └── RequestBody.java                    # JSON body deserialization
│   ├── request/
│   │   ├── Controller.java                     # @Controller("/base-path")
│   │   └── RequestMapping.java                 # @RequestMapping(value, method, description)
│   └── security/
│       ├── AuthType.java                       # Accepted auth schemes for an endpoint
│       ├── NoCors.java                         # Skip CORS headers
│       ├── NoCSRF.java                         # Skip CSRF check
│       ├── Permission.java                     # Required permissions
│       └── RequireRole.java                    # Required role(s)
├── config/
│   └── FiberConfig.java                        # JWT secret, token validity, refresh validity
├── core/
│   ├── EndpointRegistry.java                   # Route → EndpointHandler mapping
│   ├── ErrorResponse.java                      # Standardized JSON error responses
│   ├── JwtUtil.java                            # JWT generation/validation (HS256, JJWT)
│   ├── ResponseEntity.java                     # Typed HTTP response builder
│   ├── authentication/
│   │   ├── AuthenticationService.java          # Abstract: login, cookies, token lifecycle
│   │   ├── AuthCookieConfig.java               # Cookie attributes (SameSite, Secure, etc.)
│   │   ├── AuthMiddleware.java                 # Legacy request-attribute auth helper
│   │   ├── AuthResolver.java                   # Routes request to matching Authenticator
│   │   ├── AuthScheme.java                     # Enum: BEARER, COOKIE, BASIC
│   │   ├── Authenticator.java                  # Interface: scheme() + authenticate(request)
│   │   ├── RoleRegistry.java                   # Register and query Role classes
│   │   ├── SameSitePolicy.java                 # Enum: STRICT, LAX, NONE
│   │   ├── entities/
│   │   │   ├── Role.java                       # Abstract role with permissions + hierarchy
│   │   │   ├── UserAuth.java                   # Interface: getId(), getRole(), session helpers
│   │   │   └── UserFieldUtil.java              # Reflection on @IdentifierField/@PasswordField
│   │   ├── impl/
│   │   │   ├── BasicAuthenticator.java         # OAuth2 client_id/client_secret via Basic
│   │   │   ├── BearerAuthenticator.java        # Authorization: Bearer <jwt>
│   │   │   └── CookieAuthenticator.java        # access_token cookie
│   │   └── oauth2/                             # OAuth2 provider + client system
│   ├── challenge/                              # Challenge/verify flow (email confirm, etc.)
│   ├── dto/
│   │   ├── DTOCache.java
│   │   └── DTOConvertible.java                 # Interface: asDTO() auto-serialization
│   ├── email/
│   │   ├── Email.java                          # Email model (to, subject, html, attachments)
│   │   ├── EmailService.java                   # SMTP sender (async, templates)
│   │   └── EmailTemplateEngine.java            # {var} substitution, @import, tables
│   ├── security/
│   │   ├── BCryptUtil.java
│   │   ├── annotations/
│   │   │   ├── AuditLog.java                   # @AuditLog(action, logParameters, mask...)
│   │   │   └── RateLimit.java                  # @RateLimit(attempts, timeout, unit)
│   │   ├── cors/CorsService.java               # Fluent CORS configuration
│   │   ├── csrf/CsrfController.java            # GET /internal/csrf/token
│   │   ├── filters/SecurityHeadersFilter.java  # Auto security headers
│   │   ├── interceptors/RateLimitInterceptor.java
│   │   ├── logging/AuditLogProcessor.java, AuditContext.java
│   │   └── processors/
│   │       ├── PermissionProcessor.java
│   │       └── RateLimitProcessor.java
│   ├── session/
│   │   ├── FiberSession.java                   # JPA entity: id, userId, ip, ua, timestamps
│   │   ├── SessionContext.java                 # ThreadLocal<FiberSession> per-request
│   │   └── SessionService.java                 # CRUD, invalidation, cleanup
│   └── upload/
│       ├── FileUploadManager.java              # Singleton, chunked upload tracking
│       └── UploadedFile.java                   # File wrapper: moveTo, cleanup, getInputStream
├── handler/
│   ├── EndpointHandler.java                    # Per-route handler: security → params → invoke
│   ├── FiberErrorHandler.java                  # Jetty error handler (JSON)
│   ├── ParameterResolver.java                  # Resolves method arguments from request
│   ├── ResponseWriter.java                     # Writes result to HttpServletResponse
│   ├── RouterServlet.java                      # Single servlet dispatching all requests
│   ├── SecurityPipeline.java                   # CSRF → auth → permissions chain
│   ├── SecurityResult.java                     # Pipeline output (user, app, proceed)
│   └── parameter/
│       ├── ParameterHandler.java               # Interface: canHandle + handle
│       ├── ParameterHandlerRegistry.java       # Handler discovery and registration
│       ├── AuthenticatedUserParameterHandler.java
│       ├── SessionParameterHandler.java        # @CurrentSession / FiberSession
│       ├── FileUploadParameterHandler.java
│       ├── OAuth2ApplicationInfoParameterHandler.java
│       ├── PathVariableParameterHandler.java
│       ├── QueryParameterHandler.java
│       ├── RequestBodyParameterHandler.java
│       └── ServletParameterHandler.java        # HttpServletRequest/Response injection
├── middleware/
│   ├── Middleware.java                         # Interface: priority() + handle()
│   └── impl/CsrfMiddleware.java
├── util/
│   ├── FiberObjectMapper.java                  # Pre-configured Jackson ObjectMapper
│   ├── HttpUtil.java                           # Proxy-aware IP resolution
│   ├── JsonUtil.java                           # toJson / fromJson helpers
│   └── RandomUtil.java
└── validation/
    ├── Email.java, Min.java, NotBlank.java, NotNull.java  # Annotations
    ├── ValidationInitializer.java              # Registers built-in validators
    ├── ValidationProcessor.java                # Field-level validation (standalone)
    ├── ValidationRegistry.java                 # Extensible validator registry
    └── validators/                             # EmailValidator, MinValidator, etc.
```

## Server Bootstrap

```java
FiberServer server = new FiberServer(8080);          // port only
FiberServer server = new FiberServer(8080, true);    // port + enable API docs
server.enableDevelopmentMode();                      // relaxes CORS/cookies for local dev
```

Key configuration methods (call before `start()`):
- `setAuthService(AuthenticationService<?>)` — JWT auth
- `setSessionService(SessionService)` — server-side sessions (optional)
- `setOAuthService(OAuth2AuthenticationService<?>)` — OAuth2 provider auth
- `setOauthClientService(OAuth2ClientService)` — OAuth2 client credentials
- `setCorsService(CorsService)` — CORS policy
- `enableCSRFProtection()` — enables CSRF middleware + token endpoint
- `setAuditLogService(AuditLogService)` — audit log backend
- `setEmailService(EmailService)` — SMTP email
- `setServerHeader(String)` — custom Server response header
- `getRoleRegistry().registerRoleClasses(...)` — role/permission system
- `addMiddleware(Middleware)` — global middleware
- `registerController(Object)` or `registerController(Class<?>)` — register controllers
- `preloadDto()` — pre-cache DTOConvertible fields for faster first requests
- `start()` / `stop()`

Singleton access after construction: `FiberServer.get()`.

## Creating Controllers

```java
@Controller("/api/users")
public class UserController {

    @RequestMapping(value = "/", method = RequestMapping.Method.GET, description = "List users")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(userRepository.all());
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    public ResponseEntity<?> getById(@PathVariable("id") long id) {
        User user = userRepository.findById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound();
    }

    @RequestMapping(value = "/", method = RequestMapping.Method.POST)
    public ResponseEntity<?> create(@RequestBody User user) {
        userRepository.save(user);
        return ResponseEntity.created(user);
    }
}
```

Controllers can be registered by class (`server.registerController(UserController.class)` — requires no-arg constructor) or by instance (`server.registerController(new UserController(deps))` — for dependency injection).

### HTTP Methods

`RequestMapping.Method` enum: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS`.

### Path Matching

- Static: `/users/me`
- Parameterized: `/users/{id}`
- Wildcard: `/docs/css/*`
- Paths are normalized: `//` → `/`, trailing slashes removed.
- When multiple patterns match, the most specific (fewest path variables) wins.

## Parameter Injection

Parameters are resolved in order by the first matching `ParameterHandler`:

1. `HttpServletRequest` / `HttpServletResponse` — injected directly
2. `@RequestBody` — JSON deserialization + validation
3. `@Param("name")` — query string, with type conversion and optional `required = false`
4. `@PathVariable("name")` — from URL path segment
5. `@AuthenticatedUser` — resolved by SecurityPipeline (not via handler registry)
6. `@CurrentSession` or `FiberSession` type — current session from SessionContext
7. `@FileUpload` — multipart file upload
8. `OAuth2ApplicationInfo` — OAuth2 client via Basic auth

### Type Conversion

`@Param` and `@PathVariable` auto-convert to: `String`, `int`/`Integer`, `long`/`Long`, `double`/`Double`, `boolean`/`Boolean`.

## ResponseEntity

```java
ResponseEntity.ok(body)                          // 200
ResponseEntity.created(body)                     // 201
ResponseEntity.noContent()                       // 204
ResponseEntity.badRequest(body)                  // 400
ResponseEntity.unauthorized(body)                // 401
ResponseEntity.notFound()                        // 404
ResponseEntity.gone(body)                        // 410
ResponseEntity.tooManyRequest(body)              // 429
ResponseEntity.serverError(body)                 // 500

// Fluent API
ResponseEntity.ok(bytes).contentType("image/png").header("X-Custom", "value");
```

Body handling:
- `byte[]` → raw binary via `OutputStream` (for images, files, etc.)
- `String` → wrapped in `{"uri":..., "status":..., "message":...}`
- Any other object → Jackson JSON serialization

## Authentication

### AuthenticationService

Extend `AuthenticationService<T extends UserAuth>`:

```java
public class MyAuthService extends AuthenticationService<User> {
    public MyAuthService(GenericRepository<User> repo) {
        super(repo, "/auth");  // refresh token cookie path
    }
    // Or with custom cookie config:
    // super(repo, "/auth", new AuthCookieConfig().setSameSite(SameSitePolicy.LAX));
}
```

Key methods inherited:
- `getUserById(Object id)` — load from repository (Caffeine-cached, 30s TTL)
- `findUserByIdentifer(String identifier)` — looks up `@IdentifierField` fields
- `validateCredentials(UserAuth user, String password)` — BCrypt compare
- `doesIdentifiersAlreadyExists(UserAuth user)` — uniqueness check
- `generateToken(UserAuth user, HttpServletRequest req)` — JWT access token
- `validateToken(String token, HttpServletRequest req)` — JWT validation
- `setAuthCookies(UserAuth user, req, resp)` — sets access_token + refresh_token cookies, creates session if SessionService configured
- `clearAuthCookies(req, resp)` — clears cookies, invalidates current session
- `evictUser(Object id)` — invalidate a single user from cache (call after save/update/delete)
- `evictAllUsers()` — invalidate the entire user cache

#### User Cache

`getUserById()` is backed by a Caffeine cache (30s TTL, 10k max entries). The cache is transparent — first call hits DB, subsequent calls serve from memory.

**IMPORTANT**: When you save/update/delete a user outside of `AuthenticationService`, you MUST call `evictUser(id)` to keep the cache in sync:

```java
userRepository.save(user);
authService.evictUser(user.getId()); // keep cache consistent
```

The cache can be customized by overriding `buildUserCache()` in your `AuthenticationService` subclass. Return `null` to disable caching entirely.

### UserAuth Interface

```java
public interface UserAuth {
    Object getId();
    String getRole();

    // Session methods (require SessionService):
    default FiberSession getSession() { ... }
    default List<FiberSession> getSessions() { ... }
    default void invalidateSession(String sessionId) { ... }
    default void invalidateAllSessions() { ... }
    default void invalidateOtherSessions() { ... }
}
```

### Authenticators

Register on `FiberServer.get().getAuthResolver()`:
- `CookieAuthenticator` — reads `access_token` cookie
- `BearerAuthenticator` — reads `Authorization: Bearer <token>` header
- `BasicAuthenticator` — for OAuth2 client credentials (auto-detected)

### @AuthType

Control which auth schemes an endpoint accepts:

```java
@AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
@RequestMapping(value = "/api/data", method = RequestMapping.Method.GET)
public ResponseEntity<?> data(@AuthenticatedUser User user) { ... }
```

Default (no `@AuthType`): if `@AuthenticatedUser` is present → `COOKIE` only.

### Roles

```java
public class AdminRole extends Role {
    public AdminRole() { super("admin"); }
    @Override protected void initializePermissions() {
        addPermission("users.manage");
        addPermission("settings.edit");
    }
    @Override protected void initializeParentRoles() {
        addParentRole("user"); // inherits all "user" permissions
    }
}
```

Registration: `server.getRoleRegistry().registerRoleClasses(UserRole.class, AdminRole.class)`.

Usage: `@RequireRole("admin")` or `@Permission({"users.manage"})`.

## Sessions

Optional server-side session tracking. Without it, auth is pure stateless JWT.

### Setup

```java
// FiberSession is a JPA entity — Architect handles the table
GenericRepository<FiberSession> sessionRepo = new GenericRepository<>(FiberSession.class);
server.setSessionService(new SessionService(sessionRepo));
// Custom TTL: new SessionService(sessionRepo, 30 * 24 * 3600 * 1000L)
```

### How It Works

1. `setAuthCookies()` creates a `FiberSession` in DB (+ puts it in cache) and embeds `sessionId` in the JWT claims.
2. On each request, `CookieAuthenticator`/`BearerAuthenticator` extracts `sessionId`, loads the session (from Caffeine cache or DB), checks `active` + `expiresAt`.
3. If invalid → 401. If valid → `SessionContext.set(session)`.
4. `SessionContext.clear()` is called in `RouterServlet.finally` to prevent thread-local leaks.

### Session Cache

`getSession()` is backed by a Caffeine cache (30s TTL, 50k max entries). The cache is automatically invalidated on `invalidate()`, `invalidateAllForUser()`, `invalidateOtherSessions()`, and populated on `createSession()` (write-through). No manual eviction needed — all `SessionService` methods handle cache consistency internally.

### FiberSession Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | UUID primary key |
| `userId` | `String` | Owner user ID |
| `ipAddress` | `String` | Client IP at creation |
| `userAgent` | `String` | User-Agent at creation |
| `createdAt` | `long` | Timestamp |
| `lastAccessedAt` | `long` | Updated on access |
| `expiresAt` | `long` | Expiration timestamp |
| `active` | `boolean` | Set to false on invalidation |

### SessionService API

- `createSession(UserAuth user, HttpServletRequest req)` — called automatically by `setAuthCookies`
- `getSession(String sessionId)` — returns null if expired or inactive
- `getUserSessions(Object userId)` — all active sessions
- `invalidate(String sessionId)`
- `invalidateAllForUser(Object userId)`
- `invalidateOtherSessions(Object userId, String keepSessionId)`
- `touchSession(String sessionId)` — update lastAccessedAt
- `cleanupExpired()` — runs automatically every hour

### Inject in Controller

```java
@RequestMapping(value = "/session-info", method = RequestMapping.Method.GET)
public ResponseEntity<?> info(@CurrentSession FiberSession session) {
    return ResponseEntity.ok(Map.of(
        "sessionId", session.getSessionId(),
        "ip", session.getIpAddress(),
        "created", session.getCreatedAt()
    ));
}
```

## Validation

### Built-in Annotations

| Annotation | Target | Behavior |
|------------|--------|----------|
| `@NotNull` | Field, Parameter | Must not be null |
| `@NotBlank` | Field, Parameter | String must not be null/empty/blank |
| `@Min(value)` | Field, Parameter | Number must be >= value |
| `@Email` | Field, Parameter | Must match email regex |

Validation runs automatically on `@RequestBody` deserialization and `@Param`/`@PathVariable` parameters. Failures return 400 with error messages.

### Custom Validators

```java
public class MyValidator implements Validator<MyAnnotation> {
    @Override public Class<MyAnnotation> getAnnotationType() { return MyAnnotation.class; }
    @Override public boolean isValid(Object value) { ... }
    @Override public String getMessage() { return "Invalid value"; }
}

// Register at startup
ValidationRegistry.register(new MyValidator());
```

## Security

### CORS

```java
server.setCorsService(new CorsService()
    .addAllowedOrigin("https://myapp.com")
    .addAllowedOrigin("https://*.myapp.com")   // wildcard patterns
    .setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
    .setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN"))
    .setAllowCredentials(true)
    .setMaxAge(3600));
```

Skip CORS for specific endpoints: `@NoCors`.

### CSRF

`server.enableCSRFProtection()` activates:
- XSRF-TOKEN cookie (auto-regenerated)
- X-CSRF-TOKEN header validation on unsafe methods
- `GET /internal/csrf/token` endpoint

Skip for specific endpoints: `@NoCSRF`.

### Rate Limiting

Annotation-based rate limiting with two algorithms (fixed window, sliding window), keyed by IP or by authenticated user.

```java
// Basic — fixed window, IP-based
@RateLimit(attempts = 10, timeout = 1, unit = TimeUnit.MINUTES)

// Sliding window — more accurate for bursty traffic
@RateLimit(attempts = 100, timeout = 1, unit = TimeUnit.HOURS, slidingWindow = true)

// Per-user — falls back to IP if unauthenticated
@RateLimit(attempts = 10, timeout = 5, unit = TimeUnit.MINUTES, perUser = true)

// Shared bucket across endpoints
@RateLimit(attempts = 50, timeout = 1, unit = TimeUnit.HOURS, key = "writes")

// Class-level — applies to all methods in the controller
@Controller("/api/heavy")
@RateLimit(attempts = 20, timeout = 1, unit = TimeUnit.MINUTES)
public class HeavyController { ... }
```

Annotation attributes:
- `attempts` (int, default 5) — max requests per window
- `timeout` (int, default 15) — window duration
- `unit` (TimeUnit, default MINUTES) — time unit
- `message` (String) — custom 429 error message
- `slidingWindow` (boolean, default false) — use sliding window algorithm
- `perUser` (boolean, default false) — key by userId instead of IP
- `key` (String) — group endpoints under the same rate-limit bucket

Returns 429 with `Retry-After` header and JSON body `{"status":429, "message":"...", "retryAfter": <seconds>}`.

Proxy-aware (X-Forwarded-For, X-Real-IP). Resets on 200 response. `RateLimitInterceptor.clearAll()` for testing.

### Audit Logging

Track security-relevant actions with `@AuditLog`. Logs go to SLF4J and optionally to a custom `AuditLogService`.

```java
@AuditLog(action = "USER_LOGIN", logParameters = true, logResult = true, maskSensitiveData = true)
```

Annotation attributes:
- `action` (String, required) — action identifier in the log
- `logParameters` (boolean, default true) — include method parameters
- `logResult` (boolean, default true) — include response body
- `maskSensitiveData` (boolean, default true) — mask `@PasswordField` fields and keys matching password/secret/token

#### AuditContext — Custom Data

Use `AuditContext` to attach custom key-value data from inside any `@AuditLog` endpoint:

```java
@AuditLog(action = "PAYMENT")
@RequestMapping(value = "/pay", method = RequestMapping.Method.POST)
public ResponseEntity<?> pay(@RequestBody PaymentReq req) {
    Payment result = service.process(req);
    AuditContext.put("paymentId", result.getId());
    AuditContext.put("amount", result.getAmount());
    return ResponseEntity.ok(result);
}
```

`AuditContext` is thread-local, automatically cleared after each request. Available via `AuditContext.put(key, value)`, `AuditContext.get(key)`, `AuditContext.getAll()`.

#### Custom AuditLogService

```java
public class MyLogService extends AuditLogService {
    @Override
    public void onAuditLog(AuditLog log) {
        // log.getAction(), log.getIp(), log.getStatus(), log.getCustomData(), etc.
        db.save(log);
    }
}
server.setAuditLogService(new MyLogService());
```

`AuditLog` DTO fields: `timestamp`, `ip`, `userAgent`, `method`, `uri`, `action`, `status`, `queryParams`, `pathVariables`, `requestBody`, `parameters`, `response`, `customData` (Map from AuditContext), `rawLog` (full JSON string).

### Security Headers (automatic)

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Content-Security-Policy: default-src 'self'`
- `Cross-Origin-Opener-Policy: same-origin`

## File Uploads

```java
@RequestMapping(value = "/upload", method = RequestMapping.Method.POST)
public ResponseEntity<?> upload(
        @FileUpload(maxSize = 10_485_760, allowedMimeTypes = {"image/*", "application/pdf"})
        UploadedFile file) {

    file.moveTo(Path.of("uploads/" + file.getOriginalFilename()));
    return ResponseEntity.ok(Map.of("name", file.getOriginalFilename(), "size", file.getSize()));
}
```

Chunked uploads: client sends `uploadId`, `chunkIndex`, `totalChunks` as query params. Chunks are assembled server-side. Incomplete uploads are auto-cleaned after 24h.

Filenames are sanitized (path separators, special characters removed).

## Email

```java
EmailService emailService = new EmailService(
    "smtp.example.com", "noreply@example.com", 465,
    "username", "password", true, true);
server.setEmailService(emailService);

Email email = new Email();
email.setTo("user@example.com");
email.setSubject("Welcome!");
email.setHtmlContent("<h1>Hello {name}!</h1>");
email.setTemplatePath("templates/welcome.html");
email.setTemplateVariables(Map.of("name", "John"));

FiberServer.get().getEmailService().sendEmail(email); // returns CompletableFuture<Void>
```

Templates support `{variable}` substitution and `@import="path/to/partial.html"@` includes.

## Middleware

```java
public class TimingMiddleware implements Middleware {
    @Override public int priority() { return 5; } // lower = runs earlier

    @Override
    public boolean handle(HttpServletRequest req, HttpServletResponse resp) {
        req.setAttribute("startTime", System.currentTimeMillis());
        return true; // false would abort the request
    }
}

server.addMiddleware(new TimingMiddleware());
```

Middleware executes after authentication/permissions but before parameter resolution. Register middleware before controllers.

## Challenges

For flows requiring user verification (email confirmation, 2FA, etc.):

```java
Challenge challenge = server.registerChallenge(myChallenge, new ChallengeCallback() {
    @Override public void onSuccess(Challenge c, HttpServletRequest req, HttpServletResponse resp) {
        // Verification succeeded
    }
    @Override public void onFailure(Challenge c, String reason, HttpServletRequest req, HttpServletResponse resp) {
        // Verification failed
    }
});
```

Verify via `POST /internal/challenge/verify/{challengeId}` with JSON body containing the response.

## DTOConvertible

Auto-serialize entities excluding sensitive fields:

```java
@Entity @Table(name = "users")
public class User implements IdentifiableEntity, UserAuth, DTOConvertible {
    private String name;
    private String email;
    @IgnoreDTO private String password; // excluded from asDTO()

    // asDTO() returns Map<String, Object> of all non-@IgnoreDTO, non-null fields
}
```

Pre-cache at startup: `server.preloadDto()`.

## Key Rules

- Always call `server.start()` after all configuration is done.
- Register authenticators on `server.getAuthResolver()` before handling requests.
- Middleware added after controller registration won't apply to already-registered endpoints.
- `SessionService` is optional — without it, auth is pure stateless JWT and session methods on `UserAuth` throw `IllegalStateException`.
- Both `AuthenticationService` and `SessionService` use Caffeine caches (30s TTL). `getUserById()` requires manual `evictUser(id)` after mutations outside the service. `SessionService` invalidates its cache automatically on all write operations.
- JWT secret must be at least 32 characters in production (auto-generated if default/short).
- `FiberServer.get()` is available after the constructor returns (singleton).
- Path variables use `{name}` syntax and are extracted as named regex groups.
- `@RequestBody` validation errors return 400 automatically.
- `@RateLimit` counter resets on successful (200) responses. Supports `slidingWindow`, `perUser`, `key` for shared buckets, and class-level application.
- `@AuditLog` supports custom data via `AuditContext.put(key, value)` inside endpoints. `AuditContext` is cleared automatically per request.
- File upload filenames are automatically sanitized against path traversal.

## Installation

### Gradle

```groovy
dependencies {
    implementation 'sh.fyz:Fiber:2.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>sh.fyz</groupId>
    <artifactId>Fiber</artifactId>
    <version>2.0.0</version>
</dependency>
```