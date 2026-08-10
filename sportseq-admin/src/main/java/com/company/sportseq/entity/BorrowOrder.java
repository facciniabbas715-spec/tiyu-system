package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("borrow_order")
public class BorrowOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long deptId;
    private Integer borrowType;
    private String purpose;
    private LocalDate expectedReturnDate;
    private Integer totalQuantity;
    private Integer status;
    private Integer locked;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long issueBy;
    private LocalDateTime issueTime;
    private Integer extendCount;
    private String remark;
}
