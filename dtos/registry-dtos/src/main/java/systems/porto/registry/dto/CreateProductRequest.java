package systems.porto.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateProductRequest(
    String barcode,
    @NotBlank String description,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    BigDecimal discount,
    BigDecimal taxRate,
    @NotBlank String unit,
    String imageURL,
    @NotNull Integer categoryId,
    Boolean enabled
) {
}
