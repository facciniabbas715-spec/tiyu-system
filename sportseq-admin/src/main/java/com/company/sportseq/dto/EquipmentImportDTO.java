package com.company.sportseq.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 器材 Excel 导入模型，列头与模板一致。
 */
@Data
public class EquipmentImportDTO {

    @ExcelProperty("器材名称")
    private String equipmentName;

    @ExcelProperty("分类编码")
    private String categoryCode;

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

    @ExcelProperty("最长借用天数")
    private Integer maxBorrowDays;

    @ExcelProperty("描述")
    private String description;
}
