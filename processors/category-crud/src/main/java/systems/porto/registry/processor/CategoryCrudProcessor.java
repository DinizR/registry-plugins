package systems.porto.registry.processor;

import lombok.Data;
import systems.porto.business.BusinessProcessor;
import systems.porto.business.config.Config;
import systems.porto.context.Context;
import systems.porto.registry.dto.CategoryDto;
import systems.porto.registry.dto.CategoryPageResponse;
import systems.porto.registry.dto.CreateCategoryRequest;
import systems.porto.registry.dto.UpdateCategoryRequest;
import systems.porto.registry.persistence.CategoryPersistence;
import systems.porto.api.spi.OperationLog;
import systems.porto.api.spi.RequestContext;
import systems.porto.api.spi.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

@Data
public class CategoryCrudProcessor implements BusinessProcessor<Context> {
    private static final String DB = "DB";

    private Config config;

    @Override
    public String id() {
        return "category-crud";
    }

    @Override
    public void process(final Context context) {
        if (!(context instanceof RequestContext requestContext)) {
            throw new IllegalStateException("Registry request context is required");
        }
        String operation = requestContext.getOperation();
        OperationLog.start(operation);
        try {
            CategoryPersistence persistence = requestContext.requireClientAdapter(DB, CategoryPersistence.class);
            Object response = switch (operation) {
                case "category-list" -> list(persistence, requestContext);
                case "category-read" -> read(persistence, requestContext);
                case "category-search" -> search(persistence, requestContext);
                case "category-create" -> create(persistence, requestContext);
                case "category-update" -> update(persistence, requestContext);
                case "category-delete" -> delete(persistence, requestContext);
                default -> throw new IllegalArgumentException("Unknown category operation: " + operation);
            };
            requestContext.setResponse(response);
            OperationLog.completed(operation);
        } catch (RuntimeException | Error ex) {
            OperationLog.failed(operation, ex);
            throw ex;
        }
    }

    private CategoryPageResponse list(final CategoryPersistence persistence, final RequestContext ctx) {
        int page = ctx.getIntQueryParam("page", 0);
        int size = ctx.getIntQueryParam("size", defaultPageSize());
        List<CategoryDto> items = persistence.list(page, size);
        return CategoryPageResponse.builder()
            .items(items)
            .page(page)
            .size(size)
            .total(persistence.count())
            .build();
    }

    private CategoryDto read(final CategoryPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Category id is required"));
        return persistence.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private List<CategoryDto> search(final CategoryPersistence persistence, final RequestContext ctx) {
        String q = ctx.getQueryParam("q").orElse("");
        return persistence.search(q);
    }

    private CategoryDto create(final CategoryPersistence persistence, final RequestContext ctx) {
        CreateCategoryRequest request = ctx.getRequestBody(CreateCategoryRequest.class);
        return persistence.create(request);
    }

    private CategoryDto update(final CategoryPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Category id is required"));
        UpdateCategoryRequest request = ctx.getRequestBody(UpdateCategoryRequest.class);
        return persistence.update(id, request)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private Map<String, String> delete(final CategoryPersistence persistence, final RequestContext ctx) {
        int id = ctx.getPathId()
            .orElseThrow(() -> new IllegalArgumentException("Category id is required"));
        if (!persistence.delete(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
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
