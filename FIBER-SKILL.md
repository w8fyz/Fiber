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
│   ├── JwtUtil.java                            # JWT generation/validation (HS256, JJWT, lazy init)
│   ├── ResponseEntity.java                     # Typed HTTP response builder
│   ├── authentication/
│   │   ├── AuthenticationService.java          # Abstract: login, cookies, token lifecycle
│   │   ├── AuthCookieConfig.java               # Cookie attributes (SameSite, Secure, etc.)
│   │   ├── AuthMiddleware.java                 # Legacy request-attribute auth helper
│   │   ├── AuthResolver.java                   # Routes request to matching Authenticator
│   │   ├── AuthScheme.java                     # Enum: BEARER, COOKIE, BASIC
│   │   ├── Authenticator.java                  # Interface: scheme() + authenticate(request)
│   │   ├── RoleRegistry.java                   # Register and query Role classes (ConcurrentHashMap)
│   │   ├── SameSitePolicy.java                 # Enum: STRICT, LAX, NONE
│   │   ├── entities/
│   │   │   ├── Role.java                       # Abstract role with permissions + hierarchy
│   │   │   ├── UserAuth.java                   # Interface: getId(), getRole(), session helpers
│   │   │   └── UserFieldUtil.java              # Reflection on @IdentifierField/@PasswordField + password policy
│   │   ├── impl/
│   │   │   ├── BasicAuthenticator.java         # OAuth2 client_id/client_secret via Basic
│   │   │   ├── BearerAuthenticator.java        # Authorization: Bearer <jwt>
│   │   │   ├── CookieAuthenticator.java        # access_token cookie
│   │   │   └── SessionValidator.java           # Shared session validation (extracted from Cookie/Bearer)
│   │   └── oauth2/
│   │       ├── OAuth2Provider.java             # Interface: getProviderId, getAuthorizationUrl, processCallback, mapUserData
│   │       ├── AbstractOAuth2Provider.java     # Base impl: code exchange, userInfo fetch, URL building, SSRF protection
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
│   │   ├── cors/CorsService.java               # Fluent CORS configuration (safe wildcard matching)
│   │   ├── csrf/CsrfController.java            # GET /internal/csrf/token
│   │   ├── filters/SecurityHeadersFilter.java  # Auto security headers
│   │   ├── interceptors/RateLimitInterceptor.java # Caffeine-bounded, thread-safe
│   │   ├── logging/AuditLogProcessor.java, AuditContext.java # Async audit logging on virtual threads
│   │   └── processors/
│   │       ├── PermissionProcessor.java        # Returns 403 Forbidden (not 401)
│   │       └── RateLimitProcessor.java
│   ├── session/
│   │   ├── FiberSession.java                   # JPA entity: id, userId, ip, ua, timestamps
│   │   ├── SessionContext.java                 # ThreadLocal<FiberSession> per-request
│   │   └── SessionService.java                 # CRUD, invalidation, cleanup
│   └── upload/
│       ├── FileUploadManager.java              # Singleton, chunked upload tracking, auto-cleanup
│       └── UploadedFile.java                   # File wrapper: moveTo, cleanup, getInputStream
├── handler/
│   ├── EndpointHandler.java                    # Per-route handler: security → params → invoke
│   ├── FiberErrorHandler.java                  # Jetty error handler (JSON)
│   ├── ParameterResolver.java                  # Resolves method arguments from request
│   ├── ResponseWriter.java                     # Writes result to HttpServletResponse (writeValueAsBytes)
│   ├── RouterServlet.java                      # O(1) static route lookup, linear scan for dynamic routes
│   ├── SecurityPipeline.java                   # CSRF → auth → permissions chain
│   ├── SecurityResult.java                     # Pipeline output (user, app, proceed)
│   └── parameter/
│       ├── ParameterHandler.java               # Interface: canHandle + handle
│       ├── ParameterHandlerRegistry.java       # CopyOnWriteArrayList, handler discovery
│       ├── AuthenticatedUserParameterHandler.java
│       ├── SessionParameterHandler.java        # @CurrentSession / FiberSession
│       ├── FileUploadParameterHandler.java
│       ├── OAuth2ApplicationInfoParameterHandler.java
│       ├── PathVariableParameterHandler.java   # Uses shared TypeConverter
│       ├── QueryParameterHandler.java          # Uses shared TypeConverter
│       ├── RequestBodyParameterHandler.java    # Caches raw body for audit logging
│       └── ServletParameterHandler.java        # HttpServletRequest/Response injection
├── middleware/
│   ├── Middleware.java                         # Interface: priority() + handle()
│   └── impl/CsrfMiddleware.java               # HMAC-signed tokens, stable per session
├── util/
│   ├── FiberObjectMapper.java                  # Pre-configured Jackson ObjectMapper
│   ├── HttpUtil.java                           # Trusted-proxy-aware IP resolution
│   ├── JsonUtil.java                           # toJson / fromJson helpers
│   ├── RandomUtil.java
│   └── TypeConverter.java                      # Shared String→primitive conversion
└── validation/
    ├── Email.java, Min.java, NotBlank.java, NotNull.java  # Annotations
    ├── ValidationInitializer.java              # Registers built-in validators
    ├── ValidationProcessor.java                # Field-level validation (standalone, legacy)
    ├── ValidationRegistry.java                 # Extensible validator registry with field metadata cache
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
- `setServerHeader(String)` — custom Server response header (default: "Fiber")
- `setTrustedProxies(List<String>)` — IP addresses of trusted reverse proxies (required for X-Forwarded-For)
- `getRoleRegistry().registerRoleClasses(...)` — role/permission system
- `addMiddleware(Middleware)` — global middleware
- `registerController(Object)` or `registerController(Class<?>)` — register controllers
- `preloadDto()` — pre-cache DTOConvertible fields for faster first requests
- `setMaxFileSize(long)` — max file size in bytes (default 50MB), call before `start()`
- `setMaxRequestSize(long)` — max multipart request size in bytes (default 100MB)
- `setFileSizeThreshold(int)` — size in bytes before writing to disk (default 1MB)
- `start()` / `stop()` — both throw checked `Exception`

