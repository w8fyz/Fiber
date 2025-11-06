package sh.fyz.fiber.util;

import sh.fyz.fiber.annotations.dto.IgnoreDTO;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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



}
