package sh.fyz.fiber.util;

/**
 * Classe utilitaire pour la gestion du JSON.
 */
public class JsonUtil {
    private static final FiberObjectMapper objectMapper = new FiberObjectMapper();

    /**
     * Convertit un objet en chaîne JSON.
     *
     * @param value L'objet à convertir
     * @return La chaîne JSON
     * @throws Exception Si une erreur survient pendant la conversion
     */
    public static String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /**
     * Convertit une chaîne JSON en objet.
     *
     * @param json La chaîne JSON
     * @param type Le type de l'objet à créer
     * @return L'objet créé
     * @throws Exception Si une erreur survient pendant la conversion
     */
    public static <T> T fromJson(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }
} 