package sh.fyz.fiber.core.authentication.impl;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationAuthenticator;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicAuthenticator implements OAuth2ApplicationAuthenticator {
    private final Map<String, OAuth2ApplicationInfo> applications = new ConcurrentHashMap<>();

    public void registerApplication(OAuth2ApplicationInfo appInfo) {
        applications.put(appInfo.getClientId(), appInfo);
    }

    public OAuth2ApplicationInfo getApplicationInfo(String clientId) {
        return applications.get(clientId);
    }

    @Override
    public OAuth2ApplicationInfo authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials;
            try {
                credentials = new String(Base64.getDecoder().decode(base64Credentials));
            } catch (Exception e) {
                return null;
            }
            String[] values = credentials.split(":", 2);
            
            if (values.length == 2) {
                String clientId = values[0];
                String clientSecret = values[1];
                
                OAuth2ApplicationInfo appInfo = applications.get(clientId);
                if (appInfo != null && appInfo.getClientSecret().equals(clientSecret)) {
                    return appInfo;
                }
            }
        }
        return null;
    }
} 