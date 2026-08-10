package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("borrow_item")
public class BorrowItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long borrowId;
    private Long equipmentId;
    private Long warehouseId;
    private Integer quantity;
    private Integer issuedQuantity;
    private Integer returnedQuantity;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private Integer overdueFlag;
    private Integer overdueDays;
    private Integer status;
    private String remark;
}
