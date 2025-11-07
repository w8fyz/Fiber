package sh.fyz.fiber.util;

import sh.fyz.fiber.annotations.dto.IgnoreDTO;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {

    protected static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static List<Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> list = new ArrayList<>();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    f.setAccessible(true);
                    list.add(f);
                }
                c = c.getSuperclass();
            }
            return Collections.unmodifiableList(list);
        });
    }


    public static Class<Object> getGenericListType(Field f) {
        if (List.class.isAssignableFrom(f.getType())) {
            Type genericType = f.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType listType = (ParameterizedType) genericType;
                Type elementType = listType.getActualTypeArguments()[0];

                if (elementType instanceof Class<?>) {
                    return (Class<Object>) elementType; // e.g. String.class
                }
            }
        }
        return Object.class; // default / unknown
    }
}
