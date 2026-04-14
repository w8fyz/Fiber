package sh.fyz.fiber.test;

import sh.fyz.fiber.annotations.dto.IgnoreDTO;
import sh.fyz.fiber.core.dto.DTOConvertible;

public class TestDtoObject extends DTOConvertible {
    private String name;
    private int age;

    @IgnoreDTO
    private String secret;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
