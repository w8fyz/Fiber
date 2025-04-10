package sh.fyz.fiber.core.security;

/**
 * Interface définissant une permission dans le système.
 * Les permissions peuvent être hiérarchiques, où une permission peut en impliquer d'autres.
 */
public interface Permission {
    /**
     * Obtient le nom de la permission
     * @return Le nom de la permission
     */
    String getName();

    /**
     * Vérifie si cette permission implique une autre permission
     * @param other La permission à vérifier
     * @return true si cette permission implique l'autre permission
     */
    boolean implies(Permission other);
} 