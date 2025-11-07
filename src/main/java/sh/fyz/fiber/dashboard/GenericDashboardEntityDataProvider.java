package sh.fyz.fiber.dashboard;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.dashboard.crud.DashboardEntityDataProvider;
import sh.fyz.fiber.dashboard.crud.Page;

import java.util.Map;

public class GenericDashboardEntityDataProvider<T> extends DashboardEntityDataProvider<T> {

    private final GenericRepository<T> repository;

    public GenericDashboardEntityDataProvider(GenericRepository<T> repository) {
        super();
        this.repository = repository;
    }


    @Override
    public Page<T> list(int page, int size) {
        return null;
    }

    @Override
    public Page<T> search(String query, int page, int size) {
        return null;
    }

    @Override
    public T getById(Object id) {
        return repository.findById(id);
    }

    @Override
    public T create(T entity) {
        return null;
    }

    @Override
    public T update(Object id, Map<String, Object> data) {
        return null;
    }

    @Override
    public boolean delete(Object id) {
        try {
            repository.deleteWhere("id", id.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
