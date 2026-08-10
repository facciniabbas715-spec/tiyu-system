package com.company.sportseq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnItemDTO {

    @NotNull(message = "借用明细不能为空")
    private Long borrowItemId;

    @NotNull(message = "归还数量不能为空")
    @Min(value = 1, message = "归还数量必须大于0")
    private Integer quantity;

    @NotNull(message = "器材状况不能为空")
    private Integer conditionStatus;

    private String damageDesc;
    private BigDecimal penaltyAmount;
}
