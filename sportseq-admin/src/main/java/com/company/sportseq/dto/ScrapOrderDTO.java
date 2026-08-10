package com.company.sportseq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ScrapOrderDTO {

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "报废类型不能为空")
    private Integer scrapType;

    private String remark;

    @Valid
    @NotEmpty(message = "报废明细不能为空")
    private List<ScrapItemDTO> items;
}
