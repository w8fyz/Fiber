package sh.fyz.fiber.example;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.fiber.core.UserAuth;
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

    private String role;

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

    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Override
    public String getUsername() {
        return name;
    }
}