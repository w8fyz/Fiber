package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

public class ExampleImplAuthService extends AuthenticationService<ExampleUser> {

    public ExampleImplAuthService(GenericRepository<ExampleUser> userRepository) {
        super(userRepository, "/auth");
    }
}
