package sh.fyz.fiber.core.upload;

import jakarta.servlet.http.Part;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Represents an uploaded file, possibly delivered in multiple chunks.
 *
 * <p>Mutable counters and the {@code isComplete} flag are {@code volatile} so that
 * concurrent readers (e.g. the cleanup scheduler) always observe a consistent state.
 * Chunks are appended to a {@code .stage} file and atomically renamed to the final
 * temporary file when the upload completes — the consumer never sees a half-written
 * file.</p>
 */
public class UploadedFile {
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final Path stageFile;
    private final Path tempFile;
    private final String uploadId;
    private final long createdAt;
    private volatile boolean isComplete;
    private volatile int totalChunks;
    private volatile int receivedChunks;

    public UploadedFile(Part part, String uploadId, int totalChunks) throws IOException {
        if (totalChunks < 1) {
            throw new IllegalArgumentException("totalChunks must be >= 1");
        }
        this.originalFilename = sanitizeFilename(part.getSubmittedFileName());
        this.contentType = part.getContentType();
        this.size = part.getSize();
        this.uploadId = uploadId != null ? uploadId : UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.totalChunks = totalChunks;
        this.receivedChunks = 0;
        this.isComplete = totalChunks <= 1;

        this.tempFile = Files.createTempFile("upload_" + this.uploadId + "_", "_" + this.originalFilename);
        this.stageFile = isComplete ? tempFile
                : tempFile.resolveSibling(tempFile.getFileName().toString() + ".stage");

        if (isComplete) {
            try (InputStream in = part.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                in.transferTo(out);
            }
        } else {
            // Pre-create the staging file so subsequent appends find it.
            Files.deleteIfExists(stageFile);
            Files.createFile(stageFile);
        }
    }

    /**
     * Append a chunk and re-check {@code totalChunks} hasn't been altered between calls.
     */
    public synchronized void addChunk(Part part, int chunkIndex, int expectedTotalChunks) throws IOException {
        if (isComplete) {
            throw new IllegalStateException("File is already complete");
        }
        if (expectedTotalChunks != this.totalChunks) {
            throw new IllegalArgumentException(
                    "totalChunks mismatch: expected " + this.totalChunks + " but got " + expectedTotalChunks);
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("chunkIndex out of bounds: " + chunkIndex);
        }
        if (chunkIndex != receivedChunks) {
            throw new IllegalArgumentException(
                    "Chunk received out of order (expected " + receivedChunks + ", got " + chunkIndex + ")");
        }

        try (InputStream in = part.getInputStream();
             OutputStream out = new FileOutputStream(stageFile.toFile(), true)) {
            in.transferTo(out);
        }

        receivedChunks++;
        if (receivedChunks == totalChunks) {
            // Promote the staging file atomically to the final location.
            Files.move(stageFile, tempFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            isComplete = true;
        }
    }

    /** Backwards-compat overload — assumes totalChunks unchanged. */
    public synchronized void addChunk(Part part, int chunkIndex) throws IOException {
        addChunk(part, chunkIndex, totalChunks);
    }

    public void moveTo(Path destination) throws IOException {
        if (!isComplete) {
            throw new IllegalStateException("File is not yet complete");
        }
        Files.move(tempFile, destination);
    }

    public void cleanup() throws IOException {
        Files.deleteIfExists(tempFile);
        if (stageFile != null && !stageFile.equals(tempFile)) {
            Files.deleteIfExists(stageFile);
        }
    }

    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getUploadId() { return uploadId; }
    public boolean isComplete() { return isComplete; }
    public int getTotalChunks() { return totalChunks; }
    public int getReceivedChunks() { return receivedChunks; }
    public long getCreatedAt() { return createdAt; }

    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(tempFile);
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        filename = filename.replace("/", "").replace("\\", "");
        filename = filename.replace("..", "");
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (filename.isEmpty()) {
            return "unnamed";
        }
        return filename;
    }
}