Singleton access after construction: `FiberServer.get()`.

**IMPORTANT**: `start()` will throw `IllegalStateException` in production mode (non-dev) if no JWT secret is configured. Call `enableDevelopmentMode()` before `start()` for local development, or set `FIBER_SECRET_KEY` env variable / `JWT_SECRET_KEY` in `fiberconfig.json`.

### Virtual Threads (Java 21)

Fiber automatically configures Jetty with a virtual thread executor. This removes the 200-thread ceiling for I/O-bound workloads and significantly improves throughput under high concurrency. No configuration needed — it's enabled by default.

### Trusted Proxies

By default, Fiber uses `request.getRemoteAddr()` as the client IP. Proxy headers (X-Forwarded-For, X-Real-IP) are **ignored** unless you explicitly configure trusted proxies:

```java
server.setTrustedProxies(List.of("10.0.0.1", "10.0.0.2")); // your load balancer IPs
```

This prevents IP spoofing via forged headers, which would bypass rate limiting and JWT IP binding.

### Configuration (`FiberConfig`)

Loaded from `fiberconfig.json` (or env variables prefixed `FIBER_`). Fields:

| Field | Default | Description |
|-------|---------|-------------|
| `JWT_SECRET_KEY` | (auto-generated in dev) | HS256 signing key. **Required** in production (min 32 chars). Set via `FIBER_SECRET_KEY` env var or `fiberconfig.json`. Fails at `start()` if missing in non-dev mode |
| `JWT_TOKEN_VALIDITY` | `3600000` (1h) | Access token lifetime in ms |
| `JWT_REFRESH_TOKEN_VALIDITY` | `604800000` (7d) | Refresh token lifetime in ms |

### Request Processing Pipeline

For each incoming request, `RouterServlet.service()` executes:

