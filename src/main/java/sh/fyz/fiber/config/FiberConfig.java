package sh.fyz.fiber.config;

import sh.fyz.yellowconfig.Config;
import sh.fyz.yellowconfig.ConfigField;

public class FiberConfig extends Config {

    public FiberConfig() {
        loadInstance("fiberconfig", "Fiber", this);
    }

    @ConfigField
    private String JWT_SECRET_KEY = "your-secret-key-please-dont-use-that-in-prod";

    @ConfigField
    private long JWT_TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds

    @ConfigField
    private long JWT_REFRESH_TOKEN_VALIDITY = 7 * 24 * 3600000; // 7 days in milliseconds

    public String getJwtSecretKey() {
        return JWT_SECRET_KEY;
    }

    public long getJwtTokenValidity() {
        return JWT_TOKEN_VALIDITY;
    }

    public long getJwtRefreshTokenValidity() {
        return JWT_REFRESH_TOKEN_VALIDITY;
    }
}
