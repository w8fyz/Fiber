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
│   │   └── oauth2/
│   │       ├── OAuth2Provider.java             # Interface: getProviderId, getAuthorizationUrl, processCallback, mapUserData
│   │       ├── AbstractOAuth2Provider.java     # Base impl: code exchange, userInfo fetch, URL building
│   │       ├── OAuth2AuthenticationService.java # Abstract: state store, provider registry, handleCallback
│   │       ├── OAuth2ApplicationInfo.java      # Record: clientId + clientSecret (for server-side OAuth2)
│   │       ├── OAuth2ApplicationAuthenticator.java # Interface for Basic auth → OAuth2ApplicationInfo
│   │       ├── OAuth2ClientService.java        # Client registration, auth codes, token generation
│   │       ├── OAuth2TokenResponse.java        # Token response DTO
│   │       ├── impl/DiscordOAuth2Provider.java # Built-in Discord provider
│   │       └── client/controller/OAuth2ClientController.java # GET /oauth/client/authorize, POST /oauth/client/token
│   ├── challenge/
│   │   ├── Challenge.java                     # Interface: id, userId, expiry, validateResponse, complete, fail
│   │   ├── AbstractChallenge.java             # Base impl: UUID gen, expiry, metadata, DTOConvertible
│   │   ├── ChallengeCallback.java             # Interface: onSuccess/onFailure → ResponseEntity
│   │   ├── ChallengeStatus.java               # Enum: PENDING, COMPLETED, FAILED, EXPIRED, CANCELLED
│   │   ├── ChallengeRegistry.java             # ConcurrentHashMap storage, validateChallenge, cleanup
│   │   └── internal/ChallengeController.java  # POST /internal/challenge/verify/{challengeID}
│   ├── dto/
│   │   ├── DTOCache.java
│   │   └── DTOConvertible.java                 # Abstract class: asDTO() auto-serialization
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
- `setMaxFileSize(long)` — max file size in bytes (default 50MB), call before `start()`
- `setMaxRequestSize(long)` — max multipart request size in bytes (default 100MB)
- `setFileSizeThreshold(int)` — size in bytes before writing to disk (default 1MB)
- `start()` / `stop()` — both throw checked `Exception`

Singleton access after construction: `FiberServer.get()`.

### Configuration (`FiberConfig`)

Loaded from `fiberconfig.json` (or env variables prefixed `FIBER_`). Fields:

| Field | Default | Description |
|-------|---------|-------------|
| `JWT_SECRET_KEY` | (auto-generated) | HS256 signing key. If missing or < 32 chars, a random 48-byte key is generated at startup with a warning. Set via `FIBER_SECRET_KEY` env var or `fiberconfig.json` |
| `JWT_TOKEN_VALIDITY` | `3600000` (1h) | Access token lifetime in ms |
| `JWT_REFRESH_TOKEN_VALIDITY` | `604800000` (7d) | Refresh token lifetime in ms |

### Request Processing Pipeline

For each incoming request, `RouterServlet.service()` executes:

1. **OPTIONS** → `CorsService.handlePreflightRequest()` → return
2. **CORS** → `CorsService.configureCorsHeaders()` → 403 if origin blocked
3. **Route matching** → iterate `EndpointRegistry`, match by path + HTTP method, prefer fewest path variables
4. **Rate limiting** → `RateLimitProcessor.process()` → 429 if exceeded
5. **Security pipeline** (`EndpointHandler` → `SecurityPipeline.execute()`):
   a. **CSRF** → `CsrfMiddleware.handle()` (skipped if `@NoCSRF`)
   b. **Basic auth** → only for OAuth2 endpoints needing `OAuth2ApplicationInfo`
   c. **User auth** → `AuthResolver.resolveUser()` tries each registered `Authenticator` for the accepted `AuthScheme`s
   d. **Permissions** → `PermissionProcessor.process()` checks `@RequireRole` / `@Permission` (method-level then class-level)
