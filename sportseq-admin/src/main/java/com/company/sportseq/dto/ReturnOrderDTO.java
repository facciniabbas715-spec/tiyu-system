package com.company.sportseq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReturnOrderDTO {

    @NotNull(message = "借用单不能为空")
    private Long borrowOrderId;

    private String remark;

    @Valid
    @NotEmpty(message = "归还明细不能为空")
    private List<ReturnItemDTO> items;
}
