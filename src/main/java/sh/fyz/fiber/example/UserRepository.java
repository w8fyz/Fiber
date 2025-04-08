package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import java.util.List;

public class UserRepository extends GenericRepository<User> {

    public UserRepository() {
        super(User.class);
    }

    public User findByProviderIdAndExternalId(String providerId, String externalId) {
        List<User> users = all();
        return users.stream()
                .filter(user -> providerId.equals(user.getProviderId()) && 
                              externalId.equals(user.getExternalId()))
                .findFirst()
                .orElse(null);
    }
}
