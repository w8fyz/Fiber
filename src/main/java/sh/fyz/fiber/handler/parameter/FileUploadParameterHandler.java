package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import sh.fyz.fiber.annotations.params.FileUpload;
import sh.fyz.fiber.core.upload.FileUploadManager;
import sh.fyz.fiber.core.upload.UploadedFile;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.regex.Matcher;

/**
 * Handler pour les paramètres annotés avec @FileUpload.
 */
public class FileUploadParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(FileUpload.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        if (!request.getContentType().startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Le contenu doit être multipart/form-data");
        }

        FileUpload annotation = parameter.getAnnotation(FileUpload.class);
        String uploadId = request.getParameter("uploadId");
        String chunkIndexStr = request.getParameter("chunkIndex");
        String totalChunksStr = request.getParameter("totalChunks");

        // Si on a déjà un uploadId, c'est un chunk d'un fichier existant
        if (uploadId != null && chunkIndexStr != null && totalChunksStr != null) {
            int chunkIndex = Integer.parseInt(chunkIndexStr);
            int totalChunks = Integer.parseInt(totalChunksStr);

            UploadedFile existingFile = FileUploadManager.getInstance().getUpload(uploadId);
            if (existingFile == null) {
                throw new IllegalArgumentException("Upload ID invalide");
            }

            Part filePart = request.getPart(parameter.getName());
            validateFilePart(filePart, annotation);

            existingFile.addChunk(filePart, chunkIndex);
            return existingFile;
        }
        // Sinon, c'est un nouveau fichier
        else {
            Part filePart = request.getPart(parameter.getName());
            validateFilePart(filePart, annotation);

            int totalChunks = 1;
            if (annotation.maxChunkSize() > 0 && filePart.getSize() > annotation.maxChunkSize()) {
                totalChunks = (int) Math.ceil((double) filePart.getSize() / annotation.maxChunkSize());
            }

            UploadedFile file = new UploadedFile(filePart, uploadId, totalChunks);
            FileUploadManager.getInstance().registerUpload(file);
            return file;
        }
    }

    private void validateFilePart(Part filePart, FileUpload annotation) {
        if (filePart == null) {
            throw new IllegalArgumentException("No file was sent");
        }

        if (annotation.maxSize() > 0 && filePart.getSize() > annotation.maxSize()) {
            throw new IllegalArgumentException("File exceeds maximum allowed size");
        }

        if (annotation.allowedMimeTypes().length > 0) {
            String contentType = filePart.getContentType();
            if (contentType == null || !matchesMimeType(contentType, annotation.allowedMimeTypes())) {
                throw new IllegalArgumentException("File type not allowed");
            }
        }
    }

    private boolean matchesMimeType(String contentType, String[] allowed) {
        for (String pattern : allowed) {
            if (pattern.equals(contentType)) return true;
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (contentType.startsWith(prefix)) return true;
            }
            if (pattern.equals("*/*")) return true;
        }
        return false;
    }
} 