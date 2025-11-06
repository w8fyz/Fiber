package sh.fyz.fiber.example.repo;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.util.List;

public class ExampleUserRepository extends GenericRepository<ExampleUser> {

    public ExampleUserRepository() {
        super(ExampleUser.class);
    }

    public ExampleUser findByProviderIdAndExternalId(String providerId, String externalId) {
        List<ExampleUser> exampleUsers = all();
        return exampleUsers.stream()
                .filter(user -> providerId.equals(user.getProviderId()) && 
                              externalId.equals(user.getExternalId()))
                .findFirst()
                .orElse(null);
    }
}