1. **OPTIONS** → `CorsService.handlePreflightRequest()` → return
2. **Route matching** → O(1) HashMap lookup for static routes, linear scan for dynamic routes (path variables/wildcards). Prefer fewest path variables on tie.
3. **CORS** → `CorsService.configureCorsHeaders()` → 403 if origin blocked (skipped if endpoint has `@NoCors`)
4. **Rate limiting** → `RateLimitProcessor.process()` → 429 if exceeded
5. **Security pipeline** (`EndpointHandler` → `SecurityPipeline.execute()`):
   a. **CSRF** → `CsrfMiddleware.handle()` (skipped if `@NoCSRF`)
   b. **Basic auth** → only for OAuth2 endpoints needing `OAuth2ApplicationInfo`
   c. **User auth** → `AuthResolver.resolveUser()` tries each registered `Authenticator` for the accepted `AuthScheme`s
   d. **Permissions** → `PermissionProcessor.process()` checks `@RequireRole` / `@Permission` (method-level then class-level) → returns **403 Forbidden** (not 401)
6. **Middleware** → all registered `Middleware` in priority order
7. **Parameter resolution** → `ParameterResolver` iterates `ParameterHandlerRegistry` handlers
8. **Method invocation** → controller method called via reflection
9. **Audit log** → if `@AuditLog` present, `AuditLogProcessor.logAuditEvent()` runs **asynchronously on a virtual thread** + `AuditContext` collection
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

- Static: `/users/me` — O(1) HashMap lookup
- Parameterized: `/users/{id}` — `{name}` segments become named regex groups
- Wildcard: `/docs/css/*`
- Paths are normalized via `EndpointHandler.normalizePath()`: `//` → `/`, trailing slashes removed, leading `/` ensured
- **Priority resolution**: when multiple patterns match the same URL (e.g., `/blog/rss` matches both `/blog/rss` and `/blog/{slug}`), the endpoint with the fewest path variables wins. This is tracked by `pathVariableCount` on each `EndpointHandler`.

## Parameter Injection

Parameters are resolved in order by the first matching `ParameterHandler`:

1. `HttpServletRequest` / `HttpServletResponse` — injected directly
2. `@RequestBody` — JSON deserialization + validation (raw body cached for audit logging)
3. `@Param("name")` — query string, with type conversion and optional `required = false`
4. `@PathVariable("name")` — from URL path segment
5. `@AuthenticatedUser` — resolved by SecurityPipeline (not via handler registry)
6. `@CurrentSession` or `FiberSession` type — current session from SessionContext
7. `@FileUpload` — multipart file upload
8. `OAuth2ApplicationInfo` — OAuth2 client via Basic auth

### Type Conversion

`@Param` and `@PathVariable` auto-convert to: `String`, `int`/`Integer`, `long`/`Long`, `double`/`Double`, `boolean`/`Boolean`. Shared via `TypeConverter` utility class.

## ResponseEntity

```java
ResponseEntity.ok(body)                          // 200
ResponseEntity.created(body)                     // 201
ResponseEntity.noContent()                       // 204
ResponseEntity.badRequest(body)                  // 400
ResponseEntity.unauthorized(body)                // 401
ResponseEntity.forbidden(body)                   // 403
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
- The response is serialized to `byte[]` via `ObjectMapper.writeValueAsBytes()`, then written to the output stream with `Content-Length` set.
- `ResponseEntity.write()` checks `resp.isCommitted()` before writing. Methods return `this` for fluent chaining: `.header(name, value)`, `.contentType(type)`.

## Authentication

### AuthenticationService

Extend `AuthenticationService<T extends UserAuth>`:

```java
public class MyAuthService extends AuthenticationService<User> {
    public MyAuthService(GenericRepository<User> repo) {
        super(repo, "/auth");  // refresh token cookie path
    }

