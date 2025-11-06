package sh.fyz.fiber.dashboard.crud;

import java.util.List;
import java.util.Map;

public interface DashboardEntityDataProvider<T> {

    Page<T> list(int page, int size);

    Page<T> search(String query, int page, int size);

    T getById(Object id);

    T create(Map<String, Object> data);

    T update(Object id, Map<String, Object> data);

    boolean delete(Object id);
}
