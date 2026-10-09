package systems.porto.registry.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CategoryPageResponse(
    List<CategoryDto> items,
    int page,
    int size,
    long total
) {
}
