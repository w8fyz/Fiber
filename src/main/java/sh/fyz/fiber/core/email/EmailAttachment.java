package sh.fyz.fiber.core.email;

import java.io.File;

public class EmailAttachment {
    private final File file;
    private final String fileName;

    public EmailAttachment(File file) {
        this(file, file.getName());
    }

    public EmailAttachment(File file, String fileName) {
        this.file = file;
        this.fileName = fileName;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }
} 