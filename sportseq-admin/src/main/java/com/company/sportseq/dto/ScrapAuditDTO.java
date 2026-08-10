package com.company.sportseq.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScrapAuditDTO {

    @NotNull(message = "报废单ID不能为空")
    private Long orderId;

    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    private String remark;
}