6. **Middleware** → all registered `Middleware` in priority order
7. **Parameter resolution** → `ParameterResolver` iterates `ParameterHandlerRegistry` handlers
8. **Method invocation** → controller method called via reflection
9. **Audit log** → if `@AuditLog` present, `AuditLogProcessor.logAuditEvent()` + `AuditContext` collection
10. **Rate limit success** → if status 200, `RateLimitProcessor.onSuccess()` resets counter
11. **Cleanup** → `AuditContext.clear()`, `SessionContext.clear()` in `finally`

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
- Parameterized: `/users/{id}` — `{name}` segments become named regex groups
- Wildcard: `/docs/css/*`
- Paths are normalized via `EndpointHandler.normalizePath()`: `//` → `/`, trailing slashes removed, leading `/` ensured
- **Priority resolution**: when multiple patterns match the same URL (e.g., `/blog/rss` matches both `/blog/rss` and `/blog/{slug}`), the endpoint with the fewest path variables wins. This is tracked by `pathVariableCount` on each `EndpointHandler`.

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
- If body implements `DTOConvertible`, `asDTO()` is called before serialization

**Serialization behavior**:
- `DTOConvertible` objects are automatically converted via `asDTO()` before Jackson serialization — `@IgnoreDTO` fields and null fields are excluded. This applies recursively inside Maps, Lists, and arrays.
- The response is serialized to `byte[]` first, then written to the output stream with `Content-Length` set. This prevents truncated JSON from streaming errors (e.g., Hibernate lazy-loading exceptions mid-write).
- `ResponseEntity.write()` checks `resp.isCommitted()` before writing. Methods return `this` for fluent chaining: `.header(name, value)`, `.contentType(type)`.
- `ResponseEntity.prepareForSerialization(Object)` is public and can be used directly if needed.

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

Default (no `@AuthType`):
- If `@AuthenticatedUser` is present → `COOKIE` only
- If `@RequireRole` or `@Permission` is present (on method or class) → `COOKIE` + `BEARER` (authentication is implicitly required)

### OAuth2 — Social Login (Provider)

Allows users to authenticate via external providers (Discord, Google, etc.).

**Setup:**

```java
OAuth2AuthenticationService<User> oauthService = new MyOAuth2Service(authService, userRepo);
oauthService.registerProvider(new DiscordOAuth2Provider<>("clientId", "secret"));
server.setOAuthService(oauthService);
```

**Implement `OAuth2AuthenticationService`** (abstract):

```java
public class MyOAuth2Service extends OAuth2AuthenticationService<User> {
    public MyOAuth2Service(AuthenticationService<User> authService, GenericRepository<User> repo) {
        super(authService, repo);
    }

    @Override
    protected ResponseContext<User> findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<User> provider) {
        String externalId = (String) userInfo.get(provider.getIdField());
        User existing = userRepo.query().where("discordId", externalId).findFirst();
        if (existing != null) return new ResponseContext<>(existing);
        User newUser = new User();
        provider.mapUserData(userInfo, newUser);
        userRepo.save(newUser);
        return new ResponseContext<>(newUser);
    }
}
```

**Create a custom provider** by extending `AbstractOAuth2Provider`:

```java
public class MyDiscordProvider extends DiscordOAuth2Provider<User> {
    public MyDiscordProvider(String clientId, String secret) {
        super(clientId, secret);
    }

    @Override
    public void mapUserData(Map<String, Object> userInfo, User user) {
        user.setDiscordId((String) userInfo.get("id"));
        user.setUsername((String) userInfo.get("username"));
        user.setEmail((String) userInfo.get("email"));
    }
}
```

**Flow**: your controller calls `oauthService.getAuthorizationUrl(providerId, redirectUri)` → user redirected → callback calls `oauthService.handleCallback(code, state, redirectUri, req, resp)` → cookies set automatically if `findOrCreateUser` returns a user without a state.

Built-in provider: `DiscordOAuth2Provider`. For others, extend `AbstractOAuth2Provider` with the provider's authorization/token/userInfo endpoints.

