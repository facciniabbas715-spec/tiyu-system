package com.company.sportseq.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 器材列表 Excel 导出模型。
 */
@Data
@AllArgsConstructor
public class EquipmentExportVO {

    @ExcelProperty("器材编码")
    private String equipmentCode;

    @ExcelProperty("器材名称")
    private String equipmentName;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("计量单位")
    private String unit;

    @ExcelProperty("采购单价")
    private BigDecimal purchasePrice;

    @ExcelProperty("安全库存")
    private Integer safeStock;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
