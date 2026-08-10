package com.company.sportseq.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockInAuditDTO {

    @NotNull(message = "入库单ID不能为空")
    private Long orderId;

    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    private String remark;
}
