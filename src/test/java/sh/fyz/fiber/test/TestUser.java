package sh.fyz.fiber.test;

import jakarta.persistence.*;
import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.fiber.annotations.auth.IdentifierField;
import sh.fyz.fiber.annotations.auth.PasswordField;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.validation.Email;
import sh.fyz.fiber.validation.NotBlank;

@Entity
@Table(name = "fiber_test_users")
public class TestUser implements IdentifiableEntity, UserAuth {

    @Id
    @GeneratedValue
    private long id;

    @NotBlank
    @IdentifierField
    private String username;

    @Email
    private String email;

    @PasswordField
    private String password;

    @Column(name = "role")
    private String role;

    public TestUser() {}

    @Override public Object getId() { return id; }
    @Override public String getRole() { return role; }

    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}
