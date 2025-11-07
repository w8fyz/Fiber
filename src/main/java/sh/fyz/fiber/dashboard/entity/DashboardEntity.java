package sh.fyz.fiber.dashboard.entity;

import jakarta.persistence.Id;
import sh.fyz.architect.entities.IdentifiableEntity;
import sh.fyz.fiber.dashboard.crud.CrudCapability;
import sh.fyz.fiber.dashboard.crud.DashboardEntityDataProvider;
import sh.fyz.fiber.util.ReflectionUtil;
import sh.fyz.fiber.validation.Email;
import sh.fyz.fiber.validation.Min;
import sh.fyz.fiber.validation.NotBlank;
import sh.fyz.fiber.validation.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DashboardEntity<T extends IdentifiableEntity> {

    private final Class<T> entityClass;
    private final String name;
    private final List<DashboardEntityField> fields;
    private final Set<CrudCapability> capabilities;
    private DashboardEntityDataProvider<T> dataProvider;

    public DashboardEntity(Class<T> clazz, String name) {
        this.entityClass = clazz;
        this.name =  clazz.getSimpleName();
        this.fields = new ArrayList<>();
        this.capabilities = EnumSet.noneOf(CrudCapability.class);
        for (Field f : ReflectionUtil.getFields(clazz)) {
            DashboardEntityField def = new DashboardEntityField(
                f.getName(),
                name,
                f.getType().getSimpleName()
            );
            if (f.isAnnotationPresent(NotNull.class) || f.isAnnotationPresent(NotBlank.class)) {
                def.set("required", true);
            }
            Min min = f.getAnnotation(Min.class);
            if (min != null) {
                def.set("min", min.value());
            }
            if (f.isAnnotationPresent(Email.class)) {
                def.setType("email");
            }
            if(f.isAnnotationPresent(Id.class)) {
                def.set("isId", true);
            }
            if(def.getType().equals("list")) {
                def.set("itemType", ReflectionUtil.getGenericListType(f).getSimpleName());
            }
            this.fields.add(def);
        }
    }

    public DashboardEntity<T> withCapabilities(CrudCapability... caps) {
        Collections.addAll(this.capabilities, caps);
        return this;
    }

    public DashboardEntity<T> withDataProvider(DashboardEntityDataProvider<T> provider) {
        this.dataProvider = provider;
        return this;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public String getName() {
        return name;
    }

    public List<DashboardEntityField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public Set<CrudCapability> getCapabilities() { return Collections.unmodifiableSet(capabilities); }

    public DashboardEntityDataProvider<T> getDataProvider() { return dataProvider; }
}
