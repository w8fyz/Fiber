package sh.fyz.fiber.core.upload;

import jakarta.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Représente un fichier téléchargé, potentiellement en plusieurs chunks.
 */
public class UploadedFile {
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final Path tempFile;
    private final String uploadId;
    private boolean isComplete;
    private int totalChunks;
    private int receivedChunks;

    public UploadedFile(Part part, String uploadId, int totalChunks) throws IOException {
        this.originalFilename = part.getSubmittedFileName();
        this.contentType = part.getContentType();
        this.size = part.getSize();
        this.uploadId = uploadId != null ? uploadId : UUID.randomUUID().toString();
        this.totalChunks = totalChunks;
        this.receivedChunks = 0;
        this.isComplete = totalChunks <= 1;

        // Créer un fichier temporaire
        this.tempFile = Files.createTempFile("upload_" + this.uploadId + "_", "_" + this.originalFilename);
        
        // Si c'est un upload simple (pas de chunks), écrire directement le contenu
        if (isComplete) {
            try (InputStream in = part.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                in.transferTo(out);
            }
        }
    }

    /**
     * Ajoute un chunk au fichier.
     */
    public synchronized void addChunk(Part part, int chunkIndex) throws IOException {
        if (isComplete) {
            throw new IllegalStateException("Le fichier est déjà complet");
        }

        // Vérifier que le chunk est dans le bon ordre
        if (chunkIndex != receivedChunks) {
            throw new IllegalArgumentException("Chunk reçu dans le mauvais ordre");
        }

        // Écrire le chunk à la fin du fichier
        try (InputStream in = part.getInputStream();
             OutputStream out = new FileOutputStream(tempFile.toFile(), true)) {
            in.transferTo(out);
        }

        receivedChunks++;
        if (receivedChunks == totalChunks) {
            isComplete = true;
        }
    }

    /**
     * Déplace le fichier vers son emplacement final.
     */
    public void moveTo(Path destination) throws IOException {
        if (!isComplete) {
            throw new IllegalStateException("Le fichier n'est pas encore complet");
        }
        Files.move(tempFile, destination);
    }

    /**
     * Supprime le fichier temporaire.
     */
    public void cleanup() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    // Getters
    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getUploadId() {
        return uploadId;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getReceivedChunks() {
        return receivedChunks;
    }

    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(tempFile);
    }
} 