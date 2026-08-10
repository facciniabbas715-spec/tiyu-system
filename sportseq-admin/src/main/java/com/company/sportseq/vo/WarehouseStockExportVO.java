package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 各仓库库存量 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class WarehouseStockExportVO {

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("库存数量")
    private Long quantity;
}
