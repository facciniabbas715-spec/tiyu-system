package com.company.sportseq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowItemDTO {

    @NotNull(message = "器材不能为空")
    private Long equipmentId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "借用数量不能为空")
    @Min(value = 1, message = "借用数量必须大于0")
    private Integer quantity;
}
