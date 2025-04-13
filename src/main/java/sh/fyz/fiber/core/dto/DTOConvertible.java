package sh.fyz.fiber.core.dto;

import sh.fyz.fiber.annotations.dto.IgnoreDTO;
import sh.fyz.fiber.util.JsonUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DTOConvertible {


    /**
     * Transform data before convesion to JSON-compatible Map
     */
    public void transform() {}

    /**
     * Get all fields from the class and its parent classes
     * @param clazz The class to get fields from
     * @return List of all fields in the inheritance chain
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Convert the entity to a JSON-compatible Map
     * @return Map containing field names and their values
     */
    public Map<String, Object> asDTO() {
        transform();
        Map<String, Object> dto = new HashMap<>();
        List<Field> fields = getAllFields(this.getClass());

        for (Field field : fields) {
            // Skip fields marked with @IgnoreDTO
            if (field.isAnnotationPresent(IgnoreDTO.class)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(this);
                if (value != null) {
                    // Try to serialize the value to JSON to verify it's JSON-serializable
                    try {
                        JsonUtil.toJson(value);
                        dto.put(field.getName(), value);
                    } catch (Exception e) {
                        // Skip this field if JSON serialization fails
                        continue;
                    }
                }
            } catch (IllegalAccessException e) {
                // Log error or handle appropriately
                e.printStackTrace();
            }
        }

        return dto;
    }
}