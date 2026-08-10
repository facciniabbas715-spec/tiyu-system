package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("return_item")
public class ReturnItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long returnId;
    private Long borrowItemId;
    private Long equipmentId;
    private Integer quantity;
    private Integer conditionStatus;
    private String damageDesc;
    private Integer isOverdue;
    private Integer overdueDays;
    private BigDecimal penaltyAmount;
    private String remark;
}
