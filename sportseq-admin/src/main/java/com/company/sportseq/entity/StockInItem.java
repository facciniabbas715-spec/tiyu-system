package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("stock_in_item")
public class StockInItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long equipmentId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
}
