package sh.fyz.fiber.example;

import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.PathVariable;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.handler.crud.CrudController;

import java.util.List;

@Controller("/api/users")
public class UserCrudController extends CrudController<GenericRepository<User>, User> {
    
    public UserCrudController() {
        super(new GenericRepository<>(User.class));
    }

    @RequestMapping(value = "/", method = RequestMapping.Method.GET)
    @Override
    public ResponseEntity<List<User>> getAll() {
        return super.getAll();
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    @Override
    public ResponseEntity<User> getById(@PathVariable("id") Object id) {
        return super.getById(id);
    }

    @RequestMapping(method = RequestMapping.Method.POST)
    @Override
    public ResponseEntity<User> create(User entity) {
        return super.create(entity);
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.PUT)
    @Override
    public ResponseEntity<User> update(@PathVariable("id") Object id, User entity) {
        return super.update(id, entity);
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.DELETE)
    @Override
    public ResponseEntity<Void> delete(@PathVariable("id") Object id) {
        return super.delete(id);
    }
}
