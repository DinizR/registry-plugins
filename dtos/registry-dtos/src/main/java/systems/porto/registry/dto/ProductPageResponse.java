package systems.porto.registry.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductPageResponse(
    List<ProductDto> items,
    int page,
    int size,
    long total
) {
}
