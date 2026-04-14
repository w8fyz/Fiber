package sh.fyz.fiber.test;

import sh.fyz.fiber.validation.NotBlank;
import sh.fyz.fiber.validation.Email;

public class TestRequestBody {
    @NotBlank
    private String username;

    @Email
    private String email;

    public TestRequestBody() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
