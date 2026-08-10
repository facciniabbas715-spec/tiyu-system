package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment")
public class Equipment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String equipmentCode;
    private String equipmentName;
    private Long categoryId;
    private String brand;
    private String model;
    private String spec;
    private String unit;
    private BigDecimal purchasePrice;
    private Integer safeStock;
    private Integer maxBorrowDays;
    private String imageUrl;
    private Integer status;
    private String description;
}
