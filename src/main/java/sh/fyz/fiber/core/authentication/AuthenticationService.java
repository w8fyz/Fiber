package sh.fyz.fiber.core.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthenticationService<T extends UserAuth> {

    private final GenericRepository<T> userRepository;
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastLoginAttempt = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_TIMEOUT_MINUTES = 15;
    private String refreshTokenPath = "/auth";



    /*
        * Constructor for AuthenticationService
        * @param userRepository The repository for user data
        * @param authEndpoint The generic endpoint for authentication, used to generate the refresh token path
     */
    public AuthenticationService(GenericRepository<T> userRepository, String authEndpoint) {
        this.userRepository = userRepository;
        this.refreshTokenPath = authEndpoint;
    }

    public Class<T> getUserClass() {
        return userRepository.getEntityClass();
    }

    public T getUserById(Object id) {
        return userRepository.findById(id);
    }

    public boolean validateCredentials(UserAuth user, String password) {
        return UserFieldUtil.validatePassword(user, password);
    }

    public UserAuth findUserByIdentifer(String identifier) {
        return UserFieldUtil.findUserByIdentifier(identifier, userRepository.all());
    }

    public boolean doesIdentifiersAlreadyExists(UserAuth user) {
        Map<String, String> identifiers = UserFieldUtil.getIdentifiers(user);
        for(Map.Entry<String, String> entry : identifiers.entrySet()) {
            if(findUserByIdentifer(entry.getValue()) != null) {
                return true;
            }
        }
        return false;
    }

    public String generateToken(UserAuth user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return JwtUtil.generateToken(user, ipAddress, userAgent);
    }

    public boolean validateToken(String token, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return JwtUtil.validateToken(token, ipAddress, userAgent);
    }

    public void setAuthCookies(UserAuth user, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = generateToken(user, request);
        String refreshToken = JwtUtil.generateRefreshToken(user, getClientIpAddress(request), request.getHeader("User-Agent"));
        
        // Set access token cookie
        response.addHeader("Set-Cookie", 
            "access_token=" + accessToken + 
            "; HttpOnly; "+(FiberServer.get().isDev() ? "SameSite=Lax;" : "Secure; SameSite=Strict;")+" Path=/; Max-Age=3600");
        
        // Set refresh token cookie
        response.addHeader("Set-Cookie", 
            "refresh_token=" + refreshToken + 
            "; HttpOnly; "+(FiberServer.get().isDev() ? "SameSite=Lax;" : "Secure; SameSite=Strict;")+" Path=" + refreshTokenPath + "; Max-Age=604800");
    }

    public void clearAuthCookies(HttpServletResponse response) {
        // Clear access token cookie
        response.addHeader("Set-Cookie", 
            "access_token=; "+(FiberServer.get().isDev() ? "SameSite=Lax;" : "Secure; SameSite=Strict;")+" Path=/; Max-Age=0");
        
        // Clear refresh token cookie
        response.addHeader("Set-Cookie", 
            "refresh_token=; HttpOnly; "+(FiberServer.get().isDev() ? "SameSite=Lax;" : "Secure; SameSite=Strict;")+" Path=" + refreshTokenPath + "; Max-Age=0");
    }


    public String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
} 