    // Override for efficient database lookup instead of loading all users
    @Override
    public UserAuth findUserByIdentifer(String identifier) {
        return repo.query().where("email", identifier).findFirst();
    }
}
```

Key methods inherited:
- `getUserById(Object id)` — load from repository (Caffeine-cached, 30s TTL)
- `findUserByIdentifer(String identifier)` — override this for efficient DB lookup (default loads all users)
- `findByIdentifier(String identifier)` — protected fallback that loads all users (avoid in production)
- `validateCredentials(UserAuth user, String password)` — BCrypt compare
- `doesIdentifiersAlreadyExists(UserAuth user)` — uniqueness check
- `generateToken(UserAuth user, HttpServletRequest req)` — JWT access token
- `validateToken(String token, HttpServletRequest req)` — JWT validation
- `setAuthCookies(UserAuth user, req, resp)` — sets access_token + refresh_token cookies, creates session if SessionService configured
- `clearAuthCookies(req, resp)` — clears cookies, invalidates current session
- `evictUser(Object id)` — invalidate a single user from cache (call after save/update/delete)
- `evictAllUsers()` — invalidate the entire user cache

**IMPORTANT**: Override `findUserByIdentifer()` in your `AuthenticationService` subclass with a targeted database query. The default implementation loads ALL users into memory on every login attempt.

#### User Cache

`getUserById()` is backed by a Caffeine cache (30s TTL, 10k max entries). The cache is transparent — first call hits DB, subsequent calls serve from memory.

**IMPORTANT**: When you save/update/delete a user outside of `AuthenticationService`, you MUST call `evictUser(id)` to keep the cache in sync:

```java
userRepository.save(user);
authService.evictUser(user.getId()); // keep cache consistent
```

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

All authenticators use shared `SessionValidator` for session validation.

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

### Password Policy

Configure password strength requirements at startup:

```java
UserFieldUtil.setPasswordPolicy(12, true, true); // min 12 chars, require uppercase, require digit
```

Default: minimum 8 characters, no uppercase/digit requirement. Validation runs automatically when `setPassword()` is called.

### OAuth2 — Social Login (Provider)

Allows users to authenticate via external providers (Discord, Google, etc.).

**Setup:**

```java
OAuth2AuthenticationService<User> oauthService = new MyOAuth2Service(authService, userRepo);
oauthService.registerProvider(new DiscordOAuth2Provider<>("clientId", "secret"));
server.setOAuthService(oauthService);
```

**Security**: `AbstractOAuth2Provider` validates that token and userInfo endpoints use HTTPS and do not target private/loopback addresses (SSRF protection). HTTP requests have a 10-second timeout.

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

### OAuth2 — Client Credentials (Server)

Fiber can act as an OAuth2 authorization server. The `/oauth/client/authorize` endpoint now **requires authentication** via `@AuthenticatedUser`.

**Setup:**

```java
GenericRepository<OAuth2Client> clientRepo = new GenericRepository<>(OAuth2Client.class);
server.setOauthClientService(new OAuth2ClientService(clientRepo));
```

Endpoints:
- `GET /oauth/client/authorize?client_id=...&redirect_uri=...&response_type=code&state=...` — authorization (requires authenticated user)
- `POST /oauth/client/token?code=...` — token exchange (requires Basic auth with client credentials)

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
    }
    @Override public void initializeParentRoles() {
        addParentRole(new UserRole()); // inherits all "user" permissions
    }
}
```

**IMPORTANT**: `addParentRole()` takes a `Role` instance, not a String. `initializeParentRoles()` must be `public`.

Registration: `server.getRoleRegistry().registerRoleClasses(UserRole.class, AdminRole.class)`.

Usage: `@RequireRole("admin")` on method or class level. `@Permission({"users.manage"})` for fine-grained control. `PermissionProcessor` checks method-level first, then class-level annotations. Returns **403 Forbidden** when the user lacks required permissions.

## Sessions

Optional server-side session tracking. Without it, auth is pure stateless JWT.

### Setup

```java
GenericRepository<FiberSession> sessionRepo = new GenericRepository<>(FiberSession.class);
server.setSessionService(new SessionService(sessionRepo));
// Custom TTL: new SessionService(sessionRepo, 30 * 24 * 3600 * 1000L)
```

