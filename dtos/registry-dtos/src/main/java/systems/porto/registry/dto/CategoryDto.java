package systems.porto.registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import systems.porto.api.dto.Dto;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryDto(
    Integer id,
    String name,
    String imageURL,
    Boolean enabled,
    String barcodeDigit
) implements Dto<Integer> {
}
