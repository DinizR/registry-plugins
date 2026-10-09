package systems.porto.registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import systems.porto.api.dto.Dto;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDto(
    Integer id,
    String barcode,
    String description,
    BigDecimal unitPrice,
    BigDecimal discount,
    BigDecimal taxRate,
    String unit,
    String imageURL,
    Integer categoryId,
    Boolean enabled
) implements Dto<Integer> {
}
