package com.company.sportseq.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowAuditDTO {

    @NotNull(message = "借用单ID不能为空")
    private Long orderId;

    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    private String remark;
}
