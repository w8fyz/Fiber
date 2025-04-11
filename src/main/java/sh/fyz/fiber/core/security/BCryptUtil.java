package sh.fyz.fiber.core.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Classe utilitaire pour gérer les opérations de hachage avec BCrypt.
 */
public class BCryptUtil {
    
    private BCryptUtil() {
        // Empêche l'instanciation
    }

    /**
     * Hache un mot de passe en utilisant BCrypt.
     * @param password Le mot de passe en clair à hacher
     * @return Le mot de passe haché
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hash.
     * @param password Le mot de passe en clair à vérifier
     * @param hashedPassword Le hash du mot de passe stocké
     * @return true si le mot de passe correspond, false sinon
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(password, hashedPassword);
    }
} 