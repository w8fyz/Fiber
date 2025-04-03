package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.AuthenticationService;

public class ImplAuthService extends AuthenticationService<User> {

    public ImplAuthService(GenericRepository<User> userRepository) {
        super(userRepository);
    }
}