### How It Works

1. `setAuthCookies()` creates a `FiberSession` in DB (+ puts it in cache) and embeds `sessionId` in the JWT claims.
2. On each request, `CookieAuthenticator`/`BearerAuthenticator` uses shared `SessionValidator` to extract `sessionId`, load the session (from Caffeine cache or DB), and check `active` + `expiresAt`.
3. If invalid → 401. If valid → `SessionContext.set(session)`.
4. `SessionContext.clear()` is called in `RouterServlet.finally` to prevent thread-local leaks.

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

Validation runs automatically on `@RequestBody` deserialization and `@Param`/`@PathVariable` parameters. Failures return 400 with error messages. Field metadata is cached per class for performance.

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
    .addAllowedOrigin("https://*.myapp.com")   // wildcard patterns (safe matching, no regex)
    .setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
    .setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN"))
    .setAllowCredentials(true)
    .setMaxAge(3600));
```

Skip CORS for specific endpoints: `@NoCors` — this annotation is now properly enforced at runtime.

**Default CORS**: In dev mode, `localhost:3000` is included. In production, only `localhost:{port}` is allowed by default. Always configure explicit origins for production.

### CSRF

`server.enableCSRFProtection()` activates:
- XSRF-TOKEN cookie with HMAC-signed tokens (bound to server secret)
- X-CSRF-TOKEN header validation on unsafe methods (POST, PUT, DELETE, PATCH)
- Token is stable during GET requests, regenerated only after successful state-changing requests
- `GET /internal/csrf/token` endpoint to generate initial token

Skip for specific endpoints: `@NoCSRF`.

#### CSRF Frontend Implementation

To properly integrate CSRF protection in your frontend:

**Vanilla JavaScript / Fetch API:**

```javascript
// 1. On page load or app init, fetch a CSRF token
async function initCsrf() {
  const res = await fetch('/internal/csrf/token', { credentials: 'include' });
  // The XSRF-TOKEN cookie is now set automatically
}

// 2. For every state-changing request, read the cookie and send it as a header
function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

async function postData(url, data) {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': getCsrfToken(),
    },
    body: JSON.stringify(data),
  });
  // After a successful POST, the server returns a new XSRF-TOKEN cookie
  // automatically — no need to re-fetch
  return response.json();
}
```

**Axios (global interceptor):**

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://api.example.com',
  withCredentials: true,
});

// Automatically attach CSRF token to every request
api.interceptors.request.use((config) => {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  if (match) {
    config.headers['X-CSRF-TOKEN'] = decodeURIComponent(match[1]);
  }
  return config;
});

// On app startup
await api.get('/internal/csrf/token');

// Then use normally
await api.post('/api/users', { name: 'John' });
```

**React (custom hook):**

```javascript
import { useEffect } from 'react';

function useCsrf() {
  useEffect(() => {
    fetch('/internal/csrf/token', { credentials: 'include' });
  }, []);
}

// In your App component
function App() {
  useCsrf();
  return <Router>...</Router>;
}
```

### Rate Limiting

Annotation-based rate limiting with two algorithms (fixed window, sliding window), keyed by IP or by authenticated user. Rate limit state is stored in **Caffeine bounded caches** (max 100k entries) to prevent memory exhaustion under DDoS.

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

Returns 429 with `Retry-After` header and JSON body `{"status":429, "message":"...", "retryAfter": <seconds>}`.

Rate limit identifiers are consistent between `process()` and `onSuccess()` — both use the same `resolveIdentifier()` logic. `RateLimitInterceptor.clearAll()` for testing.

### Audit Logging

Track security-relevant actions with `@AuditLog`. Logs go to SLF4J and optionally to a custom `AuditLogService`. **Audit processing runs asynchronously on virtual threads** to avoid adding latency to request handling.

```java
@AuditLog(action = "USER_LOGIN", logParameters = true, logResult = true, maskSensitiveData = true)
```

