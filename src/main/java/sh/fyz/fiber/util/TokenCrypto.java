package sh.fyz.fiber.util;

import sh.fyz.fiber.FiberServer;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM envelope for persisted OAuth2 provider tokens.
 *
 * <p>Key is HKDF-SHA256-derived from the configured JWT secret with the info
 * string {@code "oauth2-token"}. A new random 12-byte IV is generated per
 * encryption; output format is {@code base64(iv || ciphertext || 16-byte-tag)}.
 *
 * <p>Rationale: refresh tokens are long-lived bearer credentials. A DB dump
 * must not hand an attacker usable provider-level access.
 */
public final class TokenCrypto {

    private static final String HKDF_INFO = "oauth2-token";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RNG = new SecureRandom();
    private static volatile SecretKeySpec cachedKey;

    private TokenCrypto() {
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RNG.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("TokenCrypto encrypt failed", e);
        }
    }

    public static String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encoded);
            if (raw.length < IV_LENGTH + 16) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            byte[] iv = Arrays.copyOfRange(raw, 0, IV_LENGTH);
            byte[] ct = Arrays.copyOfRange(raw, IV_LENGTH, raw.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("TokenCrypto decrypt failed", e);
        }
    }

    private static SecretKeySpec key() {
        SecretKeySpec k = cachedKey;
        if (k != null) {
            return k;
        }
        synchronized (TokenCrypto.class) {
            if (cachedKey == null) {
                String secret = FiberServer.get().getConfig().getJwtSecretKey();
                byte[] ikm = secret.getBytes(StandardCharsets.UTF_8);
                byte[] derived = hkdf(ikm, HKDF_INFO.getBytes(StandardCharsets.UTF_8), KEY_LENGTH);
                cachedKey = new SecretKeySpec(derived, "AES");
            }
            return cachedKey;
        }
    }

    /** HKDF-SHA256 with empty salt — sufficient since the IKM is already high-entropy. */
    private static byte[] hkdf(byte[] ikm, byte[] info, int length) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] salt = new byte[mac.getMacLength()];
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] prk = mac.doFinal(ikm);

            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            byte[] okm = new byte[length];
            byte[] t = new byte[0];
            int pos = 0;
            for (int i = 1; pos < length; i++) {
                mac.reset();
                mac.update(t);
                mac.update(info);
                mac.update((byte) i);
                t = mac.doFinal();
                int copy = Math.min(t.length, length - pos);
                System.arraycopy(t, 0, okm, pos, copy);
                pos += copy;
            }
            return okm;
        } catch (Exception e) {
            throw new IllegalStateException("HKDF failed", e);
        }
    }

    /** Test hook — drop cached key so tests rebuilding FiberConfig see a fresh derivation. */
    public static void resetForTesting() {
        cachedKey = null;
    }

    /** Test hook — inject a secret directly, bypassing FiberServer lookup. */
    public static void setSecretForTesting(String secret) {
        byte[] ikm = secret.getBytes(StandardCharsets.UTF_8);
        byte[] derived = hkdf(ikm, HKDF_INFO.getBytes(StandardCharsets.UTF_8), KEY_LENGTH);
        cachedKey = new SecretKeySpec(derived, "AES");
    }
}
