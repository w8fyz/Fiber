package sh.fyz.fiber.core.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt password hashing helper backed by {@code at.favre.lib:bcrypt}.
 *
 * <p>The work factor is configurable via {@link #setCost(int)} and defaults to {@code 12},
 * which matches the OWASP 2024 recommendation for interactive logins. Hashes generated
 * by the previous {@code jbcrypt} implementation ({@code $2a$}) remain verifiable.</p>
 */
public class BCryptUtil {

    private static volatile int cost = 12;

    private BCryptUtil() {}

    /** @return current BCrypt work factor (cost). */
    public static int getCost() {
        return cost;
    }

    /**
     * Override the BCrypt work factor used for new hashes. Must be in {@code [4, 31]}.
     */
    public static void setCost(int newCost) {
        if (newCost < 4 || newCost > 31) {
            throw new IllegalArgumentException("BCrypt cost must be between 4 and 31");
        }
        cost = newCost;
    }

    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.withDefaults().hashToString(cost, password.toCharArray());
    }

    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer()
                    .verify(password.getBytes(StandardCharsets.UTF_8),
                            hashedPassword.getBytes(StandardCharsets.UTF_8));
            return result.verified;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
