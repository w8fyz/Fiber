package sh.fyz.fiber.core;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.architect.repositories.GenericRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthenticationService<T extends UserAuth> {

    private GenericRepository<T> userRepository;

    public AuthenticationService(GenericRepository<T> userRepository) {
        this.userRepository = userRepository;
    }

    public Class<T> getUserClass() {
        return userRepository.getEntityClass();
    }

    public T getUserById(Object id) {
        return userRepository.findById(id);
    }

    public boolean validateCredentials(Object id, String password) {
        T user = userRepository.findById(id);
        if (user == null) {
            return false;
        }
        return UserFieldUtil.validatePassword(user, password);
    }

    public UserAuth findUserByIdentifer(String identifier) {
        return UserFieldUtil.findUserByIdentifier(identifier, userRepository.all());
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

    private String getClientIpAddress(HttpServletRequest request) {
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