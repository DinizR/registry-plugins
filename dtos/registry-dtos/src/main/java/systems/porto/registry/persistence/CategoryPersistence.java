package systems.porto.registry.persistence;

import systems.porto.api.persistence.Persistence;
import systems.porto.registry.dto.CategoryDto;
import systems.porto.registry.dto.CreateCategoryRequest;
import systems.porto.registry.dto.UpdateCategoryRequest;

/**
 * Capability interface for category persistence client adapters (JDBC, REST, …).
 */
public interface CategoryPersistence
    extends Persistence<CategoryDto, Integer, CreateCategoryRequest, UpdateCategoryRequest> {

    boolean existsById(Integer id);
}
