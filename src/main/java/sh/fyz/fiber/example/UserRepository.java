package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;

public class UserRepository extends GenericRepository<User> {

    public UserRepository() {
        super(User.class);
    }
}
