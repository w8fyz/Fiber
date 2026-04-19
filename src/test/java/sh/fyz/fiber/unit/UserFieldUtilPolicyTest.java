package sh.fyz.fiber.unit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;

import static org.junit.jupiter.api.Assertions.*;

class UserFieldUtilPolicyTest {

    @BeforeAll
    static void strict() {
        UserFieldUtil.setPasswordPolicy(10, true, true, true, true);
    }

    @AfterAll
    static void restore() {
        // Keep the library default (8, upper, digit, special, reject-blank) for the rest of the suite.
        UserFieldUtil.setPasswordPolicy(8, true, true, true, true);
    }

    @Test
    void blankPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength(""));
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength("   "));
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength(null));
    }

    @Test
    void tooShortRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength("Ab1!xyz"));
    }

    @Test
    void missingUppercaseRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength("abcdef1!23"));
    }

    @Test
    void missingDigitRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength("Abcdefghi!"));
    }

    @Test
    void missingSpecialRejected() {
        assertThrows(IllegalArgumentException.class, () -> UserFieldUtil.validatePasswordStrength("Abcdef1234"));
    }

    @Test
    void validPasswordAccepted() {
        assertDoesNotThrow(() -> UserFieldUtil.validatePasswordStrength("Abcdef1!23"));
    }
}
