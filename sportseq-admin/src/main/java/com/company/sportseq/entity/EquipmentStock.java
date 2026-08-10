package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("equipment_stock")
public class EquipmentStock {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long equipmentId;
    private Long warehouseId;
    private Integer quantity;
    private Integer lockedQuantity;
    @Version
    private Integer version;
    private Long updateBy;
    private LocalDateTime updateTime;
}
