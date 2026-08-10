package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("return_order")
public class ReturnOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long borrowOrderId;
    private Long userId;
    private Long warehouseId;
    private Integer totalQuantity;
    private Integer returnType;
    private Integer status;
    private Long confirmBy;
    private LocalDateTime confirmTime;
    private String confirmRemark;
    private String remark;
}
