package sh.fyz.fiber.core.authentication;

public class AuthCookieConfig {
    private SameSitePolicy sameSite = SameSitePolicy.STRICT;
    private boolean secure = true;
    private boolean httpOnly = true;
    private String domain;
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

    public AuthCookieConfig setDomain(String domain) {
        this.domain = domain;
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

    public String getDomain() {
        return domain;
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

    public String buildCookieAttributes() {
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
        
        if (domain != null && !domain.trim().isEmpty()) {
            attributes.append("; Domain=").append(domain.trim());
        }
        
        attributes.append("; Path=").append(path);
        
        return attributes.toString();
    }

    public String buildCookieAttributesWithMaxAge(int maxAge) {
        return buildCookieAttributes() + "; Max-Age=" + maxAge;
    }
} 