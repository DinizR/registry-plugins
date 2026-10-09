package systems.porto.registry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateCategoryRequest(
    @NotBlank String name,
    String imageURL,
    Boolean enabled,
    String barcodeDigit
) {
}
