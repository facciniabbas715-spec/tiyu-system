package com.company.sportseq.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScrapDisposeDTO {

    @NotNull(message = "报废单ID不能为空")
    private Long orderId;

    @NotNull(message = "处置方式不能为空")
    private Integer disposeMethod;

    private String remark;
}
