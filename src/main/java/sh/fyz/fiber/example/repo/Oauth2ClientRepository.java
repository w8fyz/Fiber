package sh.fyz.fiber.example.repo;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;

public class Oauth2ClientRepository extends GenericRepository<OAuth2Client> {
    public Oauth2ClientRepository() {
        super(OAuth2Client.class);
    }
}
