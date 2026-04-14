package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RolePermissionTest extends IntegrationTestBase {

    private static Map<String, String> adminCookies;
    private static Map<String, String> userCookies;
    private static final String ADMIN_USERNAME = "roleadmin" + RUN_ID;
    private static final String USER_USERNAME = "roleuser" + RUN_ID;

    @Test
    @Order(1)
    void testSetupRoleUsers() throws Exception {
        registerUser(ADMIN_USERNAME, ADMIN_USERNAME + "@test.com", "admin123", "admin");
        HttpResponse<String> adminLogin = loginUser(ADMIN_USERNAME, "admin123");
        assertEquals(200, adminLogin.statusCode(), "Admin login failed: " + adminLogin.body());
        adminCookies = extractCookies(adminLogin);

        registerUser(USER_USERNAME, USER_USERNAME + "@test.com", "user123", "user");
        HttpResponse<String> userLogin = loginUser(USER_USERNAME, "user123");
        assertEquals(200, userLogin.statusCode(), "User login failed: " + userLogin.body());
        userCookies = extractCookies(userLogin);
    }

    @Test
    @Order(2)
    void testAdminCanAccessAdminEndpoint() throws Exception {
        assertNotNull(adminCookies);
        HttpResponse<String> resp = get("/admin/stats",
                Map.of("Cookie", cookieHeader(adminCookies)));
        assertEquals(200, resp.statusCode(), "Admin access failed: " + resp.body());
        assertTrue(resp.body().contains("\"admin\""));
    }

    @Test
    @Order(3)
    void testUserCannotAccessAdminEndpoint() throws Exception {
        assertNotNull(userCookies);
        HttpResponse<String> resp = get("/admin/stats",
                Map.of("Cookie", cookieHeader(userCookies)));
        assertTrue(resp.statusCode() == 401 || resp.statusCode() == 403,
                "User with role 'user' should be denied access to admin endpoint, got: " + resp.statusCode() + " body: " + resp.body());
    }

    @Test
    @Order(4)
    void testUnauthenticatedCannotAccessAdminEndpoint() throws Exception {
        HttpResponse<String> resp = get("/admin/stats");
        assertEquals(401, resp.statusCode());
    }
}
