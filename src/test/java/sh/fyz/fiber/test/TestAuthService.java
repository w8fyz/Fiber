package sh.fyz.fiber.test;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;

public class TestAuthService extends AuthenticationService<TestUser> {

    private final GenericRepository<TestUser> userRepository;

    public TestAuthService(GenericRepository<TestUser> userRepository) {
        super(userRepository, "/test-auth");
        this.userRepository = userRepository;
    }

    public void saveUser(TestUser user) {
        userRepository.save(user);
    }
}
