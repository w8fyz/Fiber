package sh.fyz.fiber.example.repo;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;

public class ExampleOauth2ClientRepository extends GenericRepository<OAuth2Client> {
    public ExampleOauth2ClientRepository() {
        super(OAuth2Client.class);
    }
}
