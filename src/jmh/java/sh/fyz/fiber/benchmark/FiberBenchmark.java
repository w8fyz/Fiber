package sh.fyz.fiber.benchmark;

import org.openjdk.jmh.annotations.*;
import sh.fyz.architect.Architect;
import sh.fyz.architect.persistent.DatabaseCredentials;
import sh.fyz.architect.persistent.sql.provider.PostgreSQLAuth;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.core.security.logging.AuditContext;
import sh.fyz.fiber.core.security.logging.AuditLogService;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionService;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.NotBlank;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class FiberBenchmark {

    private static final int PORT = 18090;
    private static final String BASE = "http://localhost:" + PORT;
    private static final String UA = "JMH-Benchmark/1.0";
    private FiberServer server;
    private Architect architect;
    private HttpClient client;
    private String accessToken;
    private String csrfToken;
    private String csrfCookie;

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "fiber_bench_users")
    public static class BenchUser implements sh.fyz.architect.entities.IdentifiableEntity, UserAuth {
        @jakarta.persistence.Id @jakarta.persistence.GeneratedValue
        private long id;
        @sh.fyz.fiber.annotations.auth.IdentifierField
        private String username;
        @sh.fyz.fiber.annotations.auth.PasswordField
        private String password;
        private String role;

        public BenchUser() {}
        @Override public Object getId() { return id; }
        @Override public String getRole() { return role; }
        public void setId(long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public void setRole(String role) { this.role = role; }
    }

    public static class BenchAuthService extends AuthenticationService<BenchUser> {
        private final GenericRepository<BenchUser> repo;
        public BenchAuthService(GenericRepository<BenchUser> repo) {
            super(repo, "/bench-auth");
            this.repo = repo;
        }
        public void saveUser(BenchUser user) { repo.save(user); }
    }

    public static class BenchAuditLogService extends AuditLogService {
        @Override public void onAuditLog(sh.fyz.fiber.core.security.logging.AuditLog log) { }
    }

    public static class BenchMiddleware implements Middleware {
        @Override public int priority() { return 10; }
        @Override public boolean handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            req.setAttribute("bench-mw", System.nanoTime());
            return true;
        }
    }

    public static class BenchRequestBody {
        @NotBlank private String username;
        private String email;
        public BenchRequestBody() {}
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @sh.fyz.fiber.annotations.request.Controller("/bench")
    public static class BenchController {

        //  Routing 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/hello", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public Map<String, String> hello() {
            return Map.of("message", "hello");
        }

        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/echo", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public Map<String, Object> echo(@sh.fyz.fiber.annotations.params.Param("name") String name) {
            return Map.of("name", name);
        }

        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/users/{id}", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public Map<String, String> userById(@sh.fyz.fiber.annotations.params.PathVariable("id") String id) {
            return Map.of("userId", id);
        }

        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/users/me", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public Map<String, String> currentUser() {
            return Map.of("userId", "me");
        }

        //  POST / JSON body 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/body", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.POST)
        @sh.fyz.fiber.annotations.security.NoCSRF
        public Map<String, Object> body(@sh.fyz.fiber.annotations.params.RequestBody Map<String, String> data) {
            return Map.of("received", data);
        }

        //  Validation 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/validated-body", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.POST)
        @sh.fyz.fiber.annotations.security.NoCSRF
        public Map<String, String> validatedBody(@sh.fyz.fiber.annotations.params.RequestBody BenchRequestBody body) {
            return Map.of("username", body.getUsername());
        }

        //  PUT / DELETE 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/update", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.PUT)
        @sh.fyz.fiber.annotations.security.NoCSRF
        public Map<String, String> update() {
            return Map.of("status", "updated");
        }

        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/delete/{id}", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.DELETE)
        @sh.fyz.fiber.annotations.security.NoCSRF
        public Map<String, String> delete(@sh.fyz.fiber.annotations.params.PathVariable("id") String id) {
            return Map.of("deleted", id);
        }

        //  Auth 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/protected", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        @sh.fyz.fiber.annotations.security.AuthType({AuthScheme.BEARER})
        public Map<String, Object> protectedEndpoint(@sh.fyz.fiber.annotations.params.AuthenticatedUser UserAuth user) {
            return Map.of("userId", user.getId());
        }

        //  Session 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/session", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        @sh.fyz.fiber.annotations.security.AuthType({AuthScheme.BEARER})
        public Map<String, Object> sessionEndpoint(@sh.fyz.fiber.annotations.params.CurrentSession FiberSession session) {
            return Map.of("sessionId", session.getSessionId());
        }

        //  Audit log 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/audited", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.POST)
        @sh.fyz.fiber.annotations.security.NoCSRF
        @AuditLog(action = "BENCH_ACTION", logParameters = true, logResult = true)
        public Map<String, String> audited(@sh.fyz.fiber.annotations.params.RequestBody Map<String, String> data) {
            AuditContext.put("benchKey", "benchVal");
            return Map.of("status", "audited");
        }

        //  Middleware attribute 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/middleware", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public Map<String, Object> middleware(HttpServletRequest request) {
            Object val = request.getAttribute("bench-mw");
            return Map.of("mw", val != null ? val : "none");
        }

        //  ResponseEntity 
        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/bytes", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public ResponseEntity<byte[]> bytesEndpoint() {
            byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
            return ResponseEntity.ok(png).contentType("image/png");
        }

        @sh.fyz.fiber.annotations.request.RequestMapping(value = "/status/{code}", method = sh.fyz.fiber.annotations.request.RequestMapping.Method.GET)
        public ResponseEntity<Map<String, String>> statusCode(@sh.fyz.fiber.annotations.params.PathVariable("code") String code) {
            int status = Integer.parseInt(code);
            return ResponseEntity.ok(Map.of("code", code)).status(status);
        }
    }

    @Setup(Level.Trial)
    public void setup() throws Exception {
        String dbHost = env("DB_HOST", "localhost");
        int dbPort = Integer.parseInt(env("DB_PORT", "5440"));
        String dbName = env("DB_NAME", "freshapi");
        String dbUser = env("DB_USER", "freshapi");
        String dbPassword = env("DB_PASSWORD", "freshapi");

        architect = new Architect()
                .setDatabaseCredentials(new DatabaseCredentials(
                        new PostgreSQLAuth(dbHost, dbPort, dbName),
                        dbUser, dbPassword, 4, 2));
        architect.start();

        GenericRepository<BenchUser> userRepo = new GenericRepository<>(BenchUser.class);
        GenericRepository<FiberSession> sessionRepo = new GenericRepository<>(FiberSession.class);

        server = new FiberServer(PORT);
        server.enableDevelopmentMode();

        BenchAuthService authService = new BenchAuthService(userRepo);
        server.setAuthService(authService);
        server.setSessionService(new SessionService(sessionRepo));
        server.setAuditLogService(new BenchAuditLogService());

        server.setCorsService(new CorsService()
                .allowNullOrigin()
                .addAllowedOrigin("*")
                .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"))
                .setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-CSRF-TOKEN", "User-Agent"))
                .setAllowCredentials(false));

        server.enableCSRFProtection();
        server.addMiddleware(new BenchMiddleware());
        server.registerController(new BenchController());
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        BenchUser user = new BenchUser();
        user.setUsername("benchuser" + System.currentTimeMillis());
        user.setRole("user");
        UserFieldUtil.setPassword(user, "benchpass");
        authService.saveUser(user);

        accessToken = sh.fyz.fiber.core.JwtUtil.generateToken(user, "127.0.0.1", UA);

        HttpResponse<String> csrfResp = sendGet("/internal/csrf/token");
        if (csrfResp.statusCode() == 200) {
            String body = csrfResp.body();
            int idx = body.indexOf("\"token\"");
            if (idx != -1) {
                int qs = body.indexOf("\"", body.indexOf(":", idx) + 1);
                int qe = body.indexOf("\"", qs + 1);
                csrfToken = body.substring(qs + 1, qe);
            }
            csrfResp.headers().allValues("set-cookie").stream()
                    .filter(c -> c.startsWith("CSRF-TOKEN="))
                    .findFirst()
                    .ifPresent(c -> csrfCookie = c.split(";")[0].split("=", 2)[1]);
        }

        Thread.sleep(500);
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        if (server != null) server.stop();
    }

    @Benchmark
    public HttpResponse<String> simpleGet() throws Exception {
        return sendGet("/bench/hello");
    }

    @Benchmark
    public HttpResponse<String> queryParameter() throws Exception {
        return sendGet("/bench/echo?name=Alice");
    }

    @Benchmark
    public HttpResponse<String> pathVariable() throws Exception {
        return sendGet("/bench/users/123");
    }

    @Benchmark
    public HttpResponse<String> staticVsParameterized() throws Exception {
        return sendGet("/bench/users/me");
    }

    @Benchmark
    public HttpResponse<String> notFound() throws Exception {
        return sendGet("/bench/nonexistent");
    }

    //  HTTP methods 

    @Benchmark
    public HttpResponse<String> postJson() throws Exception {
        return sendPost("/bench/body", "{\"key\":\"value\"}");
    }

    @Benchmark
    public HttpResponse<String> putMethod() throws Exception {
        return sendPut("/bench/update", "{}");
    }

    @Benchmark
    public HttpResponse<String> deleteMethod() throws Exception {
        return sendDelete("/bench/delete/42");
    }

    //  Validation 

    @Benchmark
    public HttpResponse<String> validatedBody() throws Exception {
        return sendPost("/bench/validated-body", "{\"username\":\"valid\",\"email\":\"a@b.com\"}");
    }

    @Benchmark
    public HttpResponse<String> validationRejection() throws Exception {
        return sendPost("/bench/validated-body", "{\"username\":\"\",\"email\":\"a@b.com\"}");
    }

    //  Auth 

    @Benchmark
    public HttpResponse<String> authenticatedGet() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/bench/protected"))
                .GET()
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Benchmark
    public HttpResponse<String> unauthenticatedGet() throws Exception {
        return sendGet("/bench/protected");
    }

    //  Session 

    @Benchmark
    public HttpResponse<String> sessionGet() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/bench/session"))
                .GET()
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    //  Audit log 

    @Benchmark
    public HttpResponse<String> auditedPost() throws Exception {
        return sendPost("/bench/audited", "{\"data\":\"test\"}");
    }

    //  Middleware 

    @Benchmark
    public HttpResponse<String> middlewareGet() throws Exception {
        return sendGet("/bench/middleware");
    }

    //  Response types 

    @Benchmark
    public HttpResponse<byte[]> binaryResponse() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/bench/bytes"))
                .GET()
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    @Benchmark
    public HttpResponse<String> customStatusCode() throws Exception {
        return sendGet("/bench/status/201");
    }

    //  CORS 

    @Benchmark
    public HttpResponse<String> corsPreflightRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/bench/hello"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://allowed-origin.com")
                .header("Access-Control-Request-Method", "GET")
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPut(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendDelete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .header("User-Agent", UA)
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null && !val.isEmpty() ? val : defaultValue;
    }
}
