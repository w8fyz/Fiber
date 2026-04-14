# Fiber

A modern Java 21+ framework for building RESTful APIs on top of Jetty. Minimal boilerplate, built-in JWT authentication, OAuth2, CORS, CSRF protection, rate limiting, sessions, validation, file uploads, and auto-generated API documentation.

## Table of Contents

- [Quick Start](#quick-start)
- [Controllers & Routing](#controllers--routing)
- [Parameter Injection](#parameter-injection)
- [Responses](#responses)
- [Authentication](#authentication)
- [Sessions](#sessions)
- [Validation](#validation)
- [Security](#security)
- [File Uploads](#file-uploads)
- [Email](#email)
- [Middleware](#middleware)
- [API Documentation](#api-documentation)
- [Full Example](#full-example)

## Quick Start

### Dependencies (Gradle)

```groovy
dependencies {
    implementation 'sh.fyz:Fiber:2.0.1'
    implementation 'sh.fyz:Architect:2.0.0' // ORM (optional, required for sessions)
}
```

### Minimal Server

```java
public class Main {
    public static void main(String[] args) throws Exception {
        FiberServer server = new FiberServer(8080, true); // port, enable docs
        server.registerController(new HelloController());
        server.start();
    }
}

@Controller("/api")
public class HelloController {

    @RequestMapping(value = "/hello", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of("message", "Hello, World!"));
    }
}
```

`GET /api/hello` returns `{"status":200,"message":"Hello, World!"}`.

## Controllers & Routing

Annotate a class with `@Controller("/base-path")` and methods with `@RequestMapping`.

```java
@Controller("/users")
public class UserController {

    @RequestMapping(value = "/", method = RequestMapping.Method.GET,
                    description = "List all users")
    public ResponseEntity<?> list() { ... }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    public ResponseEntity<?> getById(@PathVariable("id") long id) { ... }

    @RequestMapping(value = "/", method = RequestMapping.Method.POST)
    public ResponseEntity<?> create(@RequestBody User user) { ... }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.DELETE)
    @RequireRole("ADMIN")
    public ResponseEntity<?> delete(@PathVariable("id") long id) { ... }
}
```

Supported HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS`.

Path variables use `{name}` syntax. Wildcard `*` matches any segment.

Static routes always take priority over parameterized ones (`/users/me` wins over `/users/{id}`).

## Parameter Injection

| Annotation | Source | Example |
|---|---|---|
| `@Param("name")` | Query string | `?name=john` |
| `@PathVariable("id")` | URL path segment | `/users/{id}` |
| `@RequestBody` | JSON body | Deserialized to object |
| `@AuthenticatedUser` | JWT session | Current user entity |
| `@CurrentSession` | Active session | `FiberSession` object |
| `@FileUpload` | Multipart body | Uploaded file |
| (none) | `HttpServletRequest` / `HttpServletResponse` | Raw servlet objects |

Query parameters support automatic type conversion (`int`, `long`, `double`, `boolean`, `String`) and optional values:

```java
@RequestMapping(value = "/search", method = RequestMapping.Method.GET)
public ResponseEntity<?> search(
        @NotBlank @Param("q") String query,
        @Param(value = "page", required = false) int page) {
    ...
}
```

## Responses

Use `ResponseEntity<T>` to control status, headers, and content type:

```java
ResponseEntity.ok(body)              // 200
ResponseEntity.created(body)         // 201
ResponseEntity.badRequest(body)      // 400
ResponseEntity.unauthorized(body)    // 401
ResponseEntity.notFound()            // 404
ResponseEntity.noContent()           // 204
ResponseEntity.serverError(body)     // 500

// Custom status, headers, content type
ResponseEntity.ok(imageBytes)
    .contentType("image/png")
    .header("Cache-Control", "max-age=3600");
```

Returning a POJO (non-`ResponseEntity`) is automatically serialized to JSON. Returning `byte[]` inside a `ResponseEntity` writes raw binary (images, files, etc.).

## Authentication

### Setup

```java
// 1. Implement AuthenticationService
public class MyAuthService extends AuthenticationService<User> {
    public MyAuthService(GenericRepository<User> userRepo) {
        super(userRepo, "/auth"); // refresh token cookie path
    }
}

// 2. Register on the server
server.setAuthService(new MyAuthService(userRepository));

// 3. Register authenticators
server.getAuthResolver().registerAuthenticator(new CookieAuthenticator());
server.getAuthResolver().registerAuthenticator(new BearerAuthenticator());
```

### User Entity

Your user class must implement both `IdentifiableEntity` (Architect) and `UserAuth` (Fiber):

```java
@Entity
@Table(name = "users")
public class User implements IdentifiableEntity, UserAuth {
    @Id @GeneratedValue
    private long id;

    @IdentifierField
    private String email;

    @PasswordField
    private String password;

    @Column(name = "role")
    private String role;

    public User() {}
    @Override public Object getId() { return id; }
    @Override public String getRole() { return role; }
    // getters/setters...
}
```

`@IdentifierField` marks fields used for login lookup (email, username, etc.). `@PasswordField` marks the BCrypt-hashed password field.

### Login / Logout

```java
@Controller("/auth")
public class AuthController {

    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    @RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest req, HttpServletResponse resp) {
        AuthenticationService<?> auth = FiberServer.get().getAuthService();
        UserAuth user = auth.findUserByIdentifer(body.get("identifier"));

        if (user == null || !auth.validateCredentials(user, body.get("password"))) {
            return ResponseEntity.unauthorized("Invalid credentials");
        }

        auth.setAuthCookies(user, req, resp); // sets access_token + refresh_token cookies
        return ResponseEntity.ok(Map.of("message", "Logged in"));
    }

    @RequestMapping(value = "/logout", method = RequestMapping.Method.POST)
    public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse resp) {
        FiberServer.get().getAuthService().clearAuthCookies(req, resp);
        return ResponseEntity.ok("Logged out");
    }

    @RequestMapping(value = "/me", method = RequestMapping.Method.GET)
    public ResponseEntity<?> me(@AuthenticatedUser User user) {
        return ResponseEntity.ok(user);
    }
}
```

### Auth Schemes

By default, `@AuthenticatedUser` uses cookie-based auth. Use `@AuthType` to accept other schemes:

```java
@AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
@RequestMapping(value = "/data", method = RequestMapping.Method.GET)
public ResponseEntity<?> getData(@AuthenticatedUser User user) { ... }
```

### Roles & Permissions

```java
public class AdminRole extends Role {
    public AdminRole() { super("admin"); }

    @Override protected void initializePermissions() {
        addPermission("users.delete");
        addPermission("users.edit");
    }

    @Override protected void initializeParentRoles() {}
}

// Register at startup
server.getRoleRegistry().registerRoleClasses(AdminRole.class);

// Protect endpoints
@RequireRole("admin")
@RequestMapping(value = "/admin/stats", method = RequestMapping.Method.GET)
public ResponseEntity<?> adminStats() { ... }
```

## Sessions

Fiber supports server-side sessions backed by a database (via Architect). Sessions are created automatically on login and validated on each authenticated request.

### Setup

```java
GenericRepository<FiberSession> sessionRepo = new GenericRepository<>(FiberSession.class);
server.setSessionService(new SessionService(sessionRepo));
// or with custom TTL: new SessionService(sessionRepo, 30 * 24 * 60 * 60 * 1000L) // 30 days
```

Make sure `FiberSession` is registered as a Hibernate entity so the table is created.

### Usage in Controllers

```java
@RequestMapping(value = "/me", method = RequestMapping.Method.GET)
public ResponseEntity<?> me(@AuthenticatedUser User user, @CurrentSession FiberSession session) {
    // Current session info
    String ip = session.getIpAddress();
    long created = session.getCreatedAt();

    // List all active sessions for this user
    List<FiberSession> sessions = user.getSessions();

    // Invalidate all other sessions (logout everywhere else)
    user.invalidateOtherSessions();

    return ResponseEntity.ok(Map.of("user", user, "sessions", sessions));
}

@RequestMapping(value = "/sessions/{id}", method = RequestMapping.Method.DELETE)
public ResponseEntity<?> revokeSession(@AuthenticatedUser User user,
                                        @PathVariable("id") String sessionId) {
    user.invalidateSession(sessionId);
    return ResponseEntity.ok("Session revoked");
}
```

Session methods available on any `UserAuth` instance: `getSession()`, `getSessions()`, `invalidateSession(id)`, `invalidateAllSessions()`, `invalidateOtherSessions()`.

If no `SessionService` is configured, everything works as before (pure stateless JWT).

## Validation

Annotations can be placed on entity fields and/or method parameters:

```java
@Entity @Table(name = "users")
public class User implements IdentifiableEntity, UserAuth {
    @NotBlank private String name;
    @Email private String email;
    @Min(18) private int age;
    @NotNull private String role;
    // ...
}

@RequestMapping(value = "/register", method = RequestMapping.Method.POST)
public ResponseEntity<?> register(@RequestBody User user) {
    // Validation runs automatically on @RequestBody deserialization.
    // Returns 400 with error messages if validation fails.
}
```

| Annotation | Behavior |
|---|---|
| `@NotNull` | Value must not be `null` |
| `@NotBlank` | String must not be `null` or empty/blank |
| `@Min(value)` | Numeric value must be >= `value` |
| `@Email` | Must be a valid email format |

## Security

### CORS

```java
server.setCorsService(new CorsService()
    .addAllowedOrigin("http://localhost:3000")
    .addAllowedOrigin("https://*.example.com")  // wildcard support
    .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"))
    .setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-CSRF-TOKEN"))
    .setAllowCredentials(true)
    .setMaxAge(3600));
```

Use `@NoCors` on an endpoint to skip CORS headers for that route.

### CSRF Protection

```java
server.enableCSRFProtection();
```

Once enabled, non-safe requests (`POST`, `PUT`, `DELETE`, etc.) require a `X-CSRF-TOKEN` header matching the `XSRF-TOKEN` cookie. Safe methods (`GET`, `HEAD`, `OPTIONS`) are exempt. Use `@NoCSRF` to disable CSRF on a specific endpoint.

Fetch the initial token: `GET /internal/csrf/token`.

### Rate Limiting

Annotation-based rate limiting with fixed or sliding window, per-IP or per-user, and `Retry-After` headers.

```java
// Basic — 5 attempts per 15 minutes, per IP
@RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
@RequestMapping(value = "/login", method = RequestMapping.Method.POST)
public ResponseEntity<?> login(...) { ... }

// Sliding window — more accurate counting than fixed window
@RateLimit(attempts = 100, timeout = 1, unit = TimeUnit.HOURS, slidingWindow = true)
@RequestMapping(value = "/api/search", method = RequestMapping.Method.GET)
public ResponseEntity<?> search(...) { ... }

// Per-user — keyed by authenticated userId instead of IP
@RateLimit(attempts = 10, timeout = 1, unit = TimeUnit.MINUTES, perUser = true)
@RequestMapping(value = "/api/export", method = RequestMapping.Method.GET)
public ResponseEntity<?> export(@AuthenticatedUser User user) { ... }

// Shared bucket — group multiple endpoints under the same limit
@RateLimit(attempts = 50, timeout = 1, unit = TimeUnit.HOURS, key = "api-write")
@RequestMapping(value = "/api/posts", method = RequestMapping.Method.POST)
public ResponseEntity<?> createPost(...) { ... }

@RateLimit(attempts = 50, timeout = 1, unit = TimeUnit.HOURS, key = "api-write")
@RequestMapping(value = "/api/comments", method = RequestMapping.Method.POST)
public ResponseEntity<?> createComment(...) { ... }

// Class-level — applies to all endpoints in the controller
@Controller("/api/heavy")
@RateLimit(attempts = 20, timeout = 1, unit = TimeUnit.MINUTES)
public class HeavyController { ... }
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `attempts` | `5` | Max requests per window |
| `timeout` | `15` | Window duration |
| `unit` | `MINUTES` | Time unit for timeout |
| `message` | `"Too many attempts..."` | Custom 429 error message |
| `slidingWindow` | `false` | Use sliding window instead of fixed |
| `perUser` | `false` | Key by userId (falls back to IP if unauthenticated) |
| `key` | `""` | Shared bucket key across endpoints |

When exceeded, returns 429 with a `Retry-After` header (seconds) and a JSON body:

```json
{"status": 429, "message": "Too many attempts. Please try again later.", "retryAfter": 42}
```

Rate limiting is proxy-aware (X-Forwarded-For, X-Real-IP). The counter resets on a 200 response.

### Audit Logging

Track security-relevant actions with `@AuditLog`. Logs are sent to SLF4J and to your custom `AuditLogService`.

```java
@AuditLog(action = "USER_LOGIN", logParameters = true, logResult = true, maskSensitiveData = true)
@RequestMapping(value = "/login", method = RequestMapping.Method.POST)
public ResponseEntity<?> login(...) { ... }
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `action` | (required) | Action name in the log entry |
| `logParameters` | `true` | Include method parameters |
| `logResult` | `true` | Include the response body |
| `maskSensitiveData` | `true` | Mask `@PasswordField` fields and keys containing `password`, `secret`, `token` |

#### Custom Data with AuditContext

Attach arbitrary data from inside any endpoint. The data is automatically collected by the `AuditLogProcessor` and included in the log entry:

```java
@AuditLog(action = "PAYMENT_PROCESSED")
@RequestMapping(value = "/pay", method = RequestMapping.Method.POST)
public ResponseEntity<?> pay(@RequestBody PaymentRequest req) {
    Payment result = paymentService.process(req);
    
    AuditContext.put("paymentId", result.getId());
    AuditContext.put("amount", result.getAmount());
    AuditContext.put("currency", result.getCurrency());

    return ResponseEntity.ok(result);
}
```

The `AuditContext` is thread-local and automatically cleared after each request — no cleanup needed.

#### Custom AuditLogService

```java
public class MyAuditLogService extends AuditLogService {
    @Override
    public void onAuditLog(AuditLog log) {
        System.out.println("[" + log.getAction() + "] " + log.getMethod() + " " + log.getUri());
        System.out.println("  IP: " + log.getIp());
        System.out.println("  Status: " + log.getStatus());
        
        // Custom data set by the endpoint
        if (log.getCustomData() != null) {
            log.getCustomData().forEach((k, v) -> System.out.println("  " + k + ": " + v));
        }
        
        // Persist to DB, send to ELK, etc.
    }
}

server.setAuditLogService(new MyAuditLogService());
```

The `AuditLog` object contains: `timestamp`, `ip`, `userAgent`, `method`, `uri`, `action`, `status`, `queryParams`, `pathVariables`, `requestBody`, `parameters`, `response`, `customData`, `rawLog`.

### Security Headers

Fiber automatically adds security headers to all responses: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`, `Content-Security-Policy`, `Cross-Origin-Opener-Policy`. Custom server header: `server.setServerHeader("MyApp")`.

## File Uploads

```java
@RequestMapping(value = "/upload", method = RequestMapping.Method.POST)
public ResponseEntity<?> upload(
        @FileUpload(maxSize = 5_242_880, allowedMimeTypes = {"image/jpeg", "image/png"})
        UploadedFile file) {

    file.moveTo(Path.of("/uploads/" + file.getOriginalFilename()));
    return ResponseEntity.ok(Map.of("filename", file.getOriginalFilename()));
}
```

Chunked uploads are supported via `uploadId`, `chunkIndex`, `totalChunks` query parameters. Incomplete uploads are automatically cleaned up after 24 hours.

## Email

```java
server.setEmailService(new EmailService(
    "smtp.example.com", "noreply@example.com", 465,
    "apikey", "password", true, true
));

Email email = new Email();
email.setTo("user@example.com");
email.setSubject("Welcome");
email.setContent("Welcome to our platform!");
email.setHtmlContent("<h1>Welcome!</h1>");

FiberServer.get().getEmailService().sendEmail(email); // async (CompletableFuture)
```

Template support with variable substitution (`{variable}`) and `@import` directives for partials.

## Middleware

```java
public class LoggingMiddleware implements Middleware {
    @Override public int priority() { return 10; }

    @Override
    public boolean handle(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println(req.getMethod() + " " + req.getRequestURI());
        return true; // continue processing (false = abort)
    }
}

server.addMiddleware(new LoggingMiddleware());
```

Middleware runs in priority order (lower = earlier) after authentication but before parameter resolution.

## API Documentation

Pass `true` as the second argument to `FiberServer`:

```java
FiberServer server = new FiberServer(8080, true);
```

- UI: `http://localhost:8080/docs/ui`
- Raw JSON: `http://localhost:8080/docs/api`

Add descriptions to endpoints via `@RequestMapping(description = "...")`.

## Full Example

```java
public class Main {
    public static void main(String[] args) throws Exception {
        // Database
        Architect architect = new Architect()
            .setDatabaseCredentials(new DatabaseCredentials(
                new PostgreSQLAuth("localhost", 5432, "mydb"),
                "postgres", "password", 16));
        architect.start();

        // Server
        FiberServer server = new FiberServer(8080, true);
        server.enableDevelopmentMode();

        // Auth
        UserRepository userRepo = new UserRepository();
        MyAuthService authService = new MyAuthService(userRepo);
        server.setAuthService(authService);
        server.getAuthResolver().registerAuthenticator(new CookieAuthenticator());
        server.getAuthResolver().registerAuthenticator(new BearerAuthenticator());

        // Sessions
        server.setSessionService(new SessionService(new GenericRepository<>(FiberSession.class)));

        // Security
        server.enableCSRFProtection();
        server.setCorsService(new CorsService()
            .addAllowedOrigin("http://localhost:3000")
            .setAllowCredentials(true));

        // Roles
        server.getRoleRegistry().registerRoleClasses(AdminRole.class, UserRole.class);

        // Controllers
        server.registerController(new AuthController());
        server.registerController(new UserController());
        server.registerController(new FileController());

        server.start();
    }
}
```

## License

Fiber is distributed under the MIT License. See `LICENSE` for details.
