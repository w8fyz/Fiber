package sh.fyz.fiber;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import sh.fyz.architect.Architect;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionService;
import sh.fyz.fiber.test.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    protected static final int PORT = 18080;
    protected static final String RUN_ID = String.valueOf(System.currentTimeMillis() % 100000);
    private static final AtomicBoolean started = new AtomicBoolean(false);
    protected static FiberServer server;
    protected static Architect architect;
    protected static HttpClient client;
    protected static GenericRepository<TestUser> userRepository;
    protected static GenericRepository<FiberSession> sessionRepository;
    protected static SessionService sessionService;
    protected static TestAuthService authService;
    protected static TestAuditLogService auditLogService;

    @BeforeAll
    void setupAll() throws Exception {
        if (!started.compareAndSet(false, true)) {
            // Already initialized by another test class
            if (client == null) {
                client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
            }
            return;
        }

        String dbHost = env("DB_HOST", "localhost");
        int dbPort = Integer.parseInt(env("DB_PORT", "5440"));
        String dbName = env("DB_NAME", "freshapi");
        String dbUser = env("DB_USER", "freshapi");
        String dbPassword = env("DB_PASSWORD", "freshapi");

        architect = new Architect()
                .setDatabaseCredentials(
                        new DatabaseCredentials(
                                new PostgreSQLAuth(dbHost, dbPort, dbName),
                                dbUser, dbPassword, 4, 2));
        architect.start();

        userRepository = new GenericRepository<>(TestUser.class);
        sessionRepository = new GenericRepository<>(FiberSession.class);
        sessionService = new SessionService(sessionRepository);

        server = new FiberServer(PORT);
        server.enableDevelopmentMode();

        authService = new TestAuthService(userRepository);
        server.setAuthService(authService);
        server.setSessionService(sessionService);

        server.setCorsService(new CorsService()
                .allowNullOrigin()
                .addAllowedOrigin("http://localhost:" + PORT)
                .addAllowedOrigin("http://allowed-origin.com")
                .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"))
                .setAllowedHeaders(Arrays.asList(
                        "Content-Type", "Authorization", "X-CSRF-TOKEN",
                        "X-Requested-With", "Accept", "Origin"))
                .setAllowCredentials(true)
                .setMaxAge(3600));

        auditLogService = new TestAuditLogService();
        server.setAuditLogService(auditLogService);

        server.enableCSRFProtection();

        server.getRoleRegistry().registerRoleClasses(
                TestUserRole.class,
                TestAdminRole.class
        );

        server.addMiddleware(new TestMiddleware());

        server.registerController(new TestController());
        server.registerController(new TestAdminController());
        server.registerController(new TestAuthController(authService));

        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Thread.sleep(500);
    }

    @AfterAll
    void teardownAll() throws Exception {
        // Don't stop server here since it's shared across test classes
    }

    protected String baseUrl() {
        return "http://localhost:" + PORT;
    }

    protected HttpResponse<String> get(String path) throws Exception {
        return get(path, Map.of());
    }

    private static final String TEST_USER_AGENT = "FiberTestClient/1.0";

    protected HttpResponse<String> get(String path, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .GET()
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<byte[]> getBytes(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .GET()
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    protected HttpResponse<String> post(String path, String jsonBody) throws Exception {
        return post(path, jsonBody, Map.of());
    }

    protected HttpResponse<String> post(String path, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> put(String path, String jsonBody) throws Exception {
        return put(path, jsonBody, Map.of());
    }

    protected HttpResponse<String> put(String path, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> delete(String path) throws Exception {
        return delete(path, Map.of());
    }

    protected HttpResponse<String> delete(String path, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .DELETE()
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> options(String path, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", TEST_USER_AGENT)
                .timeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected Map<String, String> extractCookies(HttpResponse<?> response) {
        Map<String, String> cookies = new HashMap<>();
        List<String> setCookieHeaders = response.headers().allValues("set-cookie");
        for (String header : setCookieHeaders) {
            String[] parts = header.split(";")[0].split("=", 2);
            if (parts.length == 2) {
                cookies.put(parts[0].trim(), parts[1].trim());
            }
        }
        return cookies;
    }

    protected String cookieHeader(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    protected HttpResponse<String> registerUser(String username, String email, String password, String role) throws Exception {
        String json = String.format(
                "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                username, email, password, role);
        return post("/test-auth/register", json);
    }

    protected HttpResponse<String> loginUser(String username, String password) throws Exception {
        String json = String.format(
                "{\"identifier\":\"%s\",\"password\":\"%s\"}",
                username, password);
        return post("/test-auth/login", json);
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null && !val.isEmpty() ? val : defaultValue;
    }
}
