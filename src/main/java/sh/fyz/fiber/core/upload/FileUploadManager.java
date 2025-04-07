package sh.fyz.fiber.core.upload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire des fichiers téléchargés.
 * Gère le stockage temporaire et le nettoyage des fichiers incomplets.
 */
public class FileUploadManager {
    private static final FileUploadManager INSTANCE = new FileUploadManager();
    private final Map<String, UploadedFile> uploads;
    private final ScheduledExecutorService cleanupExecutor;
    private static final long CLEANUP_INTERVAL = 1; // en heures
    private static final long MAX_UPLOAD_AGE = 24; // en heures

    private FileUploadManager() {
        this.uploads = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask();
    }

    public static FileUploadManager getInstance() {
        return INSTANCE;
    }

    /**
     * Enregistre un nouveau fichier téléchargé.
     */
    public void registerUpload(UploadedFile file) {
        uploads.put(file.getUploadId(), file);
    }

    /**
     * Récupère un fichier téléchargé par son ID.
     */
    public UploadedFile getUpload(String uploadId) {
        return uploads.get(uploadId);
    }

    /**
     * Supprime un fichier téléchargé du gestionnaire.
     */
    public void removeUpload(String uploadId) {
        UploadedFile file = uploads.remove(uploadId);
        if (file != null) {
            try {
                file.cleanup();
            } catch (Exception e) {
                // Ignorer les erreurs de nettoyage
            }
        }
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupOldUploads,
            CLEANUP_INTERVAL,
            CLEANUP_INTERVAL,
            TimeUnit.HOURS
        );
    }

    private void cleanupOldUploads() {
        long now = System.currentTimeMillis();
        uploads.entrySet().removeIf(entry -> {
            UploadedFile file = entry.getValue();
            if (!file.isComplete() && (now - file.getUploadId().hashCode()) > MAX_UPLOAD_AGE * 3600000) {
                try {
                    file.cleanup();
                } catch (Exception e) {
                    // Ignorer les erreurs de nettoyage
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Arrête le gestionnaire et nettoie les ressources.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        uploads.values().forEach(file -> {
            try {
                file.cleanup();
            } catch (Exception e) {
                // Ignorer les erreurs de nettoyage
            }
        });
        uploads.clear();
    }
} 