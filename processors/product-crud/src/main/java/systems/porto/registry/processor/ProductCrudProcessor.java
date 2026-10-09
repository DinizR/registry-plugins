package systems.porto.registry.processor;

import lombok.Data;
import systems.porto.business.BusinessProcessor;
import systems.porto.business.config.Config;
import systems.porto.context.Context;
import systems.porto.registry.dto.CreateProductRequest;
import systems.porto.registry.dto.ProductDto;
import systems.porto.registry.dto.ProductPageResponse;
import systems.porto.registry.dto.UpdateProductRequest;
import systems.porto.registry.persistence.ProductPersistence;
import systems.porto.api.spi.OperationLog;
import systems.porto.api.spi.RequestContext;
import systems.porto.api.spi.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

@Data
public class ProductCrudProcessor implements BusinessProcessor<Context> {
    private static final String DB = "DB";

    private Config config;

    @Override
    public String id() {
        return "product-crud";
    }

    @Override
    public void process(final Context context) {
        if (!(context instanceof RequestContext requestContext)) {
            throw new IllegalStateException("Registry request context is required");
        }
        String operation = requestContext.getOperation();
        OperationLog.start(operation);
        try {
            ProductPersistence persistence = requestContext.requireClientAdapter(DB, ProductPersistence.class);
            Object response = switch (operation) {
                case "product-list" -> list(persistence, requestContext);
                case "product-read" -> read(persistence, requestContext);
                case "product-search" -> search(persistence, requestContext);
                case "product-create" -> create(persistence, requestContext);
                case "product-update" -> update(persistence, requestContext);
                case "product-delete" -> delete(persistence, requestContext);
                default -> throw new IllegalArgumentException("Unknown product operation: " + operation);
            };
            requestContext.setResponse(response);
            OperationLog.completed(operation);
        } catch (RuntimeException | Error ex) {
            OperationLog.failed(operation, ex);
            throw ex;
        }
    }

    private ProductPageResponse list(final ProductPersistence persistence, final RequestContext ctx) {
        int page = ctx.getIntQueryParam("page", 0);
        int size = ctx.getIntQueryParam("size", defaultPageSize());
        List<ProductDto> items = persistence.list(page, size);
        return ProductPageResponse.builder()
            .items(items)
            .page(page)
            .size(size)
            .total(persistence.count())
            .build();
    }

    private ProductDto read(final ProductPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Product id is required"));
        return persistence.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private List<ProductDto> search(final ProductPersistence persistence, final RequestContext ctx) {
        String q = ctx.getQueryParam("q").orElse("");
        return persistence.search(q);
    }

    private ProductDto create(final ProductPersistence persistence, final RequestContext ctx) {
        CreateProductRequest request = ctx.getRequestBody(CreateProductRequest.class);
        return persistence.create(request);
    }

    private ProductDto update(final ProductPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Product id is required"));
        UpdateProductRequest request = ctx.getRequestBody(UpdateProductRequest.class);
        return persistence.update(id, request)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Map<String, String> delete(final ProductPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Product id is required"));
        if (!persistence.delete(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return Map.of("status", "deleted", "id", String.valueOf(id));
    }

    private int defaultPageSize() {
        String value = config != null ? config.getPropertyByKey("page.default.size") : null;
        if (value == null || value.isBlank()) {
            return 8;
        }
        return Integer.parseInt(value);
    }

}
