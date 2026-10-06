package sh.fyz.fiber.core.upload;

import sh.fyz.fiber.core.log.FiberLog;

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
        // Daemon: this class-loaded singleton must not keep the JVM alive.
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fiber-upload-cleanup");
            t.setDaemon(true);
            return t;
        });
        startCleanupTask();
    }

    public static FileUploadManager getInstance() {
        return INSTANCE;
    }

    /**
     * Enregistre un nouveau fichier téléchargé.
     */
    public void registerUpload(UploadedFile file) {
        // Never replace a live upload: its files would be orphaned and its owner hijacked.
        if (uploads.putIfAbsent(file.getUploadId(), file) != null) {
            try {
                file.cleanup();
            } catch (Exception e) {
                FiberLog.handleSilent(e);
            }
            throw new IllegalArgumentException("Upload ID already in use");
        }
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
                FiberLog.handleSilent(e);
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

    private static final long COMPLETED_UPLOAD_TTL = 1; // hours

    private void cleanupOldUploads() {
        long now = System.currentTimeMillis();
        uploads.entrySet().removeIf(entry -> {
            UploadedFile file = entry.getValue();
            long ageMs = now - file.getCreatedAt();
            boolean shouldRemove = false;
            if (!file.isComplete() && ageMs > MAX_UPLOAD_AGE * 3600000) {
                shouldRemove = true;
            } else if (file.isComplete() && ageMs > COMPLETED_UPLOAD_TTL * 3600000) {
                shouldRemove = true;
            }
            if (shouldRemove) {
                try {
                    file.cleanup();
                } catch (Exception e) {
                    FiberLog.handleSilent(e);
                }
            }
            return shouldRemove;
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
                FiberLog.handleSilent(e);
            }
        });
        uploads.clear();
    }
} 