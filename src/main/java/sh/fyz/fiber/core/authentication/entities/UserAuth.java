package sh.fyz.fiber.core.authentication.entities;

/**
 * Interface defining basic user authentication information.
 * This interface can be extended by your User class to provide additional user-specific data.
 */
public interface UserAuth {
    /**
     * Get the unique identifier of the user
     * @return The user's ID
     */
    Object getId();

    /**
     * Get the username of the user
     * @return The user's username
     */
    String getUsername();

    /**
     * Get the role/permissions of the user
     * @return The user's role
     */
    String getRole();
} 