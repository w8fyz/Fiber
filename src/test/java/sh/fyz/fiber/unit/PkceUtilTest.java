package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.authentication.oauth2.PkceUtil;

import static org.junit.jupiter.api.Assertions.*;

class PkceUtilTest {

    // Reference vector from RFC 7636 §4.1/§4.2.
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE_S256 = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void s256VectorMatches() {
        assertEquals(CHALLENGE_S256, PkceUtil.sha256Base64Url(VERIFIER));
        assertTrue(PkceUtil.verify(VERIFIER, CHALLENGE_S256, "S256"));
    }

    @Test
    void s256RejectsWrongVerifier() {
        assertFalse(PkceUtil.verify(VERIFIER + "x", CHALLENGE_S256, "S256"));
    }

    @Test
    void plainMatchesVerifierDirectly() {
        assertTrue(PkceUtil.verify(VERIFIER, VERIFIER, "plain"));
        assertFalse(PkceUtil.verify(VERIFIER, "different_verifier_but_correctly_formatted_aaaaaaaa", "plain"));
    }

    @Test
    void shortOrMalformedVerifierIsRejected() {
        assertFalse(PkceUtil.verify("short", CHALLENGE_S256, "S256"));
        assertFalse(PkceUtil.verify("contains spaces which are illegal!!!!!!!!!!!!!!!", CHALLENGE_S256, "S256"));
        assertFalse(PkceUtil.verify(null, CHALLENGE_S256, "S256"));
    }

    @Test
    void unknownMethodRejected() {
        assertFalse(PkceUtil.verify(VERIFIER, CHALLENGE_S256, "md5"));
    }

    @Test
    void isSupportedMethodSelective() {
        assertTrue(PkceUtil.isSupportedMethod("S256"));
        assertTrue(PkceUtil.isSupportedMethod("plain"));
        assertFalse(PkceUtil.isSupportedMethod("S512"));
        assertFalse(PkceUtil.isSupportedMethod(null));
    }
}
