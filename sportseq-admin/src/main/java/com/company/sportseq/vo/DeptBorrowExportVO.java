package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 部门借用统计 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class DeptBorrowExportVO {

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("借用单数")
    private Long borrowCount;

    @ExcelProperty("借用数量")
    private Long borrowQuantity;

    @ExcelProperty("在借数量")
    private Long outstandingQuantity;
}
