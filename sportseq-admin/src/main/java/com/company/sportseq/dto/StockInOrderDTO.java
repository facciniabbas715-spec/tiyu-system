package com.company.sportseq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StockInOrderDTO {

    private Long id;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    private String supplier;

    @NotNull(message = "入库类型不能为空")
    private Integer inType;

    private String remark;

    @Valid
    @NotEmpty(message = "入库明细不能为空")
    private List<StockInItemDTO> items;
}
