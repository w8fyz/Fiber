package sh.fyz.fiber.core.authentication;

import java.net.URI;
import java.util.ArrayList;

public class AuthCookieConfig {
    private SameSitePolicy sameSite = SameSitePolicy.STRICT;
    private boolean secure = true;
    private boolean httpOnly = true;
    private ArrayList<String> domains = new ArrayList<>();
    private String path = "/";
    private int accessTokenMaxAge = 3600; // 1 hour
    private int refreshTokenMaxAge = 604800; // 7 days

    public AuthCookieConfig() {}

    public AuthCookieConfig setSameSite(SameSitePolicy sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    public AuthCookieConfig setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public AuthCookieConfig setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public AuthCookieConfig addDomain(String domain) {
        this.domains.add(domain);
        return this;
    }

    public AuthCookieConfig setDomains(ArrayList<String> domains) {
        this.domains = domains;
        return this;
    }

    public AuthCookieConfig setPath(String path) {
        this.path = path;
        return this;
    }

    public AuthCookieConfig setAccessTokenMaxAge(int accessTokenMaxAge) {
        this.accessTokenMaxAge = accessTokenMaxAge;
        return this;
    }

    public AuthCookieConfig setRefreshTokenMaxAge(int refreshTokenMaxAge) {
        this.refreshTokenMaxAge = refreshTokenMaxAge;
        return this;
    }

    public SameSitePolicy getSameSite() {
        return sameSite;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public ArrayList<String> getDomains() {
        return domains;
    }

    public String getPath() {
        return path;
    }

    public int getAccessTokenMaxAge() {
        return accessTokenMaxAge;
    }

    public int getRefreshTokenMaxAge() {
        return refreshTokenMaxAge;
    }

    private String toCookieDomain(String origin) {
        try {
            URI uri = new URI(origin);
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                return null;
            }
            return "." + host;
        } catch (Exception e) {
            return null;
        }
    }


    public String buildCookieAttributes(String domain) {
        StringBuilder attributes = new StringBuilder();
        
        if (httpOnly) {
            attributes.append("; HttpOnly");
        }
        
        if (secure) {
            attributes.append("; Secure");
        }
        
        if (sameSite != null) {
            attributes.append("; SameSite=").append(sameSite.getValue());
        }

        String formatedDomain = toCookieDomain(domain);

        if (domain != null && !domain.trim().isEmpty() && domains.contains(formatedDomain)) {
            attributes.append("; Domain=").append(domain.trim());
        }
        
        attributes.append("; Path=").append(path);
        
        return attributes.toString();
    }

    public String buildCookieAttributesWithMaxAge(String origin, int maxAge) {
        return buildCookieAttributes(origin) + "; Max-Age=" + maxAge;
    }
} 