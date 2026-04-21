package sh.fyz.fiber.unit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.util.TokenCrypto;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class TokenCryptoTest {

    private static final String TEST_SECRET = "test-secret-abcdefghijklmnopqrstuvwxyz-0123456789";

    @BeforeAll
    static void setUp() {
        TokenCrypto.setSecretForTesting(TEST_SECRET);
    }

    @AfterAll
    static void tearDown() {
        TokenCrypto.resetForTesting();
    }

    @Test
    void roundTripRecoversPlaintext() {
        String plaintext = "discord-access-token-abcdef.ghijkl.mnopqr";
        String ct = TokenCrypto.encrypt(plaintext);
        assertNotNull(ct);
        assertNotEquals(plaintext, ct);
        assertEquals(plaintext, TokenCrypto.decrypt(ct));
    }

    @Test
    void nullPassesThrough() {
        assertNull(TokenCrypto.encrypt(null));
        assertNull(TokenCrypto.decrypt(null));
    }

    @Test
    void freshIvPerEncryption() {
        String plaintext = "same-value";
        String a = TokenCrypto.encrypt(plaintext);
        String b = TokenCrypto.encrypt(plaintext);
        assertNotEquals(a, b, "AES-GCM must use a fresh IV per encryption");
        assertEquals(plaintext, TokenCrypto.decrypt(a));
        assertEquals(plaintext, TokenCrypto.decrypt(b));
    }

    @Test
    void tamperedCiphertextRejected() {
        String ct = TokenCrypto.encrypt("payload");
        byte[] raw = Base64.getDecoder().decode(ct);
        // Flip a byte inside the authenticated portion.
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> TokenCrypto.decrypt(tampered));
    }
}
