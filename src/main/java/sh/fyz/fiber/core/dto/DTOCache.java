package sh.fyz.fiber.core.dto;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DTOCache {

    protected static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

}
