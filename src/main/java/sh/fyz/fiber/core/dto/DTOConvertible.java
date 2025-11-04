package sh.fyz.fiber.core.dto;

import sh.fyz.fiber.annotations.dto.IgnoreDTO;
import sh.fyz.fiber.util.JsonUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DTOConvertible {

    // Cache field metadata per class
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Optional: transform data before conversion to DTO
     */
    public void transform() {}

    /**
     * Cached reflection lookup for fields
     */
    private static List<Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> list = new ArrayList<>();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!f.isAnnotationPresent(IgnoreDTO.class)) {
                        f.setAccessible(true);
                        list.add(f);
                    }
                }
                c = c.getSuperclass();
            }
            return Collections.unmodifiableList(list);
        });
    }

    /**
     * Convert to a Map<String,Object> recursively
     */
    public Map<String, Object> asDTO() {
        transform();
        Map<String, Object> dto = new HashMap<>();

        for (Field field : getCachedFields(this.getClass())) {
            try {
                Object value = field.get(this);
                if (value == null) continue;

                if (value instanceof DTOConvertible convertible) {
                    dto.put(field.getName(), convertible.asDTO());
                } else if (value instanceof Collection<?> col) {
                    List<Object> list = new ArrayList<>(col.size());
                    for (Object o : col) {
                        if (o instanceof DTOConvertible inner)
                            list.add(inner.asDTO());
                        else
                            list.add(o);
                    }
                    dto.put(field.getName(), list);
                } else if (value.getClass().isArray()) {
                    int len = java.lang.reflect.Array.getLength(value);
                    List<Object> arr = new ArrayList<>(len);
                    for (int i = 0; i < len; i++) {
                        Object o = java.lang.reflect.Array.get(value, i);
                        if (o instanceof DTOConvertible inner)
                            arr.add(inner.asDTO());
                        else
                            arr.add(o);
                    }
                    dto.put(field.getName(), arr);
                } else if (value instanceof Map<?, ?> map) {
                    Map<Object, Object> newMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : map.entrySet()) {
                        Object val = e.getValue();
                        if (val instanceof DTOConvertible inner) {
                            newMap.put(e.getKey(), inner.asDTO());
                        } else {
                            newMap.put(e.getKey(), val);
                        }
                    }
                    dto.put(field.getName(), newMap);
                } else {
                    dto.put(field.getName(), value);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return dto;
    }
}