package sh.fyz.fiber.core.authentication.oauth2;

import java.util.Map;

/**
 * Result of an OAuth2 provider callback: the userInfo map returned by the
 * provider's user endpoint plus the full token response (access, refresh,
 * expiry, scope) needed to persist long-lived credentials per user.
 */
public record OAuth2CallbackResult(Map<String, Object> userInfo, OAuth2TokenResponse tokens) {
}
