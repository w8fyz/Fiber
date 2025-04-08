package sh.fyz.fiber.example;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.annotations.IdentifierField;
import sh.fyz.fiber.core.authentication.annotations.PasswordField;
import sh.fyz.fiber.validation.Email;
import sh.fyz.fiber.validation.Min;
import sh.fyz.fiber.validation.NotBlank;

@Entity
@Table(name = "fiber_users")
public class User implements IdentifiableEntity, UserAuth {
    @NotBlank
    private String name;

    @Min(value = 0)
    private int age;

    @GeneratedValue
    @Id
    private long id;

    @NotBlank
    @Email
    private String email;

    @IdentifierField
    private String username;

    @PasswordField
    private String password;

    private String role;

    // OAuth2 specific fields
    private String providerId;
    private String externalId;
    private String avatar;
    private String discriminator;

    // Default constructor for Jackson
    public User() {}

    public User(String name, int age, String email, String role) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.role = role;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long setId(long id) { return this.id = id; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // OAuth2 getters and setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getDiscriminator() { return discriminator; }
    public void setDiscriminator(String discriminator) { this.discriminator = discriminator; }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}