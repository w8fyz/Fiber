package sh.fyz.fiber.core.authentication.entities;

import sh.fyz.fiber.core.authentication.annotations.IdentifierField;
import sh.fyz.fiber.core.authentication.annotations.PasswordField;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFieldUtil {
    private static final Map<Class<?>, Field> passwordFields = new HashMap<>();
    private static final Map<Class<?>, Map<String, Field>> identifierFields = new HashMap<>();

    public static void validateUserClass(Class<?> userClass) {
        if (!UserAuth.class.isAssignableFrom(userClass)) {
            throw new IllegalArgumentException("User class must implement UserAuth interface");
        }

        // Find password field
        Field passwordField = findPasswordField(userClass);
        if (passwordField == null) {
            throw new IllegalArgumentException("User class must have a field annotated with @PasswordField");
        }
        passwordFields.put(userClass, passwordField);

        // Find identifier fields
        Map<String, Field> identifiers = findIdentifierFields(userClass);
        if (identifiers.isEmpty()) {
            throw new IllegalArgumentException("User class must have at least one field annotated with @IdentifierField");
        }
        identifierFields.put(userClass, identifiers);
    }

    private static Field findPasswordField(Class<?> userClass) {
        for (Field field : userClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PasswordField.class)) {
                if (!field.getType().equals(String.class)) {
                    throw new IllegalArgumentException("Password field must be of type String");
                }
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static Map<String, Field> findIdentifierFields(Class<?> userClass) {
        Map<String, Field> identifiers = new HashMap<>();
        for (Field field : userClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(IdentifierField.class)) {
                if (!field.getType().equals(String.class)) {
                    throw new IllegalArgumentException("Identifier field must be of type String");
                }
                field.setAccessible(true);
                identifiers.put(field.getName(), field);
            }
        }
        return identifiers;
    }

    public static String getPassword(UserAuth user) {
        try {
            Field passwordField = passwordFields.get(user.getClass());
            if (passwordField == null) {
                validateUserClass(user.getClass());
                passwordField = passwordFields.get(user.getClass());
            }
            return (String) passwordField.get(user);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get password", e);
        }
    }

    public static Map<String, String> getIdentifiers(UserAuth user) {
        try {
            Map<String, Field> fields = identifierFields.get(user.getClass());
            if (fields == null) {
                validateUserClass(user.getClass());
                fields = identifierFields.get(user.getClass());
            }

            Map<String, String> identifiers = new HashMap<>();
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                identifiers.put(entry.getKey(), (String) entry.getValue().get(user));
            }
            return identifiers;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get identifiers", e);
        }
    }

    public static boolean validatePassword(UserAuth user, String password) {
        return getPassword(user).equals(password);
    }

    public static UserAuth findUserByIdentifier(String identifier, List<? extends UserAuth> users) {
        for (UserAuth user : users) {
            Map<String, String> userIdentifiers = getIdentifiers(user);
            for (String identifierValue : userIdentifiers.values()) {
                if (identifierValue.equals(identifier)) {
                    return user;
                }
            }
        }
        return null;
    }
}