package sh.fyz.fiber.annotations.params;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileUpload {
    /**
     * Taille maximale du fichier en octets
     * -1 signifie pas de limite
     */
    long maxSize() default -1;

    /**
     * Types MIME autorisés
     * Un tableau vide signifie tous les types
     */
    String[] allowedMimeTypes() default {};

    /**
     * Taille maximale d'un chunk en octets
     * -1 signifie pas de chunking
     */
    int maxChunkSize() default -1;
} 