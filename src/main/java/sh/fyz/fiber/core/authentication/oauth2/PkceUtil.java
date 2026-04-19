package sh.fyz.fiber.core.authentication.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Small RFC 7636 (PKCE) helper used by {@link OAuth2ClientService}.
 *
 * <p>Exposed package-private-ish so that tests can exercise the verifier / challenge
 * contract without booting a full Fiber server.</p>
 */
public final class PkceUtil {

    /** RFC 7636 §4.1: code_verifier = 43–128 chars from unreserved set. */
    public static final Pattern CODE_VERIFIER_PATTERN =
            Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");

    public static final String METHOD_S256 = "S256";
    public static final String METHOD_PLAIN = "plain";

    private PkceUtil() {}

    /** @return {@code true} iff {@code method} is {@code S256} or {@code plain}. */
    public static boolean isSupportedMethod(String method) {
        return METHOD_S256.equals(method) || METHOD_PLAIN.equals(method);
    }

    /** @return the base64url(no-pad) SHA-256 hash of {@code input}. */
    public static String sha256Base64Url(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Constant-time verification of a PKCE {@code code_verifier} against the challenge
     * captured at {@code /authorize}.
     *
     * @param codeVerifier       the verifier sent by the client at {@code /token}
     * @param storedChallenge    the challenge originally sent at {@code /authorize}
     * @param challengeMethod    {@code S256} or {@code plain}
     * @return {@code true} iff the verifier syntactically matches and, after hashing as
     *         appropriate, equals the stored challenge.
     */
    public static boolean verify(String codeVerifier, String storedChallenge, String challengeMethod) {
        if (codeVerifier == null || storedChallenge == null) return false;
        if (!CODE_VERIFIER_PATTERN.matcher(codeVerifier).matches()) return false;
        String method = challengeMethod == null ? METHOD_PLAIN : challengeMethod;
        if (!isSupportedMethod(method)) return false;

        String computed = METHOD_S256.equals(method) ? sha256Base64Url(codeVerifier) : codeVerifier;
        byte[] a = computed.getBytes(StandardCharsets.UTF_8);
        byte[] b = storedChallenge.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
