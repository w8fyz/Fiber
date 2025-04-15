package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.example.repo.entities.User;

public class ImplAuthService extends AuthenticationService<User> {

    public ImplAuthService(GenericRepository<User> userRepository) {
        super(userRepository, "/auth");
    }
}
