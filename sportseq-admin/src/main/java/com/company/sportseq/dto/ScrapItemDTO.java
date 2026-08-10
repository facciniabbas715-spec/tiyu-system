package com.company.sportseq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScrapItemDTO {

    @NotNull(message = "器材不能为空")
    private Long equipmentId;

    @NotNull(message = "报废数量不能为空")
    @Min(value = 1, message = "报废数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "报废原因不能为空")
    private String scrapReason;
}
