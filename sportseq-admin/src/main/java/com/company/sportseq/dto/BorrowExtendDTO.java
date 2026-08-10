package com.company.sportseq.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowExtendDTO {

    @NotNull(message = "借用单ID不能为空")
    private Long orderId;

    @NotNull(message = "续借天数不能为空")
    @Min(value = 1, message = "续借天数至少1天")
    @Max(value = 90, message = "单次续借最多90天")
    private Integer days;
}
