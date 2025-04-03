package sh.fyz.fiber.handler.crud;

import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.annotations.PathVariable;
import sh.fyz.fiber.annotations.RequestBody;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;

import java.util.List;

public abstract class CrudController<T extends GenericRepository<X>, X extends IdentifiableEntity> {

    protected final T repository;

    public CrudController(T repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/", method = RequestMapping.Method.GET)
    abstract public ResponseEntity<List<X>> getAll();

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    public ResponseEntity<X> getById(@PathVariable("id") Object id) {
        X entity = repository.findById(id);
        if (entity == null) {
            return ResponseEntity.notFound();
        }
        return ResponseEntity.ok(entity);
    }

    @RequestMapping(method = RequestMapping.Method.POST)
    public ResponseEntity<X> create(@RequestBody X entity) {
        repository.save(entity);
        return ResponseEntity.created(entity);
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.PUT)
    public ResponseEntity<X> update(@PathVariable("id") Object id, @RequestBody X entity) {
        X existing = repository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound();
        }
        existing = repository.save(entity);
        return ResponseEntity.ok(existing);
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.DELETE)
    public ResponseEntity<Void> delete(@PathVariable("id") Object id) {
        X existing = repository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound();
        }
        repository.delete(existing);
        return ResponseEntity.noContent();
    }
}
