package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileUploadTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testUploadImageJpeg() throws Exception {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "photo.jpg", "image/jpeg", fakeJpeg);

        assertEquals(200, resp.statusCode(), "Body: " + resp.body());
        assertTrue(resp.body().contains("\"filename\""));
        assertTrue(resp.body().contains("photo.jpg"));
        assertTrue(resp.body().contains("\"complete\""));
    }

    @Test
    @Order(2)
    void testUploadImagePng() throws Exception {
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "image.png", "image/png", fakePng);

        assertEquals(200, resp.statusCode(), "Body: " + resp.body());
        assertTrue(resp.body().contains("image.png"));
    }

    @Test
    @Order(3)
    void testUploadTextPlain() throws Exception {
        byte[] text = "Hello, this is a test file.".getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "readme.txt", "text/plain", text);

        assertEquals(200, resp.statusCode(), "Body: " + resp.body());
        assertTrue(resp.body().contains("readme.txt"));
    }

    @Test
    @Order(4)
    void testUploadRejectedWrongMimeType() throws Exception {
        byte[] data = "not a pdf".getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "script.sh", "application/x-sh", data);

        assertNotEquals(200, resp.statusCode(), "Should reject disallowed MIME type");
    }

    @Test
    @Order(5)
    void testUploadRejectedTooLarge() throws Exception {
        byte[] oversized = new byte[200];
        HttpResponse<String> resp = postMultipart("/test/upload-restricted", "file", "big.pdf", "application/pdf", oversized);

        assertNotEquals(200, resp.statusCode(), "Should reject file exceeding maxSize");
    }

    @Test
    @Order(6)
    void testUploadRejectedNotMultipart() throws Exception {
        HttpResponse<String> resp = post("/test/upload", "{\"file\":\"not a file\"}");

        assertNotEquals(200, resp.statusCode(), "Should reject non-multipart request");
    }

    @Test
    @Order(7)
    void testUploadWildcardMimeMatching() throws Exception {
        byte[] fakeWebp = new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0};
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "photo.webp", "image/webp", fakeWebp);

        assertEquals(200, resp.statusCode(), "image/webp should match image/* wildcard. Body: " + resp.body());
        assertTrue(resp.body().contains("photo.webp"));
    }

    @Test
    @Order(8)
    void testUploadFilenameSanitized() throws Exception {
        byte[] data = new byte[]{1, 2, 3, 4};
        HttpResponse<String> resp = postMultipart("/test/upload", "file", "../../../etc/passwd", "image/png", data);

        assertEquals(200, resp.statusCode(), "Body: " + resp.body());
        String body = resp.body();
        int fnStart = body.indexOf("\"filename\"") + "\"filename\"".length();
        int fnValStart = body.indexOf("\"", fnStart) + 1;
        int fnValEnd = body.indexOf("\"", fnValStart);
        String filename = body.substring(fnValStart, fnValEnd);

        assertFalse(filename.contains(".."), "Path traversal dots should be sanitized, got: " + filename);
        assertFalse(filename.contains("/"), "Forward slashes should be sanitized, got: " + filename);
        assertFalse(filename.contains("\\"), "Backslashes should be sanitized, got: " + filename);
    }
}
