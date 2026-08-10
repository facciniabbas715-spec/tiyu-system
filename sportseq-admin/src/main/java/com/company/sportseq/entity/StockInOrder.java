package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_in_order")
public class StockInOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String supplier;
    private Integer inType;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private Integer status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long receiveBy;
    private LocalDateTime receiveTime;
    private String remark;
}