### OAuth2 — Client Credentials (Server)

Fiber can act as an OAuth2 authorization server, allowing third-party apps to access your API on behalf of users.

**Setup:**

```java
GenericRepository<OAuth2Client> clientRepo = new GenericRepository<>(OAuth2Client.class);
server.setOauthClientService(new OAuth2ClientService(clientRepo));
```

This auto-registers `OAuth2ClientController` at `/oauth/client/` with two endpoints:
- `GET /oauth/client/authorize?client_id=...&redirect_uri=...&response_type=code&state=...` — authorization endpoint (redirects to login if unauthenticated, then redirects back with `code`)
- `POST /oauth/client/token?code=...` — token exchange (requires Basic auth with client credentials)

**Client registration** (server-side):

```java
OAuth2Client client = server.getOauthClientService().registerClient("My App", "https://myapp.com/callback");
// client.getClientId(), client.getClientSecret()
```

**Token exchange** uses `OAuth2ApplicationInfo` which is resolved via `BasicAuthenticator` from the `Authorization: Basic <base64(clientId:clientSecret)>` header.

Authorization codes are single-use, expire after 10 minutes, and are stored in-memory with scheduled cleanup.

### Roles

```java
public class UserRole extends Role {
    public UserRole() { super("user"); }
    @Override public void initializePermissions() {
        addPermission("profile.view");
        addPermission("profile.edit");
    }
    @Override public void initializeParentRoles() {}
}

public class AdminRole extends Role {
    public AdminRole() { super("admin"); }
    @Override public void initializePermissions() {
        addPermission("users.manage");
        addPermission("settings.edit");
    }
    @Override public void initializeParentRoles() {
        addParentRole(new UserRole()); // inherits all "user" permissions — must pass Role instance, not String
    }
}
```

**IMPORTANT**: `addParentRole()` takes a `Role` instance, not a String. `initializeParentRoles()` must be `public` (not protected).

Registration: `server.getRoleRegistry().registerRoleClasses(UserRole.class, AdminRole.class)`.

Usage: `@RequireRole("admin")` on method or class level. `@Permission({"users.manage"})` for fine-grained control. `PermissionProcessor` checks method-level first, then class-level annotations.

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
    try {
        file.moveTo(Path.of("uploads/" + file.getOriginalFilename()));
        return ResponseEntity.ok(Map.of("name", file.getOriginalFilename(), "size", file.getSize()));
    } catch (IOException e) {
        return ResponseEntity.serverError(Map.of("error", "Failed to save file"));
    }
}
```

### UploadedFile API

| Method | Throws | Description |
|--------|--------|-------------|
| `moveTo(Path destination)` | `IOException` | Move temp file to final location. Throws `IllegalStateException` if chunked upload is incomplete |
| `cleanup()` | `IOException` | Delete the temp file |
| `getInputStream()` | `IOException` | Read the file content |
| `getOriginalFilename()` | — | Sanitized filename |
| `getContentType()` | — | MIME type |
| `getSize()` | — | File size in bytes |
| `isComplete()` | — | True if all chunks received |
| `getUploadId()` | — | UUID for chunked uploads |

**Important**: `moveTo()` and `cleanup()` throw checked `IOException` — always wrap in try-catch.

Chunked uploads: client sends `uploadId`, `chunkIndex`, `totalChunks` as query params. Chunks are assembled server-side via `addChunk(Part, int)` (must be in order). Incomplete uploads are auto-cleaned after 24h.

Filenames are sanitized: path separators (`/`, `\`), `..` sequences removed, only `[a-zA-Z0-9._-]` kept.

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

A challenge is a time-limited verification flow (email confirmation, 2FA code, CAPTCHA, etc.). The system stores challenges in-memory and exposes a single verification endpoint.

### Architecture

- `Challenge` — interface defining the contract (id, userId, expiry, status, validateResponse, complete, fail, metadata, callback)
- `AbstractChallenge` — base class implementing common logic (UUID generation, expiry check, status management, metadata map). Extends `DTOConvertible` for serialization
- `ChallengeCallback` — interface with `onSuccess` and `onFailure` returning `ResponseEntity<Object>`
- `ChallengeStatus` — enum: `PENDING`, `COMPLETED`, `FAILED`, `EXPIRED`, `CANCELLED`
- `ChallengeRegistry` — in-memory `ConcurrentHashMap<String, Challenge>` storage + validation logic
- `ChallengeController` — internal endpoint `POST /internal/challenge/verify/{challengeID}`

### Creating a Challenge

Extend `AbstractChallenge` and implement `validateResponse`:

```java
public class EmailVerificationChallenge extends AbstractChallenge {
    private final String expectedCode;

