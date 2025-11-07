package sh.fyz.fiber.dashboard.crud;

import sh.fyz.fiber.example.repo.ExampleUserRepository;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.util.List;
import java.util.Map;

public abstract class DashboardEntityDataProvider<T> {

    public abstract Page<T> list(int page, int size);

    public abstract Page<T> search(String query, int page, int size);

    public abstract T getById(Object id);

    public abstract T create(T data);

    public abstract T update(Object id, Map<String, Object> data);

    public abstract boolean delete(Object id);
}
