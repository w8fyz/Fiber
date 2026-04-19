package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.security.BCryptUtil;

import static org.junit.jupiter.api.Assertions.*;

class BCryptUtilTest {

    @Test
    void hashIsNotPlaintext() {
        String hash = BCryptUtil.hashPassword("hunter2!");
        assertNotNull(hash);
        assertNotEquals("hunter2!", hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void checkPasswordRoundTrip() {
        String hash = BCryptUtil.hashPassword("correct-horse-battery-staple");
        assertTrue(BCryptUtil.checkPassword("correct-horse-battery-staple", hash));
        assertFalse(BCryptUtil.checkPassword("wrong", hash));
    }

    @Test
    void checkPasswordNullSafe() {
        assertFalse(BCryptUtil.checkPassword(null, "$2a$10$abcdefghijklmnopqrstuu"));
        assertFalse(BCryptUtil.checkPassword("password", null));
        assertFalse(BCryptUtil.checkPassword("password", ""));
        assertFalse(BCryptUtil.checkPassword("password", "not-a-bcrypt-hash"));
    }

    @Test
    void legacy2aHashStillVerifies() {
        // Generate a $2a$ hash explicitly (legacy version used by jbcrypt) and make sure
        // BCryptUtil (built on at.favre.lib) still accepts it. This guarantees the
        // migration does not invalidate existing production hashes.
        String legacy = at.favre.lib.crypto.bcrypt.BCrypt.with(at.favre.lib.crypto.bcrypt.BCrypt.Version.VERSION_2A)
                .hashToString(10, "password".toCharArray());
        assertTrue(legacy.startsWith("$2a$"), "expected $2a$ prefix, got: " + legacy);
        assertTrue(BCryptUtil.checkPassword("password", legacy));
    }

    @Test
    void hashRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashPassword(null));
    }

    @Test
    void setCostRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.setCost(3));
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.setCost(32));
    }
}