    public EmailVerificationChallenge(Object userId, String code) {
        super(userId, Instant.now().plus(Duration.ofMinutes(15)));
        this.expectedCode = code;
        addMetadata("type", "email_verification");
    }

    @Override
    public boolean validateResponse(Object response) {
        if (response instanceof Map<?, ?> map) {
            return expectedCode.equals(map.get("code"));
        }
        return false;
    }
}
```

`AbstractChallenge` provides:
- Auto-generated UUID `id`
- `createdAt` = `Instant.now()`
- `status` starts as `PENDING`
- `complete()` → sets status to `COMPLETED`, calls `callback.onSuccess()`
- `fail()` → sets status to `FAILED`, calls `callback.onFailure(this, "INVALID_RESPONSE", ...)`
- `setStatus(EXPIRED, ...)` → calls `callback.onFailure(this, "EXPIRED", ...)`
- `addMetadata(key, value)` — attach arbitrary data to the challenge
- `asDTO()` — serializes public fields (metadata and callback are `@IgnoreDTO`)

### Registering a Challenge

```java
String verificationCode = "123456";
EmailVerificationChallenge challenge = new EmailVerificationChallenge(user.getId(), verificationCode);

Challenge registered = server.registerChallenge(challenge, new ChallengeCallback() {
    @Override
    public ResponseEntity<Object> onSuccess(Challenge c, HttpServletRequest req, HttpServletResponse resp) {
        userService.markEmailVerified(c.getUserId());
        return ResponseEntity.ok(Map.of("verified", true));
    }

    @Override
    public ResponseEntity<Object> onFailure(Challenge c, String reason, HttpServletRequest req, HttpServletResponse resp) {
        return ResponseEntity.badRequest(Map.of("error", reason));
    }
});

// Send the challenge ID to the user (e.g., in an email link or API response)
String challengeId = registered.getId();
```

`registerChallenge`:
1. On first call, auto-registers `ChallengeController` at `/internal/challenge/`
2. Attaches the callback to the challenge
3. Stores the challenge in `ChallengeRegistry` (keyed by UUID)
4. Returns the challenge (with its generated `id`)

### Verification Flow

Client sends: `POST /internal/challenge/verify/{challengeId}`

```json
{"code": "123456"}
```

The `ChallengeController` (annotated with `@AuditLog(action = "CHALLENGE_VERIFICATION")`) does:
1. Looks up the challenge by ID → 404 if not found
2. Calls `challengeRegistry.validateChallenge()`:
   - If expired → sets status `EXPIRED`, calls `onFailure("EXPIRED")`, returns 410
   - If `validateResponse()` returns true → calls `complete()` → `onSuccess()` → returns success response
   - If `validateResponse()` returns false → calls `fail()` → `onFailure("INVALID_RESPONSE")` → returns failure response
3. If the callback returns null → returns 410 "Challenge expired"

### Lifecycle

```
PENDING → validateResponse(true)  → COMPLETED  (onSuccess called)
PENDING → validateResponse(false) → FAILED     (onFailure called with "INVALID_RESPONSE")
PENDING → isExpired()             → EXPIRED    (onFailure called with "EXPIRED")
PENDING → manual                  → CANCELLED
```

### Cleanup

Challenges are NOT auto-removed after completion/failure. Call `ChallengeRegistry` methods manually:
- `removeChallenge(id)` — remove a specific challenge
- `cleanupExpiredChallenges()` — remove all expired challenges

For production, schedule periodic cleanup:

```java
scheduler.scheduleAtFixedRate(
    () -> server.getChallengeRegistry().cleanupExpiredChallenges(),
    1, 1, TimeUnit.HOURS);
