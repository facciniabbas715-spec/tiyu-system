package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 器材使用率排行 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class EquipmentUsageExportVO {

    @ExcelProperty("排名")
    private Integer rank;

    @ExcelProperty("器材编码")
    private String equipmentCode;

    @ExcelProperty("器材名称")
    private String equipmentName;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("借用次数")
    private Long borrowCount;
}
