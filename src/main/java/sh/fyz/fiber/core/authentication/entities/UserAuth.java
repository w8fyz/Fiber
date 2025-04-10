package sh.fyz.fiber.core.authentication.entities;

import java.util.Set;

/**
 * Interface définissant les informations d'authentification de base d'un utilisateur.
 * Cette interface peut être étendue par votre classe User pour fournir des données supplémentaires.
 */
public interface UserAuth {
    /**
     * Obtient l'identifiant unique de l'utilisateur
     * @return L'ID de l'utilisateur
     */
    Object getId();

    /**
     * Obtient le nom d'utilisateur
     * @return Le nom d'utilisateur
     */
    String getUsername();

    /**
     * Obtient les rôles de l'utilisateur
     * @return L'ensemble des rôles de l'utilisateur
     */
    Set<String> getRoles();

    /**
     * Vérifie si l'utilisateur a un rôle spécifique
     * @param role Le rôle à vérifier
     * @return true si l'utilisateur a le rôle
     */
    default boolean hasRole(String role) {
        return getRoles().contains(role);
    }
} 