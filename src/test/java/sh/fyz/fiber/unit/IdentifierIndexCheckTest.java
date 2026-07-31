package sh.fyz.fiber.unit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.annotations.auth.IdentifierField;
import sh.fyz.fiber.annotations.auth.PasswordField;
import sh.fyz.fiber.core.authentication.entities.IdentifierIndexCheck;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * findByIdentifier() queries one column per @IdentifierField, so an unindexed
 * identifier column turns every login into a sequential scan. These cover the
 * startup advisor that flags that mapping mistake.
 */
class IdentifierIndexCheckTest {

    @Test
    void flagsIdentifierWithNoIndex() {
        assertEquals(List.of("username"),
                IdentifierIndexCheck.findUnindexedIdentifiers(BareUser.class));
    }

    @Test
    void acceptsUniqueColumn() {
        assertTrue(IdentifierIndexCheck.findUnindexedIdentifiers(UniqueColumnUser.class).isEmpty());
    }

    @Test
    void acceptsTableIndex() {
        assertTrue(IdentifierIndexCheck.findUnindexedIdentifiers(IndexedUser.class).isEmpty());
    }

    @Test
    void acceptsUniqueConstraint() {
        assertTrue(IdentifierIndexCheck.findUnindexedIdentifiers(ConstrainedUser.class).isEmpty());
    }

    /** The index names the physical column; the field is camelCase. Same column. */
    @Test
    void matchesSnakeCaseColumnAgainstCamelCaseField() {
        assertTrue(IdentifierIndexCheck.findUnindexedIdentifiers(SnakeCaseIndexUser.class).isEmpty());
    }

    /** A @Column(name=...) rename must be resolved before matching the index. */
    @Test
    void matchesRenamedColumn() {
        assertTrue(IdentifierIndexCheck.findUnindexedIdentifiers(RenamedColumnUser.class).isEmpty());
    }

    /**
     * Only the leading column of a composite index can serve an equality lookup
     * on its own, so a trailing identifier is still unindexed for our purposes.
     */
    @Test
    void flagsNonLeadingColumnOfCompositeIndex() {
        assertEquals(List.of("username"),
                IdentifierIndexCheck.findUnindexedIdentifiers(CompositeIndexUser.class));
    }

    @Test
    void reportsEveryUnindexedIdentifier() {
        List<String> missing = IdentifierIndexCheck.findUnindexedIdentifiers(TwoIdentifierUser.class);
        assertEquals(1, missing.size(), "email is indexed, username is not: " + missing);
        assertEquals("username", missing.get(0));
    }

    @Test
    void warnIfUnindexedNeverThrowsOnNonEntity() {
        assertDoesNotThrow(() -> IdentifierIndexCheck.warnIfUnindexed(String.class));
        assertDoesNotThrow(() -> IdentifierIndexCheck.warnIfUnindexed(null));
    }

    // --- fixtures ---------------------------------------------------------

    private static abstract class BaseUser implements UserAuth {
        @Id
        protected long id;
        @PasswordField
        protected String password;

        @Override public Object getId() { return id; }
        @Override public String getRole() { return "USER"; }
    }

    @Entity
    @Table(name = "bare_users")
    private static class BareUser extends BaseUser {
        @IdentifierField
        private String username;
    }

    @Entity
    @Table(name = "unique_column_users")
    private static class UniqueColumnUser extends BaseUser {
        @IdentifierField
        @Column(unique = true)
        private String username;
    }

    @Entity
    @Table(name = "indexed_users", indexes = @Index(columnList = "username"))
    private static class IndexedUser extends BaseUser {
        @IdentifierField
        private String username;
    }

    @Entity
    @Table(name = "constrained_users",
            uniqueConstraints = @UniqueConstraint(columnNames = "username"))
    private static class ConstrainedUser extends BaseUser {
        @IdentifierField
        private String username;
    }

    @Entity
    @Table(name = "snake_users", indexes = @Index(columnList = "login_name DESC"))
    private static class SnakeCaseIndexUser extends BaseUser {
        @IdentifierField
        private String loginName;
    }

    @Entity
    @Table(name = "renamed_users", indexes = @Index(columnList = "user_email"))
    private static class RenamedColumnUser extends BaseUser {
        @IdentifierField
        @Column(name = "user_email")
        private String email;
    }

    @Entity
    @Table(name = "composite_users", indexes = @Index(columnList = "tenant, username"))
    private static class CompositeIndexUser extends BaseUser {
        @IdentifierField
        private String username;
        private String tenant;
    }

    @Entity
    @Table(name = "two_identifier_users", indexes = @Index(columnList = "email"))
    private static class TwoIdentifierUser extends BaseUser {
        @IdentifierField
        private String username;
        @IdentifierField
        private String email;
    }
}