```

## DTOConvertible

`DTOConvertible` is an **abstract class** (not an interface). Entities **extend** it to get automatic `asDTO()` serialization that excludes `@IgnoreDTO` and null fields.

```java
@Entity @Table(name = "users")
public class User extends DTOConvertible implements IdentifiableEntity, UserAuth {
    private String name;
    private String email;
    @IgnoreDTO private String password; // excluded from asDTO()

    // asDTO() returns Map<String, Object> of all non-@IgnoreDTO, non-null fields
    // Nested DTOConvertible objects, collections, arrays, and maps are recursively converted
}
```

`asDTO()` calls `transform()` first (override to mutate fields before serialization). Field resolution uses cached reflection (`getCachedFields()`) and walks the class hierarchy up to `Object`.

Pre-cache at startup: `server.preloadDto()` — scans classpath for all `DTOConvertible` subclasses and warms the field cache.

## Key Rules

### Database / Entities

### Lifecycle
- Always call `server.start()` after all configuration is done.
- `FiberServer.get()` is available after the constructor returns (singleton).
- Register authenticators on `server.getAuthResolver()` (Cookie + Bearer are registered by default).
- Middleware added after controller registration won't apply to already-registered endpoints.
- `registerController(Class<?>)` requires a no-arg constructor; `registerController(Object)` uses the provided instance.

### Authentication & Sessions
- `SessionService` is optional — without it, auth is pure stateless JWT and session methods on `UserAuth` throw `IllegalStateException`.
- Both `AuthenticationService` and `SessionService` use Caffeine caches (30s TTL). `getUserById()` requires manual `evictUser(id)` after mutations outside the service. `SessionService` invalidates its cache automatically on all write operations.
- JWT secret must be at least 32 characters in production (auto-generated with warning if default/short). Configure via `FIBER_SECRET_KEY` env var or `fiberconfig.json`.
- JWT tokens embed `sessionId` in claims when `SessionService` is configured. `User-Agent` is also embedded and validated on each request.

### Routing & Parameters
- Path variables use `{name}` syntax and are extracted as named regex groups.
- When multiple patterns match, the endpoint with fewest path variables wins.
- `@RequestBody` validation errors return 400 automatically.
- `@Param` supports `required = false` for optional query parameters.

### Security
- `@RateLimit` counter resets on successful (200) responses. Supports `slidingWindow`, `perUser`, `key` for shared buckets, and class-level application.
- `@AuditLog` supports custom data via `AuditContext.put(key, value)` inside endpoints. `AuditContext` is cleared automatically per request via `RouterServlet.finally`.
- `@RequireRole` and `@Permission` are checked at both method and class level (method takes priority).
- `addParentRole()` in `Role` takes a `Role` instance, not a String. `initializeParentRoles()` must be `public`.

### Challenges
- Challenges are stored in-memory (`ConcurrentHashMap`), NOT in the database.
- Challenges are NOT auto-removed after completion/failure — call `removeChallenge(id)` or schedule `cleanupExpiredChallenges()`.
- `ChallengeController` is auto-registered on first `registerChallenge()` call.
- The callback's `ResponseEntity` return value is sent directly to the client as the verification response.

### File Uploads & Other
- File upload filenames are automatically sanitized against path traversal.
- Chunked uploads use `uploadId`, `chunkIndex`, `totalChunks` query params. Incomplete uploads auto-cleaned after 24h.
- `DTOConvertible` is an abstract class (extend, not implement). `asDTO()` excludes `@IgnoreDTO` and null fields, recursively converts nested `DTOConvertible` objects. Pre-cache with `server.preloadDto()`.

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