package com.company.sportseq.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustDTO {

    @NotNull(message = "器材不能为空")
    private Long equipmentId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "调整数量不能为空")
    private Integer changeQuantity;

    private String remark;
}
