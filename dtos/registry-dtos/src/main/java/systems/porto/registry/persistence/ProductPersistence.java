package systems.porto.registry.persistence;

import systems.porto.api.persistence.Persistence;
import systems.porto.registry.dto.CreateProductRequest;
import systems.porto.registry.dto.ProductDto;
import systems.porto.registry.dto.UpdateProductRequest;

/**
 * Capability interface for product persistence client adapters (JDBC, REST, …).
 */
public interface ProductPersistence
    extends Persistence<ProductDto, Integer, CreateProductRequest, UpdateProductRequest> {
}
