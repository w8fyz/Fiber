package sh.fyz.fiber.handler.crud;

import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.annotations.PathVariable;
import sh.fyz.fiber.core.ResponseEntity;

import java.util.List;

public class CrudController<T extends GenericRepository<X>, X extends IdentifiableEntity> {

    protected final T repository;

    public CrudController(T repository) {
        this.repository = repository;
    }

    public ResponseEntity<List<X>> getAll() {
        return ResponseEntity.ok(repository.all());
    }

    public ResponseEntity<X> getById(@PathVariable("id") Object id) {
        X entity = repository.findById(id);
        if (entity == null) {
            return ResponseEntity.notFound();
        }
        return ResponseEntity.ok(entity);
    }

    public ResponseEntity<X> create(X entity) {
        repository.save(entity);
        return ResponseEntity.created(entity);
    }

    public ResponseEntity<X> update(@PathVariable("id") Object id, X entity) {
        X existing = repository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound();
        }
        existing = repository.save(entity);
        return ResponseEntity.ok(existing);
    }

    public ResponseEntity<Void> delete(@PathVariable("id") Object id) {
        X existing = repository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound();
        }
        repository.delete(existing);
        return ResponseEntity.noContent();
    }
}
