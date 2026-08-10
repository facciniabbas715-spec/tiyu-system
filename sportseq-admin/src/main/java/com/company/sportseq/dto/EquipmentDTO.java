package com.company.sportseq.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EquipmentDTO {

    private Long id;

    @NotBlank(message = "器材名称不能为空")
    @Size(max = 100, message = "器材名称长度不能超过100")
    private String equipmentName;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String brand;
    private String model;
    private String spec;

    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过20")
    private String unit;

    @DecimalMin(value = "0", message = "采购单价不能为负")
    private BigDecimal purchasePrice;

    @Min(value = 0, message = "安全库存不能为负")
    private Integer safeStock;

    @Min(value = 1, message = "最长借用天数至少为1")
    private Integer maxBorrowDays;

    private String imageUrl;
    private Integer status;
    private String description;
}
