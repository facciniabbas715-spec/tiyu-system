package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 借用趋势 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class BorrowTrendExportVO {

    @ExcelProperty("月份")
    private String month;

    @ExcelProperty("借用数量")
    private Long borrowQuantity;

    @ExcelProperty("归还数量")
    private Long returnQuantity;
}
