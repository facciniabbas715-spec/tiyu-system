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
@TableName("scrap_order")
public class ScrapOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long warehouseId;
    private Integer scrapType;
    private Integer totalQuantity;
    private BigDecimal totalLossAmount;
    private Integer status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long disposeBy;
    private LocalDateTime disposeTime;
    private Integer disposeMethod;
    private String remark;
}
