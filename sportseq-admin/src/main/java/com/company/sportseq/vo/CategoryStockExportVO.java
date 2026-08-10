package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 库存分布 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class CategoryStockExportVO {

    @ExcelProperty("器材分类")
    private String categoryName;

    @ExcelProperty("库存数量")
    private Long quantity;
}
