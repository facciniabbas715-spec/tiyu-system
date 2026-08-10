package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 逾期明细 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class OverdueExportVO {

    @ExcelProperty("归还单号")
    private String returnOrderNo;

    @ExcelProperty("借用单号")
    private String borrowOrderNo;

    @ExcelProperty("器材编码")
    private String equipmentCode;

    @ExcelProperty("器材名称")
    private String equipmentName;

    @ExcelProperty("归还数量")
    private Integer quantity;

    @ExcelProperty("逾期天数")
    private Integer overdueDays;

    @ExcelProperty("违约金(元)")
    private BigDecimal penaltyAmount;

    @ExcelProperty("确认时间")
    private LocalDateTime confirmTime;
}
