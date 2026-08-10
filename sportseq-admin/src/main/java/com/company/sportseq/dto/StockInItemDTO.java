package com.company.sportseq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockInItemDTO {

    @NotNull(message = "器材不能为空")
    private Long equipmentId;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    private Integer quantity;

    @DecimalMin(value = "0", message = "单价不能为负")
    private BigDecimal unitPrice;
    private String remark;
}