Sensitive data masking is **recursive** — nested objects and maps containing keys matching `password`, `secret`, or `token` are masked at all levels. Parameter objects are **cloned via JSON serialization** before masking, so the original request data is never mutated.

The raw request body is automatically cached by `RequestBodyParameterHandler` and available for audit logging even after the input stream is consumed.

### Security Headers (automatic)

- `Server: Fiber` (customizable via `setServerHeader()`)
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

Incomplete uploads are auto-cleaned after 24h. **Completed uploads are also cleaned after 1h** to prevent memory leaks.

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

Middleware list uses `CopyOnWriteArrayList` for thread safety. Middleware executes after authentication/permissions but before parameter resolution.

## Challenges

A challenge is a time-limited verification flow (email confirmation, 2FA code, CAPTCHA, etc.). The system stores challenges in-memory and exposes a single verification endpoint.

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

### Registering and Verifying

```java
Challenge registered = server.registerChallenge(challenge, new ChallengeCallback() {
    @Override
    public ResponseEntity<Object> onSuccess(Challenge c, HttpServletRequest req, HttpServletResponse resp) {
        return ResponseEntity.ok(Map.of("verified", true));
    }
    @Override
    public ResponseEntity<Object> onFailure(Challenge c, String reason, HttpServletRequest req, HttpServletResponse resp) {
        return ResponseEntity.badRequest(Map.of("error", reason));
    }
});
```

Client sends: `POST /internal/challenge/verify/{challengeId}` with JSON body.

## DTOConvertible

`DTOConvertible` is an **abstract class** (not an interface). Entities **extend** it to get automatic `asDTO()` serialization.

```java
@Entity @Table(name = "users")
public class User extends DTOConvertible implements IdentifiableEntity, UserAuth {
    private String name;
    private String email;
    @IgnoreDTO private String password; // excluded from asDTO()
}
```

Pre-cache at startup: `server.preloadDto()`.

## Key Rules

### Lifecycle
- Always call `server.start()` after all configuration is done.
- `FiberServer.get()` is available after the constructor returns (singleton).
- `stop()` performs graceful shutdown: stops Jetty and cleans up FileUploadManager.
- Register authenticators on `server.getAuthResolver()` (Cookie + Bearer are registered by default).
- `registerController(Class<?>)` requires a no-arg constructor; `registerController(Object)` uses the provided instance.
- Duplicate endpoint registrations are logged as warnings and silently ignored.

### Authentication & Sessions
- `SessionService` is optional — without it, auth is pure stateless JWT and session methods on `UserAuth` throw `IllegalStateException`.
- JWT secret is **mandatory** in production mode. `start()` throws `IllegalStateException` if not configured. Use `enableDevelopmentMode()` for local dev.
- JwtUtil uses lazy initialization — it does not require `FiberServer` to exist at class loading time.
- Override `findUserByIdentifer()` in your `AuthenticationService` subclass for efficient user lookup.

### Security
- `@NoCors` is enforced at runtime — CORS headers are skipped for annotated endpoints.
- CSRF tokens use HMAC signatures bound to the server's JWT secret. Tokens are stable during GET requests.
- Rate limiting uses bounded Caffeine caches (100k max entries) to prevent OOM under DDoS.
- Permission checks return 403 Forbidden (not 401 Unauthorized).
- Trusted proxies must be explicitly configured for X-Forwarded-For to be trusted.
- OAuth2 provider URLs are validated against SSRF (no private IPs, timeouts enforced).

### Routing & Parameters
- Static routes use O(1) HashMap lookup. Dynamic routes use linear scan.
- The `Matcher` from route matching is passed to `handleRequest()` to avoid double regex evaluation.
- Path variables use `{name}` syntax and are extracted as named regex groups.
- `@RequestBody` validation errors return 400 automatically.

## Installation

### Gradle

```groovy
dependencies {
    implementation 'sh.fyz:Fiber:2.0.2'
}
```

### Maven

```xml
<dependency>
    <groupId>sh.fyz</groupId>
    <artifactId>Fiber</artifactId>
    <version>2.0.2</version>
</dependency>
```
