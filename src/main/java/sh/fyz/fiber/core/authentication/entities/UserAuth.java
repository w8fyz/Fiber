package sh.fyz.fiber.core.authentication.entities;

/**
 * Interface defining basic authentication information for a user.
 * This interface can be extended by your User class to provide additional data.
 */
public interface UserAuth {
    /**
     * Get the unique identifier of the user
     * @return The user's ID
     */
    Object getId();

    /**
     * Get the user's role identifier
     * @return The role identifier
     */
    String getRole();
} 