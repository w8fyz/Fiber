package sh.fyz.fiber.core.authentication.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import sh.fyz.fiber.core.log.FiberLog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login resolves a user with one {@code WHERE <identifier_column> = ?} query per
 * {@link sh.fyz.fiber.annotations.auth.IdentifierField}. That is only fast if the
 * column is indexed — otherwise every login attempt is a sequential scan, which is
 * an easy denial-of-service target on a large user table.
 *
 * <p>Fiber cannot add the index itself: the user entity belongs to the application
 * and schema DDL is Architect's business. So it inspects the JPA mapping at startup
 * and warns once per user class when an identifier column has no index or unique
 * constraint declared.
 *
 * <p>Detection is annotation-based, not a database round-trip — an index created by
 * hand or by a migration tool is invisible here and will still produce the warning.
 */
public final class IdentifierIndexCheck {

    private static final Set<Class<?>> WARNED = ConcurrentHashMap.newKeySet();

    private IdentifierIndexCheck() {
    }

    /**
     * Logs a WARN listing the identifier fields of {@code userClass} that have no
     * declared index. No-op on subsequent calls for the same class, and never throws
     * — a malformed or non-JPA user class must not break server startup.
     */
    public static void warnIfUnindexed(Class<?> userClass) {
        if (userClass == null || !WARNED.add(userClass)) {
            return;
        }
        try {
            List<String> missing = findUnindexedIdentifiers(userClass);
            if (missing.isEmpty()) {
                return;
            }
            FiberLog.get(IdentifierIndexCheck.class).warn(
                    "@IdentifierField(s) {} on {} have no declared index or unique constraint. "
                            + "Every login runs WHERE <column> = ? against them, which is a full table scan "
                            + "without an index. Fix with @Column(unique = true) on the field, or "
                            + "@Table(indexes = @Index(columnList = \"<column>\")) on the entity.",
                    missing, userClass.getName());
        } catch (Throwable t) {
            // Reflection over an unexpected entity shape, or jakarta.persistence
            // absent at runtime. Advisory only — never fail startup over it.
            FiberLog.handleSilent(t);
        }
    }

    /**
     * Identifier field names on {@code userClass} that are not covered by a
     * declared index, unique constraint, or primary key.
     */
    public static List<String> findUnindexedIdentifiers(Class<?> userClass) {
        Map<String, Field> identifiers = UserFieldUtil.findIdentifierFields(userClass);
        if (identifiers.isEmpty()) {
            return List.of();
        }
        Set<String> indexed = collectIndexedColumns(userClass);

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Field> entry : identifiers.entrySet()) {
            Field field = entry.getValue();
            if (field.isAnnotationPresent(Id.class)) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.unique()) {
                continue;
            }
            if (disjoint(indexed, columnAliases(field))) {
                missing.add(entry.getKey());
            }
        }
        return missing;
    }

    /**
     * Columns named by {@code @Table(indexes = ...)} or
     * {@code @Table(uniqueConstraints = ...)} anywhere in the class hierarchy,
     * normalized to lower case.
     */
    private static Set<String> collectIndexedColumns(Class<?> userClass) {
        Set<String> columns = new HashSet<>();
        Class<?> current = userClass;
        while (current != null && current != Object.class) {
            Table table = current.getAnnotation(Table.class);
            if (table != null) {
                for (Index index : table.indexes()) {
                    // columnList is a comma-separated list, each entry optionally
                    // suffixed with ASC/DESC. Only the leading column of a composite
                    // index can serve an equality lookup on its own.
                    String[] parts = index.columnList().split(",");
                    if (parts.length > 0) {
                        String leading = parts[0].trim().split("\\s+")[0];
                        if (!leading.isEmpty()) {
                            columns.add(normalize(leading));
                        }
                    }
                }
                for (UniqueConstraint constraint : table.uniqueConstraints()) {
                    String[] names = constraint.columnNames();
                    if (names.length > 0) {
                        columns.add(normalize(names[0]));
                    }
                }
            }
            current = current.getSuperclass();
        }
        return columns;
    }

    /**
     * Names the column of {@code field} may appear under. The entity declares a
     * Java field name; the index declares a physical column name, and Hibernate's
     * naming strategy may have converted camelCase to snake_case in between — so
     * accept either spelling rather than warn about an index that does exist.
     */
    private static Set<String> columnAliases(Field field) {
        Set<String> aliases = new HashSet<>();
        aliases.add(normalize(field.getName()));
        aliases.add(normalize(camelToSnake(field.getName())));
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            aliases.add(normalize(column.name()));
        }
        return aliases;
    }

    private static boolean disjoint(Set<String> indexed, Set<String> aliases) {
        for (String alias : aliases) {
            if (indexed.contains(alias)) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String name) {
        return name.trim().replace("\"", "").replace("`", "").toLowerCase(Locale.ROOT);
    }

    private static String camelToSnake(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
